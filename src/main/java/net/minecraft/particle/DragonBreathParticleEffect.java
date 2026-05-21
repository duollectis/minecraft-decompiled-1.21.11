package net.minecraft.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * {@code DragonBreathParticleEffect}.
 */
public class DragonBreathParticleEffect implements ParticleEffect {

	private final ParticleType<DragonBreathParticleEffect> type;
	private final float power;

	public static MapCodec<DragonBreathParticleEffect> createCodec(ParticleType<DragonBreathParticleEffect> type) {
		return Codec.FLOAT
				.xmap(power -> new DragonBreathParticleEffect(type, power), effect -> effect.power)
				.optionalFieldOf("power", of(type, 1.0F));
	}

	public static PacketCodec<? super ByteBuf, DragonBreathParticleEffect> createPacketCodec(ParticleType<DragonBreathParticleEffect> type) {
		return PacketCodecs.FLOAT.xmap(power -> new DragonBreathParticleEffect(type, power), effect -> effect.power);
	}

	private DragonBreathParticleEffect(ParticleType<DragonBreathParticleEffect> type, float power) {
		this.type = type;
		this.power = power;
	}

	@Override
	public ParticleType<DragonBreathParticleEffect> getType() {
		return this.type;
	}

	public float getPower() {
		return this.power;
	}

	public static DragonBreathParticleEffect of(ParticleType<DragonBreathParticleEffect> type, float power) {
		return new DragonBreathParticleEffect(type, power);
	}
}
