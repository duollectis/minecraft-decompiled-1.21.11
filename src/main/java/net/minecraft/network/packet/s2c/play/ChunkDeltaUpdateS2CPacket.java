package net.minecraft.network.packet.s2c.play;

import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;

import java.util.function.BiConsumer;

/**
 * Класс chunk delta update s2 c packet.
 */
public class ChunkDeltaUpdateS2CPacket implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<PacketByteBuf, ChunkDeltaUpdateS2CPacket> CODEC = Packet.createCodec(
			ChunkDeltaUpdateS2CPacket::write, ChunkDeltaUpdateS2CPacket::new
	);
	private static final int SECTION_COORD_BITS = 12;
	private final ChunkSectionPos sectionPos;
	private final short[] positions;
	private final BlockState[] blockStates;

	public ChunkDeltaUpdateS2CPacket(ChunkSectionPos sectionPos, ShortSet positions, ChunkSection section) {
		this.sectionPos = sectionPos;
		int i = positions.size();
		this.positions = new short[i];
		this.blockStates = new BlockState[i];
		int j = 0;

		for (ShortIterator var6 = positions.iterator(); var6.hasNext(); j++) {
			short s = (Short) var6.next();
			this.positions[j] = s;
			this.blockStates[j] =
					section.getBlockState(
							ChunkSectionPos.unpackLocalX(s),
							ChunkSectionPos.unpackLocalY(s),
							ChunkSectionPos.unpackLocalZ(s)
					);
		}
	}

	private ChunkDeltaUpdateS2CPacket(PacketByteBuf buf) {
		this.sectionPos = ChunkSectionPos.PACKET_CODEC.decode(buf);
		int i = buf.readVarInt();
		this.positions = new short[i];
		this.blockStates = new BlockState[i];

		for (int j = 0; j < i; j++) {
			long l = buf.readVarLong();
			this.positions[j] = (short) (l & 4095L);
			this.blockStates[j] = Block.STATE_IDS.get((int) (l >>> SECTION_COORD_BITS));
		}
	}

	private void write(PacketByteBuf buf) {
		ChunkSectionPos.PACKET_CODEC.encode(buf, this.sectionPos);
		buf.writeVarInt(this.positions.length);

		for (int i = 0; i < this.positions.length; i++) {
			buf.writeVarLong((long) Block.getRawIdFromState(this.blockStates[i]) << SECTION_COORD_BITS | this.positions[i]);
		}
	}

	@Override
	public PacketType<ChunkDeltaUpdateS2CPacket> getPacketType() {
		return PlayPackets.SECTION_BLOCKS_UPDATE;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onChunkDeltaUpdate(this);
	}

	/**
	 * Visit updates.
	 *
	 * @param visitor visitor
	 */
	public void visitUpdates(BiConsumer<BlockPos, BlockState> visitor) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int i = 0; i < this.positions.length; i++) {
			short s = this.positions[i];
			mutable.set(
					this.sectionPos.unpackBlockX(s),
					this.sectionPos.unpackBlockY(s),
					this.sectionPos.unpackBlockZ(s)
			);
			visitor.accept(mutable, this.blockStates[i]);
		}
	}
}
