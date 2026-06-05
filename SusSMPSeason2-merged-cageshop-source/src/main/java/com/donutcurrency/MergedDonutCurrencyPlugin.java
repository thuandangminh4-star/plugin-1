package com.donutcurrency;

import com.donutcurrency.cageshop.CageShopFeature;
import org.bukkit.Bukkit;

public class MergedDonutCurrencyPlugin extends DonutCurrencyPlugin {
    private CageShopFeature cageShopFeature;

    @Override
    public void onEnable() {
        super.onEnable();
        this.cageShopFeature = new CageShopFeature(this);
        cageShopFeature.start();
        getLogger().info("Cage shop merged into main plugin.");
    }

    @Override
    public void onDisable() {
        try {
            if (cageShopFeature != null) {
                cageShopFeature.shutdown();
            }
        } finally {
            super.onDisable();
        }
    }
}
