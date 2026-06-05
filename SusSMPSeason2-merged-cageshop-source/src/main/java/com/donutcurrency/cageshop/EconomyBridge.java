
package com.donutcurrency.cageshop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

final class EconomyBridge {
    private final Object economyManager;
    private final Method getBalancePlayer;
    private final Method getBalanceUuid;
    private final Method removeBalanceUuid;
    private final Method setBalanceUuid;
    private final Method formatBalance;

    private EconomyBridge(Object economyManager,
                          Method getBalancePlayer, Method getBalanceUuid,
                          Method removeBalanceUuid, Method setBalanceUuid,
                          Method formatBalance) {
        this.economyManager = economyManager;
        this.getBalancePlayer = getBalancePlayer;
        this.getBalanceUuid = getBalanceUuid;
        this.removeBalanceUuid = removeBalanceUuid;
        this.setBalanceUuid = setBalanceUuid;
        this.formatBalance = formatBalance;
    }

    static EconomyBridge tryConnect(Plugin requester) {
        Plugin main = Bukkit.getPluginManager().getPlugin("SusSMPSeason2");
        if (main == null) {
            requester.getLogger().warning("Main plugin SusSMPSeason2 not found.");
            return null;
        }
        try {
            Method gm = main.getClass().getMethod("getEconomyManager");
            Object econ = gm.invoke(main);
            Method getBalancePlayer = findMethod(econ.getClass(), "getBalance", Player.class);
            Method getBalanceUuid = findMethod(econ.getClass(), "getBalance", UUID.class);
            Method removeBalanceUuid = findMethod(econ.getClass(), "removeBalance", UUID.class, double.class);
            Method setBalanceUuid = findMethod(econ.getClass(), "setBalance", UUID.class, double.class);
            Method formatBalance = findMethod(econ.getClass(), "formatBalance", double.class);
            requester.getLogger().info("Connected to existing economy manager.");
            return new EconomyBridge(econ, getBalancePlayer, getBalanceUuid, removeBalanceUuid, setBalanceUuid, formatBalance);
        } catch (Exception e) {
            requester.getLogger().warning("Could not connect to economy manager: " + e.getMessage());
            return null;
        }
    }

    double balance(Player player) {
        if (economyManager == null) return 0.0;
        try {
            if (getBalancePlayer != null) return ((Number) getBalancePlayer.invoke(economyManager, player)).doubleValue();
            if (getBalanceUuid != null) return ((Number) getBalanceUuid.invoke(economyManager, player.getUniqueId())).doubleValue();
        } catch (Exception ignored) {}
        return 0.0;
    }

    boolean hasEnough(Player player, double amount) {
        return balance(player) >= amount;
    }

    boolean withdraw(Player player, double amount) {
        if (economyManager == null) return false;
        try {
            if (removeBalanceUuid != null) {
                Object result = removeBalanceUuid.invoke(economyManager, player.getUniqueId(), amount);
                if (result instanceof Boolean b) return b;
                return true;
            }
            double newBalance = Math.max(0.0, balance(player) - amount);
            if (setBalanceUuid != null) {
                setBalanceUuid.invoke(economyManager, player.getUniqueId(), newBalance);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    String format(double amount) {
        if (economyManager == null) return String.format(java.util.Locale.US, "%.2f", amount);
        try {
            if (formatBalance != null) return String.valueOf(formatBalance.invoke(economyManager, amount));
        } catch (Exception ignored) {}
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            Method m = type.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (Exception ignored) {
            return null;
        }
    }
}
