package yaoluna.totemofundyingreplacer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TotemOfUndyingReplacerConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("TotemOfUndyingReplacerConfig.json");

    private static TotemOfUndyingReplacerConfig CONFIG = new TotemOfUndyingReplacerConfig();

    private TotemOfUndyingReplacerConfigManager() {}

    public static TotemOfUndyingReplacerConfig get() {
        return CONFIG;
    }

    public static void loadOrCreate() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            String json = Files.readString(PATH, StandardCharsets.UTF_8);
            TotemOfUndyingReplacerConfig loaded = GSON.fromJson(json, TotemOfUndyingReplacerConfig.class);
            if (loaded != null) CONFIG = loaded;
        } catch (Exception e) {
            CONFIG = new TotemOfUndyingReplacerConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(CONFIG), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}
