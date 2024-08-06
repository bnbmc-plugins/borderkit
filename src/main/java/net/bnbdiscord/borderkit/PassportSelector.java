package net.bnbdiscord.borderkit;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.Consumer;

public class PassportSelector implements Listener {
    private final Inventory inv;
    private final Player player;
    private final Plugin plugin;
    private final Consumer<Passport> callback;
    private boolean accepted = false;

    private PassportSelector(Player player, Plugin plugin, Consumer<Passport> callback) {
        this.player = player;
        this.plugin = plugin;
        this.callback = callback;

        inv = Bukkit.createInventory(null, player.getEnderChest().getSize(), Component.text("Choose a passport"));
        for (int i = 0; i < player.getEnderChest().getSize(); i++) {
            var item = player.getEnderChest().getItem(i);
            if (item == null) continue;
            inv.setItem(i, item.clone());
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void selectPassport(Player player, Plugin plugin, Consumer<Passport> callback) {
        var selector = new PassportSelector(player, plugin, callback);
        player.openInventory(selector.inv);
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent e) {
        if (!e.getInventory().equals(inv)) return;

        if (!accepted) {
            callback.accept(null);
        }
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir() || !Passport.isValidPassport(plugin, clickedItem)) return;

        callback.accept(new Passport(plugin, clickedItem));
        accepted = true;
        player.updateInventory();
        player.closeInventory(InventoryCloseEvent.Reason.PLAYER);
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }
}
