package com.gam0zing.stage_enchantment.command_pattern;


import com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;

/// 使用这个Command的要求：
/// 需要把生成的Command对象放在一个Map里作为Value
/// Map的Key可以是任何东西，数字、字符串、UUID，只要方便使用指令的时候输入查找就可以
/// 和最大等级那张Map一样，也是一个存档对应一个，所以需要生成Json文件在SeverConfig里，名字就叫stage_enchantment-commands.json
/// json的格式不重要，完全存储需要的值，便于读写即可
public class EnchCommand implements ICommand {

    private final Map<Enchantment, Integer> effects;
    private boolean executed;

    public EnchCommand(Map<Enchantment, Integer> effects) {
        this.effects = effects;
        this.executed = false;
    }

    //执行方法
    //加在指令里
    @Override
    public boolean execute() {

        if (this.executed) return false;

        effects.forEach(this::doEffect);

        executed = true;

        return true;
    }

    //撤销方法
    //加在指令里
    @Override
    public boolean unexecute() {

        if (!this.executed) return false;

        effects.forEach(this::undoEffect);

        executed = false;

        return true;
    }

    //添加或修改对应附魔的效果并重新应用
    //加在指令里
    public void addEffect(Enchantment ench, Integer value) {

        boolean flag = this.executed;

        if (flag) unexecute();

        effects.put(ench, value);

        if (flag) execute();
    }

    //删除对应附魔的效果并重新应用
    //加在指令里
    public void removeEffect(Enchantment ench) {

        boolean flag = this.executed;

        if (flag) unexecute();

        effects.remove(ench);

        if (flag) execute();
    }

    private void doEffect(Enchantment ench, Integer value) {
        DynamicEnchantmentManager.addMaxLevel(ench, value);
    }

    private void undoEffect(Enchantment ench, Integer value) {
        DynamicEnchantmentManager.addMaxLevel(ench, -value);
    }
}
