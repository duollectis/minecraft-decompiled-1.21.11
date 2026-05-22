package net.minecraft.entity.effect;

/**
 * Базовый класс для мгновенных статусных эффектов.
 *
 * <p>Мгновенные эффекты применяются однократно при броске зелья или стрелы.
 * {@link #canApplyUpdateEffect} разрешает применение только при {@code duration >= 1},
 * что гарантирует ровно один вызов {@link #applyUpdateEffect}.</p>
 */
public class InstantStatusEffect extends StatusEffect {

	public InstantStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean isInstant() {
		return true;
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return duration >= 1;
	}
}
