package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.util.math.Vec3d;

/**
 * Задача мозга Вардена, реализующая атаку звуковым ударом.
 * Наносит урон и нокбэк цели в радиусе, игнорируя блоки; урон и нокбэк снижаются сопротивлением к отбрасыванию.
 */
public class SonicBoomTask extends MultiTickTask<WardenEntity> {

	private static final int HORIZONTAL_RANGE = 15;
	private static final int VERTICAL_RANGE = 20;
	private static final double VERTICAL_KNOCKBACK_MULTIPLIER = 0.5;
	private static final double HORIZONTAL_KNOCKBACK_MULTIPLIER = 2.5;
	private static final float SONIC_BOOM_DAMAGE = 10.0F;
	private static final float SOUND_VOLUME = 3.0F;
	private static final float SOUND_PITCH = 1.0F;
	private static final byte STATUS_SONIC_BOOM = 62;

	public static final int COOLDOWN = 40;

	private static final int SOUND_DELAY = 34;
	private static final int RUN_TIME = 60;

	public SonicBoomTask() {
		super(
				ImmutableMap.of(
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.SONIC_BOOM_COOLDOWN,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.SONIC_BOOM_SOUND_DELAY,
						MemoryModuleState.REGISTERED
				),
				RUN_TIME
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, WardenEntity entity) {
		return entity.isInRange(
				entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).get(),
				HORIZONTAL_RANGE,
				VERTICAL_RANGE
		);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, WardenEntity entity, long time) {
		return true;
	}

	@Override
	protected void run(ServerWorld world, WardenEntity entity, long time) {
		entity.getBrain().remember(MemoryModuleType.ATTACK_COOLING_DOWN, true, RUN_TIME);
		entity.getBrain().remember(MemoryModuleType.SONIC_BOOM_SOUND_DELAY, Unit.INSTANCE, SOUND_DELAY);
		world.sendEntityStatus(entity, STATUS_SONIC_BOOM);
		entity.playSound(SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SOUND_VOLUME, SOUND_PITCH);
	}

	@Override
	protected void keepRunning(ServerWorld world, WardenEntity entity, long time) {
		entity.getBrain()
		      .getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET)
		      .ifPresent(target -> entity.getLookControl().lookAt(target.getEntityPos()));

		boolean soundDelayActive = entity.getBrain().hasMemoryModule(MemoryModuleType.SONIC_BOOM_SOUND_DELAY);
		boolean soundOnCooldown = entity.getBrain().hasMemoryModule(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN);

		if (soundDelayActive || soundOnCooldown) {
			return;
		}

		entity.getBrain().remember(
				MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN,
				Unit.INSTANCE,
				RUN_TIME - SOUND_DELAY
		);
		entity.getBrain()
		      .getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET)
		      .filter(entity::isValidTarget)
		      .filter(target -> entity.isInRange(target, HORIZONTAL_RANGE, VERTICAL_RANGE))
		      .ifPresent(target -> {
			      Vec3d chestPos = entity.getEntityPos().add(
					      entity.getAttachments().getPoint(EntityAttachmentType.WARDEN_CHEST, 0, entity.getYaw())
			      );
			      Vec3d toTarget = target.getEyePos().subtract(chestPos);
			      Vec3d direction = toTarget.normalize();
			      int particleCount = MathHelper.floor(toTarget.length()) + 7;

			      for (int step = 1; step < particleCount; step++) {
				      Vec3d particlePos = chestPos.add(direction.multiply(step));
				      world.spawnParticles(
						      ParticleTypes.SONIC_BOOM,
						      particlePos.x,
						      particlePos.y,
						      particlePos.z,
						      1,
						      0.0,
						      0.0,
						      0.0,
						      0.0
				      );
			      }

			      entity.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SOUND_VOLUME, SOUND_PITCH);

			      if (target.damage(world, world.getDamageSources().sonicBoom(entity), SONIC_BOOM_DAMAGE)) {
				      double knockbackResistance = target.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE);
				      double verticalKnockback = VERTICAL_KNOCKBACK_MULTIPLIER * (1.0 - knockbackResistance);
				      double horizontalKnockback = HORIZONTAL_KNOCKBACK_MULTIPLIER * (1.0 - knockbackResistance);
				      target.addVelocity(
						      direction.getX() * horizontalKnockback,
						      direction.getY() * verticalKnockback,
						      direction.getZ() * horizontalKnockback
				      );
			      }
		      });
	}

	@Override
	protected void finishRunning(ServerWorld world, WardenEntity entity, long time) {
		cooldown(entity, COOLDOWN);
	}

	public static void cooldown(LivingEntity warden, int cooldown) {
		warden.getBrain().remember(MemoryModuleType.SONIC_BOOM_COOLDOWN, Unit.INSTANCE, cooldown);
	}
}
