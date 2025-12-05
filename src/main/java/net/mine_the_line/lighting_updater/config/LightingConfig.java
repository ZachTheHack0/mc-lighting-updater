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
    private static final String defaultJSONConfig = """
    {
        "radius": 8,
        "update_interval": 2,
        "reupdate_interval": 10
    }
    """;

    public static LightingConfig load(@NotNull File file) {
        Gson gson = new Gson();
        LightingConfig config = new LightingConfig();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = gson.fromJson(reader, LightingConfig.class); // attempt to read JSON
            } catch (IOException e) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(defaultJSONConfig); // write default
                } catch (IOException f) {
                    f.printStackTrace();
                }
            }
        } else {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(defaultJSONConfig); // write default
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return config;
    }
}
