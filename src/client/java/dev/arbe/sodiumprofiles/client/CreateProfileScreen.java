package dev.arbe.sodiumprofiles.client;

import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CreateProfileScreen extends Screen {

    public static final int MAX_PROFILES = 5;

    private final Screen parent;
    private EditBox nameField;
    private Button saveButton;

    // Sodium-style colors
    private static final int OVERLAY_COLOR = 0x70090909;
    private static final int BOX_BG = 0xFF171717;
    private static final int BOX_BORDER = 0xFF121212;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;

    private static final int BOX_WIDTH = 260;
    private static final int BOX_HEIGHT = 100;
    private static final int PADDING = 10;

    public CreateProfileScreen(Screen parent) {
        super(Component.translatable("sodium-profiles.screen.create_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int boxLeft = centerX - BOX_WIDTH / 2;
        int boxTop = (this.height - BOX_HEIGHT) / 2;
        int contentLeft = boxLeft + PADDING;
        int contentWidth = BOX_WIDTH - PADDING * 2;

        // Name input field
        int fieldY = boxTop + PADDING + 14 + 6;
        this.nameField = new EditBox(this.font, contentLeft, fieldY, contentWidth, 20,
                Component.translatable("sodium-profiles.field.name"));
        this.nameField.setMaxLength(32);
        this.nameField.setHint(Component.translatable("sodium-profiles.field.name.hint"));
        this.nameField.setResponder(text -> {
            if (this.saveButton != null) {
                this.saveButton.active = !text.trim().isEmpty();
            }
        });
        this.addRenderableWidget(this.nameField);

        // Save + Cancel buttons
        int btnY = fieldY + 26;
        int halfW = (contentWidth - 4) / 2;

        this.saveButton = this.addRenderableWidget(Button.builder(
                Component.translatable("sodium-profiles.button.save"),
                btn -> {
                    String name = this.nameField.getValue().trim();
                    if (!name.isEmpty()) {
                        UserProfiles.saveProfile(name);
                        UserProfiles.invalidate();
                        int newIndex = UserProfiles.getProfileCount() - 1;
                        Profiles.setActiveProfile("user_" + newIndex);
                        // Rebuild Sodium config so the new profile button appears
                        ConfigManager.registerConfigsLate();
                        Minecraft.getInstance().setScreen(this.parent);
                    }
                })
                .bounds(contentLeft, btnY, halfW, 20).build());
        this.saveButton.active = false;

        this.addRenderableWidget(Button.builder(
                Component.translatable("sodium-profiles.button.cancel"),
                btn -> this.onClose())
                .bounds(contentLeft + halfW + 4, btnY, halfW, 20).build());

        this.setInitialFocus(this.nameField);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Semi-transparent overlay
        graphics.fill(0, 0, this.width, this.height, OVERLAY_COLOR);

        int centerX = this.width / 2;
        int boxLeft = centerX - BOX_WIDTH / 2;
        int boxTop = (this.height - BOX_HEIGHT) / 2;

        // Box background + border (Sodium style)
        graphics.fill(boxLeft, boxTop, boxLeft + BOX_WIDTH, boxTop + BOX_HEIGHT, BOX_BG);
        graphics.fill(boxLeft, boxTop, boxLeft + BOX_WIDTH, boxTop + 1, BOX_BORDER);
        graphics.fill(boxLeft, boxTop + BOX_HEIGHT - 1, boxLeft + BOX_WIDTH, boxTop + BOX_HEIGHT, BOX_BORDER);
        graphics.fill(boxLeft, boxTop, boxLeft + 1, boxTop + BOX_HEIGHT, BOX_BORDER);
        graphics.fill(boxLeft + BOX_WIDTH - 1, boxTop, boxLeft + BOX_WIDTH, boxTop + BOX_HEIGHT, BOX_BORDER);

        // Title
        graphics.drawCenteredString(this.font, this.title, centerX, boxTop + PADDING, TEXT_WHITE);

        // Profile count
        int count = UserProfiles.getProfileCount();
        String countText = count + " / " + UserProfiles.MAX_PROFILES;
        int countColor = count >= UserProfiles.MAX_PROFILES ? 0xFFFF5555 : TEXT_GRAY;
        graphics.drawString(this.font, countText,
                boxLeft + BOX_WIDTH - PADDING - this.font.width(countText),
                boxTop + PADDING, countColor);

        // Render widgets
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }
}
