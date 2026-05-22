package net.minecraft.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Эффект частицы заряда скалка.
 * Поле {@code roll} задаёт угол поворота текстуры частицы в радианах.
 */
public record SculkChargeParticleEffect(float roll) implements ParticleEffect {

	public static final MapCodec<SculkChargeParticleEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codec.FLOAT.fieldOf("roll").forGetter(SculkChargeParticleEffect::roll))
					.apply(instance, SculkChargeParticleEffect::new)
	);
	public static final PacketCodec<RegistryByteBuf, SculkChargeParticleEffect> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT, SculkChargeParticleEffect::roll, SculkChargeParticleEffect::new
	);

	@Override
	public ParticleType<SculkChargeParticleEffect> getType() {
		return ParticleTypes.SCULK_CHARGE;
	}
}
