package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.CollisionEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Котёл с лавой — всегда полный, поджигает сущности при контакте и снимает заморозку.
 * Выдаёт сигнал компаратора уровня 3 (максимальный для котла).
 */
public class LavaCauldronBlock extends AbstractCauldronBlock {

	public static final MapCodec<LavaCauldronBlock> CODEC = createCodec(LavaCauldronBlock::new);
	private static final double FLUID_HEIGHT = 0.9375;
	private static final int COMPARATOR_OUTPUT = 3;
	private static final VoxelShape LAVA_SHAPE = Block.createColumnShape(12.0, 4.0, 15.0);
	private static final VoxelShape INSIDE_COLLISION_SHAPE = VoxelShapes.union(
		AbstractCauldronBlock.OUTLINE_SHAPE,
		LAVA_SHAPE
	);

	@Override
	public MapCodec<LavaCauldronBlock> getCodec() {
		return CODEC;
	}

	public LavaCauldronBlock(AbstractBlock.Settings settings) {
		super(settings, CauldronBehavior.LAVA_CAULDRON_BEHAVIOR);
	}

	@Override
	protected double getFluidHeight(BlockState state) {
		return FLUID_HEIGHT;
	}

	@Override
	public boolean isFull(BlockState state) {
		return true;
	}

	@Override
	protected VoxelShape getInsideCollisionShape(BlockState state, BlockView world, BlockPos pos, Entity entity) {
		return INSIDE_COLLISION_SHAPE;
	}

	@Override
	protected void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean bl
	) {
		handler.addEvent(CollisionEvent.CLEAR_FREEZE);
		handler.addEvent(CollisionEvent.LAVA_IGNITE);
		handler.addPostCallback(CollisionEvent.LAVA_IGNITE, Entity::setOnFireFromLava);
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return COMPARATOR_OUTPUT;
	}
}
