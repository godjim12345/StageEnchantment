package com.gam0zing.stage_enchantment.command_pattern;

import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;

public interface ICommand {
    boolean execute();
    boolean unexecute();

    boolean setEffect(Enchantment ench, Integer value);
    boolean removeEffect(Enchantment ench);
    Map<Enchantment, Integer> getEffects();

    boolean getCurrent();
}
