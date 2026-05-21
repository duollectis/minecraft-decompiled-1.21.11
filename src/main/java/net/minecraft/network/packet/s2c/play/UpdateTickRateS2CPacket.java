package net.minecraft.network.packet.s2c.play;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.world.tick.TickManager;

/**
 * Запись update tick rate s2 c packet.
 */
public record UpdateTickRateS2CPacket(float tickRate, boolean isFrozen) implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<PacketByteBuf, UpdateTickRateS2CPacket> CODEC = Packet.createCodec(
			UpdateTickRateS2CPacket::write, UpdateTickRateS2CPacket::new
	);

	private UpdateTickRateS2CPacket(PacketByteBuf buf) {
		this(buf.readFloat(), buf.readBoolean());
	}

	/**
	 * Create.
	 *
	 * @param tickManager tick manager
	 *
	 * @return UpdateTickRateS2CPacket — результат операции
	 */
	public static UpdateTickRateS2CPacket create(TickManager tickManager) {
		return new UpdateTickRateS2CPacket(tickManager.getTickRate(), tickManager.isFrozen());
	}

	private void write(PacketByteBuf buf) {
		buf.writeFloat(this.tickRate);
		buf.writeBoolean(this.isFrozen);
	}

	@Override
	public PacketType<UpdateTickRateS2CPacket> getPacketType() {
		return PlayPackets.TICKING_STATE;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onUpdateTickRate(this);
	}
}
