package xyz.mrehber.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.mrehber.listener.ChatListener;
import xyz.mrehber.manager.ConfigManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReportCommand implements CommandExecutor {

    private static final long BILDIRI_COOLDOWN = 180_000L;

    private final ChatListener chatListener;
    private final ConfigManager configManager;
    private final Map<String, Long> sonBildiriZamani = new ConcurrentHashMap<>();

    public ReportCommand(ChatListener chatListener, ConfigManager configManager) {
        this.chatListener = chatListener;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getMesaj("bildir.sadece-oyuncu"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(configManager.getMesaj("bildir.kullanim"));
            return true;
        }

        String hedefAdi = args[0];
        Player hedef = Bukkit.getPlayerExact(hedefAdi);

        if (hedef == null || !hedef.isOnline()) {
            player.sendMessage(configManager.getMesaj("bildir.oyuncu-yok"));
            return true;
        }

        String oyuncuAdi = player.getName();
        long simdikiZaman = System.currentTimeMillis();
        if (sonBildiriZamani.containsKey(oyuncuAdi)) {
            long gecenSure = simdikiZaman - sonBildiriZamani.get(oyuncuAdi);
            long kalanMs = BILDIRI_COOLDOWN - gecenSure;

            if (kalanMs > 0) {
                String kalanMetin = formatSure(kalanMs);
                player.sendMessage(configManager.getMesaj("bildir.cooldown", "sure", kalanMetin));
                return true;
            }
        }

        sonBildiriZamani.put(oyuncuAdi, simdikiZaman);
        player.sendMessage(configManager.getMesaj("bildir.isleniyor"));

        chatListener.oyuncuBildir(hedefAdi, player);

        return true;
    }

    private String formatSure(long millis) {
        long saniye = millis / 1000;
        long dakika = saniye / 60;
        long kalanSaniye = saniye % 60;

        if (dakika == 0) {
            return kalanSaniye + " saniye";
        } else if (kalanSaniye == 0) {
            return dakika + " dakika";
        } else {
            return dakika + " dakika " + kalanSaniye + " saniye";
        }
    }
}