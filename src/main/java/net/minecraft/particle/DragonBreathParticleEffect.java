package net.minecraft.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Эффект частицы дыхания дракона с настраиваемой мощностью.
 * Создаётся только через фабричные методы {@link #of}.
 */
public class DragonBreathParticleEffect implements ParticleEffect {

	private final ParticleType<DragonBreathParticleEffect> type;
	private final float power;

	private DragonBreathParticleEffect(ParticleType<DragonBreathParticleEffect> type, float power) {
		this.type = type;
		this.power = power;
	}

	public static MapCodec<DragonBreathParticleEffect> createCodec(ParticleType<DragonBreathParticleEffect> type) {
		return Codec.FLOAT
				.xmap(power -> new DragonBreathParticleEffect(type, power), effect -> effect.power)
				.optionalFieldOf("power", of(type, 1.0F));
	}

	public static PacketCodec<? super ByteBuf, DragonBreathParticleEffect> createPacketCodec(
			ParticleType<DragonBreathParticleEffect> type
	) {
		return PacketCodecs.FLOAT.xmap(power -> new DragonBreathParticleEffect(type, power), effect -> effect.power);
	}

	public static DragonBreathParticleEffect of(ParticleType<DragonBreathParticleEffect> type, float power) {
		return new DragonBreathParticleEffect(type, power);
	}

	@Override
	public ParticleType<DragonBreathParticleEffect> getType() {
		return type;
	}

	public float getPower() {
		return power;
	}
}
