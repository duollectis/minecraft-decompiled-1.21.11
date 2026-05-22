package net.minecraft.item;

import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Map;

/**
 * Предмет «Лопата». Позволяет превращать блоки земли в тропинку, а также
 * тушить костры. Таблица преобразований задана в {@link #PATH_STATES}.
 */
public class ShovelItem extends Item {

	private static final int PATH_UPDATE_FLAGS = 11;
	private static final int CAMPFIRE_EXTINGUISH_EVENT = 1009;

	protected static final Map<Block, BlockState> PATH_STATES = Maps.newHashMap(
		new Builder<Block, BlockState>()
			.put(Blocks.GRASS_BLOCK, Blocks.DIRT_PATH.getDefaultState())
			.put(Blocks.DIRT, Blocks.DIRT_PATH.getDefaultState())
			.put(Blocks.PODZOL, Blocks.DIRT_PATH.getDefaultState())
			.put(Blocks.COARSE_DIRT, Blocks.DIRT_PATH.getDefaultState())
			.put(Blocks.MYCELIUM, Blocks.DIRT_PATH.getDefaultState())
			.put(Blocks.ROOTED_DIRT, Blocks.DIRT_PATH.getDefaultState())
			.build()
	);

	public ShovelItem(ToolMaterial material, float attackDamage, float attackSpeed, Item.Settings settings) {
		super(settings.shovel(material, attackDamage, attackSpeed));
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState blockState = world.getBlockState(pos);

		if (context.getSide() == Direction.DOWN) {
			return ActionResult.PASS;
		}

		PlayerEntity player = context.getPlayer();
		BlockState resultState = resolveResultState(world, pos, blockState, player);

		if (resultState == null) {
			return ActionResult.PASS;
		}

		if (!world.isClient()) {
			world.setBlockState(pos, resultState, PATH_UPDATE_FLAGS);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, resultState));

			if (player != null) {
				context.getStack().damage(1, player, context.getHand().getEquipmentSlot());
			}
		}

		return ActionResult.SUCCESS;
	}

	private BlockState resolveResultState(World world, BlockPos pos, BlockState blockState, PlayerEntity player) {
		BlockState pathState = PATH_STATES.get(blockState.getBlock());

		if (pathState != null && world.getBlockState(pos.up()).isAir()) {
			world.playSound(player, pos, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
			return pathState;
		}

		if (blockState.getBlock() instanceof CampfireBlock && blockState.get(CampfireBlock.LIT)) {
			if (!world.isClient()) {
				world.syncWorldEvent(null, CAMPFIRE_EXTINGUISH_EVENT, pos, 0);
			}

			CampfireBlock.extinguish(player, world, pos, blockState);
			return blockState.with(CampfireBlock.LIT, false);
		}

		return null;
	}
}
