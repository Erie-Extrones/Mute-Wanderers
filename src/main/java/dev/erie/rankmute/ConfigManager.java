package dev.erie.rankmute;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import net.fabricmc.loader.api.FabricLoader;

import dev.erie.rankmute.filter.RankMatcher;

/**
 * Owns the in-memory config and its compiled {@link RankMatcher}.
 * Loaded on startup, saved on every change. A malformed file logs a warning and falls back to defaults.
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final String FILE_NAME = "rankmute.json";

	private static RankMuteConfig config = new RankMuteConfig();
	private static RankMatcher matcher = config.buildMatcher();

	private ConfigManager() {
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	public static RankMuteConfig get() {
		return config;
	}

	public static RankMatcher matcher() {
		return matcher;
	}

	/** Re-reads the file. Writes defaults if it is missing. Returns true if the file parsed cleanly. */
	public static synchronized boolean load() {
		Path file = path();
		if (!Files.exists(file)) {
			config = new RankMuteConfig();
			rebuildMatcher();
			save();
			RankMuteClient.LOGGER.info("Wrote default config to {}", file);
			return true;
		}

		try {
			String json = Files.readString(file, StandardCharsets.UTF_8);
			RankMuteConfig loaded = GSON.fromJson(json, RankMuteConfig.class);
			if (loaded == null) {
				throw new JsonParseException("file is empty");
			}
			config = loaded.normalize();
			rebuildMatcher();
			return true;
		} catch (IOException | JsonParseException e) {
			RankMuteClient.LOGGER.warn("Could not read {} ({}). Using defaults; the file is left untouched until the next change saves it.", file, e.getMessage());
			config = new RankMuteConfig();
			rebuildMatcher();
			return false;
		}
	}

	public static synchronized void save() {
		Path file = path();
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(config), StandardCharsets.UTF_8);
		} catch (IOException e) {
			RankMuteClient.LOGGER.error("Could not save {}", file, e);
		}
	}

	/** Recompiles the matcher from the current config and logs any patterns that were skipped. */
	public static synchronized void rebuildMatcher() {
		matcher = config.buildMatcher();
		for (String error : matcher.errors()) {
			RankMuteClient.LOGGER.warn("Skipping invalid pattern: {}", error);
		}
	}

	/** Rebuild + save, for every command that edits the config. */
	public static synchronized void changed() {
		rebuildMatcher();
		save();
	}
}
