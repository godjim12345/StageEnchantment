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
 * @author xWode
 * @version 1.0
 */
//客户端功能
@Mod.EventBusSubscriber(modid = StageEnchantment.MODID, value = Dist.CLIENT)
public class CreativeTabRefreshHandler {
    //含有附魔书的CreativeModeTab
    private static final ArrayList<CreativeModeTab> tabs = new ArrayList<>();
    public static boolean haveOpenedCreativeScreen = false;
    //BuildCreativeModeTabContentsEvent是客户端事件，每位玩家初始化GUI时候调用，每个标签页会触发，重新进入新存档也会触发
    //每次打开创造物品栏会触发两次
    //使用SubscribeEvent注解 告诉 EventBus “这是一个事件处理方法”
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
                /*for (ItemStack stack : collection) {
                    if (stack.getItem() == Items.ENCHANTED_BOOK) {
                        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
                        if (enchants.containsKey(enchantment)) {
                            // 附魔的 ResourceLocation
                            ResourceLocation enchId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                            // 1.20 可能要用: ForgeRegistries.ENCHANTMENTS.getKey(enchantment)
                            ListTag enchantList = stack.getOrCreateTag().getList("StoredEnchantments", 10); // 10 = TAG_COMPOUND
                            for (int i = 0; i < enchantList.size(); i++) {
                                CompoundTag enchTag = enchantList.getCompound(i);
                                String id = enchTag.getString("id"); // NBT 中的 id
                                if (id.equals(enchId.toString())) {
                                    enchTag.putShort("lvl", (short)newMaxLevel);
                                    // 不 break，可能有重复附魔
                                }
                            }
                            stack.getOrCreateTag().put("StoredEnchantments", enchantList);
                            break;
                        }
                    }
                }*/
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
                //todo 可能出现多线程遍历同时更改，底层可能是ObjectLinkedOpenCustomHashSet可能线程不安全
                collection.clear();
                collection.addAll(list);
            }
        }
    }
}