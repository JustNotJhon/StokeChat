# StokeChat

A server-side Minecraft 1.6.4 Legacy Fabric mod that bridges Minecraft chat and server events with Discord.

## Features

- Forwards normal player chat, joins, leaves, deaths, achievements, and server start/stop notices to Discord through a webhook.
- Relays messages from one configured Discord channel into Minecraft through the Discord Gateway.
- Replaces Discord links with a short notice and colors sender names to match their highest colored Discord role.
- Sends Discord requests asynchronously and keeps routine success logs out of the server console.

## Requirements

- Minecraft 1.6.4, Legacy Fabric, and Java 8-compatible mod bytecode.
- A Discord webhook for Minecraft-to-Discord messages.
- A Discord bot with access to the selected channel, View Channel and Read Message History permissions, and Message Content Intent enabled.

Put the mod JAR in the server's `mods/` folder. Installing it on the client is optional; the client mixin only increases the chat input limit for entering webhook URLs in-game.

## Configure Minecraft-to-Discord

As an operator (permission level 2), run this in game chat or the server console:

```text
/webhook https://discord.com/api/webhooks/WEBHOOK_ID/WEBHOOK_TOKEN
```

`/webook` is also accepted. The command validates and saves the URL, then activates it immediately. Alternatively, put the full URL on one line in `config/stokechat.conf` under the Fabric config directory. On Debian, this is normally `~/BTW/config/stokechat.conf`. At startup the mod loads the webhook before sending server announcements. If that file is missing, it checks the previous working-directory path and legacy `mc2discord.conf` and `mc2discord.txt` files, migrating a found URL.

The webhook URL contains a secret. Never commit or share it, enter it in public chat, or include it in screenshots or logs. Revoke and replace it if exposed. StokeChat does not print webhook URLs or tokens; Discord send failures are logged with sensitive values redacted.

## Configure Discord-to-Minecraft

Enter these commands in the local dedicated-server console only, not in game chat, RCON, or the shell prompt:

```text
stokechat token YOUR_DISCORD_BOT_TOKEN
stokechat channel YOUR_DISCORD_CHANNEL_ID
```

Values are saved to `config/stokechat-discord.conf` with owner-only permissions where supported. Setting either value reconnects the Gateway once both are configured. The terminal may echo the token as you type; do not share screenshots or logs containing it. Environment variables `STOKECHAT_DISCORD_BOT_TOKEN` and `STOKECHAT_DISCORD_CHANNEL_ID` are also supported and override the corresponding file values.

### Discord Developer Portal setup

1. Open the [Discord Developer Portal](https://discord.com/developers/applications), create an application, then open its **Bot** page and add a bot if one is not already attached.
2. On **Bot**, enable **Message Content Intent** under **Privileged Gateway Intents** and save the change. This is required for the bot to read message text.
3. Open **OAuth2 → URL Generator**. Under **Scopes**, select only `bot`. Do not select `messages.read`, `email`, or other user-install scopes.
4. Under **Integration Type**, choose **Guild Install**. If it is unavailable, enable Guild Install in the application's **Installation → Installation Contexts** settings.
5. Under **Bot Permissions**, select **View Channels** and **Read Message History**. Administrator and Send Messages are not required for this relay; Minecraft-to-Discord continues to use the separate webhook.
6. Copy the generated URL, open it, choose your Discord server, and authorize the bot. Confirm that the bot appears in the server member list and can view the channel you intend to bridge.

Enable Developer Mode in the Discord client, then use the channel context menu's **Copy Channel ID** action. Enter that ID with `stokechat channel <channel-id>` in the local Minecraft server console. Configure the bot token there with `stokechat token <bot-token>`; never paste it into chat, the README, or a shared screenshot. Messages from other channels, bots, and webhooks are ignored. Discord links appear as `[Discord] name sent a link.` without forwarding the URL. The bot connection is outbound; StokeChat opens no inbound ports.

## Build

Run the Gradle wrapper from the repository root:

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. The installable JAR is created in `build/libs/` with a SemVer version such as `0.0.0-alpha.<year>.<month>.<day>.<build>`. The current build is `0.0.0-alpha.2026.10.11.3`. The `-sources.jar` is not the installable mod JAR.