package net.minecraft.network.packet.c2s.play;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.entity.TestInstanceBlockEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.registry.RegistryKey;
import net.minecraft.test.TestInstance;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;
import java.util.function.IntFunction;

public record TestInstanceBlockActionC2SPacket(
		BlockPos pos,
		TestInstanceBlockActionC2SPacket.Action action,
		TestInstanceBlockEntity.Data data
)
		implements Packet<ServerPlayPacketListener> {

	public static final PacketCodec<RegistryByteBuf, TestInstanceBlockActionC2SPacket> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC,
			TestInstanceBlockActionC2SPacket::pos,
			TestInstanceBlockActionC2SPacket.Action.CODEC,
			TestInstanceBlockActionC2SPacket::action,
			TestInstanceBlockEntity.Data.PACKET_CODEC,
			TestInstanceBlockActionC2SPacket::data,
			TestInstanceBlockActionC2SPacket::new
	);

	public TestInstanceBlockActionC2SPacket(
			BlockPos pos,
			TestInstanceBlockActionC2SPacket.Action actin,
			Optional<RegistryKey<TestInstance>> optional,
			Vec3i vec3i,
			BlockRotation blockRotation,
			boolean bl
	) {
		this(
				pos,
				actin,
				new TestInstanceBlockEntity.Data(
						optional,
						vec3i,
						blockRotation,
						bl,
						TestInstanceBlockEntity.Status.CLEARED,
						Optional.empty()
				)
		);
	}

	@Override
	public PacketType<TestInstanceBlockActionC2SPacket> getPacketType() {
		return PlayPackets.TEST_INSTANCE_BLOCK_ACTION;
	}

	public void apply(ServerPlayPacketListener serverPlayPacketListener) {
		serverPlayPacketListener.onTestInstanceBlockAction(this);
	}

	public static enum Action {
		INIT(0),
		QUERY(1),
		SET(2),
		RESET(3),
		SAVE(4),
		EXPORT(5),
		RUN(6);

		private static final IntFunction<TestInstanceBlockActionC2SPacket.Action>
				INDEX_MAPPER =
				ValueLists.createIndexToValueFunction(
						(TestInstanceBlockActionC2SPacket.Action action) -> action.index,
						values(),
						ValueLists.OutOfBoundsHandling.ZERO
				);
		public static final PacketCodec<ByteBuf, TestInstanceBlockActionC2SPacket.Action>
				CODEC =
				PacketCodecs.indexed(INDEX_MAPPER, action -> action.index);
		private final int index;

		private Action(final int index) {
			this.index = index;
		}
	}
}
