package dev.arbe.sodiumprofiles.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import dev.arbe.sodiumprofiles.SodiumProfiles;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.DeferMode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.server.level.ParticleStatus;

/**
 * User-created profiles system.
 * Saves/loads up to 5 custom profiles in config/sodium-profiles/.
 * Each profile stores all vanilla + Sodium settings as a .properties file.
 */
public final class UserProfiles {

    private UserProfiles() {
    }

    private static final String PROFILES_DIR = "sodium-profiles";
    public static final int MAX_PROFILES = 5;

    private static final List<SavedProfile> profiles = new ArrayList<>();
    private static boolean loaded = false;

    public static final class SavedProfile {
        private final String name;
        private final int index;

        SavedProfile(String name, int index) {
            this.name = name;
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }
    }

    private static Path getProfilesDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(PROFILES_DIR);
    }

    public static void loadIndex() {
        if (loaded)
            return;
        profiles.clear();
        Path dir = getProfilesDir();
        if (!Files.isDirectory(dir)) {
            loaded = true;
            return;
        }

        Path indexFile = dir.resolve("profile_index.properties");
        if (!Files.isRegularFile(indexFile)) {
            loaded = true;
            return;
        }

        try {
            Properties idx = new Properties();
            try (var in = Files.newInputStream(indexFile)) {
                idx.load(in);
            }
            int count = Integer.parseInt(idx.getProperty("count", "0"));
            for (int i = 0; i < count && i < MAX_PROFILES; i++) {
                String name = idx.getProperty("profile." + i + ".name");
                if (name != null && !name.isBlank()) {
                    profiles.add(new SavedProfile(name.trim(), i));
                }
            }
        } catch (IOException | NumberFormatException e) {
            SodiumProfiles.LOGGER.error("Error loading user profile index", e);
        }
        loaded = true;
    }

    public static void invalidate() {
        loaded = false;
    }

    public static List<SavedProfile> getProfiles() {
        loadIndex();
        return Collections.unmodifiableList(profiles);
    }

    public static int getProfileCount() {
        loadIndex();
        return profiles.size();
    }

    public static boolean canCreateMore() {
        return getProfileCount() < MAX_PROFILES;
    }

    /**
     * Save the current settings as a new user profile.
     */
    public static void saveProfile(String name) {
        loadIndex();
        if (profiles.size() >= MAX_PROFILES)
            return;

        int nextIndex = profiles.size();
        try {
            Path dir = getProfilesDir();
            Files.createDirectories(dir);

            Properties props = captureAllSettings();
            props.setProperty("profileName", name);
            Path profileFile = dir.resolve("profile_" + nextIndex + ".properties");
            try (var out = Files.newOutputStream(profileFile)) {
                props.store(out, "Sodium Profiles - " + name);
            }

            profiles.add(new SavedProfile(name, nextIndex));
            saveIndex();

            SodiumProfiles.LOGGER.info("Saved user profile '{}' at index {}", name, nextIndex);
        } catch (IOException e) {
            SodiumProfiles.LOGGER.error("Error saving user profile '{}'", name, e);
        }
    }

    /**
     * Delete a user profile by index and repack remaining profiles.
     */
    public static void deleteProfile(int profileIndex) {
        loadIndex();
        SavedProfile toRemove = null;
        for (SavedProfile p : profiles) {
            if (p.index == profileIndex) {
                toRemove = p;
                break;
            }
        }
        if (toRemove == null)
            return;

        profiles.remove(toRemove);
        Path dir = getProfilesDir();

        try {
            Files.deleteIfExists(dir.resolve("profile_" + profileIndex + ".properties"));

            // Reindex remaining files to fill the gap
            List<SavedProfile> reindexed = new ArrayList<>();
            for (int i = 0; i < profiles.size(); i++) {
                SavedProfile old = profiles.get(i);
                if (old.index != i) {
                    Path oldFile = dir.resolve("profile_" + old.index + ".properties");
                    Path newFile = dir.resolve("profile_" + i + ".properties");
                    if (Files.isRegularFile(oldFile)) {
                        Path tmpFile = dir.resolve("profile_" + i + ".tmp");
                        Files.move(oldFile, tmpFile);
                        Files.move(tmpFile, newFile);
                    }
                }
                reindexed.add(new SavedProfile(old.name, i));
            }

            // Clean up leftover files
            for (int i = profiles.size(); i < MAX_PROFILES; i++) {
                Files.deleteIfExists(dir.resolve("profile_" + i + ".properties"));
            }

            profiles.clear();
            profiles.addAll(reindexed);
            saveIndex();
        } catch (IOException e) {
            SodiumProfiles.LOGGER.error("Error deleting user profile", e);
        }
    }

    /**
     * Apply a saved user profile — loads and restores all settings.
     */
    public static void applyProfile(int profileIndex) {
        Path dir = getProfilesDir();
        Path profileFile = dir.resolve("profile_" + profileIndex + ".properties");
        if (!Files.isRegularFile(profileFile))
            return;

        try {
            Properties props = new Properties();
            try (var in = Files.newInputStream(profileFile)) {
                props.load(in);
            }
            restoreAllSettings(props);
        } catch (IOException e) {
            SodiumProfiles.LOGGER.error("Error loading user profile at index {}", profileIndex, e);
        }
    }

    // ── Private helpers ──

    private static void saveIndex() {
        try {
            Path dir = getProfilesDir();
            Files.createDirectories(dir);
            Properties idx = new Properties();
            idx.setProperty("count", String.valueOf(profiles.size()));
            for (int i = 0; i < profiles.size(); i++) {
                idx.setProperty("profile." + i + ".name", profiles.get(i).name);
            }
            Path indexFile = dir.resolve("profile_index.properties");
            try (var out = Files.newOutputStream(indexFile)) {
                idx.store(out, "Sodium Profiles Index");
            }
        } catch (IOException e) {
            SodiumProfiles.LOGGER.error("Error saving profile index", e);
        }
    }

    /**
     * Capture ALL current settings (vanilla + Sodium) into a Properties object.
     */
    public static Properties captureAllSettings() {
        Options opts = Minecraft.getInstance().options;
        SodiumOptions sodium = SodiumClientMod.options();
        Properties p = new Properties();

        // ── Vanilla General ──
        p.setProperty("v.renderDistance", String.valueOf(opts.renderDistance().get()));
        p.setProperty("v.simulationDistance", String.valueOf(opts.simulationDistance().get()));
        p.setProperty("v.gamma", String.valueOf(opts.gamma().get()));
        p.setProperty("v.guiScale", String.valueOf(opts.guiScale().get()));
        p.setProperty("v.fullscreen", String.valueOf(opts.fullscreen().get()));
        p.setProperty("v.enableVsync", String.valueOf(opts.enableVsync().get()));
        p.setProperty("v.framerateLimit", String.valueOf(opts.framerateLimit().get()));
        p.setProperty("v.attackIndicator", opts.attackIndicator().get().name());
        p.setProperty("v.showAutosaveIndicator", String.valueOf(opts.showAutosaveIndicator().get()));

        // ── Vanilla Quality ──
        p.setProperty("v.improvedTransparency", String.valueOf(opts.improvedTransparency().get()));
        p.setProperty("v.cloudStatus", opts.cloudStatus().get().name());
        p.setProperty("v.cloudRange", String.valueOf(opts.cloudRange().get()));
        p.setProperty("v.weatherRadius", String.valueOf(opts.weatherRadius().get()));
        p.setProperty("v.cutoutLeaves", String.valueOf(opts.cutoutLeaves().get()));
        p.setProperty("v.particles", opts.particles().get().name());
        p.setProperty("v.ambientOcclusion", String.valueOf(opts.ambientOcclusion().get()));
        p.setProperty("v.biomeBlendRadius", String.valueOf(opts.biomeBlendRadius().get()));
        p.setProperty("v.entityDistanceScaling", String.valueOf(opts.entityDistanceScaling().get()));
        p.setProperty("v.entityShadows", String.valueOf(opts.entityShadows().get()));
        p.setProperty("v.vignette", String.valueOf(opts.vignette().get()));
        p.setProperty("v.chunkSectionFadeInTime", String.valueOf(opts.chunkSectionFadeInTime().get()));
        p.setProperty("v.mipmapLevels", String.valueOf(opts.mipmapLevels().get()));
        p.setProperty("v.textureFiltering", opts.textureFiltering().get().name());
        p.setProperty("v.maxAnisotropyBit", String.valueOf(opts.maxAnisotropyBit().get()));

        // ── Vanilla Performance ──
        p.setProperty("v.inactivityFpsLimit", opts.inactivityFpsLimit().get().name());

        // ── Sodium Quality ──
        p.setProperty("s.hiddenFluidCulling", String.valueOf(sodium.quality.hiddenFluidCulling));
        p.setProperty("s.improvedFluidShaping", String.valueOf(sodium.quality.improvedFluidShaping));

        // ── Sodium Performance ──
        p.setProperty("s.chunkBuilderThreads", String.valueOf(sodium.performance.chunkBuilderThreads));
        p.setProperty("s.chunkBuildDeferMode", sodium.performance.chunkBuildDeferMode.name());
        p.setProperty("s.animateOnlyVisibleTextures", String.valueOf(sodium.performance.animateOnlyVisibleTextures));
        p.setProperty("s.useEntityCulling", String.valueOf(sodium.performance.useEntityCulling));
        p.setProperty("s.useFogOcclusion", String.valueOf(sodium.performance.useFogOcclusion));
        p.setProperty("s.useBlockFaceCulling", String.valueOf(sodium.performance.useBlockFaceCulling));
        p.setProperty("s.useNoErrorGLContext", String.valueOf(sodium.performance.useNoErrorGLContext));
        p.setProperty("s.quadSplittingMode", sodium.performance.quadSplittingMode.name());

        // ── Sodium Advanced ──
        p.setProperty("s.enableMemoryTracing", String.valueOf(sodium.advanced.enableMemoryTracing));
        p.setProperty("s.useAdvancedStagingBuffers", String.valueOf(sodium.advanced.useAdvancedStagingBuffers));
        p.setProperty("s.cpuRenderAheadLimit", String.valueOf(sodium.advanced.cpuRenderAheadLimit));

        return p;
    }

    /**
     * Restore ALL settings from a Properties snapshot.
     */
    private static void restoreAllSettings(Properties p) {
        Options opts = Minecraft.getInstance().options;
        SodiumOptions sodium = SodiumClientMod.options();

        // ── Vanilla General ──
        opts.renderDistance().set(getInt(p, "v.renderDistance", 12));
        opts.simulationDistance().set(getInt(p, "v.simulationDistance", 12));
        opts.gamma().set(getDouble(p, "v.gamma", 0.5));
        opts.guiScale().set(getInt(p, "v.guiScale", 0));
        opts.fullscreen().set(getBool(p, "v.fullscreen", false));
        opts.enableVsync().set(getBool(p, "v.enableVsync", true));
        opts.framerateLimit().set(getInt(p, "v.framerateLimit", 120));
        try {
            opts.attackIndicator().set(AttackIndicatorStatus.valueOf(
                    p.getProperty("v.attackIndicator", "CROSSHAIR")));
        } catch (Exception ignored) {
        }
        opts.showAutosaveIndicator().set(getBool(p, "v.showAutosaveIndicator", true));

        // ── Vanilla Quality ──
        opts.improvedTransparency().set(getBool(p, "v.improvedTransparency", false));
        try {
            opts.cloudStatus().set(CloudStatus.valueOf(
                    p.getProperty("v.cloudStatus", "FANCY")));
        } catch (Exception ignored) {
        }
        opts.cloudRange().set(getInt(p, "v.cloudRange", 16));
        opts.weatherRadius().set(getInt(p, "v.weatherRadius", 8));
        opts.cutoutLeaves().set(getBool(p, "v.cutoutLeaves", true));
        try {
            opts.particles().set(ParticleStatus.valueOf(
                    p.getProperty("v.particles", "ALL")));
        } catch (Exception ignored) {
        }
        opts.ambientOcclusion().set(getBool(p, "v.ambientOcclusion", true));
        opts.biomeBlendRadius().set(getInt(p, "v.biomeBlendRadius", 5));
        opts.entityDistanceScaling().set(getDouble(p, "v.entityDistanceScaling", 1.0));
        opts.entityShadows().set(getBool(p, "v.entityShadows", true));
        opts.vignette().set(getBool(p, "v.vignette", true));
        opts.chunkSectionFadeInTime().set(getDouble(p, "v.chunkSectionFadeInTime", 0.0));
        opts.mipmapLevels().set(getInt(p, "v.mipmapLevels", 4));
        try {
            opts.textureFiltering().set(TextureFilteringMethod.valueOf(
                    p.getProperty("v.textureFiltering", "RGSS")));
        } catch (Exception ignored) {
        }
        opts.maxAnisotropyBit().set(getInt(p, "v.maxAnisotropyBit", 0));

        // ── Vanilla Performance ──
        try {
            opts.inactivityFpsLimit().set(InactivityFpsLimit.valueOf(
                    p.getProperty("v.inactivityFpsLimit", "AFK")));
        } catch (Exception ignored) {
        }

        // ── Sodium Quality ──
        sodium.quality.hiddenFluidCulling = getBool(p, "s.hiddenFluidCulling", true);
        sodium.quality.improvedFluidShaping = getBool(p, "s.improvedFluidShaping", false);

        // ── Sodium Performance ──
        sodium.performance.chunkBuilderThreads = getInt(p, "s.chunkBuilderThreads", 0);
        try {
            sodium.performance.chunkBuildDeferMode = DeferMode.valueOf(
                    p.getProperty("s.chunkBuildDeferMode", "ALWAYS"));
        } catch (Exception ignored) {
        }
        sodium.performance.animateOnlyVisibleTextures = getBool(p, "s.animateOnlyVisibleTextures", true);
        sodium.performance.useEntityCulling = getBool(p, "s.useEntityCulling", true);
        sodium.performance.useFogOcclusion = getBool(p, "s.useFogOcclusion", true);
        sodium.performance.useBlockFaceCulling = getBool(p, "s.useBlockFaceCulling", true);
        sodium.performance.useNoErrorGLContext = getBool(p, "s.useNoErrorGLContext", true);
        try {
            sodium.performance.quadSplittingMode = QuadSplittingMode.valueOf(
                    p.getProperty("s.quadSplittingMode", "SAFE"));
        } catch (Exception ignored) {
        }

        // ── Sodium Advanced ──
        sodium.advanced.enableMemoryTracing = getBool(p, "s.enableMemoryTracing", false);
        sodium.advanced.useAdvancedStagingBuffers = getBool(p, "s.useAdvancedStagingBuffers", true);
        sodium.advanced.cpuRenderAheadLimit = getInt(p, "s.cpuRenderAheadLimit", 3);

        // Save both
        opts.save();
        try {
            SodiumOptions.writeToDisk(sodium);
        } catch (IOException e) {
            SodiumProfiles.LOGGER.error("Failed to save Sodium options", e);
        }
    }

    private static int getInt(Properties p, String key, int def) {
        try {
            return Integer.parseInt(p.getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double getDouble(Properties p, String key, double def) {
        try {
            return Double.parseDouble(p.getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean getBool(Properties p, String key, boolean def) {
        return Boolean.parseBoolean(p.getProperty(key, String.valueOf(def)));
    }
}
