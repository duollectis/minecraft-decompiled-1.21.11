package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга, направляющей существо к ближайшей позиции с открытым небом.
 * Используется для существ, которым необходимо избегать закрытых пространств (например, летучие мыши).
 */
public class SeekSkyTask {

	private static final int SEARCH_ATTEMPTS = 10;
	private static final int SEARCH_HORIZONTAL_RANGE = 20;
	private static final int SEARCH_VERTICAL_RANGE = 6;

	public static SingleTickTask<LivingEntity> create(float speed) {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET)).apply(
						context, walkTarget -> (world, entity, time) -> {
							if (world.isSkyVisible(entity.getBlockPos())) {
								return false;
							}

							Optional.ofNullable(findNearbySky(world, entity))
							        .ifPresent(pos -> walkTarget.remember(new WalkTarget(pos, speed, 0)));

							return true;
						}
				)
		);
	}

	private static @Nullable Vec3d findNearbySky(ServerWorld world, LivingEntity entity) {
		Random random = entity.getRandom();
		BlockPos origin = entity.getBlockPos();

		for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
			BlockPos candidate = origin.add(
					random.nextInt(SEARCH_HORIZONTAL_RANGE) - SEARCH_HORIZONTAL_RANGE / 2,
					random.nextInt(SEARCH_VERTICAL_RANGE) - SEARCH_VERTICAL_RANGE / 2,
					random.nextInt(SEARCH_HORIZONTAL_RANGE) - SEARCH_HORIZONTAL_RANGE / 2
			);

			if (isSkyVisible(world, entity, candidate)) {
				return Vec3d.ofBottomCenter(candidate);
			}
		}

		return null;
	}

	public static boolean isSkyVisible(ServerWorld world, LivingEntity entity, BlockPos pos) {
		return world.isSkyVisible(pos)
				&& world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos).getY() <= entity.getY();
	}
}
