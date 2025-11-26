package xyz.mrehber.util;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.mrehber.manager.ConfigManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class KufurDetector {

    private final JavaPlugin plugin;
    private final String sunucuAdi;

    private final List<String> agirKufurler = new CopyOnWriteArrayList<>();
    private final List<String> normalizeAgirKufurler = new CopyOnWriteArrayList<>();
    private final List<String> hafifHakaretler = new CopyOnWriteArrayList<>();
    private final List<String> normalizeHafifHakaretler = new CopyOnWriteArrayList<>();
    private final Set<String> whitelist = new HashSet<>();

    private long sonYuklemeZamani = 0;
    private static final long CACHE_SURESI = 300_000L;

    public KufurDetector(JavaPlugin plugin, ConfigManager configManager, String sunucuAdi) {
        this.plugin = plugin;
        this.sunucuAdi = sunucuAdi.toLowerCase();
        varsayilanDosyalariOlustur();
        yukleListeler();
        yukleWhitelist();
    }

    private void varsayilanDosyalariOlustur() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        olusturAgirKufurDosyasi();
        olusturHakaretDosyasi();
        olusturWhitelistDosyasi();
    }

    private void olusturAgirKufurDosyasi() {
        File dosya = new File(plugin.getDataFolder(), "kufurler_agir.yml");
        if (dosya.exists()) return;

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(dosya), StandardCharsets.UTF_8))) {

            writer.println("# ═════════════════════════════════════");
            writer.println("# AĞIR KÜFÜRLER - Bu kelimeler MUTE/BAN cezası verir");
            writer.println("# Her satıra bir kelime yazın");
            writer.println("# # ile başlayan satırlar yorum satırıdır");
            writer.println("# ═════════════════════════════════════");
            writer.println();

            String[] agirlar = {
                    "abaza", "abazan", "ag", "agzina sicayim", "allah", "allahsiz", "am", "amarim",
                    "ambiti", "am biti", "amcigi", "amcigin", "amcigini", "amcik", "amcuk", "amik",
                    "amina", "aminako", "amina koy", "amina koyarim", "amina koyayim", "aminakoyim",
                    "amina koyyim", "amina s", "amina sikem", "amina sokam", "amin feryadi", "amini",
                    "aminiyarraaniskiim", "aminoglu", "amin oglu", "amiyum", "amk", "amkafa",
                    "amk cocugu", "ammak", "ammna", "amn", "amna", "amnda", "amndaki", "amngtn",
                    "amnn", "amona", "amq", "amsiz", "amsz", "amteri", "amuga", "amugaa", "amuna",
                    "ana", "anaaann", "anal", "analarn", "anam", "anamla", "anan", "anana", "anandan",
                    "anani", "ananin", "ananin am", "ananin ami", "ananin dolu", "ananinki",
                    "ananisikerim", "anani sikerim", "ananisikeyim", "anani sikeyim", "ananizin",
                    "ananizin am", "anann", "ananz", "anas", "anasini", "anasinin am", "anasi orospu",
                    "anasi", "anasinin", "anay", "anayin", "anneni", "annenin", "annesiz", "anuna",
                    "aq", "a.q", "a.q.", "aq.", "ass", "atkafasi", "atmik", "attirdigim", "attrrm",
                    "auzlu", "avrat", "ayklarmalrmsikerim", "azdim", "azdir", "azdirici",
                    "babaannesi kasar", "babani", "babanin", "bacina", "bacini", "bacinin", "bacn",
                    "bacndan", "bacy", "bastard", "basur", "basi pezevenk", "bagina sicayim",
                    "bitch", "biting", "bok", "boka", "bokbok", "bokca", "bokhu", "bokkkumu",
                    "boklar", "boktan", "boku", "bokubokuna", "bokum", "bombok", "boner",
                    "bosalmak", "cenabet", "cibiliyetsiz", "cibilliyetini", "cibilliyetsiz",
                    "cif", "cikar", "cim", "cuk", "dalaksiz", "dallama", "daltassak", "dalyarak",
                    "dalyarrak", "dassagi", "diktim", "dildo", "dingil", "dingilini", "dinsiz",
                    "dkerim", "domal", "domalan", "domaldi", "domaldin", "domalik", "domaliyor",
                    "domalmak", "domalmis", "domalsin", "domalt", "domaltarak", "domaltip",
                    "domaltir", "domaltirim", "domaltmak", "dolu", "donek", "duduk", "eben",
                    "ebeni", "ebenin", "ebeninki", "ebleh", "ecdadini", "embesil", "emi",
                    "fahise", "feristah", "ferre", "fuck", "fucker", "fuckin", "fucking",
                    "gavad", "gavat", "geber", "geberik", "gebermek", "gebermis", "gebertir",
                    "giberim", "giberler", "gibis", "gibmek", "gibtiler", "goddamn", "godos",
                    "godumun", "gotelek", "gotlalesi", "gotlu", "gotten", "gotundeki", "gotunden",
                    "gotune", "gotunu", "gotveren", "goyiim", "goyum", "goyuyim", "goyyim",
                    "got", "got deligi", "gotele k", "got herif", "gotlek", "gotog lani",
                    "goto s", "gotu", "gotun", "gotunekoyim", "gotune koyim", "gotuni",
                    "got veren", "got verir", "gtelek", "gtn", "gtnde", "gtnden", "gtne",
                    "gtten", "gtveren", "hasiktir", "hassikome", "hassiktir", "has siktir",
                    "hassittir", "haysiyetsiz", "hayvan herif", "hosafi", "hoduk", "hsktr",
                    "huur", "ibnelik", "ibina", "ibine", "ibinenin", "ibne", "ibnedir",
                    "ibneleri", "ibnelik", "ibnelri", "ibneni", "ibnenin", "ibnerator", "ibnesi",
                    "idiot", "idiyot", "imansz", "ipne", "iserim", "itoglu it", "kafam girsin",
                    "kahpe", "kahpenin", "kahpenin feryadi", "kaka", "kaltak", "kancik",
                    "kappe", "karhane", "kasar", "kavat", "kavatn", "kaypak", "kayyum",
                    "kerane", "kerhane", "kerhanelerde", "kevase", "ke vase", "kevvase",
                    "koca got", "kodugmun", "kodugmunun", "kodumun", "kodumunun", "koduumun",
                    "koyarm", "koyayim", "koyiim", "koyiiym", "koyim", "koyum", "koyyim",
                    "krar", "kukudaym", "laciye boyadim", "lavuk", "libos", "madafaka",
                    "malafat", "malak", "manyak", "mcik", "meme", "memelerini", "mezveleli",
                    "minaamcik", "mincikliyim", "mna", "monakkoluyum", "motherfucker", "mudik",
                    "oc", "ocuu", "ocuun", "o c", "o. cocugu", "oglan", "ogl anci", "oglu it",
                    "orosbucocuu", "orospu", "orospucocugu", "orospu cocugu", "orospu coc",
                    "orospu cocugudur", "orospu cocuklari", "orospudur", "orospular",
                    "orospunun", "orospunun evladi", "orospuydu", "orospuyuz", "orostoban",
                    "orostopol", "orrospu", "oruspu", "oruspucocugu", "oruspu cocugu",
                    "osbir", "ossurduum", "ossurmak", "ossuruk", "osur", "osurduu", "osuruk",
                    "osururum", "otuzbir", "okuz", "os ex", "patlak zar", "penis", "pezevek",
                    "pezeven", "pezeveng", "pezevengi", "pezevengin evladi", "pezo", "pic",
                    "pici", "picler", "picin oglu", "pic kurusu", "pipi", "pipis", "pisliktir",
                    "porno", "pussy", "pust", "pusttur", "rahminde", "revizyonist",
                    "s1kerim", "s1kerm", "s1krm", "sakso", "saksofon", "saxo", "sekis",
                    "serefsiz", "sevgi koyarim", "seviselim", "sexs", "sic arim", "sictigim",
                    "sie cem", "sicarsin", "sie", "sik", "sikdi", "sikdigim", "sike", "sikecem",
                    "sikem", "siken", "sikenin", "siker", "sikerim", "sikerler", "sikersin",
                    "sikertir", "sikertmek", "sikesen", "sikesicenin", "sikey", "sikeydim",
                    "sikeyim", "sikeym", "siki", "sikicem", "sikici", "sikien", "sikienler",
                    "sikiiim", "sikiiimmm", "sikiim", "sikiir", "sikiirken", "sikik", "sikil",
                    "sikildiini", "sikilesice", "sikilmi", "sikilmie", "sikilmis", "sikilsin",
                    "sikim", "sikimde", "sikimden", "sikime", "sikimi", "sikimiin", "sikimin",
                    "sikimle", "sikims onik", "sikimtrak", "sikin", "sikinde", "sikinden",
                    "sikine", "sikini", "sikip", "sikis", "sikisek", "sikisen", "sikish",
                    "sikismis", "sikisen", "sikisme", "sikitiin", "sikiyim", "sikiym",
                    "sikiyorum", "sikkim", "sikko", "sikleri", "sikleriii", "sikli", "sikm",
                    "sikmek", "sikmem", "sikmiler", "sikmisligim", "siksem", "sikseydin",
                    "sikseyidin", "siksin", "siksinbaya", "siksinler", "siksiz", "siksok",
                    "siksz", "sikt", "sik t i", "siktigimin", "siktigiminin", "siktigim",
                    "siktigimin", "siktigiminin", "siktii", "siktiim", "siktiimin",
                    "siktiiminin", "siktiler", "siktim", "siktimin", "siktiminin", "siktir",
                    "siktir et", "siktirgit", "siktir git", "siktirir", "siktiririm",
                    "siktiriyor", "siktir lan", "siktirolgit", "siktir ol git", "sittimin",
                    "sittir", "skcem", "skecem", "skem", "sker", "skerim", "skerm", "skeyim",
                    "skiim", "skik", "skim", "skime", "skmek", "sksin", "sksn", "sksz",
                    "sktiimin", "sktrr", "skyim", "slaleni", "sokam", "sokarim", "sokarm",
                    "sokarmkoduumun", "sokayim", "sokaym", "sokiim", "soktugumunun", "sokuk",
                    "sokum", "sokus", "sokuyum", "soxum", "sulaleni", "sulalenizi", "surtuk",
                    "serefsiz", "sillik", "taaklarn", "taaklarna", "tarrakimin", "tasak",
                    "tassak", "tipini s.k", "tipinizi s.keyim", "tiyniyat", "toplarm",
                    "topsun", "totos", "vajina", "vajinani", "veled", "veledizina",
                    "veled i zina", "verdiimin", "weled", "weledizina", "whore", "xikeyim",
                    "yaaraaa", "yalama", "yalarim", "yalarun", "yaraaam", "yarak", "yaraksiz",
                    "yaraktr", "yaram", "yaraminbasi", "yaramn", "yararmorospunun", "yarra",
                    "yarraaaa", "yarraak", "yarraam", "yarraami", "yarragi", "yarragimi",
                    "yarragina", "yarragindan", "yarragm", "yarrag", "yarra g im", "yarraimin",
                    "yarrak", "yarram", "yarramin", "yarraminbasi", "yarramn", "yarran",
                    "yarrrana", "yarrrak", "yavak", "yavs", "yavsak", "yavsaktir", "yavusak",
                    "yil isik", "yilisik", "yogurtlayam", "yrrak", "zikkimim", "zibidi",
                    "zigsin", "zikeyim", "zikiiim", "zikiim", "zikik", "zikim", "ziksiiin",
                    "ziksiin", "zulliyetini", "zviyetini", "israil", "irsail", "izrail",
                    "israli", "israyil", "yaudi", "yagudi", "yahudi", "hz.muhammed", "hz.omer",
                    "pkk", "p kk", "terorist", "kurdistan", "kadek", "kongragel", "fasizm",
                    "fasist", "komunizm", "komunist", "komonist", "dhkp", "devsol", "hizbullah"
            };

            for (String kelime : agirlar) {
                writer.println(kelime);
            }

            plugin.getLogger().info("kufurler_agir.yml dosyası " + agirlar.length + " kelime ile oluşturuldu.");

        } catch (IOException e) {
            plugin.getLogger().severe("kufurler_agir.yml oluşturulamadı: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void olusturHakaretDosyasi() {
        File dosya = new File(plugin.getDataFolder(), "hakaretler.yml");
        if (dosya.exists()) return;

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(dosya), StandardCharsets.UTF_8))) {

            writer.println("# ═════════════════════════════════════");
            writer.println("# HAFİF HAKARETLER - Bu kelimeler sadece UYARI verir (mute yemez)");
            writer.println("# Her satıra bir kelime yazın");
            writer.println("# # ile başlayan satırlar yorum satırıdır");
            writer.println("# ═════════════════════════════════════");
            writer.println();

            String[] hafifler = {
                    "mal", "aptal", "salak", "gerizekali", "geri zekali", "ezik", "okuz",
                    "angut", "ahmak", "beyinsiz", "kafasiz", "kafa siz", "dangalak", "dalyarak",
                    "dalyarrak", "embesil", "idiot", "idiyot", "moron", "hoduk", "dingil",
                    "donek", "duduk", "manyak", "salaak", "odun", "kazma", "kiro", "kro",
                    "manda", "mankafa", "moloz", "suzme", "pust", "kancik", "kaltak",
                    "kasar", "yavsak", "yavsaktr", "yavsaktir", "yavusak", "hiyar",
                    "hiyaragasi", "hayvan", "it oglu it", "godos", "godumun", "lavuk",
                    "libos", "cibiliyetsiz", "cibilliyetsiz", "cenabet", "dallama",
                    "ebleh", "gerzek", "haysiyetsiz", "kancik", "kappe", "karhane",
                    "kerane", "kerhane", "ke vase", "kevase", "kevvase", "mudik",
                    "oglan", "ogl anci", "oglu it", "annesiz", "dalaksiz", "yaraksiz",
                    "dandik", "dazlak", "enayi", "enayilik", "gavur", "gergedan",
                    "gerzek", "hokka", "kafadar", "kancik", "kayisirken", "keriz",
                    "kopek", "koyun", "kuyruk", "mandalina", "maymun", "omurga",
                    "papaganimsi", "ponpon", "sacma", "salya", "sivilce", "sosis",
                    "tembel", "tinerci", "toklu", "ukalami", "varoş", "yobaz", "zurna"
            };

            for (String kelime : hafifler) {
                writer.println(kelime);
            }

            plugin.getLogger().info("hakaretler.yml dosyası " + hafifler.length + " kelime ile oluşturuldu.");

        } catch (IOException e) {
            plugin.getLogger().severe("hakaretler.yml oluşturulamadı: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void olusturWhitelistDosyasi() {
        File dosya = new File(plugin.getDataFolder(), "whitelist.yml");
        if (dosya.exists()) return;

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(dosya), StandardCharsets.UTF_8))) {

            writer.println("# ═════════════════════════════════════");
            writer.println("# WHİTELİST - Bu kelimeleri içeren mesajlar filtrelenmez");
            writer.println("# Her satıra bir kelime/ifade yazın");
            writer.println("# # ile başlayan satırlar yorum satırıdır");
            writer.println("# ═════════════════════════════════════");
            writer.println();
            writer.println("# Örnek kullanımlar:");
            writer.println("# discord.gg");
            writer.println("# youtube.com");
            writer.println("# minecraft.net");
            writer.println();

            plugin.getLogger().info("whitelist.yml dosyası oluşturuldu.");

        } catch (IOException e) {
            plugin.getLogger().severe("whitelist.yml oluşturulamadı: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void yukleListeler() {
        long now = System.currentTimeMillis();
        if (now - sonYuklemeZamani < CACHE_SURESI && !agirKufurler.isEmpty()) return;

        sonYuklemeZamani = now;
        agirKufurler.clear();
        normalizeAgirKufurler.clear();
        hafifHakaretler.clear();
        normalizeHafifHakaretler.clear();

        yukleDosya("kufurler_agir.yml", agirKufurler, normalizeAgirKufurler);
        yukleDosya("hakaretler.yml", hafifHakaretler, normalizeHafifHakaretler);

        plugin.getLogger().info("Küfür filtreleri yüklendi: " + agirKufurler.size() + " ağır küfür, " + hafifHakaretler.size() + " hafif hakaret");
    }

    private void yukleDosya(String dosyaAdi, List<String> hedef, List<String> normHedef) {
        File dosya = new File(plugin.getDataFolder(), dosyaAdi);
        if (!dosya.exists()) {
            plugin.getLogger().warning(dosyaAdi + " dosyası bulunamadı!");
            return;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(dosya), StandardCharsets.UTF_8))) {
            String satir;
            int sayac = 0;
            while ((satir = br.readLine()) != null) {
                String temiz = satir.trim();
                if (temiz.isEmpty() || temiz.startsWith("#")) continue;
                String norm = normalize(temiz);
                if (norm.length() < 2) continue;
                hedef.add(temiz);
                normHedef.add(norm);
                sayac++;
            }
            plugin.getLogger().info(dosyaAdi + " yüklendi: " + sayac + " kelime");
        } catch (Exception e) {
            plugin.getLogger().severe(dosyaAdi + " yüklenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void yukleWhitelist() {
        whitelist.clear();
        File dosya = new File(plugin.getDataFolder(), "whitelist.yml");
        if (!dosya.exists()) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(dosya), StandardCharsets.UTF_8))) {
            String satir;
            int sayac = 0;
            while ((satir = br.readLine()) != null) {
                String temiz = satir.trim();
                if (temiz.isEmpty() || temiz.startsWith("#")) continue;
                whitelist.add(normalize(temiz));
                sayac++;
            }
            plugin.getLogger().info("whitelist.yml yüklendi: " + sayac + " kelime");
        } catch (Exception e) {
            plugin.getLogger().severe("whitelist.yml yüklenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean agirKufurVar(String mesaj) {
        String norm = normalize(mesaj);
        if (whitelist.stream().anyMatch(norm::contains)) return false;
        return normalizeAgirKufurler.parallelStream().anyMatch(norm::contains);
    }

    public boolean hafifHakaretVar(String mesaj) {
        String norm = normalize(mesaj);
        if (whitelist.stream().anyMatch(norm::contains)) return false;
        return normalizeHafifHakaretler.parallelStream().anyMatch(norm::contains);
    }

    public boolean sunucuHakareti(String mesaj) {
        String norm = normalize(mesaj);
        String sunucu = normalize(sunucuAdi);
        if (!norm.contains(sunucu)) return false;
        return normalizeAgirKufurler.stream().anyMatch(k -> k.length() >= 3 && norm.contains(k));
    }

    public String normalize(String s) {
        return s.toLowerCase()
                .replace('ı', 'i').replace('İ', 'i')
                .replace('ğ', 'g').replace('Ğ', 'g')
                .replace('ü', 'u').replace('Ü', 'u')
                .replace('ş', 's').replace('Ş', 's')
                .replace('ö', 'o').replace('Ö', 'o')
                .replace('ç', 'c').replace('Ç', 'c')
                .replaceAll("[^a-z0-9\\s]", "")
                .replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a')
                .replace('5', 's').replace('6', 'g').replace('7', 't').replace('8', 'b').replace('9', 'g')
                .replaceAll("\\s+", " ").trim();
    }

    public void yenidenYukle() {
        sonYuklemeZamani = 0;
        yukleListeler();
        yukleWhitelist();
    }
}