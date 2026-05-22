package net.minecraft.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.world.World;

/**
 * Базовый класс всех иллагеров. Определяет общую логику:
 * иллагеры не атакуют детей-торговцев, считаются союзниками
 * всех существ из тега {@code illager_friends} при отсутствии команды.
 */
public abstract class IllagerEntity extends RaiderEntity {

	protected IllagerEntity(EntityType<? extends IllagerEntity> entityType, World world) {
		super(entityType, world);
	}

	public IllagerEntity.State getState() {
		return IllagerEntity.State.CROSSED;
	}

	@Override
	public boolean canTarget(LivingEntity target) {
		if (target instanceof MerchantEntity && target.isBaby()) {
			return false;
		}

		return super.canTarget(target);
	}

	@Override
	public boolean isInSameTeam(Entity other) {
		if (super.isInSameTeam(other)) {
			return true;
		}

		if (!other.getType().isIn(EntityTypeTags.ILLAGER_FRIENDS)) {
			return false;
		}

		return getScoreboardTeam() == null && other.getScoreboardTeam() == null;
	}

	protected class LongDoorInteractGoal extends net.minecraft.entity.ai.goal.LongDoorInteractGoal {

		public LongDoorInteractGoal(final RaiderEntity raider) {
			super(raider, false);
		}

		@Override
		public boolean canStart() {
			return super.canStart() && IllagerEntity.this.hasActiveRaid();
		}
	}

	public enum State {
		CROSSED,
		ATTACKING,
		SPELLCASTING,
		BOW_AND_ARROW,
		CROSSBOW_HOLD,
		CROSSBOW_CHARGE,
		CELEBRATING,
		NEUTRAL;
	}
}
