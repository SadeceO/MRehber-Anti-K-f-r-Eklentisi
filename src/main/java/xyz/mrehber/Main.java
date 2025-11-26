package xyz.mrehber;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.mrehber.command.ReportCommand;
import xyz.mrehber.listener.ChatListener;
import xyz.mrehber.manager.ConfigManager;
import xyz.mrehber.service.DiscordService;
import xyz.mrehber.service.GeminiService;
import xyz.mrehber.service.ModerationService;
import xyz.mrehber.util.KufurDetector;

public final class Main extends JavaPlugin {

    private ConfigManager configManager;
    private ChatListener chatListener;

    @Override
    public void onEnable() {
        baslat();
        getLogger().info("MRehber aktif!");
    }

    @Override
    public void onDisable() {
        durdur();
        getLogger().info("MRehber devre dışı!");
    }

    private void baslat() {
        configManager = new ConfigManager(this);

        GeminiService geminiService = new GeminiService(configManager, getServer().getName());
        DiscordService discordService = new DiscordService(this, configManager);
        ModerationService moderationService = new ModerationService(this, configManager, discordService);
        KufurDetector profanityDetector = new KufurDetector(this, configManager, getServer().getName());
        chatListener = new ChatListener(
                this,
                configManager,
                geminiService,
                moderationService,
                profanityDetector);

        getServer().getPluginManager().registerEvents(chatListener, this);
        getCommand("bildir").setExecutor(new ReportCommand(chatListener,configManager));
    }

    private void durdur() {
        if (chatListener != null) {
            chatListener.shutdown();
        }
    }
}