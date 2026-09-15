package com.fistpractice.hooks;

import com.fistpractice.FistPractice;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final FistPractice plugin;

    public DiscordWebhook(FistPractice plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("discord.enabled", false)
                && !plugin.getConfig().getString("discord.webhook-url", "").isEmpty();
    }

    public void sendMatchStarted(String player1, String player2, String mode) {
        send("**Match started:** " + player1 + " vs " + player2 + " (" + mode + ")");
    }

    public void sendMatchEnded(String winner, String loser, String mode) {
        send("**Match ended:** " + winner + " defeated " + loser + " (" + mode + ")");
    }

    public void sendRankedResult(String winner, String loser, String mode, int eloDelta) {
        send("**Ranked result:** " + winner + " beat " + loser + " in " + mode + " (+" + eloDelta + " ELO)");
    }

    public void sendTournamentResult(String tournamentId, String winner) {
        send("**Tournament finished:** `" + tournamentId + "` won by " + winner);
    }

    public void sendDivisionAchieved(String player, String division) {
        send("**New division:** " + player + " reached " + division + "!");
    }

    private void send(String content) {
        if (!enabled()) return;
        // Fire-and-forget on an async thread so we never block the main thread on network I/O.
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(plugin.getConfig().getString("discord.webhook-url"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String json = "{\"content\": \"" + content.replace("\"", "\\\"") + "\"}";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                conn.getResponseCode(); // triggers the request
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Discord webhook failed: " + e.getMessage());
            }
        });
    }
}
