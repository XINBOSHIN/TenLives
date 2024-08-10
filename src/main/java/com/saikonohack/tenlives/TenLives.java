package com.saikonohack.tenlives;

import org.bstats.bukkit.Metrics;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class TenLives extends JavaPlugin implements Listener {

    private final Set<Location> processedChests = new HashSet<>();
    private String deathAction;
    private String deathMessage;
    private String totemName;
    private int totemCustomModelData;
    private String lifeLostTitle;
    private String lifeLostSubtitle;
    private String lifeRestoredTitle;
    private String lifeRestoredSubtitle;
    private String totemReceivedMessage;
    private String commandPlayerOnlyMessage;
    private String noPermissionMessage;


    public void loadLanguage(String lang) {
        FileConfiguration languageConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "language_" + lang + ".yml"));
        // Логика работы Language
        saveDefaultConfig();
        loadLanguage(getConfig().getString("language", "ru"));

        deathAction = getConfig().getString("death_action", "spectator");
        deathMessage = languageConfig.getString("death_message", "&cВаши жизни закончились...");
        totemName = languageConfig.getString("totem_name", "&6Тотем Жизни");
        totemCustomModelData = getConfig().getInt("totem_custom_model_data", 993);
        lifeLostTitle = languageConfig.getString("life_lost_title", "&cВы потеряли жизнь");
        lifeLostSubtitle = languageConfig.getString("life_lost_subtitle", "&eБудь внимательнее, они не вечны");
        lifeRestoredTitle = languageConfig.getString("life_restored_title", "&a1 Жизнь восстановлена");
        lifeRestoredSubtitle = languageConfig.getString("life_restored_subtitle", "&eПотрать её с умом");
        totemReceivedMessage = languageConfig.getString("totem_received_message", "&aВы получили Тотем Жизни!");
        commandPlayerOnlyMessage = languageConfig.getString("command_player_only", "&cЭту команду может использовать только игрок.");
        noPermissionMessage = languageConfig.getString("no_permission_message", "&cУ вас нет прав на использование этой команды.");

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TenLives plugin enabled!");

        // Register the placeholder expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LivesPlaceholder(this).register();
        }

        int pluginId = 22916;
        new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        getLogger().info("TenLives plugin disabled!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (maxHealth != null) {
            double newMaxHealth = maxHealth.getBaseValue() - 2.0;
            if (newMaxHealth < 1.0) {
                if ("ban".equalsIgnoreCase(deathAction)) {
                    player.kickPlayer(deathMessage);
                    Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), deathMessage, null, null);
                } else if ("spectator".equalsIgnoreCase(deathAction)) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            } else {
                maxHealth.setBaseValue(newMaxHealth);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
            player.sendTitle(lifeLostTitle, lifeLostSubtitle, 10, 70, 20);
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
        }, 1L);
    }

    @EventHandler
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
            ItemStack itemInOffHand = player.getInventory().getItemInOffHand();

            if (isValidTotem(itemInMainHand) || isValidTotem(itemInOffHand)) {
                AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (maxHealth != null) {
                    double newMaxHealth = maxHealth.getBaseValue() + 2.0;
                    if (newMaxHealth > 20.0) {
                        newMaxHealth = 20.0;
                    }
                    maxHealth.setBaseValue(newMaxHealth);
                    player.sendTitle(lifeRestoredTitle, lifeRestoredSubtitle, 10, 70, 20);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof StorageMinecart minecart) {
            Location loc = minecart.getLocation();

            if (processedChests.contains(loc)) {
                return;
            }

            processedChests.add(loc);
            Random random = new Random();
            if (random.nextDouble() < 0.1) {
                ItemStack totem = createCustomTotem();
                minecart.getInventory().addItem(totem);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gettotem")) {
            Player targetPlayer = null;

            if (args.length > 0) {
                targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
            } else if (sender instanceof Player) {
                targetPlayer = (Player) sender;
            } else {
                sender.sendMessage(commandPlayerOnlyMessage);
                return true;
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission("tenlives.gettotem")) {
                    player.sendMessage(noPermissionMessage);
                    return true;
                }
            } else if (!sender.hasPermission("tenlives.gettotem.others")) {
                sender.sendMessage(noPermissionMessage);
                return true;
            }

            ItemStack totem = createCustomTotem();
            targetPlayer.getInventory().addItem(totem);
            targetPlayer.sendMessage(totemReceivedMessage);
            if (sender != targetPlayer) {
                sender.sendMessage(ChatColor.GREEN + "Totem given to " + targetPlayer.getName());
            }
            return true;
        }
        return false;
    }


    private ItemStack createCustomTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();
        meta.setDisplayName(totemName);
        meta.setCustomModelData(totemCustomModelData);
        totem.setItemMeta(meta);
        return totem;
    }

    private boolean isValidTotem(ItemStack item) {
        if (item != null && item.getType() == Material.TOTEM_OF_UNDYING && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            return meta.hasCustomModelData() && meta.getCustomModelData() == totemCustomModelData;
        }
        return false;
    }
}
