package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.arena.Arena;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class ArenaCommand implements CommandExecutor {

    private final FistPractice plugin;

    public ArenaCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendKey(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("fistpractice.arena")) {
            MessageUtil.sendKey(player, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendKey(player, "general.invalid-usage", Map.of("usage",
                    "/arena <create|delete|setspawn1|setspawn2|setspectator|setregion|enable|disable|reset> <name>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) return true;
                if (plugin.getArenaManager().get(args[1]) != null) { MessageUtil.sendRaw(player, "<red>Arena already exists."); return true; }
                Arena arena = plugin.getArenaManager().create(args[1]);
                arena.setWorldName(player.getWorld().getName());
                MessageUtil.sendRaw(player, "<green>Arena '" + args[1] + "' created.");
            }
            case "delete" -> {
                if (args.length < 2) return true;
                plugin.getArenaManager().delete(args[1]);
                MessageUtil.sendRaw(player, "<green>Arena deleted.");
            }
            case "setspawn1" -> withArena(player, args, a -> { a.setSpawn1(player.getLocation()); plugin.getArenaManager().save(a); });
            case "setspawn2" -> withArena(player, args, a -> { a.setSpawn2(player.getLocation()); plugin.getArenaManager().save(a); });
            case "setspectator" -> withArena(player, args, a -> { a.setSpectatorSpawn(player.getLocation()); plugin.getArenaManager().save(a); });
            case "setregion" -> {
                if (args.length < 2) return true;
                Arena arena = plugin.getArenaManager().get(args[1]);
                if (arena == null) { MessageUtil.sendRaw(player, "<red>No such arena."); return true; }
                if (args.length >= 3 && args[2].equalsIgnoreCase("min")) arena.setRegionMin(player.getLocation());
                else if (args.length >= 3 && args[2].equalsIgnoreCase("max")) arena.setRegionMax(player.getLocation());
                else { MessageUtil.sendRaw(player, "<red>/arena setregion <name> <min|max>"); return true; }
                plugin.getArenaManager().save(arena);
                if (arena.getRegionMin() != null && arena.getRegionMax() != null) {
                    plugin.getArenaManager().captureSnapshot(arena);
                    MessageUtil.sendRaw(player, "<green>Region set and snapshot captured for reset.");
                }
            }
            case "enable" -> withArena(player, args, a -> { a.setEnabled(true); plugin.getArenaManager().save(a); });
            case "disable" -> withArena(player, args, a -> { a.setEnabled(false); plugin.getArenaManager().save(a); });
            case "reset" -> withArena(player, args, a -> plugin.getArenaManager().resetArena(a,
                    () -> MessageUtil.sendRaw(player, "<green>Arena reset complete.")));
            case "list" -> MessageUtil.sendRaw(player, "<gray>Arenas: <white>"
                    + String.join(", ", plugin.getArenaManager().getAll().stream().map(Arena::getName).toList()));
            default -> MessageUtil.sendKey(player, "general.invalid-usage", Map.of("usage", "/arena <subcommand> <name>"));
        }
        return true;
    }

    private void withArena(Player player, String[] args, java.util.function.Consumer<Arena> action) {
        if (args.length < 2) { MessageUtil.sendRaw(player, "<red>Specify an arena name."); return; }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) { MessageUtil.sendRaw(player, "<red>No such arena."); return; }
        action.accept(arena);
        MessageUtil.sendRaw(player, "<green>Done.");
    }
}
