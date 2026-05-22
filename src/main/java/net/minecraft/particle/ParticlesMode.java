package net.minecraft.particle;

import com.mojang.serialization.Codec;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Режим отображения частиц, выбираемый игроком в настройках.
 * Определяет количество отображаемых частиц: все, уменьшенное или минимальное.
 */
public enum ParticlesMode {
	ALL(0, "options.particles.all"),
	DECREASED(1, "options.particles.decreased"),
	MINIMAL(2, "options.particles.minimal");

	private static final IntFunction<ParticlesMode> BY_ID = ValueLists.createIndexToValueFunction(
			(ParticlesMode mode) -> mode.id, values(), ValueLists.OutOfBoundsHandling.WRAP
	);
	public static final Codec<ParticlesMode> CODEC = Codec.INT.xmap(BY_ID::apply, mode -> mode.id);

	private final int id;
	private final Text text;

	ParticlesMode(int id, String translationKey) {
		this.id = id;
		this.text = Text.translatable(translationKey);
	}

	public Text getText() {
		return text;
	}
}
