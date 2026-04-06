package dev.arbe.sodiumprofiles.client;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.DeferMode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.server.level.ParticleStatus;

/**
 * Captures and applies a full snapshot of ALL vanilla + Sodium graphics
 * settings.
 */
public class ProfileSettings {

    // ── Vanilla General ──
    public int renderDistance = 12;
    public int simulationDistance = 12;
    public double gamma = 0.5;
    public int guiScale = 0;
    public boolean fullscreen = false;
    public boolean enableVsync = true;
    public int framerateLimit = 120;
    public String attackIndicator = "CROSSHAIR";
    public boolean showAutosaveIndicator = true;

    // ── Vanilla Quality ──
    public boolean improvedTransparency = false;
    public String cloudStatus = "FANCY";
    public int cloudRange = 16;
    public int weatherRadius = 8;
    public boolean cutoutLeaves = true;
    public String particles = "ALL";
    public boolean ambientOcclusion = true;
    public int biomeBlendRadius = 5;
    public double entityDistanceScaling = 1.0;
    public boolean entityShadows = true;
    public boolean vignette = true;
    public double chunkSectionFadeInTime = 0.0;
    public int mipmapLevels = 4;
    public String textureFiltering = "LINEAR";
    public int maxAnisotropyBit = 0;

    // ── Vanilla Performance ──
    public String inactivityFpsLimit = "AFK";

    // ── Sodium Quality ──
    public boolean hiddenFluidCulling = true;
    public boolean improvedFluidShaping = false;

    // ── Sodium Performance ──
    public int chunkBuilderThreads = 0;
    public String chunkBuildDeferMode = "ALWAYS";
    public boolean animateOnlyVisibleTextures = true;
    public boolean useEntityCulling = true;
    public boolean useFogOcclusion = true;
    public boolean useBlockFaceCulling = true;
    public boolean useNoErrorGLContext = true;
    public String quadSplittingMode = "SAFE";

    // ── Sodium Advanced ──
    public boolean enableMemoryTracing = false;
    public boolean useAdvancedStagingBuffers = true;
    public int cpuRenderAheadLimit = 3;

    public ProfileSettings() {
    }

    /**
     * Captures ALL current settings into this snapshot.
     */
    public static ProfileSettings captureCurrentSettings() {
        ProfileSettings s = new ProfileSettings();
        Options opts = Minecraft.getInstance().options;
        SodiumOptions sodium = SodiumClientMod.options();

        // General
        s.renderDistance = opts.renderDistance().get();
        s.simulationDistance = opts.simulationDistance().get();
        s.gamma = opts.gamma().get();
        s.guiScale = opts.guiScale().get();
        s.fullscreen = opts.fullscreen().get();
        s.enableVsync = opts.enableVsync().get();
        s.framerateLimit = opts.framerateLimit().get();
        s.attackIndicator = opts.attackIndicator().get().name();
        s.showAutosaveIndicator = opts.showAutosaveIndicator().get();

        // Quality
        s.improvedTransparency = opts.improvedTransparency().get();
        s.cloudStatus = opts.cloudStatus().get().name();
        s.cloudRange = opts.cloudRange().get();
        s.weatherRadius = opts.weatherRadius().get();
        s.cutoutLeaves = opts.cutoutLeaves().get();
        s.particles = opts.particles().get().name();
        s.ambientOcclusion = opts.ambientOcclusion().get();
        s.biomeBlendRadius = opts.biomeBlendRadius().get();
        s.entityDistanceScaling = opts.entityDistanceScaling().get();
        s.entityShadows = opts.entityShadows().get();
        s.vignette = opts.vignette().get();
        s.chunkSectionFadeInTime = opts.chunkSectionFadeInTime().get();
        s.mipmapLevels = opts.mipmapLevels().get();
        s.textureFiltering = opts.textureFiltering().get().name();
        s.maxAnisotropyBit = opts.maxAnisotropyBit().get();

        // Vanilla Performance
        s.inactivityFpsLimit = opts.inactivityFpsLimit().get().name();

        // Sodium Quality
        s.hiddenFluidCulling = sodium.quality.hiddenFluidCulling;
        s.improvedFluidShaping = sodium.quality.improvedFluidShaping;

        // Sodium Performance
        s.chunkBuilderThreads = sodium.performance.chunkBuilderThreads;
        s.chunkBuildDeferMode = sodium.performance.chunkBuildDeferMode.name();
        s.animateOnlyVisibleTextures = sodium.performance.animateOnlyVisibleTextures;
        s.useEntityCulling = sodium.performance.useEntityCulling;
        s.useFogOcclusion = sodium.performance.useFogOcclusion;
        s.useBlockFaceCulling = sodium.performance.useBlockFaceCulling;
        s.useNoErrorGLContext = sodium.performance.useNoErrorGLContext;
        s.quadSplittingMode = sodium.performance.quadSplittingMode.name();

        // Sodium Advanced
        s.enableMemoryTracing = sodium.advanced.enableMemoryTracing;
        s.useAdvancedStagingBuffers = sodium.advanced.useAdvancedStagingBuffers;
        s.cpuRenderAheadLimit = sodium.advanced.cpuRenderAheadLimit;

        return s;
    }

    /**
     * Applies ALL settings from this snapshot.
     */
    public void apply() {
        Options opts = Minecraft.getInstance().options;
        SodiumOptions sodium = SodiumClientMod.options();

        // General
        opts.renderDistance().set(this.renderDistance);
        opts.simulationDistance().set(this.simulationDistance);
        opts.gamma().set(this.gamma);
        opts.guiScale().set(this.guiScale);
        opts.fullscreen().set(this.fullscreen);
        opts.enableVsync().set(this.enableVsync);
        opts.framerateLimit().set(this.framerateLimit);
        try {
            opts.attackIndicator().set(
                    AttackIndicatorStatus.valueOf(this.attackIndicator));
        } catch (Exception ignored) {
        }
        opts.showAutosaveIndicator().set(this.showAutosaveIndicator);

        // Quality
        opts.improvedTransparency().set(this.improvedTransparency);
        opts.cloudStatus().set(CloudStatus.valueOf(this.cloudStatus));
        opts.cloudRange().set(this.cloudRange);
        opts.weatherRadius().set(this.weatherRadius);
        opts.cutoutLeaves().set(this.cutoutLeaves);
        opts.particles().set(ParticleStatus.valueOf(this.particles));
        opts.ambientOcclusion().set(this.ambientOcclusion);
        opts.biomeBlendRadius().set(this.biomeBlendRadius);
        opts.entityDistanceScaling().set(this.entityDistanceScaling);
        opts.entityShadows().set(this.entityShadows);
        opts.vignette().set(this.vignette);
        opts.chunkSectionFadeInTime().set(this.chunkSectionFadeInTime);
        opts.mipmapLevels().set(this.mipmapLevels);
        try {
            opts.textureFiltering().set(
                    TextureFilteringMethod.valueOf(this.textureFiltering));
        } catch (Exception ignored) {
        }
        opts.maxAnisotropyBit().set(this.maxAnisotropyBit);

        // Vanilla Performance
        try {
            opts.inactivityFpsLimit().set(
                    InactivityFpsLimit.valueOf(this.inactivityFpsLimit));
        } catch (Exception ignored) {
        }

        // Sodium Quality
        sodium.quality.hiddenFluidCulling = this.hiddenFluidCulling;
        sodium.quality.improvedFluidShaping = this.improvedFluidShaping;

        // Sodium Performance
        sodium.performance.chunkBuilderThreads = this.chunkBuilderThreads;
        try {
            sodium.performance.chunkBuildDeferMode = DeferMode.valueOf(this.chunkBuildDeferMode);
        } catch (Exception ignored) {
        }
        sodium.performance.animateOnlyVisibleTextures = this.animateOnlyVisibleTextures;
        sodium.performance.useEntityCulling = this.useEntityCulling;
        sodium.performance.useFogOcclusion = this.useFogOcclusion;
        sodium.performance.useBlockFaceCulling = this.useBlockFaceCulling;
        sodium.performance.useNoErrorGLContext = this.useNoErrorGLContext;
        try {
            sodium.performance.quadSplittingMode = QuadSplittingMode.valueOf(this.quadSplittingMode);
        } catch (Exception ignored) {
        }

        // Sodium Advanced
        sodium.advanced.enableMemoryTracing = this.enableMemoryTracing;
        sodium.advanced.useAdvancedStagingBuffers = this.useAdvancedStagingBuffers;
        sodium.advanced.cpuRenderAheadLimit = this.cpuRenderAheadLimit;

        // Save both
        opts.save();
        try {
            SodiumOptions.writeToDisk(sodium);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save Sodium options", e);
        }
    }
}
