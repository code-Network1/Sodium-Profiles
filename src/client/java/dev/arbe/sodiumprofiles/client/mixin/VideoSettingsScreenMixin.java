package dev.arbe.sodiumprofiles.client.mixin;

import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.OptionListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VideoSettingsScreen.class, remap = false)
public class VideoSettingsScreenMixin {

    @Shadow
    private OptionListWidget optionList;

    @Shadow
    private FlatButtonWidget applyButton;

    @Shadow
    private FlatButtonWidget undoButton;

    @Shadow
    private FlatButtonWidget closeButton;

    @Inject(method = "updateControls", at = @At("HEAD"), cancellable = true)
    private void sodiumProfiles$guardNullControls(int mouseX, int mouseY, CallbackInfo ci) {
        if (this.optionList == null || this.applyButton == null || this.undoButton == null
                || this.closeButton == null) {
            ci.cancel();
        }
    }
}
