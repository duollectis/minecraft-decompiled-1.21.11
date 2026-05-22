package net.minecraft.util.math;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Util;

import java.util.List;

/**
 * Углы Эйлера (pitch, yaw, roll) в градусах.
 * Все значения нормализуются в диапазон (-360, 360) при создании.
 * Используется для хранения ориентации броней-стоек и других сущностей.
 */
public record EulerAngle(float pitch, float yaw, float roll) {

	public static final Codec<EulerAngle> CODEC = Codec.FLOAT
			.listOf()
			.comapFlatMap(
					list -> Util
							.decodeFixedLengthList(list, 3)
							.map(angles -> new EulerAngle(
									(Float) angles.get(0),
									(Float) angles.get(1),
									(Float) angles.get(2)
							)),
					angle -> List.of(angle.pitch(), angle.yaw(), angle.roll())
			);

	public static final PacketCodec<ByteBuf, EulerAngle> PACKET_CODEC = new PacketCodec<>() {
		@Override
		public EulerAngle decode(ByteBuf byteBuf) {
			return new EulerAngle(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
		}

		@Override
		public void encode(ByteBuf byteBuf, EulerAngle eulerAngle) {
			byteBuf.writeFloat(eulerAngle.pitch);
			byteBuf.writeFloat(eulerAngle.yaw);
			byteBuf.writeFloat(eulerAngle.roll);
		}
	};

	public EulerAngle(float pitch, float yaw, float roll) {
		this.pitch = isFinite(pitch) ? pitch % 360.0F : 0.0F;
		this.yaw = isFinite(yaw) ? yaw % 360.0F : 0.0F;
		this.roll = isFinite(roll) ? roll % 360.0F : 0.0F;
	}

	private static boolean isFinite(float value) {
		return !Float.isInfinite(value) && !Float.isNaN(value);
	}
}
