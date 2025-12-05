package net.mine_the_line.lighting_updater.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("lighting_updater.json");

    public static LightingConfig config = new LightingConfig();

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH))
                config = GSON.fromJson(Files.readString(CONFIG_PATH), LightingConfig.class);
            else
                save(); // write defaults
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
