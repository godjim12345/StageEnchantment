package com.gam0zing.stage_enchantment.network.packe;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager.setMaxLevel;

/**
 * @author xWode
 * @version 1.0
 */
// 这是网络包类，用于同步服务端附魔等级到客户端
public class SyncEnchantmentPacket {
    //Forge 网络包设计上的标准做法：发送对象的 ID(ResourceLocation)，而不是对象本身
    //只能传递可序列化的数据类型(FriendlyByteBuf能write的)，例如 String 或 ResourceLocation（表示附魔 ID）
    private final Map<ResourceLocation, Integer> overrides;

    public SyncEnchantmentPacket(Map<ResourceLocation, Integer> overrides) {
        this.overrides = overrides;
    }
    // 序列化方法：把 Map 写入网络缓冲区
    public static void encode(SyncEnchantmentPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.overrides.size());
        for (Map.Entry<ResourceLocation, Integer> entry : msg.overrides.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }
    // 反序列化方法：从网络缓冲区读取 Map 数据
    public static SyncEnchantmentPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int value = buf.readVarInt();
            map.put(id, value);
        }
        return new SyncEnchantmentPacket(map);
    }
    // 客户端处理收到的数据
    public static void handle(SyncEnchantmentPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            //ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT 客户端收到
            //LogicalSide.SERVER服务端收到
            // 客户端接收数据 → 更新本地缓存
            msg.overrides.forEach((ResourceLocation id, Integer maxLevel) -> setMaxLevel
                    (ForgeRegistries.ENCHANTMENTS.getValue(id),maxLevel));
        });
        ctx.get().setPacketHandled(true); // 标记包已处理
    }
}
