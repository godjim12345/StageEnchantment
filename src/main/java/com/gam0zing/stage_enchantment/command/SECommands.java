package com.gam0zing.stage_enchantment.command;

import com.gam0zing.stage_enchantment.StageEnchantment;
import com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
                Commands.literal(StageEnchantment.MODID)
                        // fastChange 分支（与 command 并列）
                        .then(Commands.literal("fastChange")
                                .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggestResource(
                                                        ForgeRegistries.ENCHANTMENTS.getKeys(),
                                                        builder
                                                )
                                        )
                                        // add 分支
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 255))
                                                        .executes(context -> {
                                                            ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
                                                            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                                                            int value = IntegerArgumentType.getInteger(context, "value");
                                                            if (enchantment == null) {
                                                                context.getSource().sendFailure(Component.translatable("command.maxLevel.invalid"));
                                                                return 0;
                                                            }
                                                            DynamicEnchantmentManager.addMaxLevel(enchantment, value);
                                                            context.getSource().sendSuccess(() ->
                                                                            Component.translatable(
                                                                                    "command.maxLevel.add",
                                                                                    Component.translatable(enchantment.getDescriptionId()).getString(),
                                                                                    value,
                                                                                    DynamicEnchantmentManager.getDynamicMax(enchantment, DynamicEnchantmentManager.getMaxLevel(enchantment))
                                                                            ),
                                                                    false
                                                            );
                                                            return 1;
                                                        })
                                                )
                                        )
                                        // set 分支
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 255))
                                                        .executes(context -> {
                                                            ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
                                                            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                                                            int value = IntegerArgumentType.getInteger(context, "value");
                                                            if (enchantment == null) {
                                                                context.getSource().sendFailure(Component.translatable("command.maxLevel.invalid"));
                                                                return 0;
                                                            }
                                                            DynamicEnchantmentManager.setMaxLevel(enchantment, value);
                                                            context.getSource().sendSuccess(() ->
                                                                            Component.translatable(
                                                                                    "command.maxLevel.set",
                                                                                    Component.translatable(enchantment.getDescriptionId()).getString(),
                                                                                    value
                                                                            ),
                                                                    false
                                                            );
                                                            return 1;
                                                        })
                                                )
                                        )
                                        // get 分支
                                        .then(Commands.literal("get")
                                                .executes(context -> {
                                                    ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
                                                    Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                                                    if (enchantment == null) {
                                                        context.getSource().sendFailure(Component.translatable("command.maxLevel.invalid"));
                                                        return 0;
                                                    }
                                                    context.getSource().sendSuccess(() ->
                                                                    Component.translatable(
                                                                            "command.maxLevel.get",
                                                                            Component.translatable(enchantment.getDescriptionId()).getString(),
                                                                            DynamicEnchantmentManager.getDynamicMax(enchantment, DynamicEnchantmentManager.getMaxLevel(enchantment))
                                                                    ),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        // command 分支（与 fastChange 并列）
                        .then(Commands.literal("command")
                                .then(Commands.literal("create")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .executes(context -> {
                                                    String id = StringArgumentType.getString(context, "id");
                                                    var ret = DynamicEnchantmentManager.createCommand(id);
                                                    if (!ret) {
                                                        context.getSource().sendFailure(Component.translatable("command.command.create.failure", id));
                                                        return 0;
                                                    } else {
                                                        context.getSource().sendSuccess(() -> Component.translatable("command.command.create.success", id), false);
                                                        return 1;
                                                    }
                                                })
                                        )
                                )
                                .then(Commands.literal("destroy")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .executes(context -> {
                                                    String id = StringArgumentType.getString(context, "id");
                                                    var ret = DynamicEnchantmentManager.destroyCommand(id);
                                                    if (!ret) {
                                                        context.getSource().sendFailure(Component.translatable("command.command.destroy.failure"));
                                                        return 0;
                                                    } else {
                                                        context.getSource().sendSuccess(() -> Component.translatable("command.command.destroy.success", id), false);
                                                        return 1;
                                                    }
                                                })
                                        )
                                )
                                .then(Commands.literal("query")
                                        .executes(context -> {
                                            int count = DynamicEnchantmentManager.COMMANDS.size();
                                            if (count > 0) {
                                                String head = String.format("%-12s %-10s", "ID", "Executed");
                                                context.getSource().sendSystemMessage(Component.literal(head));
                                                DynamicEnchantmentManager.COMMANDS.forEach((key, value) -> {
                                                    String line = String.format("%-12s %-10s", key, value.getCurrent());
                                                    context.getSource().sendSystemMessage(Component.literal(line));
                                                });
                                            }
                                            context.getSource().sendSuccess(() -> Component.translatable("command.command.query.success", count), false);
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("addEffect")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                                        .suggests((context, builder) ->
                                                                SharedSuggestionProvider.suggestResource(
                                                                        ForgeRegistries.ENCHANTMENTS.getKeys(),
                                                                        builder
                                                                )
                                                        )
                                                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 255))
                                                                .executes(context -> {
                                                                    String id = StringArgumentType.getString(context, "id");
                                                                    ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
                                                                    Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                                                                    int value = IntegerArgumentType.getInteger(context, "value");
                                                                    if (enchantment == null) {
                                                                        context.getSource().sendFailure(Component.translatable("command.command.setEffect.invalidEnchantment"));
                                                                        return 0;
                                                                    }
                                                                    var ret = DynamicEnchantmentManager.setEffect(id, enchantment, value);
                                                                    if (ret == -1) {
                                                                        context.getSource().sendFailure(Component.translatable("command.command.setEffect.failure"));
                                                                        return 0;
                                                                    } else if (ret == 0) {
                                                                        String showValue = (value > 0 ? "+" : "") + value;
                                                                        context.getSource().sendSuccess(() -> Component.translatable(
                                                                                "command.command.setEffect.replace",
                                                                                id,
                                                                                Component.translatable(enchantment.getDescriptionId()).getString(),
                                                                                showValue), false);
                                                                        return 1;
                                                                    } else {
                                                                        String showValue = (value > 0 ? "+" : "") + value;
                                                                        context.getSource().sendSuccess(() -> Component.translatable(
                                                                                "command.command.setEffect.new",
                                                                                id,
                                                                                Component.translatable(enchantment.getDescriptionId()).getString(),
                                                                                showValue), false);
                                                                        return 1;
                                                                    }
                                                                })
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("removeEffect")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                                        .suggests((context, builder) ->
                                                                SharedSuggestionProvider.suggestResource(
                                                                        ForgeRegistries.ENCHANTMENTS.getKeys(),
                                                                        builder
                                                                )
                                                        )
                                                        .executes(context -> {
                                                            String id = StringArgumentType.getString(context, "id");
                                                            ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
                                                            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                                                            if (enchantment == null) {
                                                                context.getSource().sendFailure(Component.translatable("command.command.removeEffect.invalidEnchantment"));
                                                                return 0;
                                                            }
                                                            var ret = DynamicEnchantmentManager.removeEffect(id, enchantment);
                                                            if (!ret) {
                                                                context.getSource().sendFailure(Component.translatable("command.command.removeEffect.failure"));
                                                                return 0;
                                                            } else {
                                                                context.getSource().sendSuccess(() -> Component.translatable("command.command.removeEffect.success", id, Component.translatable(enchantment.getDescriptionId()).getString()), false);
                                                                return 1;
                                                            }
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("execute")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .executes(context -> {
                                                    String id = StringArgumentType.getString(context, "id");
                                                    var ret = DynamicEnchantmentManager.execute(id);
                                                    if (ret == -1) {
                                                        context.getSource().sendFailure(Component.translatable("command.command.execute.invalid"));
                                                        return 0;
                                                    } else if (ret == 0) {
                                                        context.getSource().sendFailure(Component.translatable("command.command.execute.failure"));
                                                        return 0;
                                                    } else {
                                                        DynamicEnchantmentManager.COMMANDS.get(id).getEffects().forEach((key, value) -> {
                                                            String showValue = (value > 0 ? "+" : "") + value;
                                                            String line = "[" + Component.translatable(key.getDescriptionId()).getString() + "]" + " " + Component.translatable("command.command.execute.maxLevel").getString() + " " + showValue;
                                                            context.getSource().sendSystemMessage(Component.literal(line));
                                                        });
                                                        context.getSource().sendSuccess(() -> Component.translatable("command.command.execute.success", id), false);
                                                        return 1;
                                                    }
                                                })
                                        )
                                )
                                .then(Commands.literal("unexecute")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .executes(context -> {
                                                    String id = StringArgumentType.getString(context, "id");
                                                    var ret = DynamicEnchantmentManager.unexecute(id);
                                                    if (ret == -1) {
                                                        context.getSource().sendFailure(Component.translatable("command.command.unexecute.invalid"));
                                                        return 0;
                                                    } else if (ret == 0) {
                                                        context.getSource().sendFailure(Component.translatable("command.command.unexecute.failure"));
                                                        return 0;
                                                    } else {
                                                        DynamicEnchantmentManager.COMMANDS.get(id).getEffects().forEach((key, value) -> {
                                                            String showValue = (-value > 0 ? "+" : "") + -value;
                                                            String line = "[" + Component.translatable(key.getDescriptionId()).getString() + "]" + " " + Component.translatable("command.command.unexecute.maxLevel").getString() + " " + showValue;
                                                            context.getSource().sendSystemMessage(Component.literal(line));
                                                        });
                                                        context.getSource().sendSuccess(() -> Component.translatable("command.command.unexecute.success", id), false);
                                                        return 1;
                                                    }
                                                })
                                        )
                                )
                        )
        );
    }
}