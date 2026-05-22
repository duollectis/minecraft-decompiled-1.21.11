package net.minecraft.item;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Предмет «Вагонетка». При использовании на рельсах размещает соответствующую
 * сущность вагонетки. Если включены улучшения вагонеток, запрещает размещение
 * поверх уже существующей вагонетки.
 */
public class MinecartItem extends Item {

	private static final double RAIL_VERTICAL_OFFSET = 0.0625;
	private static final double ASCENDING_RAIL_OFFSET = 0.5;
	private static final double RAIL_CENTER_OFFSET = 0.5;

	private final EntityType<? extends AbstractMinecartEntity> type;

	public MinecartItem(EntityType<? extends AbstractMinecartEntity> type, Item.Settings settings) {
		super(settings);
		this.type = type;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState blockState = world.getBlockState(pos);

		if (!blockState.isIn(BlockTags.RAILS)) {
			return ActionResult.FAIL;
		}

		ItemStack stack = context.getStack();
		RailShape railShape = blockState.getBlock() instanceof AbstractRailBlock railBlock
			? blockState.get(railBlock.getShapeProperty())
			: RailShape.NORTH_SOUTH;

		double verticalOffset = railShape.isAscending() ? ASCENDING_RAIL_OFFSET : 0.0;
		Vec3d spawnPos = new Vec3d(
			pos.getX() + RAIL_CENTER_OFFSET,
			pos.getY() + RAIL_VERTICAL_OFFSET + verticalOffset,
			pos.getZ() + RAIL_CENTER_OFFSET
		);

		AbstractMinecartEntity minecart = AbstractMinecartEntity.create(
			world, spawnPos.x, spawnPos.y, spawnPos.z, type, SpawnReason.DISPENSER, stack, context.getPlayer()
		);

		if (minecart == null) {
			return ActionResult.FAIL;
		}

		if (AbstractMinecartEntity.areMinecartImprovementsEnabled(world)) {
			for (Entity entity : world.getOtherEntities(null, minecart.getBoundingBox())) {
				if (entity instanceof AbstractMinecartEntity) {
					return ActionResult.FAIL;
				}
			}
		}

		if (world instanceof ServerWorld serverWorld) {
			serverWorld.spawnEntity(minecart);
			serverWorld.emitGameEvent(
				GameEvent.ENTITY_PLACE,
				pos,
				GameEvent.Emitter.of(context.getPlayer(), serverWorld.getBlockState(pos.down()))
			);
		}

		stack.decrement(1);

		return ActionResult.SUCCESS;
	}
}
