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

Install the StokeChat JAR on the dedicated server and start it once. Configure the bot in the Discord Developer Portal:

1. Create an application at the [Discord Developer Portal](https://discord.com/developers/applications). Open **Bot**, add a bot if needed, and copy its token.
2. On the **Bot** page, enable **Message Content Intent** under **Privileged Gateway Intents**.
3. Open **OAuth2 → URL Generator** and select the `bot` scope. Set **Integration Type** to **Guild Install**.
4. Under **Bot Permissions**, select **View Channels** and **Read Message History**. No Administrator, email, or `messages.read` scope is needed.
5. Open the generated invite URL, choose your Discord server, and authorize the bot. Make sure it can see the channel you want to connect.
6. In Discord, enable **Developer Mode**, then right-click the channel and select **Copy Channel ID**.

With the Minecraft server running, enter these commands in its local console. Do not enter them in game chat, RCON, or the Linux shell:

```text
stokechat token YOUR_BOT_TOKEN
stokechat channel YOUR_CHANNEL_ID
```

StokeChat saves these values in `config/stokechat-discord.conf` under the Fabric config directory (normally `~/BTW/config/` on Debian) and connects when both are set. The token is kept in that server-side config; treat it as a password and do not post it publicly. To replace either value, run the matching command again in the local server console.

Send a test message from a human account in the selected Discord channel. It should appear in Minecraft as `[Discord] name: message`. Messages from other channels, bots, and webhooks are ignored; links appear as `[Discord] name sent a link.` without forwarding the URL. This connection is outbound and requires no open inbound port. Minecraft-to-Discord uses the separate webhook configured above.

## Build

Run the Gradle wrapper from the repository root:

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. The installable JAR is created in `build/libs/` with a SemVer version such as `0.0.0-alpha.<year>.<month>.<day>.<build>`. The current build is `0.0.0-alpha.2026.10.11.3`. The `-sources.jar` is not the installable mod JAR.