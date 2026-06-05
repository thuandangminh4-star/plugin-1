package com.sussmp.spawner;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class SpawnerCageConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public SpawnerCageConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Tải hoặc tạo file config
     */
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "spawner-shop-config.yml");
        
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Tạo config mặc định
     */
    private void createDefaultConfig() {
        plugin.getDataFolder().mkdirs();
        try {
            configFile.createNewFile();
            FileConfiguration defaultConfig = new YamlConfiguration();

            // Cài đặt restock
            defaultConfig.set("restock.interval-hours", 1);
            defaultConfig.set("restock.min-amount", 5);
            defaultConfig.set("restock.max-amount", 15);

            // Cài đặt spawner
            defaultConfig.set("spawners.zombie.price", 10000);
            defaultConfig.set("spawners.zombie.max-stock", 100);
            defaultConfig.set("spawners.zombie.initial-stock", 20);

            defaultConfig.set("spawners.creeper.price", 12000);
            defaultConfig.set("spawners.creeper.max-stock", 80);
            defaultConfig.set("spawners.creeper.initial-stock", 15);

            defaultConfig.set("spawners.iron_golem.price", 50000);
            defaultConfig.set("spawners.iron_golem.max-stock", 30);
            defaultConfig.set("spawners.iron_golem.initial-stock", 5);

            defaultConfig.set("spawners.skeleton.price", 15000);
            defaultConfig.set("spawners.skeleton.max-stock", 70);
            defaultConfig.set("spawners.skeleton.initial-stock", 12);

            defaultConfig.set("spawners.blaze.price", 30000);
            defaultConfig.set("spawners.blaze.max-stock", 50);
            defaultConfig.set("spawners.blaze.initial-stock", 8);

            defaultConfig.save(configFile);
            plugin.getLogger().info("§a[SusSMP] Config spawner shop được tạo!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Lấy danh sách loại spawner có bán
     */
    public List<EntityType> getAvailableSpawners() {
        List<EntityType> spawners = new ArrayList<>();
        spawners.add(EntityType.ZOMBIE);
        spawners.add(EntityType.CREEPER);
        spawners.add(EntityType.IRON_GOLEM);
        spawners.add(EntityType.SKELETON);
        spawners.add(EntityType.BLAZE);
        return spawners;
    }

    /**
     * Lấy giá spawner
     */
    public int getPrice(EntityType type) {
        String path = "spawners." + type.name().toLowerCase() + ".price";
        return config.getInt(path, 10000);
    }

    /**
     * Lấy kho hàng tối đa
     */
    public int getMaxStock(EntityType type) {
        String path = "spawners." + type.name().toLowerCase() + ".max-stock";
        return config.getInt(path, 100);
    }

    /**
     * Lấy kho hàng ban đầu
     */
    public int getInitialStock(EntityType type) {
        String path = "spawners." + type.name().toLowerCase() + ".initial-stock";
        return config.getInt(path, 20);
    }

    /**
     * Lấy số lượng restock tối thiểu
     */
    public int getMinRestockAmount() {
        return config.getInt("restock.min-amount", 5);
    }

    /**
     * Lấy số lượng restock tối đa
     */
    public int getMaxRestockAmount() {
        return config.getInt("restock.max-amount", 15);
    }

    /**
     * Tải lại config
     */
    public void reload() {
        loadConfig();
    }
}
