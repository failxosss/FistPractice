package com.fistpractice.gui;

import com.fistpractice.FistPractice;
import com.fistpractice.party.Party;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class PartyMenu extends Menu {

    private final FistPractice plugin;

    public PartyMenu(FistPractice plugin) {
        super(plugin.getMenusConfigString("party-menu.title", "<dark_gray>Party"),
                plugin.getMenusConfigInt("party-menu.size", 27));
        this.plugin = plugin;
        build();
    }

    private void build() {
        setItem(10, Material.PLAYER_HEAD, "<white>Members", List.of("<gray>View party members"));
        setItem(11, Material.LIME_DYE, "<white>Invite", List.of("<gray>/party invite <player>"));
        setItem(12, Material.REDSTONE, "<white>Settings", List.of("<gray>Friendly fire, chat toggle"));
        setItem(13, Material.NETHERITE_SWORD, "<white>Party Duel", List.of("<gray>/party duel <party>"));
        setItem(14, Material.COMPASS, "<white>Party Queue", List.of("<gray>/party queue <mode>"));
        setItem(15, Material.WRITABLE_BOOK, "<white>Party Chat", List.of("<gray>Toggle party chat"));
        setItem(16, Material.ANVIL, "<white>Split Teams", List.of("<gray>/party split"));
        setItem(22, Material.RED_DYE, "<white>Leave Party", List.of());
        setItem(24, Material.BARRIER, "<white>Disband Party", List.of("<gray>Leader only"));
    }

    @Override
    public boolean onClick(Player player, int slot) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        switch (slot) {
            case 11 -> player.closeInventory();
            case 15 -> { if (party != null) party.toggleChat(player.getUniqueId()); }
            case 22 -> { if (party != null) { plugin.getPartyManager().removeMember(party, player.getUniqueId()); player.closeInventory(); } }
            case 24 -> { if (party != null && party.canDisbandOrTransfer(player.getUniqueId())) { plugin.getPartyManager().disbandParty(party); player.closeInventory(); } }
            default -> { }
        }
        return true;
    }
}
