package com.example.discordwhitelist;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Tracks discordUserId -> minecraftUsername so we can enforce
 * "one whitelist per Discord account" if that option is enabled.
 */
public class DiscordLinkStore {

    private final File file;
    private final YamlConfiguration yaml;
    private final DiscordWhitelistPlugin plugin;

    public DiscordLinkStore(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "discord-links.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create discord-links.yml", e);
            }
        }
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized Optional<String> getLinkedUsername(String discordUserId) {
        return Optional.ofNullable(yaml.getString(discordUserId));
    }

    public synchronized void link(String discordUserId, String minecraftUsername) {
        yaml.set(discordUserId, minecraftUsername);
        save();
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save discord-links.yml", e);
        }
    }
}
