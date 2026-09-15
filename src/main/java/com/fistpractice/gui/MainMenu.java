package com.fistpractice.gui;

import com.fistpractice.FistPractice;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class MainMenu extends Menu {

    private final FistPractice plugin;

    public MainMenu(FistPractice plugin) {
        super(plugin.getMenusConfigString("main-menu.title", "<dark_gray>Ultimate<gold>Practice"),
                plugin.getMenusConfigInt("main-menu.size", 27));
        this.plugin = plugin;
        build();
    }

    private void build() {
        setItem(10, Material.DIAMOND_SWORD, "<white>Duels", List.of("<gray>Challenge another player"));
        setItem(12, Material.PLAYER_HEAD, "<white>Party", List.of("<gray>Manage your party"));
        setItem(14, Material.COMPASS, "<white>Queues", List.of("<gray>Join matchmaking"));
        setItem(16, Material.BOOK, "<white>Profile", List.of("<gray>View your stats"));
    }

    @Override
    public boolean onClick(Player player, int slot) {
        switch (slot) {
            case 10 -> player.performCommand("duel");
            case 12 -> plugin.getMenuManager().openPartyMenu(player);
            case 14 -> player.performCommand("queue");
            case 16 -> player.performCommand("profile");
            default -> { }
        }
        return true;
    }
}
