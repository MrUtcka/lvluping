package org.mrutcka.lvluping.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;
import org.mrutcka.lvluping.LvlupingMod;

public class ModNetworking {
    public static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions((status, version) -> version == PROTOCOL_VERSION)
            .serverAcceptedVersions((status, version) -> version == PROTOCOL_VERSION)
            .simpleChannel();

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(C2SPurchaseTalent.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SPurchaseTalent::encode)
                .decoder(C2SPurchaseTalent::new)
                .consumerMainThread(C2SPurchaseTalent::handle)
                .add();

        CHANNEL.messageBuilder(S2CSyncTalents.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSyncTalents::encode)
                .decoder(S2CSyncTalents::new)
                .consumerMainThread(S2CSyncTalents::handle)
                .add();
    }
}