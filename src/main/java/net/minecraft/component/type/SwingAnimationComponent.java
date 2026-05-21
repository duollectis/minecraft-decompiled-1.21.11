package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.SwingAnimationType;
import net.minecraft.util.dynamic.Codecs;

/**
 * {@code SwingAnimationComponent}.
 */
public record SwingAnimationComponent(SwingAnimationType type, int duration) {

	public static final SwingAnimationComponent DEFAULT = new SwingAnimationComponent(SwingAnimationType.WHACK, 6);
	public static final Codec<SwingAnimationComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    SwingAnimationType.CODEC
							                    .optionalFieldOf("type", DEFAULT.type)
							                    .forGetter(SwingAnimationComponent::type),
					                    Codecs.POSITIVE_INT
							                    .optionalFieldOf("duration", DEFAULT.duration)
							                    .forGetter(SwingAnimationComponent::duration)
			                    )
			                    .apply(instance, SwingAnimationComponent::new)
	);
	public static final PacketCodec<ByteBuf, SwingAnimationComponent> PACKET_CODEC = PacketCodec.tuple(
			SwingAnimationType.PACKET_CODEC,
			SwingAnimationComponent::type,
			PacketCodecs.VAR_INT,
			SwingAnimationComponent::duration,
			SwingAnimationComponent::new
	);
}
