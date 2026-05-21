package net.minecraft.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * {@code DecorationItem}.
 */
public class DecorationItem extends Item {

	private static final Text RANDOM_TEXT = Text.translatable("painting.random").formatted(Formatting.GRAY);
	private final EntityType<? extends AbstractDecorationEntity> entityType;

	public DecorationItem(EntityType<? extends AbstractDecorationEntity> type, Item.Settings settings) {
		super(settings);
		this.entityType = type;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		BlockPos blockPos = context.getBlockPos();
		Direction direction = context.getSide();
		BlockPos blockPos2 = blockPos.offset(direction);
		PlayerEntity playerEntity = context.getPlayer();
		ItemStack itemStack = context.getStack();
		if (playerEntity != null && !this.canPlaceOn(playerEntity, direction, itemStack, blockPos2)) {
			return ActionResult.FAIL;
		}
		else {
			World world = context.getWorld();
			AbstractDecorationEntity abstractDecorationEntity;
			if (this.entityType == EntityType.PAINTING) {
				Optional<PaintingEntity> optional = PaintingEntity.placePainting(world, blockPos2, direction);
				if (optional.isEmpty()) {
					return ActionResult.CONSUME;
				}

				abstractDecorationEntity = optional.get();
			}
			else if (this.entityType == EntityType.ITEM_FRAME) {
				abstractDecorationEntity = new ItemFrameEntity(world, blockPos2, direction);
			}
			else {
				if (this.entityType != EntityType.GLOW_ITEM_FRAME) {
					return ActionResult.SUCCESS;
				}

				abstractDecorationEntity = new GlowItemFrameEntity(world, blockPos2, direction);
			}

			EntityType
					.<AbstractDecorationEntity>copier(world, itemStack, playerEntity)
					.accept(abstractDecorationEntity);
			if (abstractDecorationEntity.canStayAttached()) {
				if (!world.isClient()) {
					abstractDecorationEntity.onPlace();
					world.emitGameEvent(playerEntity, GameEvent.ENTITY_PLACE, abstractDecorationEntity.getEntityPos());
					world.spawnEntity(abstractDecorationEntity);
				}

				itemStack.decrement(1);
				return ActionResult.SUCCESS;
			}
			else {
				return ActionResult.CONSUME;
			}
		}
	}

	/**
	 * Проверяет возможность place on.
	 *
	 * @param player player
	 * @param side side
	 * @param stack stack
	 * @param pos pos
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	protected boolean canPlaceOn(PlayerEntity player, Direction side, ItemStack stack, BlockPos pos) {
		return !side.getAxis().isVertical() && player.canPlaceOn(pos, side, stack);
	}

	@Override
	public void appendTooltip(
			ItemStack stack,
			Item.TooltipContext context,
			TooltipDisplayComponent displayComponent,
			Consumer<Text> textConsumer,
			TooltipType type
	) {
		if (this.entityType == EntityType.PAINTING
				&& displayComponent.shouldDisplay(DataComponentTypes.PAINTING_VARIANT)) {
			RegistryEntry<PaintingVariant> registryEntry = stack.get(DataComponentTypes.PAINTING_VARIANT);
			if (registryEntry != null) {
				registryEntry.value().title().ifPresent(textConsumer);
				registryEntry.value().author().ifPresent(textConsumer);
				textConsumer.accept(Text.translatable(
						"painting.dimensions",
						registryEntry.value().width(),
						registryEntry.value().height()
				));
			}
			else if (type.isCreative()) {
				textConsumer.accept(RANDOM_TEXT);
			}
		}
	}
}
