
package com.donutcurrency.cageshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CageShopFeature implements Listener {

    private final JavaPlugin plugin;

    public CageShopFeature(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static final String MAIN_TITLE = color("&8[&d&lLỒNG SHOP&8]");
    private static final String PAGE_PREFIX = color("&8[&d&lLỒNG SHOP&8] &7Trang ");
    private static final String CONFIRM_PREFIX = color("&8[&d&lXÁC NHẬN LỒNG&8]");
    private static final int[] DISPLAY_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};

    private final Map<UUID, Integer> currentPage = new ConcurrentHashMap<>();
    private final Map<UUID, Offer> selectedOffer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> selectedQuantity = new ConcurrentHashMap<>();

    private final List<Offer> offers = new ArrayList<>();
    private File dataFile;
    private EconomyBridge economy;

    public void start() {
        this.dataFile = new File(plugin.getDataFolder(), "cageshop.txt");
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        loadOffers();
        this.economy = EconomyBridge.tryConnect(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        long periodTicks = 30L * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::restockAll, periodTicks, periodTicks);
        plugin.getLogger().info("Cage shop addon enabled with " + offers.size() + " offers.");
    }

    public void shutdown() {
        saveOffers();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = stripColor(event.getMessage()).trim().toLowerCase(Locale.ROOT);
        if (msg.startsWith("/shop cage") || msg.startsWith("/shop long") || msg.startsWith("/cage")) {
            event.setCancelled(true);
            openMain(event.getPlayer(), 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = stripColor(event.getView().getTitle());
        int slot = event.getRawSlot();

        if (MAIN_TITLE.equals(title) || title.startsWith(stripColor(PAGE_PREFIX))) {
            event.setCancelled(true);
            if (slot == 49) {
                openMain(player, Math.max(0, currentPage.getOrDefault(player.getUniqueId(), 0) - 1));
                return;
            }
            if (slot == 50) {
                openMain(player, currentPage.getOrDefault(player.getUniqueId(), 0) + 1);
                return;
            }
            if (slot == 48) {
                player.closeInventory();
                return;
            }

            Offer offer = offerFromSlot(currentPage.getOrDefault(player.getUniqueId(), 0), slot);
            if (offer != null) {
                openConfirm(player, offer, 1);
            }
            return;
        }

        if (title.startsWith(stripColor(CONFIRM_PREFIX))) {
            event.setCancelled(true);
            Offer offer = selectedOffer.get(player.getUniqueId());
            if (offer == null) {
                player.closeInventory();
                return;
            }
            int qty = selectedQuantity.getOrDefault(player.getUniqueId(), 1);

            if (slot == 11) openConfirm(player, offer, Math.max(1, qty - 1));
            else if (slot == 13) openConfirm(player, offer, Math.min(offer.currentStock, qty + 1));
            else if (slot == 15) openConfirm(player, offer, getMaxAffordableQuantity(player, offer));
            else if (slot == 16) buy(player, offer, qty);
            else if (slot == 18) openMain(player, currentPage.getOrDefault(player.getUniqueId(), 0));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            selectedOffer.remove(player.getUniqueId());
            selectedQuantity.remove(player.getUniqueId());
        }
    }

    private void openMain(Player player, int page) {
        page = Math.max(0, page);
        currentPage.put(player.getUniqueId(), page);

        int perPage = DISPLAY_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil(offers.size() / (double) perPage));
        page = Math.min(page, totalPages - 1);
        currentPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, page == 0 ? MAIN_TITLE : PAGE_PREFIX + (page + 1) + "/" + totalPages);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE, " ");
        inv.setItem(4, item(Material.SPAWNER, "&d&lLồng mob", List.of(
                "&7Mua lồng mob ngẫu nhiên.",
                "&7Mỗi lần restock sẽ đổi số lượng.",
                "&7Dùng &f/shop cage &7để mở menu này."
        )));

        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= offers.size()) break;
            inv.setItem(DISPLAY_SLOTS[i], offerItem(offers.get(idx)));
        }

        if (page > 0) inv.setItem(49, item(Material.ARROW, "&eTrang trước", List.of("&7Quay lại")));
        if (page < totalPages - 1) inv.setItem(50, item(Material.ARROW, "&eTrang sau", List.of("&7Xem tiếp")));
        inv.setItem(48, item(Material.BARRIER, "&cĐóng", List.of("&7Thoát menu")));
        player.openInventory(inv);
    }

    private void openConfirm(Player player, Offer offer, int quantity) {
        quantity = Math.max(1, Math.min(quantity, getMaxAffordableQuantity(player, offer)));
        selectedOffer.put(player.getUniqueId(), offer);
        selectedQuantity.put(player.getUniqueId(), quantity);

        Inventory inv = Bukkit.createInventory(null, 27, CONFIRM_PREFIX + " " + offer.displayName);
        fill(inv, Material.GRAY_STAINED_GLASS_PANE, " ");
        inv.setItem(4, offerItem(offer));
        inv.setItem(11, item(Material.RED_WOOL, "&c-1", List.of("&7Giảm 1")));
        inv.setItem(13, item(Material.PAPER, "&eSố lượng: &f" + quantity, List.of(
                "&7Giá: &f" + economyFormat(offer.price),
                "&7Tồn kho: &f" + offer.currentStock
        )));
        inv.setItem(15, item(Material.GREEN_WOOL, "&aMax", List.of("&7Mua số lượng tối đa")));
        inv.setItem(16, item(Material.LIME_WOOL, "&aMua ngay", List.of(
                "&7Tổng tiền: &f" + economyFormat(offer.price * quantity),
                "&7Bấm để mua"
        )));
        inv.setItem(18, item(Material.BARRIER, "&cHủy", List.of("&7Quay lại")));
        player.openInventory(inv);
    }

    private void buy(Player player, Offer offer, int quantity) {
        quantity = Math.max(1, Math.min(quantity, offer.currentStock));
        if (quantity <= 0) {
            player.sendMessage(color("&cHết hàng rồi."));
            player.closeInventory();
            return;
        }
        double total = round(offer.price * quantity);
        if (economy == null) {
            player.sendMessage(color("&cKhông kết nối được với hệ kinh tế."));
            return;
        }
        if (!economy.hasEnough(player, total)) {
            player.sendMessage(color("&cBạn không đủ tiền. Cần: &f" + economyFormat(total)));
            return;
        }
        if (!economy.withdraw(player, total)) {
            player.sendMessage(color("&cThanh toán thất bại."));
            return;
        }

        ItemStack stack = createSpawnerItem(offer, quantity);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        offer.currentStock -= quantity;
        saveOffers();
        player.sendMessage(color("&aBạn đã mua &f" + quantity + " &alồng &d" + offer.mobName + " &avới giá &f" + economyFormat(total)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        openMain(player, currentPage.getOrDefault(player.getUniqueId(), 0));
    }

    private Offer offerFromSlot(int page, int slot) {
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            if (DISPLAY_SLOTS[i] == slot) {
                int idx = page * DISPLAY_SLOTS.length + i;
                if (idx >= 0 && idx < offers.size()) return offers.get(idx);
            }
        }
        return null;
    }

    private int getMaxAffordableQuantity(Player player, Offer offer) {
        if (economy == null) return 1;
        int maxByMoney = (int) Math.floor(economy.balance(player) / Math.max(1.0, offer.price));
        return Math.max(1, Math.min(offer.currentStock, maxByMoney));
    }

    private void restockAll() {
        Random random = new Random();
        for (Offer offer : offers) {
            int min = Math.max(1, offer.minStock);
            int max = Math.max(min, offer.maxStock);
            offer.currentStock = min + random.nextInt(max - min + 1);
        }
        saveOffers();
        plugin.getLogger().info("Cage shop restocked at " + Instant.now());
    }

    private void loadOffers() {
        offers.clear();
        if (!dataFile.exists()) {
            offers.addAll(defaultOffers());
            saveOffers();
            return;
        }
        try {
            List<String> lines = Files.readAllLines(dataFile.toPath(), StandardCharsets.UTF_8);
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] split = line.split("\\|");
                if (split.length < 8) continue;
                Offer o = new Offer();
                o.id = split[0];
                o.displayName = split[1];
                o.mobName = split[2];
                o.price = Double.parseDouble(split[3]);
                o.minStock = Integer.parseInt(split[4]);
                o.maxStock = Integer.parseInt(split[5]);
                o.currentStock = Integer.parseInt(split[6]);
                o.glow = Boolean.parseBoolean(split[7]);
                if (split.length >= 9) {
                    try { o.icon = Material.valueOf(split[8]); } catch (Exception ignored) {}
                }
                offers.add(o);
            }
            if (offers.isEmpty()) {
                offers.addAll(defaultOffers());
                saveOffers();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load cageshop.txt, using defaults: " + e.getMessage());
            offers.clear();
            offers.addAll(defaultOffers());
            saveOffers();
        }
    }

    private void saveOffers() {
        try {
            if (!dataFile.getParentFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                dataFile.getParentFile().mkdirs();
            }
            List<String> lines = new ArrayList<>();
            lines.add("# id|displayName|mobName|price|minStock|maxStock|currentStock|glow|icon");
            for (Offer o : offers) {
                lines.add(String.join("|",
                        safe(o.id), safe(o.displayName), safe(o.mobName),
                        String.valueOf(o.price), String.valueOf(o.minStock), String.valueOf(o.maxStock),
                        String.valueOf(o.currentStock), String.valueOf(o.glow), o.icon.name()
                ));
            }
            Files.write(dataFile.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save cageshop.txt: " + e.getMessage());
        }
    }

    private List<Offer> defaultOffers() {
        return new ArrayList<>(List.of(
                offer("zombie", "Lồng Zombie", "Zombie", 12000, 1, 4, Material.ZOMBIE_SPAWN_EGG),
                offer("skeleton", "Lồng Skeleton", "Skeleton", 14000, 1, 3, Material.SKELETON_SPAWN_EGG),
                offer("creeper", "Lồng Creeper", "Creeper", 18000, 0, 2, Material.CREEPER_SPAWN_EGG),
                offer("spider", "Lồng Spider", "Spider", 15000, 1, 3, Material.SPIDER_SPAWN_EGG),
                offer("blaze", "Lồng Blaze", "Blaze", 45000, 0, 1, Material.BLAZE_SPAWN_EGG),
                offer("enderman", "Lồng Enderman", "Enderman", 40000, 0, 2, Material.ENDERMAN_SPAWN_EGG),
                offer("piglin", "Lồng Piglin", "Piglin", 38000, 0, 2, Material.PIGLIN_SPAWN_EGG),
                offer("villager", "Lồng Villager", "Villager", 50000, 0, 1, Material.VILLAGER_SPAWN_EGG)
        ));
    }

    private Offer offer(String id, String displayName, String mobName, double price, int minStock, int maxStock, Material icon) {
        Offer o = new Offer();
        o.id = id;
        o.displayName = displayName;
        o.mobName = mobName;
        o.price = price;
        o.minStock = minStock;
        o.maxStock = maxStock;
        o.currentStock = Math.max(1, minStock);
        o.icon = icon;
        o.glow = price >= 40000;
        return o;
    }

    private ItemStack offerItem(Offer offer) {
        ItemStack item = new ItemStack(offer.icon, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&d" + offer.displayName));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Mob: &f" + offer.mobName));
            lore.add(color("&7Giá: &f" + economyFormat(offer.price)));
            lore.add(color("&7Tồn kho: &f" + offer.currentStock));
            lore.add(color("&7Restock: &aNgẫu nhiên"));
            lore.add(color("&8Bấm để mua"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSpawnerItem(Offer offer, int amount) {
        ItemStack item = new ItemStack(Material.SPAWNER, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&dLồng " + offer.mobName));
            meta.setLore(List.of(
                    color("&7Mob: &f" + offer.mobName),
                    color("&7Đã mua từ shop lồng"),
                    color("&7Dùng để trang trí hoặc setup farm.")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inv, Material material, String name) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, item(material, name, List.of()));
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String s : lore) coloredLore.add(color(s));
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String economyFormat(double amount) {
        return economy != null ? economy.format(amount) : String.format(Locale.US, "%.2f", amount);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("|", "/");
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static String stripColor(String s) {
        return ChatColor.stripColor(s == null ? "" : s);
    }

    private static double round(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    private static class Offer {
        String id;
        String displayName;
        String mobName;
        double price;
        int minStock;
        int maxStock;
        int currentStock;
        boolean glow;
        Material icon = Material.SPAWNER;
    }
}
