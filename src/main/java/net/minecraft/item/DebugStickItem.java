package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DebugStickStateComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Отладочная палка оператора. Позволяет циклически переключать свойства блоков:
 * ПКМ — изменить значение выбранного свойства, ЛКМ — выбрать следующее свойство.
 * Доступна только игрокам с уровнем оператора 2.
 */
public class DebugStickItem extends Item {

	/** Флаги обновления блока при изменении свойства отладочной палкой. */
	private static final int BLOCK_UPDATE_FLAGS = 18;

	public DebugStickItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public boolean canMine(ItemStack stack, BlockState state, World world, BlockPos pos, LivingEntity user) {
		if (!world.isClient() && user instanceof PlayerEntity player) {
			applyDebugAction(player, state, world, pos, false, stack);
		}

		return false;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		World world = context.getWorld();

		if (!world.isClient() && player != null) {
			BlockPos blockPos = context.getBlockPos();

			if (!applyDebugAction(player, world.getBlockState(blockPos), world, blockPos, true, context.getStack())) {
				return ActionResult.FAIL;
			}
		}

		return ActionResult.SUCCESS;
	}

	/**
	 * Применяет действие отладочной палки: выбор свойства или изменение его значения.
	 *
	 * @param player  игрок-оператор
	 * @param state   текущее состояние блока
	 * @param world   мир
	 * @param pos     позиция блока
	 * @param update  {@code true} — изменить значение свойства, {@code false} — выбрать следующее свойство
	 * @param stack   стек отладочной палки
	 * @return {@code true}, если действие выполнено успешно
	 */
	private boolean applyDebugAction(
		PlayerEntity player,
		BlockState state,
		WorldAccess world,
		BlockPos pos,
		boolean update,
		ItemStack stack
	) {
		if (!player.isCreativeLevelTwoOp()) {
			return false;
		}

		RegistryEntry<Block> blockEntry = state.getRegistryEntry();
		StateManager<Block, BlockState> stateManager = blockEntry.value().getStateManager();
		Collection<Property<?>> properties = stateManager.getProperties();

		if (properties.isEmpty()) {
			sendMessage(player, Text.translatable(translationKey + ".empty", blockEntry.getIdAsString()));
			return false;
		}

		DebugStickStateComponent debugState = stack.get(DataComponentTypes.DEBUG_STICK_STATE);

		if (debugState == null) {
			return false;
		}

		Property<?> property = debugState.properties().get(blockEntry);

		if (update) {
			if (property == null) {
				property = properties.iterator().next();
			}

			BlockState updatedState = cycle(state, property, player.shouldCancelInteraction());
			world.setBlockState(pos, updatedState, BLOCK_UPDATE_FLAGS);
			sendMessage(
				player,
				Text.translatable(translationKey + ".update", property.getName(), getValueString(updatedState, property))
			);
		} else {
			property = cycle(properties, property, player.shouldCancelInteraction());
			stack.set(DataComponentTypes.DEBUG_STICK_STATE, debugState.with(blockEntry, property));
			sendMessage(
				player,
				Text.translatable(translationKey + ".select", property.getName(), getValueString(state, property))
			);
		}

		return true;
	}

	private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property, boolean inverse) {
		return state.with(property, cycle(property.getValues(), state.get(property), inverse));
	}

	private static <T> T cycle(Iterable<T> elements, @Nullable T current, boolean inverse) {
		return inverse ? Util.previous(elements, current) : Util.next(elements, current);
	}

	private static void sendMessage(PlayerEntity player, Text message) {
		((ServerPlayerEntity) player).sendMessageToClient(message, true);
	}

	private static <T extends Comparable<T>> String getValueString(BlockState state, Property<T> property) {
		return property.name(state.get(property));
	}
}
