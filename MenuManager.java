package com.fistpractice.gui;

import com.fistpractice.FistPractice;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete menus in this build: MainMenu, PartyMenu. The remaining menus
 * listed in the spec (Duel Menu, Ranked/Unranked Menu, Kit Editor, Arena
 * Selector, Map Selector, Profile, Statistics, Match History, Leaderboard,
 * Spectator Menu, Admin Menu) follow the exact same Menu subclass pattern -
 * extend Menu, build() the config-driven items, implement onClick(). See
 * README.md "Extending the GUI" for a worked example.
 */
public class MenuManager {

    private final FistPractice plugin;
    private final Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();

    public MenuManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Menu menu = new MainMenu(plugin);
        openMenus.put(player.getUniqueId(), menu);
        menu.open(player);
    }

    public void openPartyMenu(Player player) {
        Menu menu = new PartyMenu(plugin);
        openMenus.put(player.getUniqueId(), menu);
        menu.open(player);
    }

    public Menu getOpenMenu(UUID uuid) {
        return openMenus.get(uuid);
    }

    public void clear(UUID uuid) {
        openMenus.remove(uuid);
    }
}
