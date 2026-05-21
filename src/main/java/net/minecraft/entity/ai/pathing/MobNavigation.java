package net.minecraft.entity.ai.pathing;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * {@code MobNavigation}.
 */
public class MobNavigation extends EntityNavigation {

	private boolean avoidSunlight;
	private boolean skipRetarget;

	public MobNavigation(MobEntity mobEntity, World world) {
		super(mobEntity, world);
	}

	@Override
	protected PathNodeNavigator createPathNodeNavigator(int range) {
		this.nodeMaker = new LandPathNodeMaker();
		return new PathNodeNavigator(this.nodeMaker, range);
	}

	@Override
	protected boolean isAtValidPosition() {
		return this.entity.isOnGround() || this.entity.isInFluid() || this.entity.hasVehicle();
	}

	@Override
	protected Vec3d getPos() {
		return new Vec3d(this.entity.getX(), this.getPathfindingY(), this.entity.getZ());
	}

	@Override
	public Path findPathTo(BlockPos target, int distance) {
		WorldChunk worldChunk = this.world
				.getChunkManager()
				.getWorldChunk(
						ChunkSectionPos.getSectionCoord(target.getX()),
						ChunkSectionPos.getSectionCoord(target.getZ())
				);
		if (worldChunk == null) {
			return null;
		}
		else {
			if (!this.skipRetarget) {
				target = this.retargetToSolidBlock(worldChunk, target, distance);
			}

			return super.findPathTo(target, distance);
		}
	}

	final BlockPos retargetToSolidBlock(WorldChunk chunk, BlockPos pos, int distance) {
		if (chunk.getBlockState(pos).isAir()) {
			BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.DOWN);

			while (mutable.getY() >= this.world.getBottomY() && chunk.getBlockState(mutable).isAir()) {
				mutable.move(Direction.DOWN);
			}

			if (mutable.getY() >= this.world.getBottomY()) {
				return mutable.up();
			}

			mutable.setY(pos.getY() + 1);

			while (mutable.getY() <= this.world.getTopYInclusive() && chunk.getBlockState(mutable).isAir()) {
				mutable.move(Direction.UP);
			}

			pos = mutable;
		}

		if (!chunk.getBlockState(pos).isSolid()) {
			return pos;
		}
		else {
			BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.UP);

			while (mutable.getY() <= this.world.getTopYInclusive() && chunk.getBlockState(mutable).isSolid()) {
				mutable.move(Direction.UP);
			}

			return mutable.toImmutable();
		}
	}

	@Override
	public Path findPathTo(Entity entity, int distance) {
		return this.findPathTo(entity.getBlockPos(), distance);
	}

	private int getPathfindingY() {
		if (this.entity.isTouchingWater() && this.canSwim()) {
			int i = this.entity.getBlockY();
			BlockState
					blockState =
					this.world.getBlockState(BlockPos.ofFloored(this.entity.getX(), i, this.entity.getZ()));
			int j = 0;

			while (blockState.isOf(Blocks.WATER)) {
				blockState = this.world.getBlockState(BlockPos.ofFloored(this.entity.getX(), ++i, this.entity.getZ()));
				if (++j > 16) {
					return this.entity.getBlockY();
				}
			}

			return i;
		}
		else {
			return MathHelper.floor(this.entity.getY() + 0.5);
		}
	}

	@Override
	protected void adjustPath() {
		super.adjustPath();
		if (this.avoidSunlight) {
			if (this.world.isSkyVisible(BlockPos.ofFloored(
					this.entity.getX(),
					this.entity.getY() + 0.5,
					this.entity.getZ()
			))) {
				return;
			}

			for (int i = 0; i < this.currentPath.getLength(); i++) {
				PathNode pathNode = this.currentPath.getNode(i);
				if (this.world.isSkyVisible(new BlockPos(pathNode.x, pathNode.y, pathNode.z))) {
					this.currentPath.setLength(i);
					return;
				}
			}
		}
	}

	@Override
	public boolean canControlOpeningDoors() {
		return true;
	}

	protected boolean canWalkOnPath(PathNodeType pathType) {
		if (pathType == PathNodeType.WATER) {
			return false;
		}
		else {
			return pathType == PathNodeType.LAVA ? false : pathType != PathNodeType.OPEN;
		}
	}

	public void setAvoidSunlight(boolean avoidSunlight) {
		this.avoidSunlight = avoidSunlight;
	}

	public void setCanWalkOverFences(boolean canWalkOverFences) {
		this.nodeMaker.setCanWalkOverFences(canWalkOverFences);
	}

	public void setSkipRetarget(boolean skipRetarget) {
		this.skipRetarget = skipRetarget;
	}
}
