package com.gam0zing.stage_enchantment.events;

import com.gam0zing.stage_enchantment.StageEnchantment;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * @author 向毅灵
 * @version 1.0
 */

@Mod.EventBusSubscriber(modid = StageEnchantment.MODID, value = Dist.CLIENT)
public class CreativeTabRefreshHandler {
    //含有附魔书的CreativeModeTab
    private static final ArrayList<CreativeModeTab> tabs = new ArrayList<>();
    public static boolean haveOpenedCreativeScreen = false;
    //每次打开创造物品栏会触发两次
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init event) {
        if (event.getScreen() instanceof CreativeModeInventoryScreen) {
            if (!haveOpenedCreativeScreen) {
                init();
                haveOpenedCreativeScreen = true;
            }
        }
    }
    public static void init () {
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            // 判断是不是包含附魔书的标签
            boolean hasEnchantmentBook = tab.getDisplayItems().stream()
                    .anyMatch(stack -> stack.getItem() == Items.ENCHANTED_BOOK);
            if (hasEnchantmentBook) {
                tabs.add(tab);
            }
        }
    }
    //刷新CreativeModeTab的displayItems缓存
    public static void flush (Enchantment enchantment,int newMaxLevel) {
        for (CreativeModeTab tab : tabs) {
            //filter先筛选出含有附魔书的，再anyMatch匹配
            if (tab.getDisplayItems().stream().filter(stack -> stack.getItem() == Items.ENCHANTED_BOOK).
                    anyMatch(stack -> {
                Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
                return enchants.containsKey(enchantment);
            })) {
                Collection<ItemStack> collection = tab.getDisplayItems();
                List<ItemStack> list = new ArrayList<>(collection);
                for (ListIterator<ItemStack> it = list.listIterator(); it.hasNext();) {
                    ItemStack stack = it.next();
                    if (stack.getItem() == Items.ENCHANTED_BOOK) {
                        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
                        if (enchants.containsKey(enchantment)) {
                            ItemStack newBook = EnchantedBookItem.createForEnchantment
                                    (new EnchantmentInstance(enchantment, newMaxLevel));
                            it.set(newBook);
                            break;
                        }
                    }
                }
                // 清空原 collection，再把 list 添加回去
                collection.clear();
                collection.addAll(list);
            }
        }
    }
}