package net.minecraft.network.packet.s2c.play;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Класс select advancement tab s2 c packet.
 */
public class SelectAdvancementTabS2CPacket implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<PacketByteBuf, SelectAdvancementTabS2CPacket> CODEC = Packet.createCodec(
			SelectAdvancementTabS2CPacket::write, SelectAdvancementTabS2CPacket::new
	);
	private final @Nullable Identifier tabId;

	public SelectAdvancementTabS2CPacket(@Nullable Identifier tabId) {
		this.tabId = tabId;
	}

	private SelectAdvancementTabS2CPacket(PacketByteBuf buf) {
		this.tabId = buf.readNullable(PacketByteBuf::readIdentifier);
	}

	private void write(PacketByteBuf buf) {
		buf.writeNullable(this.tabId, PacketByteBuf::writeIdentifier);
	}

	@Override
	public PacketType<SelectAdvancementTabS2CPacket> getPacketType() {
		return PlayPackets.SELECT_ADVANCEMENTS_TAB;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onSelectAdvancementTab(this);
	}

	public @Nullable Identifier getTabId() {
		return this.tabId;
	}
}
