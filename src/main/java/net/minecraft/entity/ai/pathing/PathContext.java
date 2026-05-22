package net.minecraft.entity.ai.pathing;

import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.CollisionView;
import org.jspecify.annotations.Nullable;

/**
 * Контекст поиска пути: предоставляет доступ к миру и кэшу типов узлов.
 * На сервере использует {@link PathNodeTypeCache} для ускорения повторных запросов.
 */
public class PathContext {

	private final CollisionView world;
	private final @Nullable PathNodeTypeCache nodeTypeCache;
	private final BlockPos entityPos;
	private final BlockPos.Mutable lastNodePos = new BlockPos.Mutable();

	public PathContext(CollisionView world, MobEntity entity) {
		this.world = world;
		nodeTypeCache = entity.getEntityWorld() instanceof ServerWorld serverWorld
				? serverWorld.getPathNodeTypeCache()
				: null;
		entityPos = entity.getBlockPos();
	}

	public PathNodeType getNodeType(int x, int y, int z) {
		BlockPos pos = lastNodePos.set(x, y, z);
		return nodeTypeCache == null
				? LandPathNodeMaker.getCommonNodeType(world, pos)
				: nodeTypeCache.add(world, pos);
	}

	public BlockState getBlockState(BlockPos pos) {
		return world.getBlockState(pos);
	}

	public CollisionView getWorld() {
		return world;
	}

	public BlockPos getEntityPos() {
		return entityPos;
	}
}
