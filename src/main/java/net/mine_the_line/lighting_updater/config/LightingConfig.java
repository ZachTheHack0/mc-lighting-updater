package net.mine_the_line.lighting_updater.config;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LightingConfig {
    public int radius = 8; // default
    public int update_interval = 2;
    public int reupdate_interval = 10;
    public boolean tick_adjacent_blocks = true;
    private static final String defaultJSONConfig = """
    {
        "radius": 8,
        "update_interval": 2,
        "reupdate_interval": 10,
        "tick_adjacent_blocks": true
    }
    """;

    private static void writeDefaultConfig(@NotNull File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(defaultJSONConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static LightingConfig load(@NotNull File file) {
        Gson gson = new Gson();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                return gson.fromJson(reader, LightingConfig.class);
            } catch (IOException e) {
                writeDefaultConfig(file);
            }
        } else
            writeDefaultConfig(file);
        return new LightingConfig();
    }
}
