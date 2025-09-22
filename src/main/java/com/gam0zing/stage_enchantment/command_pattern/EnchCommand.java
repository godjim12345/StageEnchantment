package com.gam0zing.stage_enchantment.command_pattern;


import com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;

public class EnchCommand implements ICommand {

    private final Map<Enchantment, Integer> effects;
    private boolean executed;

    public EnchCommand(Map<Enchantment, Integer> effects) {
        this.effects = effects;
        this.executed = false;
    }

    @Override
    public boolean execute() {

        if (this.executed) return false;

        effects.forEach(this::doEffect);

        executed = true;

        return true;
    }

    @Override
    public boolean unexecute() {

        if (!this.executed) return false;

        effects.forEach(this::undoEffect);

        executed = false;

        return true;
    }

    private void doEffect(Enchantment ench, Integer value) {
        DynamicEnchantmentManager.addMaxLevel(ench, value);
    }

    private void undoEffect(Enchantment ench, Integer value) {
        DynamicEnchantmentManager.addMaxLevel(ench, -value);
    }
}
