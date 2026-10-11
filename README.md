# StokeChat

StokeChat is a server-side Minecraft mod that bridges in-game chat and server events with Discord. It forwards Minecraft chat through a Discord webhook and can relay messages from one Discord channel into Minecraft.

## Features

- Forwards normal player chat to Discord.
- Announces when the dedicated server starts or stops, when players join or leave, and when they die or earn achievements.
- Lets an operator configure the outgoing webhook from game chat or the server console.
- Saves Discord credentials locally and reloads them at dedicated-server startup.
- Sends messages asynchronously so Discord requests do not block the server thread.
- Relays messages from one configured Discord channel into Minecraft through the Discord Gateway.
- Replaces Discord links with a short notice and colors sender names to match their highest colored Discord role.
- Keeps routine message and HTTP success logs out of the server console.

## Requirements

- Minecraft 1.6.4
- Legacy Fabric with Fabric Loader
- Java 8-compatible mod bytecode
- A Discord webhook for the channel where messages should appear
- A Discord bot with View Channel and Read Message History permissions and the Message Content Intent enabled

Install the mod JAR in the server's `mods` directory. To use a full webhook URL from the in-game chat, install the same JAR on the client as well; the client mixin increases the chat input limit so the URL can be entered.

## Configure a webhook

As an operator, run this command in the game chat or the server console:

```text
/webhook https://discord.com/api/webhooks/WEBHOOK_ID/WEBHOOK_TOKEN
```

`/webook` is also accepted as an alias. The command validates the URL, saves it to the Fabric config directory as `stokechat.conf`, and activates it immediately. On Debian this is normally `~/BTW/config/stokechat.conf`. At startup, StokeChat loads it before sending server announcements. If the file is missing there, it checks the previous working-directory path and older `mc2discord.conf` or `mc2discord.txt` files, migrating a found URL into the Fabric config directory.

You can also create the file manually and put the complete webhook URL on a single line. Restart the server after editing it. Prefer the server console over game chat when entering the URL, because command input may be visible in chat history or logs.

## Keep your webhook private

A webhook URL contains a secret token. Do not commit `config/stokechat.conf`, post the URL in public chat, or include it in shared logs or screenshots. If the URL is exposed, revoke that webhook in Discord and create a replacement.

StokeChat does not print the webhook URL or token in its logs. Error response bodies are truncated and sanitized before logging.

## Configure Discord-to-Minecraft

Configure the Discord bot from the local dedicated-server console, not from game chat, RCON, or the shell prompt:

```text
stokechat token YOUR_DISCORD_BOT_TOKEN
stokechat channel YOUR_DISCORD_CHANNEL_ID
```

These commands are accepted only by the local server console and are not available to operators in game or through RCON. Values are saved to `config/stokechat-discord.conf` in the Fabric config directory, with owner-only permissions where supported. Setting either value reconnects the Gateway once both values are available. The channel ID can be copied from Discord with Developer Mode enabled. Messages from other channels, bots, and webhooks are ignored. Discord links are replaced by `[Discord] name sent a link.` without forwarding the URL. Sender names use the closest Minecraft chat color to their highest colored Discord role. No inbound port is opened; the mod makes an outbound Gateway connection.

The token is never printed by StokeChat, but the server terminal may echo typed commands; do not share screenshots or logs containing the token. Alternatively, set `STOKECHAT_DISCORD_BOT_TOKEN` and `STOKECHAT_DISCORD_CHANNEL_ID` in the server process environment; environment values override matching file values. The bot needs access to the selected channel and the **Message Content Intent** enabled in the Discord Developer Portal. For Gateway relay, invite it with the `bot` OAuth scope and channel viewing/read-history permissions. The outgoing webhook is configured separately.

Discord send successes are not printed to the server console; failures are still logged. Achievements are forwarded with their translated name when available and a readable fallback for untranslated keys.

## Build from source

Use the Gradle wrapper included in this repository:

```sh
./gradlew build
```

The installable JAR is written to `build/libs/` with a SemVer version such as `0.0.0-alpha.<year>.<month>.<day>.<build>`. The current build is `0.0.0-alpha.2026.10.11.3`. The `-sources.jar` file contains source code and is not the mod JAR to install.

To generate Minecraft sources:

```sh
./gradlew genSources
```

## Project details

The Fabric mod ID, Java packages, mixin configuration, project name, and built artifact are all named **StokeChat**. Existing `mc2discord.conf` and `mc2discord.txt` files are recognized and migrated on startup.
