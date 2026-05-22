package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;

/**
 * Задача мозга, заставляющая моба смотреть в случайном направлении с заданным диапазоном угла.
 * Устанавливает кулдаун взгляда через память {@code GAZE_COOLDOWN_TICKS}.
 */
public class LookAroundTask extends MultiTickTask<MobEntity> {

	private final IntProvider cooldown;
	private final float maxYaw;
	private final float minPitch;
	private final float pitchRange;

	public LookAroundTask(IntProvider cooldown, float maxYaw, float minPitch, float maxPitch) {
		super(ImmutableMap.of(
				MemoryModuleType.LOOK_TARGET,
				MemoryModuleState.VALUE_ABSENT,
				MemoryModuleType.GAZE_COOLDOWN_TICKS,
				MemoryModuleState.VALUE_ABSENT
		));
		if (minPitch > maxPitch) {
			throw new IllegalArgumentException("Minimum pitch is larger than maximum pitch! " + minPitch + " > " + maxPitch);
		}

		this.cooldown = cooldown;
		this.maxYaw = maxYaw;
		this.minPitch = minPitch;
		this.pitchRange = maxPitch - minPitch;
	}

	@Override
	protected void run(ServerWorld world, MobEntity entity, long time) {
		Random random = entity.getRandom();
		float pitch = MathHelper.clamp(random.nextFloat() * pitchRange + minPitch, -90.0F, 90.0F);
		float yaw = MathHelper.wrapDegrees(entity.getYaw() + 2.0F * random.nextFloat() * maxYaw - maxYaw);
		Vec3d lookDir = Vec3d.fromPolar(pitch, yaw);
		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new BlockPosLookTarget(entity.getEyePos().add(lookDir)));
		entity.getBrain().remember(MemoryModuleType.GAZE_COOLDOWN_TICKS, cooldown.get(random));
	}
}
