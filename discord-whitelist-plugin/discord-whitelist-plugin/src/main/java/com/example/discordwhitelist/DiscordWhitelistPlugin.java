package com.example.discordwhitelist;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class DiscordWhitelistPlugin extends JavaPlugin {

    private JDA jda;
    private DiscordLinkStore linkStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.linkStore = new DiscordLinkStore(this);
        startBot();
    }

    @Override
    public void onDisable() {
        stopBot();
    }

    private void startBot() {
        String token = getConfig().getString("discord.bot-token", "");
        String channelId = getConfig().getString("discord.whitelist-channel-id", "");

        if (token.isBlank() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            getLogger().severe("No Discord bot token set in config.yml! The bot will not start.");
            getLogger().severe("Edit plugins/DiscordWhitelist/config.yml and set discord.bot-token, then run /discordwhitelist reload.");
            return;
        }
        if (channelId.isBlank() || channelId.equals("PUT_CHANNEL_ID_HERE")) {
            getLogger().severe("No whitelist-channel-id set in config.yml! The bot will not start.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(new DiscordListener(this))
                    .build();
            getLogger().info("Discord bot starting...");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to start Discord bot. Check your bot token.", e);
        }
    }

    private void stopBot() {
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("discordwhitelist")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                stopBot();
                reloadConfig();
                startBot();
                sender.sendMessage("§a[DiscordWhitelist] Reloaded and reconnecting bot.");
                return true;
            }
            sender.sendMessage("§eUsage: /discordwhitelist reload");
            return true;
        }
        return false;
    }

    public String getMessage(String key, String placeholder, String value) {
        String raw = getConfig().getString("messages." + key, "");
        return raw.replace(placeholder, value);
    }

    public DiscordBotAccessor getDiscordBot() {
        return () -> jda;
    }

    public DiscordLinkStore getLinkStore() {
        return linkStore;
    }

    /** Small functional wrapper so DiscordListener doesn't need to null-check the plugin field directly. */
    public interface DiscordBotAccessor {
        JDA getJda();
    }
}
