package dev.arbe.sodiumprofiles.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ProfilesScreen extends Screen {

    private final Screen parent;

    public ProfilesScreen(Screen parent) {
        super(Component.translatable("sodium-profiles.screen.custom_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ProfileConfig config = ProfileConfig.getInstance();
        int centerX = this.width / 2;
        int buttonW = 220;
        int buttonH = 20;
        int gap = 24;
        int y = 40;

        // ── Custom Profiles ──
        List<String> names = new ArrayList<>(config.getCustomProfileNames());
        if (!names.isEmpty()) {
            for (String name : names) {
                Component label = Component.literal(name);
                if (name.equals(config.activeProfileName)) {
                    label = Component.literal("\u2714 ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(name));
                }
                final String profileName = name;

                // Apply button
                this.addRenderableWidget(Button.builder(label, btn -> {
                    ProfileSettings settings = config.getCustomProfile(profileName);
                    if (settings != null) {
                        settings.apply();
                        config.activeProfileName = profileName;
                        config.save();
                        this.rebuildWidgets();
                    }
                }).bounds(centerX - buttonW / 2, y, buttonW - 25, buttonH).build());

                // Delete button
                this.addRenderableWidget(Button.builder(
                        Component.literal("\u2716").withStyle(ChatFormatting.RED),
                        btn -> {
                            config.removeCustomProfile(profileName);
                            config.save();
                            this.rebuildWidgets();
                        }).bounds(centerX + buttonW / 2 - 22, y, 22, buttonH).build());
                y += gap;
            }
        } else {
            // No custom profiles yet message
            y += 20;
        }

        y += 10;

        // ── Create New Profile ──
        this.addRenderableWidget(Button.builder(
                Component.translatable("sodium-profiles.button.create").withStyle(ChatFormatting.GREEN),
                btn -> Minecraft.getInstance().setScreen(new CreateProfileScreen(this)))
                .bounds(centerX - buttonW / 2, y, buttonW, buttonH).build());

        // ── Back ──
        this.addRenderableWidget(Button.builder(
                Component.translatable("sodium-profiles.button.back"),
                btn -> this.onClose())
                .bounds(centerX - buttonW / 2, this.height - 30, buttonW, buttonH).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // Title
        graphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);

        // Empty state
        if (ProfileConfig.getInstance().getCustomProfileNames().isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("sodium-profiles.custom.empty"),
                    centerX, this.height / 2 - 20, 0x888888);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }
}
