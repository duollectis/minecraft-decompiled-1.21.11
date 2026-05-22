package net.minecraft.block;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import org.jspecify.annotations.Nullable;

/**
 * Контракт для блоков-порталов, способных телепортировать сущности.
 * Реализуется блоками, которые создают цель телепортации и могут применять
 * визуальные эффекты (например, дезориентацию при прохождении через Нижний мир).
 */
public interface Portal {

	default int getPortalDelay(ServerWorld world, Entity entity) {
		return 0;
	}

	@Nullable TeleportTarget createTeleportTarget(ServerWorld world, Entity entity, BlockPos pos);

	default Portal.Effect getPortalEffect() {
		return Portal.Effect.NONE;
	}

	enum Effect {
		CONFUSION,
		NONE;
	}
}
