package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.kit.Kit;
import com.fistpractice.kit.KitLayout;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class KitCommand implements CommandExecutor {

    private final FistPractice plugin;

    public KitCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendKey(sender, "general.player-only");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendRaw(player, "<gray>Available kits: <white>" + String.join(", ", plugin.getKitManager().getAll().keySet()));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!player.hasPermission("fistpractice.kit")) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 2) return true;
                Kit kit = new Kit(args[1]);
                for (int i = 0; i < 36; i++) {
                    var item = player.getInventory().getItem(i);
                    if (item != null && item.getType() != Material.AIR) kit.getItems().put(i, item.clone());
                }
                if (player.getInventory().getHelmet() != null) kit.getArmor().put("helmet", player.getInventory().getHelmet().clone());
                if (player.getInventory().getChestplate() != null) kit.getArmor().put("chestplate", player.getInventory().getChestplate().clone());
                if (player.getInventory().getLeggings() != null) kit.getArmor().put("leggings", player.getInventory().getLeggings().clone());
                if (player.getInventory().getBoots() != null) kit.getArmor().put("boots", player.getInventory().getBoots().clone());
                kit.setHealth(player.getHealth());
                kit.setHunger(player.getFoodLevel());
                plugin.getKitManager().register(kit);
                MessageUtil.sendRaw(player, "<green>Kit '" + args[1] + "' created from your current inventory.");
            }
            case "give" -> {
                if (args.length < 2) return true;
                if (!plugin.getKitManager().exists(args[1])) { MessageUtil.sendKey(player, "match.kit-not-found"); return true; }
                plugin.getKitManager().apply(player, args[1]);
            }
            case "delete" -> {
                if (!player.hasPermission("fistpractice.kit")) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 2) return true;
                plugin.getKitManager().getAll().remove(args[1].toLowerCase());
            }
            case "layout" -> handleLayout(player, args);
            default -> MessageUtil.sendKey(player, "general.invalid-usage", Map.of("usage", "/kit <create|give|delete|layout> ..."));
        }
        return true;
    }

    private void handleLayout(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendKey(player, "general.invalid-usage", Map.of("usage", "/kit layout <save|load|delete> <kitId> [name]"));
            return;
        }
        String action = args[1].toLowerCase();
        String kitId = args[2];
        String layoutName = args.length >= 4 ? args[3] : "default";

        switch (action) {
            case "save" -> {
                KitLayout layout = new KitLayout(kitId, layoutName);
                for (int i = 0; i < 36; i++) {
                    var item = player.getInventory().getItem(i);
                    if (item != null) layout.getSlots().put(i, item.clone());
                }
                plugin.getKitManager().saveLayout(player.getUniqueId(), layout);
                MessageUtil.sendRaw(player, "<green>Saved layout '" + layoutName + "' for kit " + kitId + ".");
            }
            case "load" -> {
                KitLayout layout = plugin.getKitManager().getLayout(player.getUniqueId(), kitId, layoutName);
                if (layout == null) { MessageUtil.sendRaw(player, "<red>No such layout."); return; }
                player.getInventory().clear();
                layout.getSlots().forEach((slot, item) -> player.getInventory().setItem(slot, item.clone()));
            }
            case "delete" -> plugin.getKitManager().deleteLayout(player.getUniqueId(), kitId, layoutName);
            default -> { }
        }
    }
}
