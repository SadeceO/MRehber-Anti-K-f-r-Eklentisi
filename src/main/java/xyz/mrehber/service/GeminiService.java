package xyz.mrehber.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.mrehber.manager.ConfigManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class GeminiService {

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final int BAGLANTI_ZAMAN_ASIMI = 20000;
    private static final int OKUMA_ZAMAN_ASIMI = 30000;
    private static final int MAX_RETRY = 3;

    private final ConfigManager configManager;
    private final String sunucuAdi;

    public GeminiService(ConfigManager configManager, String sunucuAdi) {
        this.configManager = configManager;
        this.sunucuAdi = sunucuAdi.toLowerCase();
    }

    public String mesajlariAnaliz(String mesajlar) throws IOException {
        String istem = istemOlustur(mesajlar);
        return geminiIstegiRetry(istem);
    }

    private String istemOlustur(String mesajlar) {
        return configManager.getGeminiIstem()
                .replace("{server_name}", sunucuAdi)
                .replace("{messages}", mesajlar);
    }

    private String geminiIstegiRetry(String istem) throws IOException {
        IOException lastException = null;

        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return geminiIstegi(istem);
            } catch (IOException e) {
                lastException = e;
                if (i < MAX_RETRY - 1) {
                    try {
                        Thread.sleep(1000 * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("İstek iptal edildi", ie);
                    }
                }
            }
        }

        throw lastException;
    }

    private String geminiIstegi(String istem) throws IOException {
        String apiAnahtari = configManager.getGeminiApiAnahtari();

        if (apiAnahtari == null || apiAnahtari.isEmpty()) {
            throw new IOException("Gemini API anahtarı bulunamadı!");
        }

        URL url = new URL(GEMINI_API_URL + "?key=" + apiAnahtari);
        HttpURLConnection baglanti = (HttpURLConnection) url.openConnection();

        baglantiAyarla(baglanti);
        istekGonder(baglanti, istem);

        return yanitOku(baglanti);
    }

    private void baglantiAyarla(HttpURLConnection baglanti) throws IOException {
        baglanti.setRequestMethod("POST");
        baglanti.setRequestProperty("Content-Type", "application/json");
        baglanti.setDoOutput(true);
        baglanti.setConnectTimeout(BAGLANTI_ZAMAN_ASIMI);
        baglanti.setReadTimeout(OKUMA_ZAMAN_ASIMI);
    }

    private void istekGonder(HttpURLConnection baglanti, String istem) throws IOException {
        JsonObject veri = istekVerisiOlustur(istem);

        try (OutputStream os = baglanti.getOutputStream()) {
            byte[] girdi = veri.toString().getBytes(StandardCharsets.UTF_8);
            os.write(girdi, 0, girdi.length);
        }
    }

    private JsonObject istekVerisiOlustur(String istem) {
        JsonObject veri = new JsonObject();
        JsonArray icerikler = new JsonArray();
        JsonObject icerik = new JsonObject();
        JsonArray parcalar = new JsonArray();
        JsonObject parca = new JsonObject();

        parca.addProperty("text", istem);
        parcalar.add(parca);
        icerik.add("parts", parcalar);
        icerikler.add(icerik);
        veri.add("contents", icerikler);

        return veri;
    }

    private String yanitOku(HttpURLConnection baglanti) throws IOException {
        int yanitKodu = baglanti.getResponseCode();

        if (yanitKodu != HttpURLConnection.HTTP_OK) {
            String errorMsg = "Bilinmeyen hata";
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(baglanti.getErrorStream(), StandardCharsets.UTF_8))) {
                errorMsg = reader.lines().collect(Collectors.joining());
            } catch (Exception ignored) {}

            throw new IOException("Gemini API hatası: " + yanitKodu + " - " + errorMsg);
        }

        try (BufferedReader okuyucu = new BufferedReader(
                new InputStreamReader(baglanti.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder yanitMetni = new StringBuilder();
            String satir;

            while ((satir = okuyucu.readLine()) != null) {
                yanitMetni.append(satir.trim());
            }

            return yanitParse(yanitMetni.toString());
        }
    }

    private String yanitParse(String jsonYanit) throws IOException {
        try {
            JsonObject yanitNesnesi = JsonParser.parseString(jsonYanit).getAsJsonObject();

            return yanitNesnesi.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            throw new IOException("Gemini yanıtı parse edilemedi: " + e.getMessage());
        }
    }
}