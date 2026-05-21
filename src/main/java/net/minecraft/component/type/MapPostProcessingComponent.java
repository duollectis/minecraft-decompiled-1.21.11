package net.minecraft.component.type;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * {@code MapPostProcessingComponent}.
 */
public enum MapPostProcessingComponent {
	LOCK(0),
	SCALE(1);

	public static final IntFunction<MapPostProcessingComponent> ID_TO_VALUE = ValueLists.createIndexToValueFunction(
			MapPostProcessingComponent::getId, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final PacketCodec<ByteBuf, MapPostProcessingComponent>
			PACKET_CODEC =
			PacketCodecs.indexed(ID_TO_VALUE, MapPostProcessingComponent::getId);
	private final int id;

	private MapPostProcessingComponent(final int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}
}
