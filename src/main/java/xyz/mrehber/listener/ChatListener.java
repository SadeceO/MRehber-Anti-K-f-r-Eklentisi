package xyz.mrehber.listener;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.mrehber.manager.ConfigManager;
import xyz.mrehber.service.GeminiService;
import xyz.mrehber.service.ModerationService;
import xyz.mrehber.util.KufurDetector;

import java.util.*;
import java.util.concurrent.*;

public class ChatListener implements Listener {

    private static final long HAFIZA_SURESI_MS = 180_000L;
    private static final int OTOMATIK_GEMINI_ARALIK = 50;

    private record ChatEntry(String playerName, String message, long timestamp) {
        String fullLine() { return playerName + ": " + message; }
    }

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final GeminiService geminiService;
    private final ModerationService moderationService;
    private final KufurDetector kufurDetector;

    private final Deque<ChatEntry> messageHistory = new ConcurrentLinkedDeque<>();
    private final Set<String> processingKeys = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Deque<Long>> spamKontrol = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sonOzurlenmeZamani = new ConcurrentHashMap<>();
    private final Set<String> son3DakikadaCezaAlanlar = ConcurrentHashMap.newKeySet();

    private final ExecutorService executor = Executors.newFixedThreadPool(6);
    private final Semaphore geminiRateLimit = new Semaphore(6);

    private int mesajSayaci = 0;

    public ChatListener(JavaPlugin plugin, ConfigManager configManager,
                        GeminiService geminiService, ModerationService moderationService,
                        KufurDetector kufurDetector) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.geminiService = geminiService;
        this.moderationService = moderationService;
        this.kufurDetector = kufurDetector;

        startPeriodicCleanup();
        startOtomatikGeminiKontrol();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String mesaj = event.getMessage().trim();
        if (mesaj.isEmpty()) return;
        if (player.hasPermission("moderasyon.bypass")) return;

        String tamSatir = player.getName() + ": " + mesaj;
        long now = System.currentTimeMillis();

        if (ozurIceriyor(mesaj.toLowerCase())) {
            Long sonZaman = sonOzurlenmeZamani.get(player.getUniqueId());
            if (sonZaman != null && (now - sonZaman) < 3_600_000L) {
                player.sendMessage(configManager.getMesaj("ozur.cooldown", "remaining", String.valueOf((3600000 - (now - sonZaman)) / 60000)));
                event.setCancelled(true);
                return;
            }
            if (moderationService.ozurKabulEt(player)) {
                sonOzurlenmeZamani.put(player.getUniqueId(), now);
                Bukkit.getScheduler().runTask(plugin, () -> ozurEfekti(player));
            }
            return;
        }
        if (!spamKontrolEt(player)) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMesaj("hata.spam"));
            return;
        }
        if (kufurDetector.agirKufurVar(mesaj) || kufurDetector.sunucuHakareti(mesaj)) {
            event.setCancelled(true);
            String fakeYanit = player.getName() + "|BELİRSİZLİK|" + mesaj;
            son3DakikadaCezaAlanlar.add(player.getName().toLowerCase());
            Bukkit.getScheduler().runTask(plugin, () -> {
                moderationService.yanitIsle(fakeYanit, List.of(tamSatir), true);
                cezaEfekti(player);
            });
            return;
        }
        if (kufurDetector.hafifHakaretVar(mesaj)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> moderationService.uyariVer(player, mesaj));
            return;
        }
        synchronized (messageHistory) {
            messageHistory.addLast(new ChatEntry(player.getName(), mesaj, now));
            if (messageHistory.size() > configManager.getMesajGecmisiBoyutu()) {
                messageHistory.removeFirst();
            }
        }

        mesajSayaci++;
    }

    private void startOtomatikGeminiKontrol() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mesajSayaci < OTOMATIK_GEMINI_ARALIK) return;
                mesajSayaci = 0;

                List<String> sonMesajlar;
                synchronized (messageHistory) {
                    sonMesajlar = new ArrayList<>();
                    for (ChatEntry entry : messageHistory) {
                        if (son3DakikadaCezaAlanlar.contains(entry.playerName().toLowerCase())) {
                            continue;
                        }
                        sonMesajlar.add(entry.fullLine());
                    }
                }

                if (sonMesajlar.isEmpty()) return;

                String birlesik = String.join("\n", sonMesajlar);
                String key = "auto_gemini_" + System.currentTimeMillis();

                if (!processingKeys.add(key)) return;

                executor.submit(() -> {
                    try {
                        geminiRateLimit.acquire();
                        String sonuc = geminiService.mesajlariAnaliz(birlesik);
                        if (!sonuc.trim().equals("TEMIZ") && !sonuc.trim().isEmpty()) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                moderationService.yanitIsle(sonuc, sonMesajlar, false);
                                for (String satir : sonuc.split("\n")) {
                                    if (satir.contains("|")) {
                                        String oyuncu = satir.split("\\|")[0].trim();
                                        son3DakikadaCezaAlanlar.add(oyuncu.toLowerCase());
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Otomatik Gemini hatası: " + e.getMessage());
                    } finally {
                        processingKeys.remove(key);
                        geminiRateLimit.release();
                    }
                });
            }
        }.runTaskTimerAsynchronously(plugin, 100L, 100L);
    }

    private boolean spamKontrolEt(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Deque<Long> zamanlar = spamKontrol.computeIfAbsent(uuid, k -> new ConcurrentLinkedDeque<>());
        long cutoff = now - configManager.getSpamZamanAraligi();

        zamanlar.removeIf(t -> t < cutoff);
        if (zamanlar.size() >= configManager.getSpamMaksimumMesaj()) return false;

        zamanlar.addLast(now);
        return true;
    }

    private boolean ozurIceriyor(String msg) {
        String temiz = msg.replaceAll("[\"'\\-.,;!]", "").toLowerCase();
        return temiz.contains("özür") || temiz.contains("özr") || temiz.contains("pardon") ||
                temiz.contains("afedersin") || temiz.contains("kusura bakma") || temiz.contains("üzgünüm") ||
                temiz.contains("kusur") || temiz.contains("affedersin");
    }

    private void ozurEfekti(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        p.spawnParticle(Particle.HEART, p.getLocation().add(0, 2, 0), 20, 0.5, 0.5, 0.5, 0.1);
    }

    private void cezaEfekti(Player p) {
        p.spawnParticle(Particle.ANGRY_VILLAGER, p.getLocation().add(0, 2, 0), 60, 0.6, 0.6, 0.6, 0);
        p.spawnParticle(Particle.LARGE_SMOKE, p.getLocation().add(0, 1.5, 0), 50, 0.6, 0.6, 0.6, 0.08);
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_ROAR, 1.3f, 0.8f);
    }

    public void oyuncuBildir(String hedefAdi, Player bildiren) {
        if (!bildiren.hasPermission("moderasyon.bildir")) {
            bildiren.sendMessage(configManager.getMesaj("hata.yetki-yok"));
            return;
        }

        List<String> sonMesajlar;
        synchronized (messageHistory) {
            sonMesajlar = messageHistory.stream()
                    .map(ChatEntry::fullLine)
                    .limit(200)
                    .toList();
        }

        if (sonMesajlar.isEmpty()) {
            bildiren.sendMessage(configManager.getMesaj("bildir.mesaj-yok"));
            return;
        }

        String tum = String.join("\n", sonMesajlar);
        String key = "report_" + bildiren.getUniqueId();

        if (!processingKeys.add(key)) {
            bildiren.sendMessage(configManager.getMesaj("bildir.zaten-isleniyor"));
            return;
        }

        bildiren.sendMessage(configManager.getMesaj("bildir.basladi"));

        executor.submit(() -> {
            try {
                geminiRateLimit.acquire();
                String sonuc = geminiService.mesajlariAnaliz(tum);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    boolean ihlal = !sonuc.trim().equals("TEMIZ") && !sonuc.trim().isEmpty();
                    moderationService.yanitIsle(sonuc, sonMesajlar, false);
                    bildiren.sendMessage(ihlal
                            ? configManager.getMesaj("bildir.sonuc-ihlal")
                            : configManager.getMesaj("bildir.sonuc-temiz"));
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> bildiren.sendMessage(configManager.getMesaj("bildir.hata")));
            } finally {
                processingKeys.remove(key);
                geminiRateLimit.release();
            }
        });
    }

    private void startPeriodicCleanup() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long cutoff = System.currentTimeMillis() - HAFIZA_SURESI_MS;
                synchronized (messageHistory) {
                    while (!messageHistory.isEmpty() && messageHistory.getFirst().timestamp < cutoff) {
                        messageHistory.removeFirst();
                    }
                }
                son3DakikadaCezaAlanlar.removeIf(name -> {
                    return true;
                });
            }
        }.runTaskTimerAsynchronously(plugin, 600L, 600L);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}