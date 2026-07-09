# DiscordWhitelist

A Paper plugin that watches a Discord channel — anyone who posts their Minecraft
username there gets auto-whitelisted, as long as it's a real Mojang account.

## How it works

1. You designate a Discord channel (e.g. `#whitelist`).
2. A player posts their exact Minecraft username as a message, nothing else.
3. The bot checks the name is valid and belongs to a real Mojang account.
4. If valid, it runs the equivalent of `/whitelist add <name>` and replies confirming success.
5. By default, the bot deletes the processed message and only leaves its reply, to keep the channel tidy.
6. By default, each Discord account can only whitelist one Minecraft account (tracked in `discord-links.yml`), to stop people from spamming whitelist adds.

## Setup

### 1. Create the Discord bot
1. Go to https://discord.com/developers/applications → New Application.
2. Go to the **Bot** tab → Add Bot.
3. Under **Privileged Gateway Intents**, enable **MESSAGE CONTENT INTENT**. This is required — the bot can't read message text without it.
4. Copy the bot token (Reset Token if needed).
5. Under **OAuth2 → URL Generator**, select scope `bot`, and permissions: **View Channels**, **Send Messages**, **Manage Messages** (needed if you want the bot to delete processed messages). Use the generated URL to invite the bot to your server.

### 2. Get your channel ID
Enable Developer Mode in Discord (User Settings → Advanced → Developer Mode), then right-click your whitelist channel → Copy Channel ID.

### 3. Build the plugin
This project uses Maven. From the project root:

```bash
mvn clean package
```

The built jar will be at `target/discord-whitelist.jar`. (Requires Java 21 and Maven installed locally — I couldn't build it for you directly since this environment has no network access to pull dependencies.)

### 4. Install
1. Drop `discord-whitelist.jar` into your server's `plugins/` folder.
2. Start the server once so it generates `plugins/DiscordWhitelist/config.yml`, then stop it (or just edit the config and use `/discordwhitelist reload`).
3. Edit `plugins/DiscordWhitelist/config.yml`:
   - `discord.bot-token` — your bot token
   - `discord.whitelist-channel-id` — the channel ID from step 2
4. Make sure whitelisting is actually turned on on your server (`/whitelist on`).
5. Run `/discordwhitelist reload` (or restart the server) to connect the bot.

## Config reference (`config.yml`)

| Key | Description |
|---|---|
| `discord.bot-token` | Your Discord bot token |
| `discord.whitelist-channel-id` | The channel to watch |
| `delete-message-after-processing` | Delete the user's message after handling it (default `true`) |
| `one-account-per-discord-user` | Restrict to one whitelist per Discord account (default `true`) |
| `messages.*` | Customize the bot's reply text. `%username%` and `%input%` are placeholders |

## Notes / things worth knowing

- **Case sensitivity**: Minecraft usernames aren't case-sensitive, but the plugin normalizes to the correctly-cased name from Mojang before whitelisting.
- **Cracked/offline-mode servers**: this plugin validates against Mojang accounts. If your server runs in offline mode with fake/cracked usernames that don't correspond to real Mojang accounts, the Mojang lookup will reject them — let me know if that's your setup and I can swap in a simpler format-only check instead of the Mojang verification.
- **Rate limits**: Mojang's username lookup API is rate limited. For a small/medium server this won't be an issue; if you expect a flood of signups at once you may want to add a queue — happy to add that if needed.
- **Un-whitelisting**: this plugin only adds to the whitelist, it doesn't remove. Use normal `/whitelist remove` for that.
- **`api-version` in plugin.yml** is set to `1.21`. If your Paper build reports a different major API version, you may need to adjust `paper-api` version in `pom.xml` to match (check with `/version` in-game).
