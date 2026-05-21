package net.minecraft.util.math;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Util;

import java.util.List;

/**
 * {@code EulerAngle}.
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
	public static final PacketCodec<ByteBuf, EulerAngle> PACKET_CODEC = new PacketCodec<ByteBuf, EulerAngle>() {
		/**
		 * Decode.
		 *
		 * @param byteBuf byte buf
		 *
		 * @return EulerAngle — результат операции
		 */
		public EulerAngle decode(ByteBuf byteBuf) {
			return new EulerAngle(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
		}

		/**
		 * Encode.
		 *
		 * @param byteBuf byte buf
		 * @param eulerAngle euler angle
		 */
		public void encode(ByteBuf byteBuf, EulerAngle eulerAngle) {
			byteBuf.writeFloat(eulerAngle.pitch);
			byteBuf.writeFloat(eulerAngle.yaw);
			byteBuf.writeFloat(eulerAngle.roll);
		}
	};

	public EulerAngle(float pitch, float yaw, float roll) {
		pitch = !Float.isInfinite(pitch) && !Float.isNaN(pitch) ? pitch % 360.0F : 0.0F;
		yaw = !Float.isInfinite(yaw) && !Float.isNaN(yaw) ? yaw % 360.0F : 0.0F;
		roll = !Float.isInfinite(roll) && !Float.isNaN(roll) ? roll % 360.0F : 0.0F;
		this.pitch = pitch;
		this.yaw = yaw;
		this.roll = roll;
	}
}
