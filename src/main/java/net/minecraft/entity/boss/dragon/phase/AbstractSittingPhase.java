package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.WindChargeEntity;

/**
 * Базовая фаза для всех сидячих состояний дракона. Отражает стрелы и заряды ветра,
 * поджигая их вместо нанесения урона дракону.
 */
public abstract class AbstractSittingPhase extends AbstractPhase {

	public AbstractSittingPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public boolean isSittingOrHovering() {
		return true;
	}

	@Override
	public float modifyDamageTaken(DamageSource damageSource, float damage) {
		if (damageSource.getSource() instanceof PersistentProjectileEntity
				|| damageSource.getSource() instanceof WindChargeEntity) {
			damageSource.getSource().setOnFireFor(1.0F);
			return 0.0F;
		}

		return super.modifyDamageTaken(damageSource, damage);
	}
}
