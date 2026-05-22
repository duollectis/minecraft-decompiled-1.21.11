package net.minecraft.item;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.AbstractPlantStemBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;

/**
 * Предмет «Ножницы». Поддерживает стрижку растений-стеблей (устанавливает максимальный
 * возраст) и корректно учитывает урон инструмента через компонент {@link DataComponentTypes#TOOL}.
 */
public class ShearsItem extends Item {

	public ShearsItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Создаёт компонент инструмента для ножниц с правилами скорости добычи:
	 * паутина (15x), листья (15x), шерсть (5x), лоза и светящийся лишайник (2x).
	 */
	public static ToolComponent createToolComponent() {
		RegistryEntryLookup<Block> blockLookup = Registries.createEntryLookup(Registries.BLOCK);

		return new ToolComponent(
			List.of(
				ToolComponent.Rule.ofAlwaysDropping(
					RegistryEntryList.of(Blocks.COBWEB.getRegistryEntry()),
					15.0F
				),
				ToolComponent.Rule.of(blockLookup.getOrThrow(BlockTags.LEAVES), 15.0F),
				ToolComponent.Rule.of(blockLookup.getOrThrow(BlockTags.WOOL), 5.0F),
				ToolComponent.Rule.of(
					RegistryEntryList.of(
						Blocks.VINE.getRegistryEntry(),
						Blocks.GLOW_LICHEN.getRegistryEntry()
					),
					2.0F
				)
			),
			1.0F,
			1,
			true
		);
	}

	@Override
	public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
		ToolComponent toolComponent = stack.get(DataComponentTypes.TOOL);

		if (toolComponent == null) {
			return false;
		}

		if (!world.isClient() && !state.isIn(BlockTags.FIRE) && toolComponent.damagePerBlock() > 0) {
			stack.damage(toolComponent.damagePerBlock(), miner, EquipmentSlot.MAINHAND);
		}

		return true;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState blockState = world.getBlockState(pos);

		if (!(blockState.getBlock() instanceof AbstractPlantStemBlock stemBlock)
			|| stemBlock.hasMaxAge(blockState)
		) {
			return super.useOnBlock(context);
		}

		PlayerEntity player = context.getPlayer();
		ItemStack stack = context.getStack();

		if (player instanceof ServerPlayerEntity serverPlayer) {
			Criteria.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
		}

		world.playSound(player, pos, SoundEvents.BLOCK_GROWING_PLANT_CROP, SoundCategory.BLOCKS, 1.0F, 1.0F);

		BlockState maxAgeState = stemBlock.withMaxAge(blockState);
		world.setBlockState(pos, maxAgeState);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, maxAgeState));

		if (player != null) {
			stack.damage(1, player, context.getHand().getEquipmentSlot());
		}

		return ActionResult.SUCCESS;
	}
}
