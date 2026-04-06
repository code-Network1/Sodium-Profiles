package dev.arbe.sodiumprofiles.client.mixin;

import dev.arbe.sodiumprofiles.client.SodiumProfilesConfigEntry;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Config.class, remap = false)
public class SodiumConfigMixin {

    /**
     * When Sodium checks if any option has changed (to enable/disable Apply
     * button),
     * also return true if we have a pending profile selection.
     */
    @Inject(method = "anyOptionChanged", at = @At("RETURN"), cancellable = true)
    private void sodiumProfiles$anyOptionChanged(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && SodiumProfilesConfigEntry.hasPendingProfile()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * When the Apply button is pressed, also apply our pending profile.
     */
    @Inject(method = "applyAllOptions", at = @At("RETURN"))
    private void sodiumProfiles$applyAllOptions(CallbackInfo ci) {
        if (SodiumProfilesConfigEntry.hasPendingProfile()) {
            SodiumProfilesConfigEntry.applyPendingProfile();
        } else {
            // User manually changed settings (not through our profiles) → set to Custom
            SodiumProfilesConfigEntry.onManualSettingsApplied();
        }
    }

    /**
     * When the Undo button is pressed, clear our pending profile selection.
     */
    @Inject(method = "resetAllOptionsFromBindings", at = @At("RETURN"))
    private void sodiumProfiles$resetAllOptions(CallbackInfo ci) {
        if (SodiumProfilesConfigEntry.hasPendingProfile()) {
            SodiumProfilesConfigEntry.clearPendingProfile();
        }
    }
}
