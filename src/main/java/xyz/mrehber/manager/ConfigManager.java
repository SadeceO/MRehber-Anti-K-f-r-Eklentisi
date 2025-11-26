package xyz.mrehber.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;

    private boolean geminiAktif;
    private String geminiApiAnahtari;
    private String geminiIstem;

    private boolean discordAktif;
    private String discordWebhookAdresi;

    private int topluKontrolMesajSayisi;
    private int spamMaksimumMesaj;
    private long spamZamanAraligi;
    private int mesajGecmisiBoyutu;

    private String birinciDereceSure;
    private String birinciDereceSebep;
    private String birinciDereceKomut;

    private String ikinciDereceSure;
    private String ikinciDereceSebep;
    private String ikinciDereceKomut;

    private String ucuncuDereceSure;
    private String ucuncuDereceSebep;
    private String ucuncuDereceKomut;

    private String belirsizlikSure;
    private String belirsizlikSebep;
    private String belirsizlikKomut;

    private int maksimumUyari;
    private String uyariSonrasiSure;
    private String uyariSonrasiSebep;
    private String uyariSonrasiKomut;

    private final String VARSAYILAN_ISTEM = """
            Aşağıdaki Minecraft sohbet mesajlarını analiz et. Sadece küfür/hakaret içerenleri raporla.

            Derecelendirme:
            - 1. DERECE → Normal küfür (sik, göt, am, orospu vb.)
            - 2. DERECE → Aileye küfür (ananı, bacını, babanı vb.)
            - 3. DERECE → Din, ırk, Atatürk, {server_name} gibi sunucuya hakaret
            - UYARI      → Hafif hakaret (mal, salak, aptal, ezik vb.)

            ÖNEMLİ:
            - aq, sq, wtf, lol gibi kısaltmaları küfür sayMA!
            - qmk → amk, qnqnıskm → ananıskm gibi şifreli küfürleri çöz ve tespit et!
            - Aynı oyuncudan birden fazla ihlal varsa sadece en ağır olanını yaz!
            - Her oyuncu için tek satır döndür!

            Format: OYUNCU_ADI|DERECE|ORİJİNAL_MESAJ
            Hiç ihlal yoksa sadece: TEMIZ

            Mesajlar:
            {messages}
            """;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        yukle();
    }

    public void yukle() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        mesajDosyasiniYukle();

        geminiAktif = config.getBoolean("sohbet-moderasyonu.gemini.aktif", false);
        geminiApiAnahtari = config.getString("sohbet-moderasyonu.gemini.api-anahtari", "");
        geminiIstem = config.getString("sohbet-moderasyonu.gemini.istem", VARSAYILAN_ISTEM);
        if (geminiIstem.isEmpty()) geminiIstem = VARSAYILAN_ISTEM;

        discordAktif = config.getBoolean("sohbet-moderasyonu.discord.aktif", false);
        discordWebhookAdresi = config.getString("sohbet-moderasyonu.discord.webhook-adresi", "");

        topluKontrolMesajSayisi = Math.max(1, config.getInt("sohbet-moderasyonu.ayarlar.toplu-kontrol-mesaj-sayisi", 5));
        spamMaksimumMesaj = Math.max(1, config.getInt("sohbet-moderasyonu.ayarlar.spam-maksimum-mesaj", 8));
        spamZamanAraligi = Math.max(1000, config.getLong("sohbet-moderasyonu.ayarlar.spam-zaman-araligi", 10000));
        mesajGecmisiBoyutu = Math.max(10, config.getInt("sohbet-moderasyonu.ayarlar.mesaj-gecmisi-boyutu", 50));

        maksimumUyari = Math.max(1, config.getInt("uyari.maksimum-uyari", 3));

        birinciDereceSure = config.getString("cezalar.birinci-derece.sure", "15m");
        birinciDereceSebep = config.getString("cezalar.birinci-derece.sebep", "1. dereceden küfür");
        birinciDereceKomut = config.getString("cezalar.birinci-derece.komut", "tempmute {oyuncu} {sure} {sebep}");

        ikinciDereceSure = config.getString("cezalar.ikinci-derece.sure", "30m");
        ikinciDereceSebep = config.getString("cezalar.ikinci-derece.sebep", "2. dereceden küfür");
        ikinciDereceKomut = config.getString("cezalar.ikinci-derece.komut", "tempmute {oyuncu} {sure} {sebep}");

        ucuncuDereceSure = config.getString("cezalar.ucuncu-derece.sure", "7d");
        ucuncuDereceSebep = config.getString("cezalar.ucuncu-derece.sebep", "3. dereceden küfür");
        ucuncuDereceKomut = config.getString("cezalar.ucuncu-derece.komut", "tempban {oyuncu} {sure} {sebep}");

        belirsizlikSure = config.getString("cezalar.belirsizlik.sure", "5m");
        belirsizlikSebep = config.getString("cezalar.belirsizlik.sebep", "Hızlı Tespit Sistemi");
        belirsizlikKomut = config.getString("cezalar.belirsizlik.komut", "tempmute {oyuncu} {sure} {sebep}");

        uyariSonrasiSure = config.getString("cezalar.uyari-sonrasi.sure", "15m");
        uyariSonrasiSebep = config.getString("cezalar.uyari-sonrasi.sebep", "3 uyarı sonrası otomatik ceza");
        uyariSonrasiKomut = config.getString("cezalar.uyari-sonrasi.komut", "tempban {oyuncu} {sure} {sebep}");

        if (geminiAktif && (geminiApiAnahtari == null || geminiApiAnahtari.isBlank())) {
            plugin.getLogger().warning("Gemini aktif ama API anahtarı yok! Devre dışı bırakılıyor.");
            geminiAktif = false;
        }

        if (discordAktif && (discordWebhookAdresi == null || discordWebhookAdresi.isBlank())) {
            plugin.getLogger().warning("Discord aktif ama webhook yok! Devre dışı bırakılıyor.");
            discordAktif = false;
        }
    }

    private void mesajDosyasiniYukle() {
        File mesajDosyasi = new File(plugin.getDataFolder(), "messages.yml");
        if (!mesajDosyasi.exists()) {
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) Files.copy(in, mesajDosyasi.toPath());
            } catch (IOException e) {
                plugin.getLogger().severe("messages.yml oluşturulamadı: " + e.getMessage());
            }
        }
        messages = YamlConfiguration.loadConfiguration(mesajDosyasi);
    }

    public String getMesaj(String yol, String... degiskenler) {
        String raw = messages.getString("mesajlar." + yol, "§cMesaj bulunamadı: " + yol);
        String prefix = messages.getString("mesajlar.prefix", "");
        String mesaj = ChatColor.translateAlternateColorCodes('&', prefix + raw);

        if (degiskenler.length % 2 == 0) {
            for (int i = 0; i < degiskenler.length; i += 2) {
                mesaj = mesaj.replace("{" + degiskenler[i] + "}", degiskenler[i + 1]);
            }
        }
        return mesaj;
    }

    public String getTitle(String yol) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString("mesajlar." + yol, "§cTitle: " + yol));
    }

    public String getDiscord(String yol) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString("mesajlar." + yol, "§cDiscord: " + yol));
    }

    public String getSubtitle(String yol, String... degiskenler) {
        String raw = messages.getString("mesajlar." + yol, "§cSubtitle: " + yol);
        String mesaj = ChatColor.translateAlternateColorCodes('&', raw);
        if (degiskenler.length % 2 == 0) {
            for (int i = 0; i < degiskenler.length; i += 2) {
                mesaj = mesaj.replace("{" + degiskenler[i] + "}", degiskenler[i + 1]);
            }
        }
        return mesaj;
    }

    public boolean isGeminiAktif() { return geminiAktif; }
    public String getGeminiApiAnahtari() { return geminiApiAnahtari; }
    public String getGeminiIstem() { return geminiIstem; }
    public boolean isDiscordAktif() { return discordAktif; }
    public String getDiscordWebhookAdresi() { return discordWebhookAdresi; }
    public int getTopluKontrolMesajSayisi() { return topluKontrolMesajSayisi; }
    public int getSpamMaksimumMesaj() { return spamMaksimumMesaj; }
    public long getSpamZamanAraligi() { return spamZamanAraligi; }
    public int getMesajGecmisiBoyutu() { return mesajGecmisiBoyutu; }
    public int getMaksimumUyari() { return maksimumUyari; }

    public String getBirinciDereceSure() { return birinciDereceSure; }
    public String getBirinciDereceSebep() { return birinciDereceSebep; }
    public String getBirinciDereceKomut() { return birinciDereceKomut; }

    public String getIkinciDereceSure() { return ikinciDereceSure; }
    public String getIkinciDereceSebep() { return ikinciDereceSebep; }
    public String getIkinciDereceKomut() { return ikinciDereceKomut; }

    public String getUcuncuDereceSure() { return ucuncuDereceSure; }
    public String getUcuncuDereceSebep() { return ucuncuDereceSebep; }
    public String getUcuncuDereceKomut() { return ucuncuDereceKomut; }

    public String getBelirsizlikSure() { return belirsizlikSure; }
    public String getBelirsizlikSebep() { return belirsizlikSebep; }
    public String getBelirsizlikKomut() { return belirsizlikKomut; }

    public String getUyariSonrasiSure() { return uyariSonrasiSure; }
    public String getUyariSonrasiSebep() { return uyariSonrasiSebep; }
    public String getUyariSonrasiKomut() { return uyariSonrasiKomut; }
}