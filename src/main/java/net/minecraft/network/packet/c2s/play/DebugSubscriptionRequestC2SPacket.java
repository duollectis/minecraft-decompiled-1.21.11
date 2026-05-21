package net.minecraft.network.packet.c2s.play;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.debug.DebugSubscriptionType;

import java.util.Set;

/**
 * Запись debug subscription request c2 s packet.
 */
public record DebugSubscriptionRequestC2SPacket(Set<DebugSubscriptionType<?>> subscriptions) implements Packet<ServerPlayPacketListener> {

	private static final PacketCodec<RegistryByteBuf, Set<DebugSubscriptionType<?>>>
			TYPE_SET_CODEC =
			PacketCodecs.registryValue(RegistryKeys.DEBUG_SUBSCRIPTION)
			            .collect(PacketCodecs.toCollection(ReferenceOpenHashSet::new));
	public static final PacketCodec<RegistryByteBuf, DebugSubscriptionRequestC2SPacket> CODEC = TYPE_SET_CODEC.xmap(
			DebugSubscriptionRequestC2SPacket::new, DebugSubscriptionRequestC2SPacket::subscriptions
	);

	@Override
	public PacketType<DebugSubscriptionRequestC2SPacket> getPacketType() {
		return PlayPackets.DEBUG_SUBSCRIPTION_REQUEST;
	}

	/**
	 * Apply.
	 *
	 * @param serverPlayPacketListener server play packet listener
	 */
	public void apply(ServerPlayPacketListener serverPlayPacketListener) {
		serverPlayPacketListener.onDebugSubscriptionRequest(this);
	}
}
