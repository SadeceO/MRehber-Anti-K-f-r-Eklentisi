### MRehber Gelişmiş Sohbet Moderasyon Eklentisi

Minecraft sunucunuz için Google Gemini 2.0 Flash yapay zeka destekli otomatik moderasyon sistemi. Eklenti, çok katmanlı tespit mekanizması ile çalışır: önce yerel filtre sistemi mesajları hızlıca tarar, kesin küfürleri, ağır küfür kombinasyonlarını ve sunucu ismine yapılan hakaretleri anında yakalar. Şüpheli durumlarda veya şifreli küfürlerde (mk, nnısm gibi küfürleri bunlar sansürlü) yapay zeka devreye girerek detaylı analiz yapar. Bu sayede hem yüksek performans sağlanır hem de karmaşık ve gizlenmiş küfürler tespit edilir. Sistem, her mesajı derece bazlı değerlendirir ve bağlamsal analiz yaparak yanlış pozitifleri minimize eder.


### Temel Özellikler;
Derece Bazlı Ceza Sistemi

Eklenti üç farklı ceza derecesi uygular. Birinci derece normal küfürler için 15 dakika mute, ikinci derece ailevi küfürler için 30 dakika mute, üçüncü derece ise din, ırk, ata veya sunucu hakaretleri için 7 gün mute cezası verir. Hafif küfürler ise uyarı sistemi ile kontrol edilir.


### Uyarı ve Özür Mekanizması

Oyunculara maksimum 3 uyarı hakkı tanınır. Her uyarıda görsel efektler ve sesli bildirimler oynatılır. Üç uyarı sonrası otomatik mute uygulanır. Oyuncular sohbette özür dilerim, pardon, kusura bakma gibi ifadeler kullanarak uyarılarını azaltabilir ve ikinci şans kazanabilirler fakat 1 saatd'e maximum 1 uyarı silebilirler.


### Discord Entegrasyonu

Webhook üzerinden otomatik bildirim sistemi. Her ihlalde Discord sunucunuza renkli embed mesaj gönderilir. Mesajda oyuncu adı, ihlal edilen mesaj, uygulanan ceza ve ceza süresi detayları yer alır. Ceza derecesine göre farklı renkler kullanılır.


### Performans Optimizasyonu ve Maliyet Tasarrufu

Eklenti, sunucu performansını korumak ve Gemini API maliyetlerini minimuma indirmek için özel olarak optimize edilmiştir. ConcurrentHashMap ve CopyOnWriteArrayList gibi thread-safe veri yapıları kullanılarak çoklu işlem güvenliği sağlanır. Asenkron işleme sistemi sayesinde ana thread hiçbir zaman bloke olmaz ve sunucu gecikmesi yaşanmaz. Dört thread'li ExecutorService pool ile paralel işlem desteği sunulur.

Gemini API isteklerini en aza indirmek için akıllı filtreleme sistemi kullanılır. Yerel filtre, mesajların yüzde 90'ından fazlasını yapay zekaya göndermeden tespit eder. Bu sayede hem API maliyetlerinizden tasarruf edersiniz hem de yanıt süreleri milisaniyelere düşer. Yapay zeka sadece gerçekten şüpheli veya karmaşık durumlarda devreye girer. Tekrar işlem önleme mekanizması ile aynı mesaj için birden fazla API isteği engellenir. OP oyuncular otomatik olarak kontrol dışı bırakılarak gereksiz işlem yapılmaz. Paralel stream kullanımı ile büyük veri setlerinde hızlı arama yapılır ve 3 dakikalık otomatik mesaj geçmişi temizliği ile bellek kullanımı optimize edilir.


### Yapılandırma Ayarları

Gemini API Ayarları
Yapay zeka sistemini aktif veya pasif hale getirebilirsiniz. API anahtarınızı config.yml dosyasına eklemeniz yeterlidir. Özel AI promptları yazarak tespit sistemini kendi ihtiyaçlarınıza göre özelleştirebilirsiniz. Prompt içinde server_name ve messages değişkenleri kullanılabilir.


Discord Webhook Ayarları

Discord bildirimlerini açıp kapatabilirsiniz. Webhook adresinizi config.yml dosyasına ekleyerek otomatik bildirimleri aktif edebilirsiniz. Bildirim mesajlarının içeriği ve görünümü messages.yml dosyasından düzenlenebilir.


Ceza Yapılandırması

Her derece için ayrı süre ve sebep belirleyebilirsiniz. Birinci, ikinci ve üçüncü derece cezaların sürelerini istediğiniz gibi ayarlayabilirsiniz. Uyarı sisteminin maksimum uyarı sayısını ve uyarı sonrası ceza süresini özelleştirebilirsiniz. Belirsizlik cezası için de özel ayarlar yapabilirsiniz.


Mesaj ve Geçmiş Ayarları

Toplu kontrol için kaç mesajın analiz edileceğini belirleyebilirsiniz. Mesaj geçmişi boyutunu sunucunuzun ihtiyacına göre ayarlayabilirsiniz. Tüm bildirim mesajları, uyarı metinleri ve Discord embed içerikleri messages.yml dosyasından tamamen özelleştirilebilir.

### Komutlar
/bildir oyuncu - Belirtilen oyuncunun son mesajlarını yapay zeka ile analiz eder ve gerekirse işlem uygular.

### Gereksinimler
Spigot veya Paper sunucu, Java 17 veya üzeri, mute komutu destekleyen bir eklenti (LiteBans, AdvancedBan vb.). Yapay zeka özelliği için Google Gemini API anahtarı, Discord bildirimleri için webhook adresi.


### Amaç ve Vizyon
Bu eklentinin temel amacı, sunucu sahiplerinin yetkili ekibi bulma ve yönetme yükünü minimuma indirmektir. Yapay zeka destekli otomatik moderasyon sayesinde, sürekli aktif yetkili arayışından kurtulabilir ve sohbet ortamınızı 7/24 temiz tutabilirsiniz. Eklenti her türlü öneri, tavsiye ve geri bildirimlere açıktır. Geliştirme süreci aktif olarak devam etmektedir ve kullanıcı ihtiyaçlarına göre sürekli güncellenmektedir.
