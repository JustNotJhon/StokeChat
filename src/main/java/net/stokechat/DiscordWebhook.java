package net.stokechat;

import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordWebhook {
    private static final Path CONFIG_FILE = Paths.get("config", "stokechat.conf");
    private static final Path[] LEGACY_CONFIG_FILES = {
        Paths.get("config", "mc2discord.conf"),
        Paths.get("config", "mc2discord.txt")
    };
    private static final Pattern WEBHOOK_PATH = Pattern.compile("^/api/webhooks/([0-9]+)/([^/]+)$");
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "stokechat-sender");
        t.setDaemon(true);
        return t;
    });

    private static volatile String webhookUrl;
    private static final Object LIFECYCLE_LOCK = new Object();
    private static boolean serverStarted;
    private static boolean startupAnnouncementPending;
    private static boolean stopAnnouncementSent;

    private DiscordWebhook() {}

    public static boolean load() throws IOException {
        Path absoluteConfigFile = CONFIG_FILE.toAbsolutePath().normalize();
        boolean exists = Files.exists(CONFIG_FILE);
        if (!exists) {
            for (Path legacyConfigFile : LEGACY_CONFIG_FILES) {
                if (Files.exists(legacyConfigFile)) {
                    System.out.println("[StokeChat] Found legacy config at "
                        + legacyConfigFile.toAbsolutePath().normalize() + "; migrating it.");
                    return loadConfig(legacyConfigFile, true);
                }
            }
            System.err.println("[StokeChat] Config file is missing at " + absoluteConfigFile
                + "; no legacy config file was found.");
            webhookUrl = null;
            return false;
        }

        return loadConfig(CONFIG_FILE, false);
    }

    private static boolean loadConfig(Path configFile, boolean migrate) throws IOException {
        String url = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
        if (!url.isEmpty() && url.charAt(0) == '\uFEFF') {
            url = url.substring(1);
        }
        url = url.trim();
        if (url.isEmpty()) {
            System.err.println("[StokeChat] Config is empty after trimming whitespace/BOM; no webhook was loaded.");
            webhookUrl = null;
            return false;
        }

        WebhookDetails details = inspectWebhookUrl(url);
        if (details == null) {
            System.err.println("[StokeChat] Webhook was not loaded: " + invalidWebhookReason(url) + ".");
            webhookUrl = null;
            return false;
        }

        webhookUrl = url;
        if (migrate) {
            try {
                saveConfig(url);
                System.out.println("[StokeChat] Legacy config migrated to "
                    + CONFIG_FILE.toAbsolutePath().normalize() + ".");
            } catch (IOException e) {
                System.err.println("[StokeChat] Could not migrate legacy config ("
                    + e.getClass().getSimpleName() + "); the legacy file was left untouched.");
            }
        }
        return true;
    }

    public static boolean setWebhook(String url) throws IOException {
        String value = url.trim();
        if (value.startsWith("\uFEFF")) {
            value = value.substring(1).trim();
        }
        WebhookDetails details = inspectWebhookUrl(value);
        if (details == null) return false;

        saveConfig(value);
        webhookUrl = value;
        System.out.println("[StokeChat] Webhook configured.");
        announcePendingStartup();
        return true;
    }

    private static void saveConfig(String value) throws IOException {
        Path directory = CONFIG_FILE.getParent();
        Files.createDirectories(directory);
        Path temporaryFile = Files.createTempFile(directory, "stokechat-", ".tmp");
        try {
            try {
                Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                );
                Files.setPosixFilePermissions(temporaryFile, permissions);
            } catch (UnsupportedOperationException e) {
                System.err.println("[StokeChat] Filesystem does not support POSIX permissions for the config file.");
            }

            Files.write(temporaryFile, (value + "\n").getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporaryFile, CONFIG_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                System.err.println("[StokeChat] Atomic file moves are unavailable; saving the config non-atomically.");
                Files.move(temporaryFile, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static WebhookDetails inspectWebhookUrl(String value) {
        if (value == null || value.isEmpty() || invalidWebhookReason(value) != null) return null;

        try {
            URL url = new URL(value);
            String host = url.getHost();
            Matcher matcher = WEBHOOK_PATH.matcher(url.getPath());
            if (!matcher.matches()) return null;
            return new WebhookDetails(matcher.group(1), matcher.group(2));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static String invalidWebhookReason(String value) {
        try {
            URL url = new URL(value);
            String host = url.getHost();
            if (!"https".equalsIgnoreCase(url.getProtocol())) return "protocol must be HTTPS";
            if (!("discord.com".equalsIgnoreCase(host) || "discordapp.com".equalsIgnoreCase(host))) {
                return "host must be discord.com or discordapp.com";
            }
            if (url.getPort() != -1 && url.getPort() != 443) return "port must be the default HTTPS port";
            if (url.getUserInfo() != null) return "URL must not contain user credentials";
            if (url.getRef() != null) return "URL must not contain a fragment";
            if (url.getQuery() != null) return "URL must not contain query parameters";
            if (!WEBHOOK_PATH.matcher(url.getPath()).matches()) return "path must match /api/webhooks/<ID>/<TOKEN>";
            return null;
        } catch (MalformedURLException e) {
            return "malformed URL";
        }
    }

    public static void send(String player, String message) {
        enqueue("**" + player + "**: " + message, "chat from " + player, message.length());
    }

    public static void announceServerStarted() {
        synchronized (LIFECYCLE_LOCK) {
            if (serverStarted) return;
            serverStarted = true;
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                startupAnnouncementPending = true;
                return;
            }
            enqueue("🟢 Minecraft server started.", "server startup", 0);
        }
    }

    public static void announceServerStopped() {
        synchronized (LIFECYCLE_LOCK) {
            if (!serverStarted || stopAnnouncementSent) return;
            stopAnnouncementSent = true;
        }

        Future<?> send = enqueue("🔴 Minecraft server stopped.", "server shutdown", 0);
        if (send == null) return;

        try {
            send.get(11, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.err.println("[StokeChat] Timed out waiting for the Discord shutdown announcement.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[StokeChat] Interrupted while waiting for the Discord shutdown announcement.");
        } catch (Exception e) {
            System.err.println("[StokeChat] Could not complete the Discord shutdown announcement ("
                + e.getClass().getSimpleName() + ").");
        }
    }

    public static void announcePlayerJoined(String player) {
        enqueue("➡️ **" + player + "** joined the server.", "join from " + player, 0);
    }

    public static void announcePlayerLeft(String player) {
        enqueue("⬅️ **" + player + "** left the server.", "departure of " + player, 0);
    }

    public static void announcePlayerDeath(String player, String deathMessage) {
        enqueue("💀 " + deathMessage, "death of " + player, 0);
    }

    public static void announceAchievement(String player, String achievement) {
        String name = displayAchievementName(achievement);
        enqueue("🏆 **" + player + "** earned **" + name + "**.", "achievement earned by " + player, 0);
    }

    private static String displayAchievementName(String achievement) {
        if (!achievement.startsWith("achievement.")) return achievement;

        String name = achievement.substring("achievement.".length());
        int namespaceSeparator = name.indexOf('.');
        if (namespaceSeparator >= 0) name = name.substring(namespaceSeparator + 1);
        name = name.replaceAll("([a-z])([A-Z])", "$1 $2")
            .replace('_', ' ')
            .toLowerCase(Locale.ROOT)
            .trim();

        StringBuilder title = new StringBuilder(name.length());
        boolean capitalize = true;
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (Character.isWhitespace(character)) {
                capitalize = true;
                title.append(character);
            } else {
                title.append(capitalize ? Character.toUpperCase(character) : character);
                capitalize = false;
            }
        }
        return title.toString();
    }

    private static Future<?> enqueue(String content, String description, int messageLength) {
        final String url = webhookUrl;
        if (url == null || url.isEmpty()) return null;

        if (content.length() > 1900) content = content.substring(0, 1900);

        final String json = "{\"content\":\"" + escape(content) + "\",\"allowed_mentions\":{\"parse\":[]}}";
        return EXECUTOR.submit(() -> post(url, json, description, messageLength));
    }

    private static void announcePendingStartup() {
        synchronized (LIFECYCLE_LOCK) {
            if (!serverStarted || !startupAnnouncementPending) return;
            startupAnnouncementPending = false;
            enqueue("🟢 Minecraft server started.", "server startup", 0);
        }
    }

    private static void post(String url, String json, String description, int messageLength) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "StokeChat/1.0");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                String responseBody = readErrorBody(conn);
                System.err.println("[StokeChat] Discord send failed: HTTP " + code
                    + "; response=" + sanitizeResponseBody(responseBody, url) + ".");
            }
        } catch (Exception e) {
            System.err.println("[StokeChat] Exception while sending to Discord ("
                + e.getClass().getSimpleName() + "); URL and exception message omitted.");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static boolean handleWebhookCommand(net.minecraft.command.CommandSource source, String input) {
        String command = input.trim();
        if (command.startsWith("/")) command = command.substring(1);
        String[] parts = command.split("\\s+", 2);
        if (parts.length == 0
            || (!"webhook".equalsIgnoreCase(parts[0]) && !"webook".equalsIgnoreCase(parts[0]))) return false;

        if (!source.canUseCommand(2, "webhook")) {
            source.method_5505(net.minecraft.text.ChatMessage.createTextMessage(
                "You do not have permission to configure the webhook."));
            return true;
        }

        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            source.method_5505(net.minecraft.text.ChatMessage.createTextMessage(
                "Usage: /webhook <full Discord webhook URL>"));
            return true;
        }

        try {
            String value = parts[1].trim();
            if (!value.startsWith("https://")) {
                String[] credentials = value.split("\\s+", 2);
                if (credentials.length == 2) {
                    value = "https://discord.com/api/webhooks/" + credentials[0] + "/" + credentials[1];
                }
            }
            if (!setWebhook(value)) {
                source.method_5505(net.minecraft.text.ChatMessage.createTextMessage(
                    "Invalid URL. Use a complete HTTPS Discord webhook URL."));
                return true;
            }
            source.method_5505(net.minecraft.text.ChatMessage.createTextMessage(
                "Discord webhook configured and saved."));
        } catch (IOException e) {
            System.err.println("[StokeChat] Failed to save config/stokechat.conf ("
                + e.getClass().getSimpleName() + ").");
            source.method_5505(net.minecraft.text.ChatMessage.createTextMessage(
                "Could not save the webhook configuration."));
        }
        return true;
    }

    private static String readErrorBody(HttpURLConnection conn) {
        try {
            java.io.InputStream stream = conn.getErrorStream();
            if (stream == null) return "";
            try (java.io.InputStreamReader reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)) {
                StringBuilder body = new StringBuilder(200);
                char[] chars = new char[200];
                int count;
                while (body.length() < 200 && (count = reader.read(chars, 0, Math.min(chars.length, 200 - body.length()))) != -1) {
                    body.append(chars, 0, count);
                }
                return body.toString();
            }
        } catch (IOException e) {
            return "[response body unavailable: " + e.getClass().getSimpleName() + "]";
        }
    }

    private static String sanitizeResponseBody(String body, String url) {
        String sanitized = body.replace(url, "[webhook URL redacted]");
        WebhookDetails details = inspectWebhookUrl(url);
        if (details != null) {
            sanitized = sanitized.replace(details.token, "[token redacted]");
            for (int prefixLength = Math.min(12, details.token.length()); prefixLength >= 4; prefixLength--) {
                sanitized = sanitized.replace(details.token.substring(0, prefixLength), "[token redacted]");
            }
        }
        sanitized = sanitized.replaceAll(
            "(?i)https?://(?:discord\\.com|discordapp\\.com)/api/webhooks/[0-9]+/[^\\s\"<>]*",
            "[webhook URL redacted]"
        );
        sanitized = sanitized.replace('\n', ' ').replace('\r', ' ');
        return sanitized.length() <= 200 ? sanitized : sanitized.substring(0, 200);
    }

    private static final class WebhookDetails {
        private final String id;
        private final String token;

        private WebhookDetails(String id, String token) {
            this.id = id;
            this.token = token;
        }
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}