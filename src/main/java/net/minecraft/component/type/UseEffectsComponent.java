package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * {@code UseEffectsComponent}.
 */
public record UseEffectsComponent(boolean canSprint, boolean interactVibrations, float speedMultiplier) {

	public static final UseEffectsComponent DEFAULT = new UseEffectsComponent(false, true, 0.2F);
	public static final Codec<UseEffectsComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codec.BOOL
							                    .optionalFieldOf("can_sprint", DEFAULT.canSprint)
							                    .forGetter(UseEffectsComponent::canSprint),
					                    Codec.BOOL
							                    .optionalFieldOf("interact_vibrations", DEFAULT.interactVibrations)
							                    .forGetter(UseEffectsComponent::interactVibrations),
					                    Codec
							                    .floatRange(0.0F, 1.0F)
							                    .optionalFieldOf("speed_multiplier", DEFAULT.speedMultiplier)
							                    .forGetter(UseEffectsComponent::speedMultiplier)
			                    )
			                    .apply(instance, UseEffectsComponent::new)
	);
	public static final PacketCodec<ByteBuf, UseEffectsComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.BOOLEAN,
			UseEffectsComponent::canSprint,
			PacketCodecs.BOOLEAN,
			UseEffectsComponent::interactVibrations,
			PacketCodecs.FLOAT,
			UseEffectsComponent::speedMultiplier,
			UseEffectsComponent::new
	);
}
