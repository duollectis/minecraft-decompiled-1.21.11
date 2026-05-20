package net.minecraft.network.packet.s2c.query;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.StatusPackets;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.ServerMetadata;

public record QueryResponseS2CPacket(ServerMetadata metadata) implements Packet<ClientQueryPacketListener> {
   private static final RegistryOps<JsonElement> OPS = DynamicRegistryManager.EMPTY.getOps(JsonOps.INSTANCE);
   public static final PacketCodec<ByteBuf, QueryResponseS2CPacket> CODEC = PacketCodec.tuple(
      PacketCodecs.lenientJson(32767).collect(PacketCodecs.fromCodec(OPS, ServerMetadata.CODEC)), QueryResponseS2CPacket::metadata, QueryResponseS2CPacket::new
   );

   @Override
   public PacketType<QueryResponseS2CPacket> getPacketType() {
      return StatusPackets.STATUS_RESPONSE;
   }

   public void apply(ClientQueryPacketListener clientQueryPacketListener) {
      clientQueryPacketListener.onResponse(this);
   }
}
