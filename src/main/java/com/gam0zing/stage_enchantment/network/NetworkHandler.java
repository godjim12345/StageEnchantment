package com.gam0zing.stage_enchantment.network;

import com.gam0zing.stage_enchantment.StageEnchantment;
import com.gam0zing.stage_enchantment.network.packe.SyncEnchantmentPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * @author xWode
 * @version 1.0
 */
// 网络注册类，用于创建 SimpleChannel 并注册 Packet
public class NetworkHandler {
    //只要客户端和服务端使用同一个值即可
    private static final String PROTOCOL_VERSION = "1";
    //Forge 会根据 channel + ID 来识别 Packet 类型
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            //mod_id，channel 名称，自定义即可，只要该 mod 内唯一即可，用这个channel 名称一一对应CHANNEL
            //Forge 用这个 ResourceLocation 来区分不同 mod 的网络通道channel
            //如果 namespace 重复了 → 注册表里后注册的会覆盖前面的（危险）Forge 注册表就是通过 ResourceLocation 来保证全局唯一的
            ResourceLocation.fromNamespaceAndPath(StageEnchantment.MODID, "enchantment_sync"),
            () -> PROTOCOL_VERSION,  // 协议版本提供器
            PROTOCOL_VERSION::equals,  // 客户端是否兼容
            PROTOCOL_VERSION::equals  // 服务端是否兼容
    );
    //Mod 初始化时调用 NetworkHandler.register()
    public static void register() {
        CHANNEL.registerMessage(
                0, //网络包 ID，用于 Forge 区分不同的消息类型 每注册一个 Packet，需要一个唯一 ID
                SyncEnchantmentPacket.class,
                SyncEnchantmentPacket::encode,
                SyncEnchantmentPacket::decode,
                SyncEnchantmentPacket::handle);
    }
}
