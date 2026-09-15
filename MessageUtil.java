package com.fistpractice.utilities;

import com.fistpractice.FistPractice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

/**
 * Centralised MiniMessage formatting + messages.yml lookup so every command
 * and manager sends consistent, fully player-configurable text (spec 57).
 */
public final class MessageUtil {

    private static FistPractice plugin;
    private static BukkitAudiences audiences;
    private static YamlConfiguration messages;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MessageUtil() {}

    public static void init(FistPractice pluginInstance) {
        plugin = pluginInstance;
        audiences = BukkitAudiences.create(pluginInstance);
        reload();
    }

    public static void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public static void shutdown() {
        if (audiences != null) audiences.close();
    }

    private static String prefix() {
        return messages.getString("prefix", "");
    }

    public static Component render(String miniMessageString) {
        return MM.deserialize(miniMessageString);
    }

    public static void send(CommandSender sender, String miniMessageString) {
        audiences.sender(sender).sendMessage(render(prefix() + miniMessageString));
    }

    public static void sendRaw(CommandSender sender, String miniMessageString) {
        audiences.sender(sender).sendMessage(render(miniMessageString));
    }

    /** Looks a dotted key up in messages.yml (e.g. "duel.request-sent") and sends it, prefixed. */
    public static void sendKey(CommandSender sender, String path) {
        sendKey(sender, path, Map.of());
    }

    public static void sendKey(CommandSender sender, String path, Map<String, String> placeholders) {
        String raw = messages.getString(path);
        if (raw == null) raw = "<red>Missing message: " + path;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        }
        send(sender, raw);
    }

    public static void actionBar(org.bukkit.entity.Player player, String miniMessageString) {
        audiences.player(player).sendActionBar(render(miniMessageString));
    }
}
