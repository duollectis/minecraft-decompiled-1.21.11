package net.minecraft.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * Простой тип частицы без дополнительных параметров.
 * Одновременно является и типом, и эффектом — сам себя описывает.
 */
public class SimpleParticleType extends ParticleType<SimpleParticleType> implements ParticleEffect {

	private final MapCodec<SimpleParticleType> codec = MapCodec.unit(this::getType);
	private final PacketCodec<RegistryByteBuf, SimpleParticleType> packetCodec = PacketCodec.unit(this);

	protected SimpleParticleType(boolean alwaysShow) {
		super(alwaysShow);
	}

	public SimpleParticleType getType() {
		return this;
	}

	@Override
	public MapCodec<SimpleParticleType> getCodec() {
		return codec;
	}

	@Override
	public PacketCodec<RegistryByteBuf, SimpleParticleType> getPacketCodec() {
		return packetCodec;
	}
}
