package dev.arbe.sodiumprofiles.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;

@Mixin(value = ConfigManager.class, remap = false)
public class ConfigManagerMixin {

    @ModifyVariable(method = "registerConfigs", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/config/structure/Config;<init>(Ljava/util/List;)V"), ordinal = 0)
    private static ObjectArrayList<ModOptions> reorderModOptions(ObjectArrayList<ModOptions> modConfigs) {
        // Move sodium-profiles to index 0 (first in sidebar)
        int ourIndex = -1;
        for (int i = 0; i < modConfigs.size(); i++) {
            if ("sodium-profiles".equals(modConfigs.get(i).configId())) {
                ourIndex = i;
                break;
            }
        }

        if (ourIndex > 0) {
            ModOptions ours = modConfigs.remove(ourIndex);
            modConfigs.add(0, ours);
        }

        return modConfigs;
    }
}
