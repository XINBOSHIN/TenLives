package com.saikonohack.tenlives;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

public class LivesPlaceholder extends PlaceholderExpansion {

    private final TenLives plugin;

    public LivesPlaceholder(TenLives plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tenlives";
    }

    @Override
    public @NotNull String getAuthor() {
        return "saikonohack";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equals("lives")) {
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            int lives = (int) (maxHealth / 2);
            return String.valueOf(lives);
        }

        return null;
    }
}
