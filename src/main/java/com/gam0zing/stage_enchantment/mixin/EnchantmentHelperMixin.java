package com.gam0zing.stage_enchantment.mixin;

import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author 向毅灵
 * @version 1.0
 */

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
    @Inject(method = "getDepthStrider",at = @At("RETURN"),cancellable = true)
    private static void getEnchantmentLevelT(CallbackInfoReturnable<Integer> cir) {
    }
}
