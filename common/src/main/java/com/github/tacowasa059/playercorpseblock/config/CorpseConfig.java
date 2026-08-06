package com.github.tacowasa059.playercorpseblock.config;

import com.github.tacowasa059.playercorpseblock.Constants;
import com.github.tacowasa059.playercorpseblock.platform.Services;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Forge and Fabric ship completely different config systems, so the mod keeps its settings in a plain
 * properties file inside the loader's config directory. That keeps the whole thing in the common project.
 */
public final class CorpseConfig {

    private static final String FILE_NAME = Constants.MOD_ID + ".properties";
    private static final String DEFAULT_FILE = """
            # PlayerCorpseBlock

            # Place a corpse block when a player dies.
            enabled=true

            # Horizontal radius searched for a free spot around the death position.
            # Corpses fill the lowest spot first, so a bigger radius makes wider and flatter mounds.
            pileRadius=2

            # How many corpses may stack on top of each other in a single column.
            maxPileHeight=24

            # Corpses disappear after this many seconds. 0 keeps them forever.
            despawnSeconds=0

            # Client only. Corpses further away than this (in blocks) are not rendered.
            renderDistance=32

            # Client only. Corpses further away than this (in blocks) are drawn without their outer skin
            # layer, which halves the work per body. Lower this first if a big pile costs frames.
            detailDistance=16

            # Client only. Skip corpses that are covered on all six sides, for example the ones buried
            # inside a mound. Turn this off if bodies disappear where you can still see them.
            cullHiddenCorpses=true
            """;

    private static volatile CorpseConfig instance;

    private final boolean enabled;
    private final int pileRadius;
    private final int maxPileHeight;
    private final int despawnSeconds;
    private final int renderDistance;
    private final int detailDistance;
    private final boolean cullHiddenCorpses;

    CorpseConfig(Properties properties) {

        this.enabled = readBoolean(properties, "enabled", true);
        this.pileRadius = readInt(properties, "pileRadius", 2, 0, 8);
        this.maxPileHeight = readInt(properties, "maxPileHeight", 24, 1, 128);
        this.despawnSeconds = readInt(properties, "despawnSeconds", 0, 0, 2592000);
        this.renderDistance = readInt(properties, "renderDistance", 32, 8, 256);
        this.detailDistance = readInt(properties, "detailDistance", 16, 0, 256);
        this.cullHiddenCorpses = readBoolean(properties, "cullHiddenCorpses", true);
    }

    public static CorpseConfig get() {

        CorpseConfig config = instance;
        if (config == null) {
            synchronized (CorpseConfig.class) {
                config = instance;
                if (config == null) {
                    config = load();
                    instance = config;
                }
            }
        }
        return config;
    }

    private static CorpseConfig load() {

        final Properties properties = new Properties();
        Path path = null;

        try {
            path = Services.PLATFORM.getConfigDirectory().resolve(FILE_NAME);
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path, DEFAULT_FILE, StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            Constants.LOG.warn("Failed to read {}, falling back to the default settings.", path, e);
        }

        return new CorpseConfig(properties);
    }

    private static boolean readBoolean(Properties properties, String key, boolean fallback) {

        final String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static int readInt(Properties properties, String key, int fallback, int min, int max) {

        final String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value.trim())));
        } catch (NumberFormatException e) {
            Constants.LOG.warn("Config value '{}' for '{}' is not a number, using {}.", value, key, fallback);
            return fallback;
        }
    }

    public boolean enabled() {

        return this.enabled;
    }

    public int pileRadius() {

        return this.pileRadius;
    }

    public int maxPileHeight() {

        return this.maxPileHeight;
    }

    public int despawnSeconds() {

        return this.despawnSeconds;
    }

    public int renderDistance() {

        return this.renderDistance;
    }

    public int detailDistance() {

        return this.detailDistance;
    }

    public boolean cullHiddenCorpses() {

        return this.cullHiddenCorpses;
    }
}
