package net.minecraft.entity.ai.pathing;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Навигация наземного существа: поддерживает ходьбу, плавание и обход препятствий.
 * Дополнительно умеет избегать солнечного света и перенацеливаться на твёрдые блоки.
 */
public class MobNavigation extends EntityNavigation {

	private boolean avoidSunlight;
	private boolean skipRetarget;

	public MobNavigation(MobEntity mobEntity, World world) {
		super(mobEntity, world);
	}

	@Override
	protected PathNodeNavigator createPathNodeNavigator(int range) {
		nodeMaker = new LandPathNodeMaker();
		return new PathNodeNavigator(nodeMaker, range);
	}

	@Override
	protected boolean isAtValidPosition() {
		return entity.isOnGround() || entity.isInFluid() || entity.hasVehicle();
	}

	@Override
	protected Vec3d getPos() {
		return new Vec3d(entity.getX(), getPathfindingY(), entity.getZ());
	}

	/**
	 * Перед поиском пути перенацеливает позицию на ближайший твёрдый блок,
	 * чтобы существо не пыталось идти в воздух или сквозь землю.
	 */
	@Override
	public Path findPathTo(BlockPos target, int distance) {
		WorldChunk chunk = world.getChunkManager().getWorldChunk(
				ChunkSectionPos.getSectionCoord(target.getX()),
				ChunkSectionPos.getSectionCoord(target.getZ())
		);

		if (chunk == null) {
			return null;
		}

		if (!skipRetarget) {
			target = retargetToSolidBlock(chunk, target, distance);
		}

		return super.findPathTo(target, distance);
	}

	/**
	 * Корректирует целевую позицию: если она в воздухе — опускается до твёрдого блока,
	 * если внутри твёрдого — поднимается до первого свободного.
	 */
	final BlockPos retargetToSolidBlock(WorldChunk chunk, BlockPos pos, int distance) {
		if (chunk.getBlockState(pos).isAir()) {
			BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.DOWN);

			while (mutable.getY() >= world.getBottomY() && chunk.getBlockState(mutable).isAir()) {
				mutable.move(Direction.DOWN);
			}

			if (mutable.getY() >= world.getBottomY()) {
				return mutable.up();
			}

			mutable.setY(pos.getY() + 1);

			while (mutable.getY() <= world.getTopYInclusive() && chunk.getBlockState(mutable).isAir()) {
				mutable.move(Direction.UP);
			}

			pos = mutable;
		}

		if (!chunk.getBlockState(pos).isSolid()) {
			return pos;
		}

		BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.UP);

		while (mutable.getY() <= world.getTopYInclusive() && chunk.getBlockState(mutable).isSolid()) {
			mutable.move(Direction.UP);
		}

		return mutable.toImmutable();
	}

	@Override
	public Path findPathTo(Entity entity, int distance) {
		return findPathTo(entity.getBlockPos(), distance);
	}

	/**
	 * Возвращает Y-координату для поиска пути: при плавании поднимается до поверхности воды.
	 */
	private int getPathfindingY() {
		if (!entity.isTouchingWater() || !canSwim()) {
			return MathHelper.floor(entity.getY() + 0.5);
		}

		int waterY = entity.getBlockY();
		BlockState blockState = world.getBlockState(BlockPos.ofFloored(entity.getX(), waterY, entity.getZ()));
		int depth = 0;

		while (blockState.isOf(Blocks.WATER)) {
			blockState = world.getBlockState(BlockPos.ofFloored(entity.getX(), ++waterY, entity.getZ()));

			if (++depth > 16) {
				return entity.getBlockY();
			}
		}

		return waterY;
	}

	/**
	 * Дополнительно обрезает путь при первом узле под открытым небом, если включено избегание солнца.
	 */
	@Override
	protected void adjustPath() {
		super.adjustPath();

		if (!avoidSunlight) {
			return;
		}

		if (world.isSkyVisible(BlockPos.ofFloored(entity.getX(), entity.getY() + 0.5, entity.getZ()))) {
			return;
		}

		for (int i = 0; i < currentPath.getLength(); i++) {
			PathNode node = currentPath.getNode(i);

			if (world.isSkyVisible(new BlockPos(node.x, node.y, node.z))) {
				currentPath.setLength(i);
				return;
			}
		}
	}

	@Override
	public boolean canControlOpeningDoors() {
		return true;
	}

	protected boolean canWalkOnPath(PathNodeType pathType) {
		return pathType != PathNodeType.WATER
				&& pathType != PathNodeType.LAVA
				&& pathType != PathNodeType.OPEN;
	}

	public void setAvoidSunlight(boolean avoidSunlight) {
		this.avoidSunlight = avoidSunlight;
	}

	public void setCanWalkOverFences(boolean canWalkOverFences) {
		nodeMaker.setCanWalkOverFences(canWalkOverFences);
	}

	public void setSkipRetarget(boolean skipRetarget) {
		this.skipRetarget = skipRetarget;
	}
}
