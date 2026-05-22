package net.minecraft.entity.ai.brain;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Реализация {@link LookTarget}, привязанная к живой сущности.
 * Позиция цели обновляется динамически вместе с движением сущности.
 * Видимость проверяется через кэш {@link LivingTargetCache} в памяти наблюдателя.
 */
public class EntityLookTarget implements LookTarget {

	private final Entity entity;
	private final boolean useEyeHeight;
	private final boolean blockPosAtEye;

	public EntityLookTarget(Entity entity, boolean useEyeHeight) {
		this(entity, useEyeHeight, false);
	}

	public EntityLookTarget(Entity entity, boolean useEyeHeight, boolean blockPosAtEye) {
		this.entity = entity;
		this.useEyeHeight = useEyeHeight;
		this.blockPosAtEye = blockPosAtEye;
	}

	@Override
	public Vec3d getPos() {
		return useEyeHeight
				? entity.getEntityPos().add(0.0, entity.getStandingEyeHeight(), 0.0)
				: entity.getEntityPos();
	}

	@Override
	public BlockPos getBlockPos() {
		return blockPosAtEye ? BlockPos.ofFloored(entity.getEyePos()) : entity.getBlockPos();
	}

	/**
	 * Проверяет видимость цели через кэш {@link LivingTargetCache}.
	 * Если цель — не живая сущность, считается всегда видимой.
	 * Мёртвые сущности никогда не считаются видимыми.
	 */
	@Override
	public boolean isSeenBy(LivingEntity observer) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return true;
		}

		if (!livingEntity.isAlive()) {
			return false;
		}

		Optional<LivingTargetCache> visibleMobs = observer.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS);

		return visibleMobs.isPresent() && visibleMobs.get().contains(livingEntity);
	}

	public Entity getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "EntityTracker for " + entity;
	}
}
