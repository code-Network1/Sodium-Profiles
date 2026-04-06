package dev.arbe.sodiumprofiles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arbe.sodiumprofiles.SodiumProfiles;
import dev.arbe.sodiumprofiles.client.SodiumProfilesClient;
import dev.arbe.sodiumprofiles.client.SodiumProfilesConfigEntry;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalButtonOption;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.options.control.ExternalButtonControl$ExternalButtonControlElement", remap = false)
public abstract class ExternalButtonControlElementMixin {

    // ── Click detection state (all static to survive instance recreation) ──
    @Unique
    private static boolean sodiumProfiles$mouseWasDown = false;
    @Unique
    private static boolean sodiumProfiles$clickConsumed = false;
    @Unique
    private static long sodiumProfiles$lastFrameCount = -1;
    @Unique
    private static boolean sodiumProfiles$frameMouseDown = false;

    // Captured render position from Sodium's drawString (updated per render call)
    @Unique
    private static int sodiumProfiles$capturedTextX = 0;
    @Unique
    private static int sodiumProfiles$capturedTextY = 0;

    @Unique
    private static Field sodiumProfiles$consumerField;
    @Unique
    private static Field sodiumProfiles$screenField;
    @Unique
    private static Field sodiumProfiles$optionField;
    @Unique
    private static Method sodiumProfiles$isMouseOverMethod;
    @Unique
    private static Method sodiumProfiles$getDimensionsMethod;
    @Unique
    private static boolean sodiumProfiles$fieldsInitialized = false;

    @Unique
    private static void sodiumProfiles$initFields(Object instance) {
        if (sodiumProfiles$fieldsInitialized)
            return;
        sodiumProfiles$fieldsInitialized = true;
        try {
            Class<?> clazz = instance.getClass();
            sodiumProfiles$consumerField = clazz.getDeclaredField("currentScreenConsumer");
            sodiumProfiles$consumerField.setAccessible(true);
            sodiumProfiles$screenField = clazz.getDeclaredField("screen");
            sodiumProfiles$screenField.setAccessible(true);
            sodiumProfiles$optionField = clazz.getDeclaredField("option");
            sodiumProfiles$optionField.setAccessible(true);
            // Find isMouseOver (method_25405) in class hierarchy
            Class<?> walk = clazz;
            while (walk != null && sodiumProfiles$isMouseOverMethod == null) {
                try {
                    sodiumProfiles$isMouseOverMethod = walk.getDeclaredMethod("method_25405", double.class,
                            double.class);
                    sodiumProfiles$isMouseOverMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    walk = walk.getSuperclass();
                }
            }
            // Find getDimensions() in class hierarchy (Sodium's AbstractWidget method)
            Class<?> walk4 = clazz;
            while (walk4 != null && sodiumProfiles$getDimensionsMethod == null) {
                try {
                    sodiumProfiles$getDimensionsMethod = walk4.getDeclaredMethod("getDimensions");
                    sodiumProfiles$getDimensionsMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    walk4 = walk4.getSuperclass();
                }
            }
            SodiumProfiles.LOGGER.info(
                    "[SodiumProfiles] Reflection init OK: consumer={}, screen={}, option={}, isMouseOver={}",
                    sodiumProfiles$consumerField != null, sodiumProfiles$screenField != null,
                    sodiumProfiles$optionField != null, sodiumProfiles$isMouseOverMethod != null);
        } catch (Exception e) {
            SodiumProfiles.LOGGER.error("[SodiumProfiles] Failed to init reflection fields", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Unique
    private Consumer<Screen> sodiumProfiles$getConsumer() {
        try {
            return (Consumer<Screen>) sodiumProfiles$consumerField.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private Screen sodiumProfiles$getScreen() {
        try {
            return (Screen) sodiumProfiles$screenField.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private ExternalButtonOption sodiumProfiles$getOption() {
        try {
            return (ExternalButtonOption) sodiumProfiles$optionField.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private boolean sodiumProfiles$isMouseOver(int mouseX, int mouseY) {
        try {
            if (sodiumProfiles$isMouseOverMethod != null) {
                return (boolean) sodiumProfiles$isMouseOverMethod.invoke(this, (double) mouseX, (double) mouseY);
            }
        } catch (Exception e) {
            // fallback below
        }
        return false;
    }

    @WrapOperation(method = "method_25394", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/options/control/ExternalButtonControl;formatExternalButtonText(ZLnet/caffeinemc/mods/sodium/client/gui/ColorTheme;)Lnet/minecraft/class_2561;"))
    private Component sodiumProfiles$wrapButtonText(boolean enabled, ColorTheme theme, Operation<Component> original) {
        sodiumProfiles$initFields(this);
        Consumer<Screen> consumer = sodiumProfiles$getConsumer();
        if (consumer != null) {
            Component label = SodiumProfilesConfigEntry.BUTTON_LABELS.get(consumer);
            if (label != null) {
                return label;
            }
        }
        return original.call(enabled, theme);
    }

    // Shift button text left to make room for ✖ delete zone & capture actual render
    // X
    @ModifyArg(method = "method_25394", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/options/control/ExternalButtonControl$ExternalButtonControlElement;drawString(Lnet/minecraft/class_332;Lnet/minecraft/class_2561;III)V"), index = 2)
    private int sodiumProfiles$shiftTextX(int x) {
        Consumer<Screen> consumer = sodiumProfiles$getConsumer();
        if (consumer != null && consumer == SodiumProfilesConfigEntry.ACTIVE_CONSUMER) {
            // Center the text: Sodium passes x = getLimitX() - 6 - textWidth
            // We want x = btnX + (btnWidth - textWidth) / 2
            // From Sodium: x = limitX - 6 - textWidth, so textWidth = limitX - 6 - x
            // and limitX = btnX + btnWidth, so center = btnX + (btnWidth - textWidth) / 2
            try {
                if (sodiumProfiles$getDimensionsMethod != null) {
                    Object dim = sodiumProfiles$getDimensionsMethod.invoke(this);
                    if (dim != null) {
                        Class<?> dimClass = dim.getClass();
                        int btnX = (int) dimClass.getMethod("x").invoke(dim);
                        int btnWidth = (int) dimClass.getMethod("width").invoke(dim);
                        int limitX = btnX + btnWidth;
                        int textWidth = limitX - 6 - x;
                        sodiumProfiles$capturedTextX = btnX + (btnWidth - textWidth) / 2;
                        return sodiumProfiles$capturedTextX;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (consumer != null && SodiumProfilesConfigEntry.DELETE_ACTIONS.containsKey(consumer)
                && !SodiumProfilesConfigEntry.DISABLED_CONSUMERS.contains(consumer)) {
            sodiumProfiles$capturedTextX = x;
            return x - 18;
        }
        sodiumProfiles$capturedTextX = x;
        return x;
    }

    // Capture actual render Y from Sodium's drawString
    @ModifyArg(method = "method_25394", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/options/control/ExternalButtonControl$ExternalButtonControlElement;drawString(Lnet/minecraft/class_332;Lnet/minecraft/class_2561;III)V"), index = 3)
    private int sodiumProfiles$captureTextY(int y) {
        sodiumProfiles$capturedTextY = y;
        return y;
    }

    // Cancel Sodium's original click handler for our buttons — we handle clicks in
    // afterRender
    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true)
    private void sodiumProfiles$cancelOriginalClick(CallbackInfoReturnable<Boolean> cir) {
        sodiumProfiles$initFields(this);
        Consumer<Screen> consumer = sodiumProfiles$getConsumer();
        if (consumer != null && SodiumProfilesConfigEntry.OUR_CONSUMERS.contains(consumer)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25394", at = @At("RETURN"))
    private void sodiumProfiles$afterRender(GuiGraphics graphics, int mouseX, int mouseY, float delta,
            CallbackInfo ci) {
        sodiumProfiles$initFields(this);
        Consumer<Screen> consumer = sodiumProfiles$getConsumer();
        if (consumer == null || !SodiumProfilesConfigEntry.OUR_CONSUMERS.contains(consumer))
            return;

        ExternalButtonOption opt = sodiumProfiles$getOption();
        if (opt == null || !opt.isEnabled())
            return;

        // ── Separate delete zone for custom profiles (hidden for active profiles) ──
        final boolean isDisabledProfile = SodiumProfilesConfigEntry.DISABLED_CONSUMERS.contains(consumer);
        final Consumer<Screen> deleteAction = isDisabledProfile ? null
                : SodiumProfilesConfigEntry.DELETE_ACTIONS.get(consumer);
        boolean mouseInDeleteZone = false;
        final int DELETE_ZONE_WIDTH = 18;

        if (deleteAction != null) {
            var font = Minecraft.getInstance().font;

            // Use the captured Y from Sodium's own drawString (accounts for scroll)
            // Sodium draws text at y = getCenterY() - 4, so centerY = capturedTextY + 4
            int textY = sodiumProfiles$capturedTextY;
            int textHeight = font.lineHeight; // typically 9

            // Sodium draws text at x = getLimitX() - 6 - textWidth
            // So getLimitX() = capturedTextX + 6 + textWidth
            // But with our -18 shift, capturedTextX is the ORIGINAL x (before shift)
            // So limitX = capturedTextX + 6 + textWidth
            // We need limitX from dim for the right edge
            int btnWidth = 0, btnHeight = 18;
            int limitX = 0;
            try {
                if (sodiumProfiles$getDimensionsMethod != null) {
                    Object dim = sodiumProfiles$getDimensionsMethod.invoke(this);
                    if (dim != null) {
                        Class<?> dimClass = dim.getClass();
                        int btnX = (int) dimClass.getMethod("x").invoke(dim);
                        btnWidth = (int) dimClass.getMethod("width").invoke(dim);
                        btnHeight = (int) dimClass.getMethod("height").invoke(dim);
                        limitX = btnX + btnWidth;
                    }
                }
            } catch (Exception ignored) {
            }

            // Position delete zone using captured Y (scroll-correct) and dim X (horizontal
            // doesn't scroll)
            int deleteBoxLeft = limitX - DELETE_ZONE_WIDTH;
            int deleteBoxTop = textY - 2;
            int deleteBoxBottom = textY + textHeight + 2;

            mouseInDeleteZone = mouseX >= deleteBoxLeft && mouseX <= limitX
                    && mouseY >= deleteBoxTop && mouseY <= deleteBoxBottom;

            // Draw separator line
            graphics.fill(deleteBoxLeft, deleteBoxTop, deleteBoxLeft + 1, deleteBoxBottom, 0xFF555555);

            // Draw delete zone background on hover
            if (mouseInDeleteZone) {
                graphics.fill(deleteBoxLeft + 1, deleteBoxTop, limitX, deleteBoxBottom, 0x60FF0000);
            }

            // Draw ✖ centered in delete zone
            String sym = "\u2716";
            int symWidth = font.width(sym);
            int symX = deleteBoxLeft + (DELETE_ZONE_WIDTH - symWidth) / 2;
            int symY = textY;
            int symColor = mouseInDeleteZone ? 0xFFFFFFFF : 0xFFFF5555;
            graphics.drawString(font, sym, symX, symY, symColor);
        }

        // ── Frame-based click detection ──
        long currentFrame = Minecraft.getInstance().getFrameTimeNs();
        if (currentFrame != sodiumProfiles$lastFrameCount) {
            sodiumProfiles$lastFrameCount = currentFrame;
            long window = Minecraft.getInstance().getWindow().handle();
            boolean mouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

            if (!mouseDown) {
                sodiumProfiles$mouseWasDown = false;
                sodiumProfiles$clickConsumed = false;
            } else if (!sodiumProfiles$mouseWasDown) {
                sodiumProfiles$mouseWasDown = true;
                sodiumProfiles$clickConsumed = false;
            }
            sodiumProfiles$frameMouseDown = mouseDown;
        }

        if (sodiumProfiles$frameMouseDown && !sodiumProfiles$clickConsumed
                && sodiumProfiles$isMouseOver(mouseX, mouseY)) {
            // Skip clicks on active (disabled) profiles — but still allow delete zone
            boolean isDisabled = SodiumProfilesConfigEntry.DISABLED_CONSUMERS.contains(consumer);
            if (isDisabled && !(mouseInDeleteZone && deleteAction != null)) {
                sodiumProfiles$clickConsumed = true;
                return;
            }
            sodiumProfiles$clickConsumed = true;
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            final Screen capturedScreen = sodiumProfiles$getScreen();
            if (mouseInDeleteZone && deleteAction != null) {
                SodiumProfiles.LOGGER.info("[SodiumProfiles] Delete icon clicked for profile");
                SodiumProfilesClient.deferAction(() -> deleteAction.accept(capturedScreen));
            } else {
                SodiumProfiles.LOGGER.info("[SodiumProfiles] Button clicked, loading profile");
                SodiumProfilesClient.deferAction(() -> consumer.accept(capturedScreen));
            }
        }
    }
}
