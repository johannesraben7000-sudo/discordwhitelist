package com.example.discordwhitelist;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Optional;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    private final DiscordWhitelistPlugin plugin;

    public DiscordListener(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String whitelistChannelId = plugin.getConfig().getString("discord.whitelist-channel-id", "");
        if (whitelistChannelId.isBlank() || !event.getChannel().getId().equals(whitelistChannelId)) {
            return;
        }

        Message message = event.getMessage();
        String rawInput = message.getContentRaw().trim();
        MessageChannel channel = event.getChannel();
        String discordUserId = event.getAuthor().getId();

        boolean deleteAfter = plugin.getConfig().getBoolean("delete-message-after-processing", true);

        // Do the Mojang HTTP lookup off the JDA gateway thread.
        plugin.getDiscordBot().getJda().getCallbackPool().execute(() -> {
            // "One account per discord user" check
            if (plugin.getConfig().getBoolean("one-account-per-discord-user", true)) {
                Optional<String> existing = plugin.getLinkStore().getLinkedUsername(discordUserId);
                if (existing.isPresent()) {
                    reply(channel, plugin.getMessage("already-linked", "%username%", existing.get()));
                    if (deleteAfter) safeDelete(message);
                    return;
                }
            }

            if (MojangAPI.quickFormatCheck(rawInput).isEmpty()) {
                reply(channel, plugin.getMessage("invalid-username", "%input%", rawInput));
                if (deleteAfter) safeDelete(message);
                return;
            }

            MojangAPI.LookupResult result = MojangAPI.lookup(rawInput);

            if (!result.exists()) {
                reply(channel, plugin.getMessage("not-a-real-account", "%input%", rawInput));
                if (deleteAfter) safeDelete(message);
                return;
            }

            String correctName = result.correctedName();
            UUID uuid = result.uuid();

            // Whitelist operations must happen on the main server thread.
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    // Look up by UUID (already known from Mojang) rather than the
                    // deprecated name-based getOfflinePlayer, which can block on
                    // a network call if not cached.
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

                    if (player.isWhitelisted()) {
                        reply(channel, plugin.getMessage("already-whitelisted", "%username%", correctName));
                        return;
                    }

                    player.setWhitelisted(true);

                    if (plugin.getConfig().getBoolean("one-account-per-discord-user", true)) {
                        plugin.getLinkStore().link(discordUserId, correctName);
                    }

                    reply(channel, plugin.getMessage("success", "%username%", correctName));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to whitelist " + correctName + ": " + e.getMessage());
                    reply(channel, plugin.getMessage("error", "%input%", rawInput));
                } finally {
                    if (deleteAfter) safeDelete(message);
                }
            });
        });
    }

    private void reply(MessageChannel channel, String text) {
        channel.sendMessage(text).queue(
                success -> {},
                failure -> plugin.getLogger().warning("Failed to send Discord reply: " + failure.getMessage())
        );
    }

    private void safeDelete(Message message) {
        message.delete().queue(
                success -> {},
                failure -> { /* ignore - bot may lack Manage Messages permission */ }
        );
    }
}
