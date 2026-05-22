package net.minecraft.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Описывает параметры блочной частицы: вложенный эффект, масштаб и скорость.
 * Используется для настройки поведения частиц, порождаемых блоком.
 */
public record BlockParticleEffect(ParticleEffect particle, float scaling, float speed) {

	public static final MapCodec<BlockParticleEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					ParticleTypes.TYPE_CODEC.fieldOf("particle").forGetter(BlockParticleEffect::particle),
					Codec.FLOAT.optionalFieldOf("scaling", 1.0F).forGetter(BlockParticleEffect::scaling),
					Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(BlockParticleEffect::speed)
			).apply(instance, BlockParticleEffect::new)
	);

	public static final PacketCodec<RegistryByteBuf, BlockParticleEffect> PACKET_CODEC = PacketCodec.tuple(
			ParticleTypes.PACKET_CODEC, BlockParticleEffect::particle,
			PacketCodecs.FLOAT, BlockParticleEffect::scaling,
			PacketCodecs.FLOAT, BlockParticleEffect::speed,
			BlockParticleEffect::new
	);
}
