package dev.arbe.sodiumprofiles.client;

import java.io.IOException;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.DeferMode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

public class ProfileApplier {

    public static void saveSodiumOptions() {
        try {
            SodiumOptions.writeToDisk(SodiumClientMod.options());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Sodium options", e);
        }
    }

    public static void apply(ProfileType profile) {
        Options vanillaOpts = Minecraft.getInstance().options;
        SodiumOptions sodiumOpts = SodiumClientMod.options();

        switch (profile) {
            case ULTRA_PERFORMANCE -> applyUltraPerformance(vanillaOpts, sodiumOpts);
            case PERFORMANCE -> applyPerformance(vanillaOpts, sodiumOpts);
            case BALANCED -> applyBalanced(vanillaOpts, sodiumOpts);
            case QUALITY -> applyQuality(vanillaOpts, sodiumOpts);
        }

        // Persist both configs
        vanillaOpts.save();
        try {
            SodiumOptions.writeToDisk(sodiumOpts);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Sodium options", e);
        }
    }

    private static void applyUltraPerformance(Options opts, SodiumOptions sodium) {
        // ── General ──
        opts.renderDistance().set(4);
        opts.simulationDistance().set(5);
        opts.gamma().set(0.5);
        opts.enableVsync().set(false);
        opts.framerateLimit().set(260);
        opts.showAutosaveIndicator().set(false);

        // ── Quality — absolute minimum ──
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
        opts.maxAnisotropyBit().set(0);

        // ── Vanilla Performance ──
        opts.inactivityFpsLimit().set(InactivityFpsLimit.AFK);

        // ── Sodium Quality ──
        sodium.quality.hiddenFluidCulling = true;
        sodium.quality.improvedFluidShaping = false;

        // ── Sodium Performance — all optimizations ON ──
        sodium.performance.useBlockFaceCulling = true;
        sodium.performance.useFogOcclusion = true;
        sodium.performance.useEntityCulling = true;
        sodium.performance.animateOnlyVisibleTextures = true;
        sodium.performance.useNoErrorGLContext = true;
        sodium.performance.chunkBuildDeferMode = DeferMode.ALWAYS;

        // ── Sodium Advanced ──
        sodium.advanced.enableMemoryTracing = false;
        sodium.advanced.useAdvancedStagingBuffers = true;
        sodium.advanced.cpuRenderAheadLimit = 3;
    }

    private static void applyPerformance(Options opts, SodiumOptions sodium) {
        // ── General ──
        opts.renderDistance().set(8);
        opts.simulationDistance().set(8);
        opts.gamma().set(0.5);
        opts.enableVsync().set(false);
        opts.framerateLimit().set(260);
        opts.showAutosaveIndicator().set(true);

        // ── Quality ──
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
        opts.maxAnisotropyBit().set(0);

        // ── Vanilla Performance ──
        opts.inactivityFpsLimit().set(InactivityFpsLimit.AFK);

        // ── Sodium Quality ──
        sodium.quality.hiddenFluidCulling = true;
        sodium.quality.improvedFluidShaping = false;

        // ── Sodium Performance ──
        sodium.performance.useBlockFaceCulling = true;
        sodium.performance.useFogOcclusion = true;
        sodium.performance.useEntityCulling = true;
        sodium.performance.animateOnlyVisibleTextures = true;
        sodium.performance.useNoErrorGLContext = true;
        sodium.performance.chunkBuildDeferMode = DeferMode.ALWAYS;

        // ── Sodium Advanced ──
        sodium.advanced.enableMemoryTracing = false;
        sodium.advanced.useAdvancedStagingBuffers = true;
        sodium.advanced.cpuRenderAheadLimit = 3;
    }

    private static void applyBalanced(Options opts, SodiumOptions sodium) {
        // ── General ──
        opts.renderDistance().set(12);
        opts.simulationDistance().set(12);
        opts.gamma().set(0.5);
        opts.enableVsync().set(true);
        opts.framerateLimit().set(120);
        opts.showAutosaveIndicator().set(true);

        // ── Quality ──
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
        opts.maxAnisotropyBit().set(0);

        // ── Vanilla Performance ──
        opts.inactivityFpsLimit().set(InactivityFpsLimit.AFK);

        // ── Sodium Quality ──
        sodium.quality.hiddenFluidCulling = true;
        sodium.quality.improvedFluidShaping = false;

        // ── Sodium Performance ──
        sodium.performance.useBlockFaceCulling = true;
        sodium.performance.useFogOcclusion = true;
        sodium.performance.useEntityCulling = true;
        sodium.performance.animateOnlyVisibleTextures = true;
        sodium.performance.useNoErrorGLContext = true;
        sodium.performance.chunkBuildDeferMode = DeferMode.ALWAYS;

        // ── Sodium Advanced ──
        sodium.advanced.enableMemoryTracing = false;
        sodium.advanced.useAdvancedStagingBuffers = true;
        sodium.advanced.cpuRenderAheadLimit = 3;
    }

    private static void applyQuality(Options opts, SodiumOptions sodium) {
        // ── General ──
        opts.renderDistance().set(16);
        opts.simulationDistance().set(16);
        opts.gamma().set(0.5);
        opts.enableVsync().set(true);
        opts.framerateLimit().set(260);
        opts.showAutosaveIndicator().set(true);

        // ── Quality — maximum ──
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
        opts.maxAnisotropyBit().set(3);

        // ── Vanilla Performance ──
        opts.inactivityFpsLimit().set(InactivityFpsLimit.AFK);

        // ── Sodium Quality ──
        sodium.quality.hiddenFluidCulling = true;
        sodium.quality.improvedFluidShaping = true;

        // ── Sodium Performance — all optimizations OFF for quality ──
        sodium.performance.useBlockFaceCulling = false;
        sodium.performance.useFogOcclusion = false;
        sodium.performance.useEntityCulling = false;
        sodium.performance.animateOnlyVisibleTextures = false;
        sodium.performance.useNoErrorGLContext = true;
        sodium.performance.chunkBuildDeferMode = DeferMode.ZERO_FRAMES;

        // ── Sodium Advanced ──
        sodium.advanced.enableMemoryTracing = false;
        sodium.advanced.useAdvancedStagingBuffers = true;
        sodium.advanced.cpuRenderAheadLimit = 3;
    }
}
