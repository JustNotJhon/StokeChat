# StokeChat

StokeChat is a server-side Minecraft mod that forwards normal in-game chat to a Discord channel through a Discord webhook. It can also announce server startup and player joins and departures.

## Features

- Forwards normal player chat to Discord.
- Announces when the dedicated server starts or stops and when players join or leave.
- Lets an operator configure the webhook from the game chat or server console.
- Saves the webhook locally and reloads it when the server starts.
- Sends messages asynchronously so Discord requests do not block the server thread.

## Requirements

- Minecraft 1.6.4
- Legacy Fabric with Fabric Loader
- Java 8-compatible mod bytecode
- A Discord webhook for the channel where messages should appear

Install the mod JAR in the server's `mods` directory. To use a full webhook URL from the in-game chat, install the same JAR on the client as well; the client mixin increases the chat input limit so the URL can be entered.

## Configure a webhook

As an operator, run this command in the game chat or the server console:

```text
/webhook https://discord.com/api/webhooks/WEBHOOK_ID/WEBHOOK_TOKEN
```

`/webook` is also accepted as an alias. The command validates the URL, saves it to `config/stokechat.conf`, and activates it immediately. On startup, StokeChat reads that file relative to the server's working directory. If the webhook is configured after the server starts, StokeChat sends the pending startup announcement after saving it.

You can also create `config/stokechat.conf` manually and put the complete webhook URL on a single line. Restart the server after editing the file so the new value is loaded.

## Keep your webhook private

A webhook URL contains a secret token. Do not commit `config/stokechat.conf`, post the URL in public chat, or include it in shared logs or screenshots. If the URL is exposed, revoke that webhook in Discord and create a replacement.

StokeChat does not print the webhook URL or token in its logs. Error response bodies are truncated and sanitized before logging.

## Build from source

Use the Gradle wrapper included in this repository:

```sh
./gradlew build
```

The installable JAR is written to `build/libs/`. The `-sources.jar` file contains source code and is not the mod JAR to install.

To generate Minecraft sources:

```sh
./gradlew genSources
```
