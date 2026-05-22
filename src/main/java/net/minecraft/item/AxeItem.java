package net.minecraft.item;

import com.google.common.collect.ImmutableMap.Builder;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Предмет «Топор». Поддерживает три вида взаимодействия с блоками:
 * <ul>
 *   <li>Снятие коры с брёвен и стволов (strip)</li>
 *   <li>Удаление окисления с медных блоков (scrape)</li>
 *   <li>Снятие воска с вощёных медных блоков (wax off)</li>
 * </ul>
 */
public class AxeItem extends Item {

	/** Флаги обновления блока при обработке топором: уведомить слушателей + принудительное состояние. */
	private static final int STRIP_UPDATE_FLAGS = 11;

	/** ID мирового события для визуального эффекта удаления окисления. */
	private static final int SCRAPE_EVENT_ID = 3005;

	/** ID мирового события для визуального эффекта снятия воска. */
	private static final int WAX_OFF_EVENT_ID = 3004;

	protected static final Map<Block, Block> STRIPPED_BLOCKS = new Builder<Block, Block>()
		.put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD)
		.put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG)
		.put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD)
		.put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG)
		.put(Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD)
		.put(Blocks.PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG)
		.put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD)
		.put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG)
		.put(Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD)
		.put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG)
		.put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD)
		.put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG)
		.put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD)
		.put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG)
		.put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD)
		.put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG)
		.put(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM)
		.put(Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE)
		.put(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM)
		.put(Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE)
		.put(Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD)
		.put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG)
		.put(Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK)
		.build();

	public AxeItem(ToolMaterial material, float attackDamage, float attackSpeed, Item.Settings settings) {
		super(settings.axe(material, attackDamage, attackSpeed));
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		PlayerEntity player = context.getPlayer();

		if (shouldCancelStripAttempt(context)) {
			return ActionResult.PASS;
		}

		Optional<BlockState> strippedState = tryStrip(world, pos, player, world.getBlockState(pos));

		if (strippedState.isEmpty()) {
			return ActionResult.PASS;
		}

		ItemStack stack = context.getStack();

		if (player instanceof ServerPlayerEntity serverPlayer) {
			Criteria.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
		}

		world.setBlockState(pos, strippedState.get(), STRIP_UPDATE_FLAGS);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, strippedState.get()));

		if (player != null) {
			stack.damage(1, player, context.getHand().getEquipmentSlot());
		}

		return ActionResult.SUCCESS;
	}

	private static boolean shouldCancelStripAttempt(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		return context.getHand().equals(Hand.MAIN_HAND)
			&& player.getOffHandStack().contains(DataComponentTypes.BLOCKS_ATTACKS)
			&& !player.shouldCancelInteraction();
	}

	private Optional<BlockState> tryStrip(World world, BlockPos pos, @Nullable PlayerEntity player, BlockState state) {
		Optional<BlockState> stripped = getStrippedState(state);

		if (stripped.isPresent()) {
			world.playSound(player, pos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
			return stripped;
		}

		Optional<BlockState> deoxidized = Oxidizable.getDecreasedOxidationState(state);

		if (deoxidized.isPresent()) {
			strip(world, pos, player, state, SoundEvents.ITEM_AXE_SCRAPE, SCRAPE_EVENT_ID);
			return deoxidized;
		}

		Optional<BlockState> dewaxed = Optional.ofNullable(
			(Block) HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().get(state.getBlock())
		).map(block -> block.getStateWithProperties(state));

		if (dewaxed.isPresent()) {
			strip(world, pos, player, state, SoundEvents.ITEM_AXE_WAX_OFF, WAX_OFF_EVENT_ID);
			return dewaxed;
		}

		return Optional.empty();
	}

	private static void strip(
		World world,
		BlockPos pos,
		@Nullable PlayerEntity player,
		BlockState state,
		SoundEvent sound,
		int worldEvent
	) {
		world.playSound(player, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
		world.syncWorldEvent(player, worldEvent, pos, 0);

		if (state.getBlock() instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
			BlockPos connectedPos = ChestBlock.getPosInFrontOf(pos, state);
			world.emitGameEvent(
				GameEvent.BLOCK_CHANGE,
				connectedPos,
				GameEvent.Emitter.of(player, world.getBlockState(connectedPos))
			);
			world.syncWorldEvent(player, worldEvent, connectedPos, 0);
		}
	}

	private Optional<BlockState> getStrippedState(BlockState state) {
		return Optional.ofNullable(STRIPPED_BLOCKS.get(state.getBlock()))
			.map(block -> block.getDefaultState().with(PillarBlock.AXIS, state.get(PillarBlock.AXIS)));
	}
}
