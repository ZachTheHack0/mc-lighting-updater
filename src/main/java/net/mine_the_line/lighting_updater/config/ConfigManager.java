package net.mine_the_line.lighting_updater.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import net.fabricmc.loader.api.FabricLoader;

public class ConfigManager {
	private static final File CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("lighting_updater.json").toFile();

	public static LightingConfig config = new LightingConfig();

	public static void load() {
		if (CONFIG_PATH.exists())
			try (FileReader reader = new FileReader(CONFIG_PATH)) {
				config = new Gson().fromJson(reader, LightingConfig.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
		else
			save();
	}
	public static void save() {
		try (FileWriter fw = new FileWriter(CONFIG_PATH)) {
			fw.write(new GsonBuilder().setPrettyPrinting().create().toJson(config));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
