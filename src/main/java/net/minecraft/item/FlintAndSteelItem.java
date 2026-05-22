package net.minecraft.item;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Предмет «Огниво». Поджигает костры, свечи и размещает огонь на соседних блоках.
 * При каждом использовании теряет 1 единицу прочности.
 */
public class FlintAndSteelItem extends Item {

	/** Флаги обновления блока при размещении огня. */
	private static final int FIRE_PLACE_FLAGS = 11;

	public FlintAndSteelItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState blockState = world.getBlockState(pos);

		if (CampfireBlock.canBeLit(blockState)
				|| CandleBlock.canBeLit(blockState)
				|| CandleCakeBlock.canBeLit(blockState)
		) {
			world.playSound(
					player,
					pos,
					SoundEvents.ITEM_FLINTANDSTEEL_USE,
					SoundCategory.BLOCKS,
					1.0F,
					world.getRandom().nextFloat() * 0.4F + 0.8F
			);
			world.setBlockState(pos, blockState.with(Properties.LIT, true), FIRE_PLACE_FLAGS);
			world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);

			if (player != null) {
				context.getStack().damage(1, player, context.getHand().getEquipmentSlot());
			}

			return ActionResult.SUCCESS;
		}

		BlockPos firePos = pos.offset(context.getSide());

		if (!AbstractFireBlock.canPlaceAt(world, firePos, context.getHorizontalPlayerFacing())) {
			return ActionResult.FAIL;
		}

		world.playSound(
				player,
				firePos,
				SoundEvents.ITEM_FLINTANDSTEEL_USE,
				SoundCategory.BLOCKS,
				1.0F,
				world.getRandom().nextFloat() * 0.4F + 0.8F
		);

		BlockState fireState = AbstractFireBlock.getState(world, firePos);
		world.setBlockState(firePos, fireState, FIRE_PLACE_FLAGS);
		world.emitGameEvent(player, GameEvent.BLOCK_PLACE, pos);

		ItemStack stack = context.getStack();

		if (player instanceof ServerPlayerEntity serverPlayer) {
			Criteria.PLACED_BLOCK.trigger(serverPlayer, firePos, stack);
			stack.damage(1, player, context.getHand().getEquipmentSlot());
		}

		return ActionResult.SUCCESS;
	}
}
