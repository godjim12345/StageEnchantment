package com.gam0zing.stage_enchantment.command;

import com.gam0zing.stage_enchantment.StageEnchantment;
import com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "stage_enchantment", bus = Mod.EventBusSubscriber.Bus.FORGE)
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
                                .then(Commands.argument("mode", StringArgumentType.string())
                                        .suggests((context, builder) ->  // mode补全（add/set/get）
                                                SharedSuggestionProvider.suggest(new String[]{"add", "set", "get"}, builder)
                                        )
                                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 255))
                                                .executes(context -> {
                                                    // 参数解析逻辑
                                                    ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
                                                    Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                                                    String mode = StringArgumentType.getString(context, "mode");
                                                    int value = IntegerArgumentType.getInteger(context, "value");

                                                    // 错误处理
                                                    if (enchantment == null) {
                                                        context.getSource().sendFailure(Component.literal("无效的附魔ID！"));
                                                        return 0;
                                                    }

                                                    // 执行逻辑
                                                    switch (mode) {
                                                        case "get":
                                                            context.getSource().sendSuccess(() ->
                                                                            Component.literal("附魔 [" + Component.translatable(enchantment.getDescriptionId()).getString() + "] 的等级上限为 " +
                                                                                    DynamicEnchantmentManager.getDynamicMax(enchantment, enchantment.getMaxLevel())),
                                                                    false
                                                            );
                                                            break;
                                                        case "add":
                                                            DynamicEnchantmentManager.addMaxLevel(enchantment, value);
                                                            context.getSource().sendSuccess(() ->
                                                                            Component.literal("已增加附魔 [" + Component.translatable(enchantment.getDescriptionId()).getString() + "] 的等级上限至 " +
                                                                                    DynamicEnchantmentManager.getDynamicMax(enchantment, enchantment.getMaxLevel())),
                                                                    false
                                                            );
                                                            break;
                                                        case "set":
                                                            DynamicEnchantmentManager.setMaxLevel(enchantment, value);
                                                            context.getSource().sendSuccess(() ->
                                                                            Component.literal("已设置附魔 [" + Component.translatable(enchantment.getDescriptionId()).getString() + "] 的等级上限为 " + value),
                                                                    false
                                                            );
                                                            break;
                                                        default:
                                                            context.getSource().sendFailure(Component.literal("无效的操作模式！"));
                                                            return 0;
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }
}