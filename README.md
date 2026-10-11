# StokeChat

StokeChat is a server-side Minecraft mod that bridges in-game chat and server events with Discord. It forwards Minecraft chat through a Discord webhook and can relay messages from one Discord channel into Minecraft.

## Features

- Forwards normal player chat to Discord.
- Announces when the dedicated server starts or stops and when players join or leave.
- Lets an operator configure the outgoing webhook from the game chat or server console.
- Saves the webhook locally and reloads it when the server starts.
- Sends messages asynchronously so Discord requests do not block the server thread.
- Relays messages from one configured Discord channel into Minecraft through the Discord Gateway.

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

`/webook` is also accepted as an alias. The command validates the URL, saves it to `config/stokechat.conf`, and activates it immediately. On startup, StokeChat reads that file relative to the server's working directory. If `config/stokechat.conf` is missing but an older `config/mc2discord.conf` or `config/mc2discord.txt` exists, StokeChat loads the old file and attempts to migrate it automatically. If the webhook is configured after the server starts, StokeChat sends the pending startup announcement after saving it.

You can also create `config/stokechat.conf` manually and put the complete webhook URL on a single line. Restart the server after editing the file so the new value is loaded.

## Keep your webhook private

A webhook URL contains a secret token. Do not commit `config/stokechat.conf`, post the URL in public chat, or include it in shared logs or screenshots. If the URL is exposed, revoke that webhook in Discord and create a replacement.

StokeChat does not print the webhook URL or token in its logs. Error response bodies are truncated and sanitized before logging.

## Configure Discord-to-Minecraft

Configure the Discord bot from the dedicated server console, not in game chat. Enter these commands directly in the Minecraft server console, not at the shell prompt:

```text
stokechat token YOUR_DISCORD_BOT_TOKEN
stokechat channel YOUR_DISCORD_CHANNEL_ID
```

These commands are console-only and are not available to operators in game. Values are saved to `config/stokechat-discord.conf` with owner-only permissions where supported. Setting either value reconnects the Gateway when both values are available. The channel ID can be copied from Discord with Developer Mode enabled. Messages from other channels, bots, and webhooks are ignored. Discord links are replaced with a short notice, and the sender name uses the closest Minecraft chat color to their highest colored Discord role. No inbound port is opened; the mod makes an outbound Gateway connection.

The token is never printed in StokeChat logs, but the server terminal echoes typed commands; avoid screenshots or logs that show the token. Alternatively, set `STOKECHAT_DISCORD_BOT_TOKEN` and `STOKECHAT_DISCORD_CHANNEL_ID` in the server process environment; environment values override the matching file values. The bot must have access to the selected channel, and Message Content Intent must be enabled in the Discord Developer Portal.

Discord send successes are not printed to the server console; failures are still logged. Achievements are forwarded with their translated name when available and a readable fallback for untranslated keys.

## Build from source

Use the Gradle wrapper included in this repository:

```sh
./gradlew build
```

The installable JAR is written to `build/libs/` with a SemVer version such as `0.0.0-alpha.<year>.<month>.<day>.<build>`. The `-sources.jar` file contains source code and is not the mod JAR to install.

To generate Minecraft sources:

```sh
./gradlew genSources
```

## Project details

The Fabric mod ID, Java packages, mixin configuration, project name, and built artifact are all named **StokeChat**. Existing `mc2discord.conf` and `mc2discord.txt` files are recognized and migrated on startup.
