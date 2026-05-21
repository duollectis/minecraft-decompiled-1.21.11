package net.minecraft.network.packet.s2c.play;

import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.BundlePacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;

/**
 * Класс bundle s2 c packet.
 */
public class BundleS2CPacket extends BundlePacket<ClientPlayPacketListener> {

	public BundleS2CPacket(Iterable<Packet<? super ClientPlayPacketListener>> iterable) {
		super(iterable);
	}

	@Override
	public PacketType<BundleS2CPacket> getPacketType() {
		return PlayPackets.BUNDLE;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onBundle(this);
	}
}
