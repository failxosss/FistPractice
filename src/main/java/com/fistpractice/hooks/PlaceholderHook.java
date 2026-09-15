package com.fistpractice.hooks;

import com.fistpractice.FistPractice;
import com.fistpractice.match.Match;
import com.fistpractice.party.Party;
import com.fistpractice.profile.ModeStats;
import com.fistpractice.profile.PlayerProfile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderHook extends PlaceholderExpansion {

    private final FistPractice plugin;

    public PlaceholderHook(FistPractice plugin) {
        this.plugin = plugin;
    }

    // register() is inherited from PlaceholderExpansion - call it directly, no override needed.

    @Override
    public @NotNull String getIdentifier() { return "fistpractice"; }
    @Override
    public @NotNull String getAuthor() { return "FistPractice Team"; }
    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(org.bukkit.entity.Player player, @NotNull String params) {
        if (player == null) return "";
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());

        if (params.equals("wins")) return profile == null ? "0" : String.valueOf(profile.getTotalWins());
        if (params.equals("losses")) return profile == null ? "0" : String.valueOf(profile.getTotalLosses());
        if (params.equals("winrate")) {
            if (profile == null) return "0%";
            int total = profile.getTotalWins() + profile.getTotalLosses();
            double rate = total == 0 ? 0 : (double) profile.getTotalWins() / total * 100.0;
            return String.format("%.1f%%", rate);
        }
        if (params.startsWith("elo_")) {
            String mode = params.substring("elo_".length()).toUpperCase();
            if (profile == null) return String.valueOf(plugin.getRatingManager().getStartingElo());
            return String.valueOf(profile.getModeStats(mode, plugin.getRatingManager().getStartingElo()).getElo());
        }
        if (params.equals("elo")) {
            if (profile == null) return String.valueOf(plugin.getRatingManager().getStartingElo());
            ModeStats stats = profile.getModeStats("CLASSIC", plugin.getRatingManager().getStartingElo());
            return String.valueOf(stats.getElo());
        }
        if (params.equals("streak")) {
            if (profile == null) return "0";
            return String.valueOf(profile.getModeStats("CLASSIC", plugin.getRatingManager().getStartingElo()).getCurrentStreak());
        }
        if (params.equals("best_streak")) {
            if (profile == null) return "0";
            return String.valueOf(profile.getModeStats("CLASSIC", plugin.getRatingManager().getStartingElo()).getBestStreak());
        }
        if (params.equals("rank")) {
            if (profile == null) return "Unranked";
            int elo = profile.getModeStats("CLASSIC", plugin.getRatingManager().getStartingElo()).getElo();
            return plugin.getRatingManager().getFormattedDivision(elo);
        }
        if (params.equals("party")) {
            Party party = plugin.getPartyManager().getParty(player.getUniqueId());
            return party == null ? "None" : party.getName();
        }
        if (params.equals("party_members")) {
            Party party = plugin.getPartyManager().getParty(player.getUniqueId());
            return party == null ? "0" : String.valueOf(party.size());
        }
        if (params.equals("queue")) {
            return plugin.getQueueManager().isQueued(player.getUniqueId()) ? "In Queue" : "Not Queued";
        }
        if (params.equals("match")) {
            return plugin.getMatchManager().isInMatch(player.getUniqueId()) ? "In Match" : "Not In Match";
        }
        if (params.equals("opponent")) {
            Match match = plugin.getMatchManager().getMatchOf(player.getUniqueId());
            if (match == null) return "None";
            var side = match.getSideOf(player.getUniqueId());
            var opposing = match.getOpposingSide(side);
            if (opposing == null || opposing.getMembers().isEmpty()) return "None";
            OfflinePlayer opponent = plugin.getServer().getOfflinePlayer(opposing.getMembers().iterator().next());
            return opponent.getName() == null ? "Unknown" : opponent.getName();
        }
        return null;
    }
}
