package xyz.mrehber.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.mrehber.manager.ConfigManager;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class DiscordService {

    private static final int MAX_RETRY = 3;
    private static final int TIMEOUT = 5000;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public DiscordService(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void bildirimGonder(String oyuncuAdi, String mesaj, String islem, String sure) {
        if (!configManager.isDiscordAktif()) return;

        Player oyuncu = Bukkit.getPlayer(oyuncuAdi);
        if (oyuncu != null && oyuncu.isOp()) return;

        String webhook = configManager.getDiscordWebhookAdresi();
        if (webhook == null || webhook.isEmpty()) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                webhookGonderRetry(webhook, oyuncuAdi, mesaj, islem, sure);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void webhookGonderRetry(String webhookUrl, String oyuncuAdi, String mesaj, String islem, String sure) {
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                if (webhookGonder(webhookUrl, oyuncuAdi, mesaj, islem, sure)) {
                    return;
                }

                if (i < MAX_RETRY - 1) {
                    Thread.sleep(1000 * (i + 1));
                }
            } catch (Exception e) {
                if (i == MAX_RETRY - 1) {
                    plugin.getLogger().warning("Discord webhook hatası: " + e.getMessage());
                }
            }
        }
    }

    private boolean webhookGonder(String webhookUrl, String oyuncuAdi, String mesaj, String islem, String sure) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            JsonObject json = embedOlustur(oyuncuAdi, mesaj, islem, sure);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            return responseCode == 204 || responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private JsonObject embedOlustur(String oyuncuAdi, String mesaj, String islem, String sure) {
        JsonObject embed = new JsonObject();
        String baslik = temizle(configManager.getDiscord("discord.baslik"));
        String footer = temizle(configManager.getDiscord("discord.footer"));

        embed.addProperty("title", baslik);
        embed.addProperty("description", aciklamaOlustur(oyuncuAdi, mesaj, islem, sure));
        embed.addProperty("color", renkAl(islem));
        embed.addProperty("timestamp", Instant.now().toString());

        JsonObject footerObj = new JsonObject();
        footerObj.addProperty("text", footer);
        embed.add("footer", footerObj);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);

        JsonObject payload = new JsonObject();
        payload.add("embeds", embeds);

        return payload;
    }

    private String aciklamaOlustur(String oyuncuAdi, String mesaj, String islem, String sure) {
        String oyuncuBaslik = temizle(configManager.getDiscord("discord.alan-oyuncu"));
        String mesajBaslik  = temizle(configManager.getDiscord("discord.alan-mesaj"));
        String islemBaslik  = temizle(configManager.getDiscord("discord.alan-islem"));
        String sureBaslik   = temizle(configManager.getDiscord("discord.alan-sure"));
        String kisaMesaj = mesaj.length() > 1000 ? mesaj.substring(0, 997) + "..." : mesaj;
        return String.format(
                "**%s** %s\n**%s** `%s`\n**%s** %s\n**%s** %s",
                oyuncuBaslik, oyuncuAdi,
                mesajBaslik,  kisaMesaj,
                islemBaslik,  islem,
                sureBaslik,   sure
        );
    }

    private int renkAl(String islem) {
        String lower = islem.toLowerCase();
        if (lower.contains("3. derece") || lower.contains("belirsizlik")) return 0xFF0000;
        if (lower.contains("2. derece")) return 0xFF6600;
        if (lower.contains("1. derece")) return 0xFFCC00;
        if (lower.contains("uyarı"))     return 0x00CCFF;
        return 0x808080;
    }

    private String temizle(String metin) {
        if (metin == null) return "";
        return metin
                .replaceAll("(?i)§[0-9a-fk-or]", "")
                .replaceAll("(?i)&[0-9a-fk-or]", "")
                .trim();
    }
}