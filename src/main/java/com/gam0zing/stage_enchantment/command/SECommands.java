package com.gam0zing.stage_enchantment.command;

import com.gam0zing.stage_enchantment.StageEnchantment;
import com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = StageEnchantment.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SECommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("enchantmentMaxLevel")
                        .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                .suggests((context, builder) ->  // 附魔ID补全
                                        SharedSuggestionProvider.suggestResource(
                                                ForgeRegistries.ENCHANTMENTS.getKeys(),
                                                builder
                                        )
                                )
                                // add/set 分支
                                .then(Commands.literal("add")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 255))
                                                .executes(context -> {
                                                    // 参数解析逻辑
                                                    ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
                                                    Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                                                    int value = IntegerArgumentType.getInteger(context, "value");
                                                    // 错误处理
                                                    if (enchantment == null) {
                                                        context.getSource().sendFailure(Component.literal("无效的附魔ID！"));
                                                        return 0;
                                                    }
                                                    // 执行逻辑
                                                    DynamicEnchantmentManager.addMaxLevel(enchantment, value);
                                                    context.getSource().sendSuccess(() ->
                                                                    Component.literal("已增加附魔 [" + Component.translatable(enchantment.getDescriptionId()).getString() + "] 的等级上限至 " +
                                                                            DynamicEnchantmentManager.getDynamicMax(enchantment, DynamicEnchantmentManager.getMaxLevel(enchantment))),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 255))
                                                .executes(context -> {
                                                    ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
                                                    Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                                                    int value = IntegerArgumentType.getInteger(context, "value");
                                                    if (enchantment == null) {
                                                        context.getSource().sendFailure(Component.literal("无效的附魔ID！"));
                                                        return 0;
                                                    }
                                                    DynamicEnchantmentManager.setMaxLevel(enchantment, value);
                                                    context.getSource().sendSuccess(() ->
                                                                    Component.literal("已设置附魔 [" + Component.translatable(enchantment.getDescriptionId()).getString() + "] 的等级上限为 " + value),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                                // get 分支（没有 value 参数）
                                .then(Commands.literal("get")
                                        .executes(context -> {
                                            ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
                                            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                                            if (enchantment == null) {
                                                context.getSource().sendFailure(Component.literal("无效的附魔ID！"));
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(() ->
                                                            Component.literal("附魔 [" + Component.translatable(enchantment.getDescriptionId()).getString() + "] 的等级上限为 " +
                                                                    DynamicEnchantmentManager.getDynamicMax(enchantment, DynamicEnchantmentManager.getMaxLevel(enchantment))),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
        );
    }
}