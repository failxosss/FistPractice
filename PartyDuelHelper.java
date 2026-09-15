package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.configuration.GameMode;
import com.fistpractice.kit.Kit;
import com.fistpractice.match.Match;
import com.fistpractice.match.MatchSide;
import com.fistpractice.match.MatchType;
import com.fistpractice.party.Party;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyDuelHelper {

    private final FistPractice plugin;

    public PartyDuelHelper(FistPractice plugin) {
        this.plugin = plugin;
    }

    /** /party duel <otherPartyName> [mode] [bestOf] [ranked] */
    public void handleDuel(Player leader, Party party, String[] args) {
        if (party == null || !party.canManage(leader.getUniqueId())) {
            MessageUtil.sendKey(leader, "general.no-permission");
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendKey(leader, "general.invalid-usage", Map.of("usage", "/party duel <party> [mode] [bestOf] [ranked]"));
            return;
        }
        Party opponent = plugin.getPartyManager().findByName(args[1]);
        if (opponent == null || opponent == party) {
            MessageUtil.sendKey(leader, "general.player-not-found");
            return;
        }

        String modeId = args.length >= 3 ? args[2].toUpperCase() : "NODEBUFF";
        int bestOf = args.length >= 4 ? parseIntOr(args[3], 1) : 1;
        boolean ranked = args.length >= 5 && args[4].equalsIgnoreCase("ranked");

        GameMode mode = plugin.getGameModeManager().get(modeId);
        Kit kit = plugin.getKitManager().get("default");
        if (mode == null || kit == null) {
            MessageUtil.sendKey(leader, "match.mode-disabled");
            return;
        }

        MatchSide sideA = new MatchSide(party.getName());
        for (UUID uuid : party.getMembers()) sideA.addMember(uuid);
        MatchSide sideB = new MatchSide(opponent.getName());
        for (UUID uuid : opponent.getMembers()) sideB.addMember(uuid);

        Match match = plugin.getMatchManager().startMatch(MatchType.PARTY_VS_PARTY, mode, kit,
                List.of(sideA, sideB), bestOf, ranked && mode.isRanked(), party.isFriendlyFire());
        if (match == null) return;

        notifyAll(party, "<gold>Party duel started against <white>" + opponent.getName());
        notifyAll(opponent, "<gold>Party duel started against <white>" + party.getName());
    }

    /** /party queue <mode> [ranked] */
    public void handleQueue(Player leader, Party party, String[] args) {
        if (party == null || !party.canManage(leader.getUniqueId())) {
            MessageUtil.sendKey(leader, "general.no-permission");
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendKey(leader, "general.invalid-usage", Map.of("usage", "/party queue <mode> [ranked]"));
            return;
        }
        String modeId = args[1];
        boolean ranked = args.length >= 3 && args[2].equalsIgnoreCase("ranked");
        String error = plugin.getQueueManager().joinParty(new ArrayList<>(party.getMembers()), modeId, ranked);
        if (error != null) {
            MessageUtil.sendKey(leader, error);
            return;
        }
        notifyAll(party, "<green>Your party joined the <white>" + modeId + "</white> queue (" + (ranked ? "Ranked" : "Unranked") + ").");
    }

    private int parseIntOr(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private void notifyAll(Party party, String message) {
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtil.sendRaw(p, message);
        }
    }
}
