package dev.arbe.sodiumprofiles.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import dev.arbe.sodiumprofiles.SodiumProfiles;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public class SodiumProfilesConfigEntry implements ConfigEntryPoint {

        public static final Set<Consumer<Screen>> OUR_CONSUMERS = Collections.newSetFromMap(new IdentityHashMap<>());
        public static final Map<Consumer<Screen>, Component> BUTTON_LABELS = new IdentityHashMap<>();

        // Maps each consumer to its profile ID for dynamic label refresh
        private static final Map<Consumer<Screen>, String> CONSUMER_PROFILE_IDS = new IdentityHashMap<>();

        // Maps each load consumer to its delete action (rendered as inline ✖ by mixin)
        public static final Map<Consumer<Screen>, Consumer<Screen>> DELETE_ACTIONS = new IdentityHashMap<>();

        // The active profile indicator consumer (for centering text in mixin)
        public static Consumer<Screen> ACTIVE_CONSUMER = null;

        // Consumers that should not respond to clicks (active profiles)
        public static final Set<Consumer<Screen>> DISABLED_CONSUMERS = Collections
                        .newSetFromMap(new IdentityHashMap<>());

        // Pending profile: set when user clicks a profile, applied when Apply is
        // pressed
        private static String pendingProfile = null;

        public static boolean hasPendingProfile() {
                return pendingProfile != null;
        }

        /**
         * Called when the user manually applies settings without selecting a profile.
         * Sets active profile to "custom".
         */
        public static void onManualSettingsApplied() {
                Profiles.setActiveProfile("custom");
                refreshLabels();
        }

        public static void clearPendingProfile() {
                pendingProfile = null;
                refreshLabels();
        }

        public static void applyPendingProfile() {
                if (pendingProfile == null)
                        return;
                String profileId = pendingProfile;
                pendingProfile = null;
                SodiumProfiles.LOGGER.info("[SodiumProfiles] Applying pending profile: {}", profileId);

                switch (profileId) {
                        case "ultra" -> Profiles.applyUltraPerformance();
                        case "performance" -> Profiles.applyPerformance();
                        case "balanced" -> Profiles.applyBalanced();
                        case "quality" -> Profiles.applyQuality();
                        default -> {
                                if (profileId.startsWith("user_")) {
                                        try {
                                                int index = Integer.parseInt(profileId.substring(5));
                                                UserProfiles.applyProfile(index);
                                                Profiles.setActiveProfile(profileId);
                                        } catch (NumberFormatException ignored) {
                                        }
                                }
                        }
                }
                refreshLabels();
        }

        /**
         * Refreshes all button labels based on current active + pending profile.
         */
        public static void refreshLabels() {
                String active = Profiles.getActiveProfile();
                DISABLED_CONSUMERS.clear();
                for (var entry : CONSUMER_PROFILE_IDS.entrySet()) {
                        Consumer<Screen> consumer = entry.getKey();
                        String profileId = entry.getValue();
                        if (profileId == null)
                                continue;

                        boolean isActive = profileId.equals(active);
                        boolean isPending = profileId.equals(pendingProfile);

                        if (profileId.startsWith("user_")) {
                                if (isPending && !isActive) {
                                        BUTTON_LABELS.put(consumer,
                                                        Component.literal("\u25CB Selected")
                                                                        .withStyle(Style.EMPTY.withColor(0xFFFF55)));
                                } else if (isActive) {
                                        BUTTON_LABELS.put(consumer,
                                                        Component.literal("\u2714 Active")
                                                                        .withStyle(Style.EMPTY.withColor(0x55FF55)));
                                        DISABLED_CONSUMERS.add(consumer);
                                } else {
                                        BUTTON_LABELS.put(consumer, Component.literal("Load"));
                                }
                        } else if (profileId.equals("__delete__") || profileId.equals("__create__")) {
                                // Don't change delete/create labels
                        } else if (profileId.equals("__active__")) {
                                // Update active indicator — only shows the actual active profile
                                String displayId = active;
                                if (displayId == null || displayId.isEmpty()) {
                                        BUTTON_LABELS.put(consumer,
                                                        Component.literal("None")
                                                                        .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
                                } else if (displayId.startsWith("user_")) {
                                        String name = findUserProfileName(displayId);
                                        BUTTON_LABELS.put(consumer,
                                                        Component.literal(name)
                                                                        .withStyle(Style.EMPTY.withColor(0xFF69B4)));
                                } else {
                                        BUTTON_LABELS.put(consumer,
                                                        Component.literal(Profiles.getPresetDisplayName(displayId))
                                                                        .withStyle(Style.EMPTY.withColor(0xFF69B4)));
                                }
                        } else {
                                // Preset profile (including "custom")
                                if (profileId.equals("custom")) {
                                        // Custom is always disabled — auto-activated only
                                        DISABLED_CONSUMERS.add(consumer);
                                        if (isActive) {
                                                BUTTON_LABELS.put(consumer,
                                                                Component.literal("\u2714 Active")
                                                                                .withStyle(Style.EMPTY
                                                                                                .withColor(0x55FFFF)));
                                        } else {
                                                BUTTON_LABELS.put(consumer, Component.empty());
                                        }
                                } else if (isPending && !isActive) {
                                        BUTTON_LABELS.put(consumer,
                                                        Component.literal("\u25CB Selected")
                                                                        .withStyle(Style.EMPTY.withColor(0xFFFF55)));
                                } else if (isActive) {
                                        BUTTON_LABELS.put(consumer,
                                                        Component.literal("\u2714 Active")
                                                                        .withStyle(Style.EMPTY.withColor(0x55FFFF)));
                                        DISABLED_CONSUMERS.add(consumer);
                                } else {
                                        BUTTON_LABELS.put(consumer, Component.empty());
                                }
                        }
                }
        }

        @Override
        public void registerConfigLate(ConfigBuilder builder) {
                OUR_CONSUMERS.clear();
                BUTTON_LABELS.clear();
                CONSUMER_PROFILE_IDS.clear();
                DELETE_ACTIONS.clear();
                DISABLED_CONSUMERS.clear();

                var mod = builder.registerOwnModOptions();
                mod.setNonTintedIcon(Identifier.parse("sodium-profiles:textures/icon.png"));

                var profilePage = builder.createOptionPage()
                                .setName(Component.translatable("sodium-profiles.page.profiles"));

                // ── Active Profile Indicator ──
                String active = Profiles.getActiveProfile();
                Component activeLabel;
                if (active == null || active.isEmpty()) {
                        activeLabel = Component.literal("None")
                                        .withStyle(Style.EMPTY.withColor(0xAAAAAA));
                } else if (active.startsWith("user_")) {
                        String profileName = findUserProfileName(active);
                        activeLabel = Component.literal(profileName)
                                        .withStyle(Style.EMPTY.withColor(0xFF69B4));
                } else {
                        activeLabel = Component.literal(Profiles.getPresetDisplayName(active))
                                        .withStyle(Style.EMPTY.withColor(0xFF69B4));
                }

                Consumer<Screen> activeConsumer = parentScreen -> {
                };
                OUR_CONSUMERS.add(activeConsumer);
                CONSUMER_PROFILE_IDS.put(activeConsumer, "__active__");
                BUTTON_LABELS.put(activeConsumer, activeLabel);
                ACTIVE_CONSUMER = activeConsumer;
                DISABLED_CONSUMERS.add(activeConsumer);

                profilePage.addOptionGroup(
                                builder.createOptionGroup()
                                                .setName(Component.translatable("sodium-profiles.group.active"))
                                                .addOption(
                                                                builder.createExternalButtonOption(
                                                                                Identifier.parse(
                                                                                                "sodium-profiles:active_indicator"))
                                                                                .setName(Component.literal(
                                                                                                "Active Profile"))
                                                                                .setTooltip(Component.translatable(
                                                                                                "sodium-profiles.active.tooltip"))
                                                                                .setScreenConsumer(activeConsumer)));

                // ── Preset Profiles (individual buttons) ──
                OptionGroupBuilder presetGroup = builder.createOptionGroup()
                                .setName(Component.translatable("sodium-profiles.group.presets"));

                addPresetButton(builder, presetGroup, "ultra",
                                Component.translatable("sodium-profiles.profile.ultra_performance"),
                                Component.translatable("sodium-profiles.preset.ultra.tooltip"),
                                parentScreen -> {
                                        SodiumProfiles.LOGGER.info(
                                                        "[SodiumProfiles] Selected Ultra Performance (pending Apply)");
                                        pendingProfile = "ultra";
                                        refreshLabels();
                                });

                addPresetButton(builder, presetGroup, "performance",
                                Component.translatable("sodium-profiles.profile.performance"),
                                Component.translatable("sodium-profiles.preset.performance.tooltip"),
                                parentScreen -> {
                                        SodiumProfiles.LOGGER
                                                        .info("[SodiumProfiles] Selected Performance (pending Apply)");
                                        pendingProfile = "performance";
                                        refreshLabels();
                                });

                addPresetButton(builder, presetGroup, "balanced",
                                Component.translatable("sodium-profiles.profile.balanced"),
                                Component.translatable("sodium-profiles.preset.balanced.tooltip"),
                                parentScreen -> {
                                        SodiumProfiles.LOGGER
                                                        .info("[SodiumProfiles] Selected Balanced (pending Apply)");
                                        pendingProfile = "balanced";
                                        refreshLabels();
                                });

                addPresetButton(builder, presetGroup, "quality",
                                Component.translatable("sodium-profiles.profile.quality"),
                                Component.translatable("sodium-profiles.preset.quality.tooltip"),
                                parentScreen -> {
                                        SodiumProfiles.LOGGER.info("[SodiumProfiles] Selected Quality (pending Apply)");
                                        pendingProfile = "quality";
                                        refreshLabels();
                                });

                // Custom (non-selectable, auto-activated when settings are manually changed)
                Consumer<Screen> customConsumer = parentScreen -> {
                };
                OUR_CONSUMERS.add(customConsumer);
                CONSUMER_PROFILE_IDS.put(customConsumer, "custom");
                boolean customActive = Profiles.isActive("custom");
                BUTTON_LABELS.put(customConsumer, customActive
                                ? Component.literal("\u2714 Active").withStyle(Style.EMPTY.withColor(0x55FFFF))
                                : Component.empty());
                DISABLED_CONSUMERS.add(customConsumer);
                presetGroup.addOption(
                                builder.createExternalButtonOption(
                                                Identifier.parse("sodium-profiles:preset_custom"))
                                                .setName(Component.literal("Custom"))
                                                .setTooltip(Component.literal(
                                                                "Automatically active when settings are manually changed."))
                                                .setScreenConsumer(customConsumer));

                profilePage.addOptionGroup(presetGroup);

                // ── Custom Profiles ──
                UserProfiles.invalidate();
                List<UserProfiles.SavedProfile> userProfiles = UserProfiles.getProfiles();

                OptionGroupBuilder customGroup = builder.createOptionGroup()
                                .setName(Component.literal("Custom Profiles (" + userProfiles.size() + "/5)"));

                for (var p : userProfiles) {
                        String key = "user_" + p.getIndex();
                        boolean isActive = Profiles.isActive(key);

                        Component displayName = Component.literal(p.getName());

                        Consumer<Screen> loadConsumer = parentScreen -> {
                                SodiumProfiles.LOGGER.info(
                                                "[SodiumProfiles] Selected custom profile: {} (pending Apply)",
                                                p.getName());
                                pendingProfile = key;
                                refreshLabels();
                        };
                        OUR_CONSUMERS.add(loadConsumer);
                        CONSUMER_PROFILE_IDS.put(loadConsumer, key);
                        BUTTON_LABELS.put(loadConsumer, isActive
                                        ? Component.literal("\u2714 Active").withStyle(Style.EMPTY.withColor(0x55FF55))
                                        : Component.literal("Load"));
                        if (isActive) {
                                DISABLED_CONSUMERS.add(loadConsumer);
                        }

                        customGroup.addOption(
                                        builder.createExternalButtonOption(
                                                        Identifier.parse("sodium-profiles:user_" + p.getIndex()))
                                                        .setName(displayName)
                                                        .setTooltip(Component.translatable(
                                                                        "sodium-profiles.custom.apply.tooltip",
                                                                        p.getName()))
                                                        .setScreenConsumer(loadConsumer));

                        // Store delete action for this profile (rendered as inline ✖ by mixin)
                        final int delIndex = p.getIndex();
                        final String delName = p.getName();
                        final String delKey = key;
                        DELETE_ACTIONS.put(loadConsumer, parentScreen -> {
                                SodiumProfiles.LOGGER.info("[SodiumProfiles] Deleting custom profile: {}", delName);
                                UserProfiles.deleteProfile(delIndex);
                                UserProfiles.invalidate();
                                if (Profiles.isActive(delKey)) {
                                        Profiles.setActiveProfile("");
                                }
                                pendingProfile = null;
                                net.caffeinemc.mods.sodium.client.config.ConfigManager.registerConfigsLate();
                                Minecraft.getInstance().setScreen(parentScreen);
                        });
                }

                // Create new profile button
                if (UserProfiles.canCreateMore()) {
                        Consumer<Screen> createConsumer = parentScreen -> {
                                SodiumProfiles.LOGGER.info("[SodiumProfiles] Opening Create Profile screen...");
                                Minecraft.getInstance().execute(() -> Minecraft.getInstance()
                                                .setScreen(new CreateProfileScreen(parentScreen)));
                        };
                        OUR_CONSUMERS.add(createConsumer);
                        CONSUMER_PROFILE_IDS.put(createConsumer, "__create__");
                        BUTTON_LABELS.put(createConsumer,
                                        Component.literal("\u002B").withStyle(Style.EMPTY.withColor(0xFF69B4)));

                        customGroup.addOption(
                                        builder.createExternalButtonOption(
                                                        Identifier.parse("sodium-profiles:create_profile"))
                                                        .setName(Component.translatable(
                                                                        "sodium-profiles.button.create_profile")
                                                                        .withStyle(Style.EMPTY.withColor(0xFF69B4)))
                                                        .setTooltip(Component.translatable(
                                                                        "sodium-profiles.button.create_profile.tooltip"))
                                                        .setScreenConsumer(createConsumer));
                }

                profilePage.addOptionGroup(customGroup);

                mod.addPage(profilePage);
        }

        private void addPresetButton(ConfigBuilder builder, OptionGroupBuilder group,
                        String profileId, Component name, Component tooltip,
                        Consumer<Screen> consumer) {
                boolean isActive = Profiles.isActive(profileId);
                Component displayName = name;

                OUR_CONSUMERS.add(consumer);
                CONSUMER_PROFILE_IDS.put(consumer, profileId);
                BUTTON_LABELS.put(consumer, isActive
                                ? Component.literal("\u2714 Active").withStyle(Style.EMPTY.withColor(0x55FFFF))
                                : Component.empty());
                if (isActive) {
                        DISABLED_CONSUMERS.add(consumer);
                }

                group.addOption(
                                builder.createExternalButtonOption(
                                                Identifier.parse("sodium-profiles:preset_" + profileId))
                                                .setName(displayName)
                                                .setTooltip(tooltip)
                                                .setScreenConsumer(consumer));
        }

        private static String findUserProfileName(String key) {
                if (!key.startsWith("user_"))
                        return key;
                try {
                        int index = Integer.parseInt(key.substring(5));
                        for (var p : UserProfiles.getProfiles()) {
                                if (p.getIndex() == index)
                                        return p.getName();
                        }
                } catch (NumberFormatException ignored) {
                }
                return key;
        }
}
