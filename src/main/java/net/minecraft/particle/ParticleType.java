package net.minecraft.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * Абстрактный тип частицы, определяющий кодеки для сериализации эффекта.
 *
 * @param <T> конкретный тип эффекта частицы
 */
public abstract class ParticleType<T extends ParticleEffect> {

	private final boolean alwaysShow;

	protected ParticleType(boolean alwaysShow) {
		this.alwaysShow = alwaysShow;
	}

	/**
	 * @return {@code true}, если частица должна отображаться вне зависимости от настроек графики
	 */
	public boolean shouldAlwaysSpawn() {
		return alwaysShow;
	}

	public abstract MapCodec<T> getCodec();

	public abstract PacketCodec<? super RegistryByteBuf, T> getPacketCodec();
}
