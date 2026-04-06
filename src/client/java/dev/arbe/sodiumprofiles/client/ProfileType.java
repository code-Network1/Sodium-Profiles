package dev.arbe.sodiumprofiles.client;

import net.minecraft.network.chat.Component;

public enum ProfileType {
    ULTRA_PERFORMANCE,
    PERFORMANCE,
    BALANCED,
    QUALITY;

    public Component getDisplayName() {
        return switch (this) {
            case ULTRA_PERFORMANCE -> Component.translatable("sodium-profiles.profile.ultra_performance");
            case PERFORMANCE -> Component.translatable("sodium-profiles.profile.performance");
            case BALANCED -> Component.translatable("sodium-profiles.profile.balanced");
            case QUALITY -> Component.translatable("sodium-profiles.profile.quality");
        };
    }
}
