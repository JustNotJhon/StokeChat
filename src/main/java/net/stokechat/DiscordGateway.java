package net.stokechat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardOpenOption;
import java.awt.Color;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.nio.file.attribute.PosixFilePermission;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.Console;
import net.minecraft.text.ChatMessage;
import net.minecraft.util.Tickable;

public final class DiscordGateway {
    private static final String BOT_TOKEN_ENV = "STOKECHAT_DISCORD_BOT_TOKEN";
    private static final String CHANNEL_ID_ENV = "STOKECHAT_DISCORD_CHANNEL_ID";
    private static final Path CONFIG_FILE = Paths.get("config", "stokechat-discord.conf");
    private static final BlockingQueue<String> PENDING_MESSAGES = new ArrayBlockingQueue<>(100);
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "(?i)(?:https?://|www\\.|discord\\.gg/|discord(?:app)?\\.com/invite/)\\S+"
    );
    private static final int[] MINECRAFT_COLORS = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
        0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
        0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };
    private static final char[] MINECRAFT_COLOR_CODES = "0123456789abcdef".toCharArray();
    private static volatile JDA client;
    private static volatile boolean active;
    private static volatile MinecraftServer server;
    private static boolean tickableRegistered;

    private DiscordGateway() {}

    public static synchronized void start(MinecraftServer server) {
        DiscordGateway.server = server;
        if (!tickableRegistered) {
            server.addTickable(new Tickable() {
                @Override
                public void tick() {
                    if (!active) return;
                    for (int count = 0; count < 10; count++) {
                        String message = PENDING_MESSAGES.poll();
                        if (message == null) break;
                        server.getPlayerManager().method_6061(ChatMessage.createTextMessage(message));
                    }
                }
            });
            tickableRegistered = true;
        }
        connect();
    }

    private static synchronized void connect() {
        if (client != null || server == null) return;

        Properties config;
        try {
            config = loadConfig();
        } catch (IOException e) {
            System.err.println("[StokeChat] Could not read Discord Gateway config ("
                + e.getClass().getSimpleName() + ").");
            return;
        }

        String token = getConfiguredValue(BOT_TOKEN_ENV, "bot-token", config);
        String channelId = getConfiguredValue(CHANNEL_ID_ENV, "channel-id", config);
        if (token == null || token.trim().isEmpty() || channelId == null || !channelId.matches("[0-9]{1,20}")) {
            System.out.println("[StokeChat] Discord-to-Minecraft is disabled: add bot-token and channel-id to "
                + CONFIG_FILE.toAbsolutePath().normalize() + ".");
            return;
        }

        PENDING_MESSAGES.clear();
        try {
            Long.parseLong(channelId);
            JDA startedClient = JDABuilder.createLight(token, EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
            )).setAutoReconnect(true).addEventListeners(new ListenerAdapter() {
                @Override
                public void onReady(ReadyEvent event) {
                    System.out.println("[StokeChat] Discord Gateway connected.");
                }

                @Override
                public void onSessionDisconnect(SessionDisconnectEvent event) {
                    if (active) System.err.println("[StokeChat] Discord Gateway disconnected; reconnecting.");
                }

                @Override
                public void onMessageReceived(MessageReceivedEvent event) {
                    String receivedChannelId = event.getChannel().getId();
                    boolean authorIsBot = event.getAuthor().isBot();
                    boolean webhookMessage = event.isWebhookMessage();
                    if (!channelId.equals(receivedChannelId) || authorIsBot || webhookMessage) return;

                    String rawContent = event.getMessage().getContentRaw();
                    boolean containsLink = LINK_PATTERN.matcher(rawContent).find();
                    String content = containsLink ? "" : clean(event.getMessage().getContentDisplay());
                    if (!containsLink && content.isEmpty()) return;

                    Member member = event.getMember();
                    String username = clean(member == null ? event.getAuthor().getName() : member.getEffectiveName());
                    if (username.isEmpty()) username = "Discord user";
                    String message = "\u00a77[Discord] \u00a7r" + roleColor(member) + username + "\u00a7r"
                        + (containsLink ? " sent a link." : ": " + content);
                    if (message.length() > 256) message = message.substring(0, 256);
                    if (!PENDING_MESSAGES.offer(message)) {
                        System.err.println("[StokeChat] Discord-to-Minecraft queue is full; message dropped.");
                    }
                }
            }).build();

            active = true;
            client = startedClient;
        } catch (Exception e) {
            active = false;
            PENDING_MESSAGES.clear();
            System.err.println("[StokeChat] Could not start Discord Gateway ("
                + e.getClass().getSimpleName() + "). Token omitted.");
        }
    }

    public static synchronized void stop() {
        active = false;
        PENDING_MESSAGES.clear();
        JDA currentClient = client;
        client = null;
        if (currentClient != null) {
            currentClient.shutdownNow();
        }
        server = null;
        tickableRegistered = false;
    }

    public static boolean handleConsoleCommand(CommandSource source, String input) {
        String command = input.trim();
        if (command.startsWith("/")) command = command.substring(1);
        String[] parts = command.split("\\s+", 3);
        if (parts.length == 0 || !"stokechat".equalsIgnoreCase(parts[0])) return false;

        if (source != Console.INSTANCE) {
            source.method_5505(ChatMessage.createTextMessage("StokeChat config commands are console-only."));
            return true;
        }
        if (parts.length != 3 || !("token".equalsIgnoreCase(parts[1]) || "channel".equalsIgnoreCase(parts[1]))) {
            System.out.println("[StokeChat] Console usage: stokechat token <bot-token> | stokechat channel <channel-id>");
            return true;
        }

        String key = "token".equalsIgnoreCase(parts[1]) ? "bot-token" : "channel-id";
        String value = parts[2].trim();
        if (value.isEmpty() || ("channel-id".equals(key) && !value.matches("[0-9]{1,20}"))) {
            System.out.println("[StokeChat] Invalid " + ("bot-token".equals(key) ? "bot token" : "channel ID") + ".");
            return true;
        }

        try {
            if ("channel-id".equals(key)) Long.parseLong(value);
            Properties config = loadConfig();
            config.setProperty(key, value);
            saveConfig(config);
            System.out.println("[StokeChat] " + ("bot-token".equals(key) ? "Bot token" : "Discord channel ID")
                + " saved.");
            reconnect();
        } catch (IOException | NumberFormatException e) {
            System.err.println("[StokeChat] Could not save Discord bot config ("
                + e.getClass().getSimpleName() + ").");
        }
        return true;
    }

    private static synchronized void reconnect() {
        active = false;
        PENDING_MESSAGES.clear();
        JDA oldClient = client;
        client = null;
        if (oldClient != null) oldClient.shutdownNow();
        connect();
    }

    private static String roleColor(Member member) {
        Color color = member == null ? null : member.getColor();
        if (color == null) return "\u00a7f";

        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int closestIndex = 0;
        long closestDistance = Long.MAX_VALUE;
        for (int index = 0; index < MINECRAFT_COLORS.length; index++) {
            int candidate = MINECRAFT_COLORS[index];
            int redDifference = red - ((candidate >> 16) & 0xFF);
            int greenDifference = green - ((candidate >> 8) & 0xFF);
            int blueDifference = blue - (candidate & 0xFF);
            long distance = redDifference * redDifference + greenDifference * greenDifference
                + blueDifference * blueDifference;
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = index;
            }
        }
        return "\u00a7" + MINECRAFT_COLOR_CODES[closestIndex];
    }

    private static String clean(String value) {
        return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').replace('\u00a7', '?').trim();
    }

    private static Properties loadConfig() throws IOException {
        Path configFile = CONFIG_FILE.toAbsolutePath().normalize();
        Files.createDirectories(configFile.getParent());
        if (!Files.exists(configFile)) {
            byte[] template = ("# Discord bot credentials for StokeChat. Keep this file private.\n"
                + "bot-token=\n"
                + "channel-id=\n").getBytes(StandardCharsets.UTF_8);
            try {
                Files.write(configFile, template, StandardOpenOption.CREATE_NEW);
            } catch (FileAlreadyExistsException e) {
                // Another server startup created the template first.
            }
        }

        try {
            Set<PosixFilePermission> permissions = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(configFile, permissions);
        } catch (UnsupportedOperationException e) {
            System.err.println("[StokeChat] This filesystem cannot restrict Discord config permissions automatically.");
        }

        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static void saveConfig(Properties properties) throws IOException {
        Path configFile = CONFIG_FILE.toAbsolutePath().normalize();
        Files.createDirectories(configFile.getParent());
        Path temporaryFile = Files.createTempFile(configFile.getParent(), "stokechat-discord-", ".tmp");
        try {
            try {
                Files.setPosixFilePermissions(temporaryFile, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                ));
            } catch (UnsupportedOperationException e) {
                System.err.println("[StokeChat] This filesystem cannot restrict Discord config permissions automatically.");
            }

            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, "StokeChat Discord bot configuration");
            }
            try {
                Files.move(temporaryFile, configFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String getConfiguredValue(String environmentVariable, String property, Properties config) {
        String value = System.getenv(environmentVariable);
        if (value == null || value.trim().isEmpty()) value = config.getProperty(property);
        return value == null ? null : value.trim();
    }
}