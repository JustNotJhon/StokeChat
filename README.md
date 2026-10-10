# StokeChat

A server-side Minecraft 1.6.4 Legacy Fabric mod that sends chat, player joins and leaves, deaths, achievements, and server start/stop notices to Discord.

## Install and configure

1. Put the mod JAR in the server's `mods/` folder.
2. Run `/webhook <full Discord webhook URL>` in-game as an operator (permission level 2), or enter the same command in the server console. `/webook` is also accepted. The URL is saved and activated immediately.
3. Alternatively, put the full URL on one line in `config/stokechat.conf` under the server's working directory, then restart the server.

Installing the JAR on the client is optional. It increases the in-game chat input limit so you can enter a full webhook URL there.

Keep the webhook URL private: it contains a secret token. Do not commit or share it. Revoke and replace the webhook if it is exposed.

## Build

Run from the repository root:

```sh
./gradlew build       # macOS/Linux
gradlew.bat build     # Windows
```

The installable JAR is created in `build/libs/`.
"go live your life gng"