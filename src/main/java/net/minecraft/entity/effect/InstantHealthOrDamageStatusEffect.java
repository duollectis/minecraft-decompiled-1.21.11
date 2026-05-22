package net.minecraft.entity.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

/**
 * Мгновенный эффект лечения или урона (Instant Health / Instant Damage).
 *
 * <p>Поведение инвертируется для сущностей с {@link LivingEntity#hasInvertedHealingAndHarm()}
 * (нежить): Instant Health наносит им урон, а Instant Damage — лечит.</p>
 *
 * <p>Формулы:
 * <ul>
 *   <li>Лечение: {@code 4 << amplifier} HP (4, 8, 16, …)</li>
 *   <li>Урон: {@code 6 << amplifier} HP (6, 12, 24, …)</li>
 * </ul>
 * При применении через зелье значения масштабируются на {@code proximity} (0.0–1.0).</p>
 */
class InstantHealthOrDamageStatusEffect extends InstantStatusEffect {

	private final boolean damage;

	public InstantHealthOrDamageStatusEffect(StatusEffectCategory category, int color, boolean damage) {
		super(category, color);
		this.damage = damage;
	}

	/**
	 * Применяет полный эффект лечения или урона без учёта близости.
	 * Используется при прямом применении эффекта (не через зелье).
	 */
	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		if (damage == entity.hasInvertedHealingAndHarm()) {
			entity.heal(Math.max(4 << amplifier, 0));
		} else {
			entity.damage(world, entity.getDamageSources().magic(), 6 << amplifier);
		}

		return true;
	}

	/**
	 * Применяет эффект с учётом близости к центру взрыва зелья.
	 *
	 * @param proximity коэффициент близости [0.0, 1.0]; чем ближе к центру, тем сильнее эффект
	 */
	@Override
	public void applyInstantEffect(
			ServerWorld world,
			@Nullable Entity effectEntity,
			@Nullable Entity attacker,
			LivingEntity target,
			int amplifier,
			double proximity
	) {
		if (damage == target.hasInvertedHealingAndHarm()) {
			int healAmount = (int) (proximity * (4 << amplifier) + 0.5);
			target.heal(healAmount);
		} else {
			int damageAmount = (int) (proximity * (6 << amplifier) + 0.5);
			if (effectEntity == null) {
				target.damage(world, target.getDamageSources().magic(), damageAmount);
			} else {
				target.damage(world, target.getDamageSources().indirectMagic(effectEntity, attacker), damageAmount);
			}
		}
	}
}
