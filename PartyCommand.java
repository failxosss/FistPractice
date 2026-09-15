package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.party.Party;
import com.fistpractice.party.PartyInvite;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyCommand implements CommandExecutor {

    private final FistPractice plugin;

    public PartyCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendKey(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("fistpractice.party")) {
            MessageUtil.sendKey(player, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.getMenuManager().openPartyMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());

        switch (sub) {
            case "create" -> {
                if (party != null) { MessageUtil.sendKey(player, "party.already-in-party"); return true; }
                plugin.getPartyManager().createParty(player);
                MessageUtil.sendKey(player, "party.created");
            }
            case "disband" -> {
                if (party == null) { MessageUtil.sendKey(player, "party.not-in-party"); return true; }
                if (!party.canDisbandOrTransfer(player.getUniqueId())) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                plugin.getPartyManager().disbandParty(party);
                MessageUtil.sendKey(player, "party.disbanded");
            }
            case "invite" -> {
                if (party == null) { MessageUtil.sendKey(player, "party.not-in-party"); return true; }
                if (!party.canManage(player.getUniqueId())) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 2) return true;
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { MessageUtil.sendKey(player, "general.player-not-found"); return true; }
                if (plugin.getPartyManager().isInParty(target.getUniqueId())) { MessageUtil.sendKey(player, "party.already-in-party"); return true; }
                if (party.size() >= plugin.getPartyManager().getMaxSizeFor(player)) { MessageUtil.sendKey(player, "party.too-large-for-mode"); return true; }
                plugin.getPartyManager().invite(party, player, target.getUniqueId());
                MessageUtil.sendKey(player, "party.invite-sent", Map.of("target", target.getName()));
                MessageUtil.sendKey(target, "party.invite-received", Map.of("sender", player.getName(), "party", party.getName()));
            }
            case "accept" -> {
                if (args.length < 2) return true;
                Party target = plugin.getPartyManager().findByName(args[1]);
                if (target == null) return true;
                PartyInvite invite = plugin.getPartyManager().consumeInvite(player.getUniqueId(), target.getId());
                if (invite == null) return true;
                plugin.getPartyManager().addMember(target, player.getUniqueId());
                for (UUID uuid : target.getMembers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) MessageUtil.sendKey(p, "party.joined", Map.of("player", player.getName()));
                }
            }
            case "deny" -> {
                if (args.length < 2) return true;
                Party target = plugin.getPartyManager().findByName(args[1]);
                if (target != null) plugin.getPartyManager().consumeInvite(player.getUniqueId(), target.getId());
            }
            case "leave" -> {
                if (party == null) { MessageUtil.sendKey(player, "party.not-in-party"); return true; }
                plugin.getPartyManager().removeMember(party, player.getUniqueId());
                for (UUID uuid : party.getMembers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) MessageUtil.sendKey(p, "party.left", Map.of("player", player.getName()));
                }
            }
            case "kick" -> {
                if (party == null || !party.canManage(player.getUniqueId())) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 2) return true;
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !party.isMember(target.getUniqueId())) { MessageUtil.sendKey(player, "general.player-not-found"); return true; }
                plugin.getPartyManager().removeMember(party, target.getUniqueId());
                MessageUtil.sendKey(target, "party.kicked");
            }
            case "promote" -> {
                if (party == null || !party.canDisbandOrTransfer(player.getUniqueId())) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 2) return true;
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) party.promote(target.getUniqueId());
            }
            case "demote" -> {
                if (party == null || !party.canDisbandOrTransfer(player.getUniqueId())) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 2) return true;
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) party.demote(target.getUniqueId());
            }
            case "leader" -> {
                if (party == null || !party.canDisbandOrTransfer(player.getUniqueId())) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 2) return true;
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && party.isMember(target.getUniqueId())) {
                    party.setLeader(target.getUniqueId());
                    party.getModerators().remove(target.getUniqueId());
                }
            }
            case "info", "list" -> {
                if (party == null) { MessageUtil.sendKey(player, "party.not-in-party"); return true; }
                MessageUtil.sendRaw(player, "<gray>Party: <white>" + party.getName()
                        + " <gray>(" + party.size() + "/" + plugin.getPartyManager().getMaxSizeFor(player) + ")");
                MessageUtil.sendRaw(player, "<gray>Members: <white>" + String.join(", ", memberNames(party)));
            }
            case "chat" -> {
                if (party == null) { MessageUtil.sendKey(player, "party.not-in-party"); return true; }
                if (args.length >= 2) {
                    String msg = String.join(" ", List.of(args).subList(1, args.length));
                    for (UUID uuid : party.getMembers()) {
                        if (party.isChatMuted(uuid)) continue;
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) MessageUtil.sendRaw(p, "<gray>[Party] <white>" + player.getName() + ": " + msg);
                    }
                } else {
                    party.toggleChat(player.getUniqueId());
                }
            }
            case "settings" -> {
                if (party == null || !party.canDisbandOrTransfer(player.getUniqueId())) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length >= 3 && args[1].equalsIgnoreCase("friendlyfire")) {
                    party.setFriendlyFire(Boolean.parseBoolean(args[2]));
                }
            }
            case "split" -> {
                if (party == null || !party.canDisbandOrTransfer(player.getUniqueId())) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                String strategy = args.length >= 2 ? args[1] : "RANDOM";
                var teams = plugin.getPartyManager().splitTeams(party, strategy, 2);
                for (int i = 0; i < teams.size(); i++) {
                    List<String> names = teams.get(i).stream()
                            .map(u -> { Player p = Bukkit.getPlayer(u); return p != null ? p.getName() : u.toString(); })
                            .toList();
                    for (UUID uuid : party.getMembers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) MessageUtil.sendRaw(p, "<gold>Team " + (char) ('A' + i) + ": <white>" + String.join(", ", names));
                    }
                }
            }
            case "duel" -> new PartyDuelHelper(plugin).handleDuel(player, party, args);
            case "queue" -> new PartyDuelHelper(plugin).handleQueue(player, party, args);
            default -> MessageUtil.sendKey(player, "general.invalid-usage", Map.of("usage", "/party <create|invite|accept|leave|kick|...>"));
        }
        return true;
    }

    private List<String> memberNames(Party party) {
        return party.getMembers().stream()
                .map(u -> { Player p = Bukkit.getPlayer(u); return p != null ? p.getName() : u.toString(); })
                .toList();
    }
}
