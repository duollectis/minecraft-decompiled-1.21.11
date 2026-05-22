package net.minecraft.entity.effect;

import net.minecraft.util.Formatting;

/**
 * Категория статусного эффекта, определяющая его визуальное оформление в инвентаре.
 *
 * <p>{@link #BENEFICIAL} и {@link #NEUTRAL} отображаются синим цветом,
 * {@link #HARMFUL} — красным.</p>
 */
public enum StatusEffectCategory {
	BENEFICIAL(Formatting.BLUE),
	HARMFUL(Formatting.RED),
	NEUTRAL(Formatting.BLUE);

	private final Formatting formatting;

	StatusEffectCategory(Formatting formatting) {
		this.formatting = formatting;
	}

	public Formatting getFormatting() {
		return formatting;
	}
}
