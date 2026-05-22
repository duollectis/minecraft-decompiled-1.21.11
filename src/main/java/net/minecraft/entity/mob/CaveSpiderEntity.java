package net.minecraft.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Пещерный паук — ядовитый вариант обычного паука.
 */
public class CaveSpiderEntity extends SpiderEntity {

	public CaveSpiderEntity(EntityType<? extends CaveSpiderEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createCaveSpiderAttributes() {
		return SpiderEntity.createSpiderAttributes().add(EntityAttributes.MAX_HEALTH, 12.0);
	}

	private static final int POISON_DURATION_NORMAL = 7;
	private static final int POISON_DURATION_HARD = 15;
	private static final int POISON_AMPLIFIER = 0;
	private static final int TICKS_PER_SECOND = 20;

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		if (!super.tryAttack(world, target)) {
			return false;
		}

		if (target instanceof LivingEntity livingTarget) {
			int poisonDuration = switch (getEntityWorld().getDifficulty()) {
				case NORMAL -> POISON_DURATION_NORMAL;
				case HARD -> POISON_DURATION_HARD;
				default -> 0;
			};

			if (poisonDuration > 0) {
				livingTarget.addStatusEffect(
						new StatusEffectInstance(StatusEffects.POISON, poisonDuration * TICKS_PER_SECOND, POISON_AMPLIFIER),
						this
				);
			}
		}

		return true;
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		return entityData;
	}

	@Override
	public Vec3d getVehicleAttachmentPos(Entity vehicle) {
		return vehicle.getWidth() <= getWidth()
				? new Vec3d(0.0, 0.21875 * getScale(), 0.0)
				: super.getVehicleAttachmentPos(vehicle);
	}
}
