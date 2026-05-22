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
 * Предмет декорации: картина, рамка для предметов или светящаяся рамка.
 * При использовании на блоке размещает соответствующую сущность-декорацию.
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
		Direction side = context.getSide();
		BlockPos targetPos = blockPos.offset(side);
		PlayerEntity player = context.getPlayer();
		ItemStack stack = context.getStack();

		if (player != null && !canPlaceOn(player, side, stack, targetPos)) {
			return ActionResult.FAIL;
		}

		World world = context.getWorld();
		AbstractDecorationEntity decoration;

		if (entityType == EntityType.PAINTING) {
			Optional<PaintingEntity> painting = PaintingEntity.placePainting(world, targetPos, side);

			if (painting.isEmpty()) {
				return ActionResult.CONSUME;
			}

			decoration = painting.get();
		} else if (entityType == EntityType.ITEM_FRAME) {
			decoration = new ItemFrameEntity(world, targetPos, side);
		} else if (entityType == EntityType.GLOW_ITEM_FRAME) {
			decoration = new GlowItemFrameEntity(world, targetPos, side);
		} else {
			return ActionResult.SUCCESS;
		}

		EntityType.<AbstractDecorationEntity>copier(world, stack, player).accept(decoration);

		if (!decoration.canStayAttached()) {
			return ActionResult.CONSUME;
		}

		if (!world.isClient()) {
			decoration.onPlace();
			world.emitGameEvent(player, GameEvent.ENTITY_PLACE, decoration.getEntityPos());
			world.spawnEntity(decoration);
		}

		stack.decrement(1);
		return ActionResult.SUCCESS;
	}

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
		if (entityType != EntityType.PAINTING
			|| !displayComponent.shouldDisplay(DataComponentTypes.PAINTING_VARIANT)
		) {
			return;
		}

		RegistryEntry<PaintingVariant> variant = stack.get(DataComponentTypes.PAINTING_VARIANT);

		if (variant != null) {
			variant.value().title().ifPresent(textConsumer);
			variant.value().author().ifPresent(textConsumer);
			textConsumer.accept(Text.translatable(
				"painting.dimensions",
				variant.value().width(),
				variant.value().height()
			));
		} else if (type.isCreative()) {
			textConsumer.accept(RANDOM_TEXT);
		}
	}
}
