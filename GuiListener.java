package com.fistpractice.listeners;

import com.fistpractice.FistPractice;
import com.fistpractice.gui.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GuiListener implements Listener {

    private final FistPractice plugin;

    public GuiListener(FistPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Menu menu = plugin.getMenuManager().getOpenMenu(player.getUniqueId());
        if (menu == null) return;
        if (!event.getView().getTopInventory().equals(menu.getInventory())) return;

        boolean cancel = menu.onClick(player, event.getRawSlot());
        if (cancel) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Menu menu = plugin.getMenuManager().getOpenMenu(player.getUniqueId());
        if (menu != null && event.getView().getTopInventory().equals(menu.getInventory())) {
            plugin.getMenuManager().clear(player.getUniqueId());
        }
    }
}
