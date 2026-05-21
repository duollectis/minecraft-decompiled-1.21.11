package net.minecraft.entity.ai.pathing;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkCache;

/**
 * {@code PathNodeMaker}.
 */
public abstract class PathNodeMaker {

	protected PathContext context;
	protected MobEntity entity;
	protected final Int2ObjectMap<PathNode> pathNodeCache = new Int2ObjectOpenHashMap();
	protected int entityBlockXSize;
	protected int entityBlockYSize;
	protected int entityBlockZSize;
	protected boolean canEnterOpenDoors = true;
	protected boolean canOpenDoors;
	protected boolean canSwim;
	protected boolean canWalkOverFences;

	/**
	 * Init.
	 *
	 * @param cachedWorld cached world
	 * @param entity entity
	 */
	public void init(ChunkCache cachedWorld, MobEntity entity) {
		this.context = new PathContext(cachedWorld, entity);
		this.entity = entity;
		this.pathNodeCache.clear();
		this.entityBlockXSize = MathHelper.floor(entity.getWidth() + 1.0F);
		this.entityBlockYSize = MathHelper.floor(entity.getHeight() + 1.0F);
		this.entityBlockZSize = MathHelper.floor(entity.getWidth() + 1.0F);
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.context = null;
		this.entity = null;
	}

	protected PathNode getNode(BlockPos pos) {
		return this.getNode(pos.getX(), pos.getY(), pos.getZ());
	}

	protected PathNode getNode(int x, int y, int z) {
		return (PathNode) this.pathNodeCache.computeIfAbsent(PathNode.hash(x, y, z), l -> new PathNode(x, y, z));
	}

	public abstract PathNode getStart();

	public abstract TargetPathNode getNode(double x, double y, double z);

	/**
	 * Создаёт node.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return TargetPathNode — результат операции
	 */
	protected TargetPathNode createNode(double x, double y, double z) {
		return new TargetPathNode(this.getNode(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)));
	}

	public abstract int getSuccessors(PathNode[] successors, PathNode node);

	public abstract PathNodeType getNodeType(PathContext context, int x, int y, int z, MobEntity mob);

	public abstract PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z);

	public PathNodeType getDefaultNodeType(MobEntity entity, BlockPos pos) {
		return this.getDefaultNodeType(
				new PathContext(entity.getEntityWorld(), entity),
				pos.getX(),
				pos.getY(),
				pos.getZ()
		);
	}

	public void setCanEnterOpenDoors(boolean canEnterOpenDoors) {
		this.canEnterOpenDoors = canEnterOpenDoors;
	}

	public void setCanOpenDoors(boolean canOpenDoors) {
		this.canOpenDoors = canOpenDoors;
	}

	public void setCanSwim(boolean canSwim) {
		this.canSwim = canSwim;
	}

	public void setCanWalkOverFences(boolean canWalkOverFences) {
		this.canWalkOverFences = canWalkOverFences;
	}

	/**
	 * Проверяет возможность enter open doors.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canEnterOpenDoors() {
		return this.canEnterOpenDoors;
	}

	/**
	 * Проверяет возможность open doors.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canOpenDoors() {
		return this.canOpenDoors;
	}

	/**
	 * Проверяет возможность swim.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canSwim() {
		return this.canSwim;
	}

	/**
	 * Проверяет возможность walk over fences.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canWalkOverFences() {
		return this.canWalkOverFences;
	}

	public static boolean isFireDamaging(BlockState state) {
		return state.isIn(BlockTags.FIRE)
				|| state.isOf(Blocks.LAVA)
				|| state.isOf(Blocks.MAGMA_BLOCK)
				|| CampfireBlock.isLitCampfire(state)
				|| state.isOf(Blocks.LAVA_CAULDRON);
	}
}
