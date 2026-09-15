package com.fistpractice.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Every concrete menu (MainMenu, PartyMenu, DuelMenu, KitEditor, ArenaSelector,
 * SpectatorMenu, AdminMenu, ...) extends this and is driven entirely by
 * menus.yml for slots/materials/names - see spec section 58.
 */
public abstract class Menu {

    protected final Inventory inventory;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    protected Menu(String title, int size) {
        this.inventory = org.bukkit.Bukkit.createInventory(null,
                size, net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .serialize(MM.deserialize(title)));
    }

    public Inventory getInventory() { return inventory; }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    /** @return true if the click should be fully cancelled (almost always true for menus). */
    public abstract boolean onClick(Player player, int slot);

    protected void setItem(int slot, org.bukkit.Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name));
            if (lore != null) {
                meta.lore(lore.stream().map(MM::deserialize).toList());
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }
}
