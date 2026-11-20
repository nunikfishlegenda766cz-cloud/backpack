package com.example.backpack;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BackpackPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private static final int NORMAL_SIZE = 18; // 2 řádky pro běžného hráče
    private static final int OP_SIZE = 54;     // 3x větší (6 řádků) pro hráče s OP
    private static final String TITLE_NORMAL = ChatColor.DARK_GREEN + "Batoh";
    private static final String TITLE_OP = ChatColor.GOLD + "OP Batoh (3x)";

    private final Map<UUID, Inventory> openBackpacks = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        createDataFile();
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("backpack") != null) {
            getCommand("backpack").setExecutor(this);
        }
        getLogger().info("BackpackPlugin byl úspěšně spuštěn!");
    }

    @Override
    public void onDisable() {
        for (Map.Entry<UUID, Inventory> entry : openBackpacks.entrySet()) {
            saveInventory(entry.getKey(), entry.getValue());
        }
        openBackpacks.clear();
        saveDataFile();
    }

    private void createDataFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        dataFile = new File(getDataFolder(), "backpacks.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Nepodařilo se vytvořit backpacks.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveDataFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Nepodařilo se uložit backpacks.yml!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Tento příkaz může použít jen hráč.");
            return true;
        }

        Player player = (Player) sender;
        boolean hasOp = player.isOp();
        int size = hasOp ? OP_SIZE : NORMAL_SIZE;
        String title = hasOp ? TITLE_OP : TITLE_NORMAL;

        Inventory inv = Bukkit.createInventory(player, size, title);
        loadInventory(player.getUniqueId(), inv, size);

        openBackpacks.put(player.getUniqueId(), inv);
        player.openInventory(inv);
        player.sendMessage(hasOp
                ? ChatColor.GOLD + "✔ Otevřel jsi svůj 3x větší OP batoh (54 slotů)."
                : ChatColor.GREEN + "✔ Otevřel jsi svůj batoh (18 slotů).");
        return true;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        Inventory tracked = openBackpacks.get(uuid);
        if (tracked != null && event.getInventory().equals(tracked)) {
            saveInventory(uuid, event.getInventory());
            saveDataFile();
            openBackpacks.remove(uuid);
        }
    }

    private void loadInventory(UUID uuid, Inventory inv, int targetSize) {
        List<?> rawList = dataConfig.getList("backpacks." + uuid.toString());
        if (rawList == null) return;

        int limit = Math.min(rawList.size(), targetSize);
        for (int i = 0; i < limit; i++) {
            Object obj = rawList.get(i);
            if (obj instanceof ItemStack) {
                inv.setItem(i, (ItemStack) obj);
            }
        }
    }

    private void saveInventory(UUID uuid, Inventory inv) {
        ItemStack[] contents = inv.getContents();
        dataConfig.set("backpacks." + uuid.toString(), contents);
    }
}
