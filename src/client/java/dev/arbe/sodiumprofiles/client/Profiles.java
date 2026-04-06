package dev.arbe.sodiumprofiles.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Central profile state manager and preset application logic.
 * Tracks the currently active profile and provides preset apply methods.
 */
public final class Profiles {

    private Profiles() {
    }

    private static String activeProfile = "";
    private static boolean initialized = false;
    private static final String STATE_FILE = "sodium-profiles-state.properties";

    public static String getActiveProfile() {
        if (!initialized)
            loadState();
        return activeProfile;
    }

    public static void setActiveProfile(String profile) {
        activeProfile = profile != null ? profile : "";
        saveState();
    }

    public static boolean isActive(String profile) {
        return profile.equals(getActiveProfile());
    }

    private static Path getStatePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(STATE_FILE);
    }

    public static void loadState() {
        initialized = true;
        Path path = getStatePath();
        if (Files.isRegularFile(path)) {
            try {
                Properties p = new Properties();
                try (var in = Files.newInputStream(path)) {
                    p.load(in);
                }
                activeProfile = p.getProperty("activeProfile", "");
            } catch (IOException e) {
                SodiumProfiles.LOGGER.error("Failed to load profile state", e);
            }
        }
    }

    public static void saveState() {
        try {
            Properties p = new Properties();
            p.setProperty("activeProfile", activeProfile != null ? activeProfile : "");
            Path path = getStatePath();
            Files.createDirectories(path.getParent());
            try (var out = Files.newOutputStream(path)) {
                p.store(out, "Sodium Profiles State");
            }
        } catch (IOException e) {
            SodiumProfiles.LOGGER.error("Failed to save profile state", e);
        }
    }

    public static String getPresetDisplayName(String profileId) {
        return switch (profileId) {
            case "ultra" -> "Ultra Performance";
            case "performance" -> "Performance";
            case "balanced" -> "Balanced";
            case "quality" -> "Quality";
            case "custom" -> "Custom";
            default -> profileId;
        };
    }

    // ── Preset Application Methods ──

    public static void applyUltraPerformance() {
        Options opts = Minecraft.getInstance().options;
        SodiumOptions sodium = SodiumClientMod.options();

        // General
        opts.renderDistance().set(4);
        opts.simulationDistance().set(5);
        opts.gamma().set(0.5);
        opts.guiScale().set(0);
        opts.fullscreen().set(false);
        opts.enableVsync().set(false);
        opts.framerateLimit().set(260);
        opts.attackIndicator().set(AttackIndicatorStatus.CROSSHAIR);
        opts.showAutosaveIndicator().set(false);

        // Quality — absolute minimum
        opts.improvedTransparency().set(false);
        opts.cloudStatus().set(CloudStatus.OFF);
        opts.cloudRange().set(4);
        opts.weatherRadius().set(3);
        opts.cutoutLeaves().set(false);
        opts.particles().set(ParticleStatus.MINIMAL);
        opts.ambientOcclusion().set(false);
        opts.biomeBlendRadius().set(0);
        opts.entityDistanceScaling().set(0.5);
        opts.entityShadows().set(false);
        opts.vignette().set(false);
        opts.chunkSectionFadeInTime().set(0.0);
        opts.mipmapLevels().set(0);
        opts.textureFiltering().set(TextureFilteringMethod.NONE);
        opts.maxAnisotropyBit().set(0);

        // Vanilla Performance
        opts.inactivityFpsLimit().set(InactivityFpsLimit.AFK);

        // Sodium Quality
        sodium.quality.hiddenFluidCulling = true;
        sodium.quality.improvedFluidShaping = false;

        // Sodium Performance — all optimizations ON
        sodium.performance.chunkBuilderThreads = 0;
        sodium.performance.useBlockFaceCulling = true;
        sodium.performance.useFogOcclusion = true;
        sodium.performance.useEntityCulling = true;
        sodium.performance.animateOnlyVisibleTextures = true;
        sodium.performance.useNoErrorGLContext = true;
        sodium.performance.chunkBuildDeferMode = DeferMode.ALWAYS;
        sodium.performance.quadSplittingMode = QuadSplittingMode.SAFE;

        // Sodium Advanced
        sodium.advanced.enableMemoryTracing = false;
        sodium.advanced.useAdvancedStagingBuffers = true;
        sodium.advanced.cpuRenderAheadLimit = 3;

        saveOptions(opts, sodium);
        setActiveProfile("ultra");
    }

    public static void applyPerformance() {
        Options opts = Minecraft.getInstance().options;
        SodiumOptions sodium = SodiumClientMod.options();

        // General
        opts.renderDistance().set(8);
        opts.simulationDistance().set(8);
        opts.gamma().set(0.5);
        opts.guiScale().set(0);
        opts.fullscreen().set(false);
        opts.enableVsync().set(false);
        opts.framerateLimit().set(260);
        opts.attackIndicator().set(AttackIndicatorStatus.CROSSHAIR);
        opts.showAutosaveIndicator().set(true);

        // Quality
        opts.improvedTransparency().set(false);
        opts.cloudStatus().set(CloudStatus.FAST);
        opts.cloudRange().set(8);
        opts.weatherRadius().set(5);
        opts.cutoutLeaves().set(false);
        opts.particles().set(ParticleStatus.DECREASED);
        opts.ambientOcclusion().set(true);
        opts.biomeBlendRadius().set(2);
        opts.entityDistanceScaling().set(0.75);
        opts.entityShadows().set(false);
        opts.vignette().set(false);
        opts.chunkSectionFadeInTime().set(0.0);
        opts.mipmapLevels().set(2);
        opts.textureFiltering().set(TextureFilteringMethod.NONE);
        opts.maxAnisotropyBit().set(0);

        // Vanilla Performance
        opts.inactivityFpsLimit().set(InactivityFpsLimit.AFK);

        // Sodium Quality
        sodium.quality.hiddenFluidCulling = true;
        sodium.quality.improvedFluidShaping = false;

        // Sodium Performance
        sodium.performance.chunkBuilderThreads = 0;
        sodium.performance.useBlockFaceCulling = true;
        sodium.performance.useFogOcclusion = true;
        sodium.performance.useEntityCulling = true;
        sodium.performance.animateOnlyVisibleTextures = true;
        sodium.performance.useNoErrorGLContext = true;
        sodium.performance.chunkBuildDeferMode = DeferMode.ALWAYS;
        sodium.performance.quadSplittingMode = QuadSplittingMode.SAFE;

        // Sodium Advanced
        sodium.advanced.enableMemoryTracing = false;
        sodium.advanced.useAdvancedStagingBuffers = true;
        sodium.advanced.cpuRenderAheadLimit = 3;

        saveOptions(opts, sodium);
        setActiveProfile("performance");
    }

    public static void applyBalanced() {
        Options opts = Minecraft.getInstance().options;
        SodiumOptions sodium = SodiumClientMod.options();

        // General
        opts.renderDistance().set(12);
        opts.simulationDistance().set(12);
        opts.gamma().set(0.5);
        opts.guiScale().set(0);
        opts.fullscreen().set(false);
        opts.enableVsync().set(true);
        opts.framerateLimit().set(120);
        opts.attackIndicator().set(AttackIndicatorStatus.CROSSHAIR);
        opts.showAutosaveIndicator().set(true);

        // Quality
        opts.improvedTransparency().set(false);
        opts.cloudStatus().set(CloudStatus.FANCY);
        opts.cloudRange().set(16);
        opts.weatherRadius().set(8);
        opts.cutoutLeaves().set(true);
        opts.particles().set(ParticleStatus.ALL);
        opts.ambientOcclusion().set(true);
        opts.biomeBlendRadius().set(5);
        opts.entityDistanceScaling().set(1.0);
        opts.entityShadows().set(true);
        opts.vignette().set(true);
        opts.chunkSectionFadeInTime().set(0.0);
        opts.mipmapLevels().set(4);
        opts.textureFiltering().set(TextureFilteringMethod.RGSS);
        opts.maxAnisotropyBit().set(0);

        // Vanilla Performance
        opts.inactivityFpsLimit().set(InactivityFpsLimit.AFK);

        // Sodium Quality
        sodium.quality.hiddenFluidCulling = true;
        sodium.quality.improvedFluidShaping = false;

        // Sodium Performance
        sodium.performance.chunkBuilderThreads = 0;
        sodium.performance.useBlockFaceCulling = true;
        sodium.performance.useFogOcclusion = true;
        sodium.performance.useEntityCulling = true;
        sodium.performance.animateOnlyVisibleTextures = true;
        sodium.performance.useNoErrorGLContext = true;
        sodium.performance.chunkBuildDeferMode = DeferMode.ALWAYS;
        sodium.performance.quadSplittingMode = QuadSplittingMode.SAFE;

        // Sodium Advanced
        sodium.advanced.enableMemoryTracing = false;
        sodium.advanced.useAdvancedStagingBuffers = true;
        sodium.advanced.cpuRenderAheadLimit = 3;

        saveOptions(opts, sodium);
        setActiveProfile("balanced");
    }

    public static void applyQuality() {
        Options opts = Minecraft.getInstance().options;
        SodiumOptions sodium = SodiumClientMod.options();

        // General
        opts.renderDistance().set(16);
        opts.simulationDistance().set(16);
        opts.gamma().set(0.5);
        opts.guiScale().set(0);
        opts.fullscreen().set(false);
        opts.enableVsync().set(true);
        opts.framerateLimit().set(260);
        opts.attackIndicator().set(AttackIndicatorStatus.CROSSHAIR);
        opts.showAutosaveIndicator().set(true);

        // Quality — maximum
        opts.improvedTransparency().set(true);
        opts.cloudStatus().set(CloudStatus.FANCY);
        opts.cloudRange().set(32);
        opts.weatherRadius().set(10);
        opts.cutoutLeaves().set(true);
        opts.particles().set(ParticleStatus.ALL);
        opts.ambientOcclusion().set(true);
        opts.biomeBlendRadius().set(7);
        opts.entityDistanceScaling().set(1.5);
        opts.entityShadows().set(true);
        opts.vignette().set(true);
        opts.chunkSectionFadeInTime().set(1.0);
        opts.mipmapLevels().set(4);
        opts.textureFiltering().set(TextureFilteringMethod.RGSS);
        opts.maxAnisotropyBit().set(3);

        // Vanilla Performance
        opts.inactivityFpsLimit().set(InactivityFpsLimit.AFK);

        // Sodium Quality
        sodium.quality.hiddenFluidCulling = true;
        sodium.quality.improvedFluidShaping = true;

        // Sodium Performance — all OFF for quality
        sodium.performance.chunkBuilderThreads = 0;
        sodium.performance.useBlockFaceCulling = false;
        sodium.performance.useFogOcclusion = false;
        sodium.performance.useEntityCulling = false;
        sodium.performance.animateOnlyVisibleTextures = false;
        sodium.performance.useNoErrorGLContext = true;
        sodium.performance.chunkBuildDeferMode = DeferMode.ZERO_FRAMES;
        sodium.performance.quadSplittingMode = QuadSplittingMode.SAFE;

        // Sodium Advanced
        sodium.advanced.enableMemoryTracing = false;
        sodium.advanced.useAdvancedStagingBuffers = true;
        sodium.advanced.cpuRenderAheadLimit = 3;

        saveOptions(opts, sodium);
        setActiveProfile("quality");
    }

    private static void saveOptions(Options opts, SodiumOptions sodium) {
        opts.save();
        try {
            SodiumOptions.writeToDisk(sodium);
        } catch (IOException e) {
            SodiumProfiles.LOGGER.error("Failed to save Sodium options", e);
        }
    }
}
