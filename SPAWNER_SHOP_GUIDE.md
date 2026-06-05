# 🛒 Spawner Cage Shop - Implementation Guide

## 📋 Tổng Quan

Hệ thống shop bán lồng spawner với cơ chế restock tự động mỗi 1 giờ, số lượng ngẫu nhiên.

## 🎯 Tính Năng

✅ **Bán các loại spawner:**
- Zombie Spawner (10,000)
- Creeper Spawner (12,000)
- Iron Golem Spawner (50,000)
- Skeleton Spawner (15,000)
- Blaze Spawner / Quỷ Lửa (30,000)

✅ **Restock tự động:**
- Mỗi 1 giờ = 1 lần restock
- Số lượng ngẫu nhiên (5-15 cái cho mỗi loại)
- Không vượt quá kho hàng tối đa

✅ **Hệ thống tiền tệ:**
- Tích hợp với hệ thống tiền tệ liên thông của plugin

## 📁 Cấu Trúc File

```
src/main/java/com/sussmp/spawner/
├── SpawnerCageShop.java          # Class chính quản lý shop
├── SpawnerCageConfig.java        # Class quản lý config
└── command/
    └── SpawnerShopCommand.java   # Command handler

src/main/resources/
└── spawner-shop-config.yml       # File cấu hình
```

## 🔧 Cách Sử Dụng

### Lệnh Chính

```
/spawner              # Menu chính
/spawner list         # Xem danh sách spawner và giá
/spawner buy <loại> <số lượng>  # Mua spawner
/spawner info         # Xem thông tin shop
```

### Ví Dụ:
```
/spawner buy zombie 1        # Mua 1 zombie spawner
/spawner buy creeper 5       # Mua 5 creeper spawner
/spawner buy iron_golem 2    # Mua 2 iron golem spawner
```

## ⚙️ Tích Hợp Với Plugin

### 1️⃣ Đăng Ký Shop Trong Main Plugin

Thêm vào `onEnable()` của main class:

```java
import com.sussmp.spawner.SpawnerCageShop;
import com.sussmp.spawner.command.SpawnerShopCommand;

public class SusSMPPlugin extends JavaPlugin {
    private SpawnerCageShop spawnerShop;

    @Override
    public void onEnable() {
        // Khởi tạo shop
        spawnerShop = new SpawnerCageShop(this);
        
        // Đăng ký command
        getCommand("spawner").setExecutor(new SpawnerShopCommand(spawnerShop));
        
        getLogger().info("✓ Spawner Shop loaded!");
    }

    @Override
    public void onDisable() {
        if (spawnerShop != null) {
            spawnerShop.disable();
        }
    }
}
```

### 2️⃣ Thêm Command Vào plugin.yml

```yaml
commands:
  spawner:
    description: Mở shop bán spawner cage
    usage: /spawner [list|buy|info]
    aliases: [spawner-shop]
```

### 3️⃣ Tích Hợp Hệ Thống Tiền Tệ

Trong class `SpawnerCageShop.java`, tìm 2 method này và tích hợp:

```java
private boolean hasMoney(Player player, double amount) {
    // TODO: Lấy tiền từ hệ thống tiền tệ của plugin
    // Ví dụ: return getPlayerBalance(player) >= amount;
}

private void takeMoney(Player player, double amount) {
    // TODO: Trừ tiền từ hệ thống tiền tệ của plugin
    // Ví dụ: setPlayerBalance(player, getPlayerBalance(player) - amount);
}

private void giveSpawnerCage(Player player, EntityType type, int quantity) {
    // TODO: Tạo spawner cage item
    // Có thể dùng Material.SPAWNER với NBT tag hoặc custom item
    // player.getInventory().addItem(spawnerItem);
}
```

### 4️⃣ Tích Hợp Item Spawner Cage

Tạo class `SpawnerCageItem.java`:

```java
public class SpawnerCageItem {
    public static ItemStack createSpawnerCage(EntityType type) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6Spawner Cage: §e" + type.name());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Loại: §f" + type.name());
            lore.add("§7Nhấn chuột phải để đặt");
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
```

## 📊 Cấu Hình Config

File: `spawner-shop-config.yml`

Bạn có thể chỉnh sửa:
- **Giá** từng loại spawner
- **Kho hàng tối đa** cho mỗi loại
- **Kho hàng ban đầu** khi shop mở
- **Số lượng restock** (min-max)
- **Thời gian restock** (giờ)

## 📈 Xem Dữ Liệu Shop

Kho hàng được lưu trong memory và tự động cập nhật mỗi giờ. 

Nếu muốn **lưu vĩnh viễn** (khi server restart), thêm:

```java
// Trong SpawnerCageShop.java
public void saveStock() {
    for (EntityType type : currentStock.keySet()) {
        String path = "current-stock." + type.name();
        config.set(path, currentStock.get(type));
    }
    config.save();
}
```

## 🐛 Troubleshooting

**Q: Restock không chạy?**
- Kiểm tra console xem task đã start không
- Lệnh: `/sayLet me check if the scheduler was registered`

**Q: Không thể mua spawner?**
- Kiểm tra tiền (method `hasMoney` chưa tích hợp)
- Kiểm tra kho hàng (`/spawner list`)

**Q: Item không xuất hiện?**
- Method `giveSpawnerCage` chưa tích hợp
- Kiểm tra inventory đầy không

## 🚀 Tiếp Theo

- [ ] Tích hợp hệ thống tiền tệ
- [ ] Tích hợp item spawner cage
- [ ] Thêm GUI menu (nếu có plugin GUI)
- [ ] Log lịch sử mua bán
- [ ] Thêm permission cho admin
- [ ] Thêm discount hoặc khuyến mãi theo giờ

---

**Created:** 2026-06-05  
**Status:** Ready for Integration ✅
