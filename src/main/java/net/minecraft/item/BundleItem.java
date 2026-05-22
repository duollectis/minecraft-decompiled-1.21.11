package net.minecraft.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.BundleTooltipData;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.commons.lang3.math.Fraction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Предмет котомки. Хранит несколько стеков предметов, суммарный объём которых
 * не превышает 1 единицу. Поддерживает перетаскивание предметов в интерфейсе
 * и выбрасывание содержимого при удержании.
 */
public class BundleItem extends Item {

	public static final int TOOLTIP_STACKS_COLUMNS = 4;
	public static final int TOOLTIP_STACKS_ROWS = 3;
	public static final int MAX_TOOLTIP_STACKS_SHOWN = 12;
	public static final int MAX_TOOLTIP_STACKS_SHOWN_WHEN_TOO_MANY_TYPES = 11;

	private static final int FULL_ITEM_BAR_COLOR = ColorHelper.fromFloats(1.0F, 1.0F, 0.33F, 0.33F);
	private static final int ITEM_BAR_COLOR = ColorHelper.fromFloats(1.0F, 0.44F, 0.53F, 1.0F);
	private static final int TOOLTIP_ITEM_COLUMNS = 10;
	private static final int MAX_USE_TIME = 200;

	public BundleItem(Item.Settings settings) {
		super(settings);
	}

	public static float getAmountFilled(ItemStack stack) {
		BundleContentsComponent contents = stack.getOrDefault(
			DataComponentTypes.BUNDLE_CONTENTS,
			BundleContentsComponent.DEFAULT
		);
		return contents.getOccupancy().floatValue();
	}

	@Override
	public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
		BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);

		if (contents == null) {
			return false;
		}

		ItemStack slotStack = slot.getStack();
		BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(contents);

		if (clickType == ClickType.LEFT && !slotStack.isEmpty()) {
			if (builder.add(slot, player) > 0) {
				playInsertSound(player);
			} else {
				playInsertFailSound(player);
			}

			stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
			onContentChanged(player);
			return true;
		}

		if (clickType == ClickType.RIGHT && slotStack.isEmpty()) {
			ItemStack removed = builder.removeSelected();

			if (removed != null) {
				ItemStack remainder = slot.insertStack(removed);

				if (remainder.getCount() > 0) {
					builder.add(remainder);
				} else {
					playRemoveOneSound(player);
				}
			}

			stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
			onContentChanged(player);
			return true;
		}

		return false;
	}

	@Override
	public boolean onClicked(
		ItemStack stack,
		ItemStack otherStack,
		Slot slot,
		ClickType clickType,
		PlayerEntity player,
		StackReference cursorStackReference
	) {
		if (clickType == ClickType.LEFT && otherStack.isEmpty()) {
			setSelectedStackIndex(stack, -1);
			return false;
		}

		BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);

		if (contents == null) {
			return false;
		}

		BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(contents);

		if (clickType == ClickType.LEFT && !otherStack.isEmpty()) {
			if (slot.canTakePartial(player) && builder.add(otherStack) > 0) {
				playInsertSound(player);
			} else {
				playInsertFailSound(player);
			}

			stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
			onContentChanged(player);
			return true;
		}

		if (clickType == ClickType.RIGHT && otherStack.isEmpty()) {
			if (slot.canTakePartial(player)) {
				ItemStack removed = builder.removeSelected();

				if (removed != null) {
					playRemoveOneSound(player);
					cursorStackReference.set(removed);
				}
			}

			stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
			onContentChanged(player);
			return true;
		}

		setSelectedStackIndex(stack, -1);
		return false;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		user.setCurrentHand(hand);
		return ActionResult.SUCCESS;
	}

	private void dropContentsOnUse(World world, PlayerEntity player, ItemStack stack) {
		if (dropFirstBundledStack(stack, player)) {
			playDropContentsSound(world, player);
			player.incrementStat(Stats.USED.getOrCreateStat(this));
		}
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		BundleContentsComponent contents = stack.getOrDefault(
			DataComponentTypes.BUNDLE_CONTENTS,
			BundleContentsComponent.DEFAULT
		);
		return contents.getOccupancy().compareTo(Fraction.ZERO) > 0;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		BundleContentsComponent contents = stack.getOrDefault(
			DataComponentTypes.BUNDLE_CONTENTS,
			BundleContentsComponent.DEFAULT
		);
		return Math.min(1 + MathHelper.multiplyFraction(contents.getOccupancy(), MAX_TOOLTIP_STACKS_SHOWN), 13);
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		BundleContentsComponent contents = stack.getOrDefault(
			DataComponentTypes.BUNDLE_CONTENTS,
			BundleContentsComponent.DEFAULT
		);
		return contents.getOccupancy().compareTo(Fraction.ONE) >= 0 ? FULL_ITEM_BAR_COLOR : ITEM_BAR_COLOR;
	}

	public static void setSelectedStackIndex(ItemStack stack, int selectedStackIndex) {
		BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);

		if (contents == null) {
			return;
		}

		BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(contents);
		builder.setSelectedStackIndex(selectedStackIndex);
		stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
	}

	public static boolean hasSelectedStack(ItemStack stack) {
		BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
		return contents != null && contents.getSelectedStackIndex() != -1;
	}

	public static int getSelectedStackIndex(ItemStack stack) {
		BundleContentsComponent contents = stack.getOrDefault(
			DataComponentTypes.BUNDLE_CONTENTS,
			BundleContentsComponent.DEFAULT
		);
		return contents.getSelectedStackIndex();
	}

	public static ItemStack getSelectedStack(ItemStack stack) {
		BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);

		if (contents == null || contents.getSelectedStackIndex() == -1) {
			return ItemStack.EMPTY;
		}

		return contents.get(contents.getSelectedStackIndex());
	}

	public static int getNumberOfStacksShown(ItemStack stack) {
		BundleContentsComponent contents = stack.getOrDefault(
			DataComponentTypes.BUNDLE_CONTENTS,
			BundleContentsComponent.DEFAULT
		);
		return contents.getNumberOfStacksShown();
	}

	private boolean dropFirstBundledStack(ItemStack stack, PlayerEntity player) {
		BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);

		if (contents == null || contents.isEmpty()) {
			return false;
		}

		Optional<ItemStack> popped = popFirstBundledStack(stack, player, contents);

		if (popped.isPresent()) {
			player.dropItem(popped.get(), true);
			return true;
		}

		return false;
	}

	private static Optional<ItemStack> popFirstBundledStack(
		ItemStack stack,
		PlayerEntity player,
		BundleContentsComponent contents
	) {
		BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(contents);
		ItemStack removed = builder.removeSelected();

		if (removed == null) {
			return Optional.empty();
		}

		playRemoveOneSound(player);
		stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
		return Optional.of(removed);
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(user instanceof PlayerEntity player)) {
			return;
		}

		int maxUseTicks = getMaxUseTime(stack, user);
		boolean isFirstTick = remainingUseTicks == maxUseTicks;

		if (isFirstTick || remainingUseTicks < maxUseTicks - TOOLTIP_ITEM_COLUMNS && remainingUseTicks % 2 == 0) {
			dropContentsOnUse(world, player, stack);
		}
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return MAX_USE_TIME;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BUNDLE;
	}

	@Override
	public Optional<TooltipData> getTooltipData(ItemStack stack) {
		TooltipDisplayComponent displayComponent = stack.getOrDefault(
			DataComponentTypes.TOOLTIP_DISPLAY,
			TooltipDisplayComponent.DEFAULT
		);

		if (!displayComponent.shouldDisplay(DataComponentTypes.BUNDLE_CONTENTS)) {
			return Optional.empty();
		}

		return Optional.ofNullable(stack.get(DataComponentTypes.BUNDLE_CONTENTS)).map(BundleTooltipData::new);
	}

	@Override
	public void onItemEntityDestroyed(ItemEntity entity) {
		BundleContentsComponent contents = entity.getStack().get(DataComponentTypes.BUNDLE_CONTENTS);

		if (contents == null) {
			return;
		}

		entity.getStack().set(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
		ItemUsage.spawnItemContents(entity, contents.iterateCopy());
	}

	public static List<BundleItem> getBundles() {
		return Stream.of(
			Items.BUNDLE,
			Items.WHITE_BUNDLE,
			Items.ORANGE_BUNDLE,
			Items.MAGENTA_BUNDLE,
			Items.LIGHT_BLUE_BUNDLE,
			Items.YELLOW_BUNDLE,
			Items.LIME_BUNDLE,
			Items.PINK_BUNDLE,
			Items.GRAY_BUNDLE,
			Items.LIGHT_GRAY_BUNDLE,
			Items.CYAN_BUNDLE,
			Items.BLACK_BUNDLE,
			Items.BROWN_BUNDLE,
			Items.GREEN_BUNDLE,
			Items.RED_BUNDLE,
			Items.BLUE_BUNDLE,
			Items.PURPLE_BUNDLE
		).map(item -> (BundleItem) item).toList();
	}

	public static Item getBundle(DyeColor color) {
		return switch (color) {
			case WHITE -> Items.WHITE_BUNDLE;
			case ORANGE -> Items.ORANGE_BUNDLE;
			case MAGENTA -> Items.MAGENTA_BUNDLE;
			case LIGHT_BLUE -> Items.LIGHT_BLUE_BUNDLE;
			case YELLOW -> Items.YELLOW_BUNDLE;
			case LIME -> Items.LIME_BUNDLE;
			case PINK -> Items.PINK_BUNDLE;
			case GRAY -> Items.GRAY_BUNDLE;
			case LIGHT_GRAY -> Items.LIGHT_GRAY_BUNDLE;
			case CYAN -> Items.CYAN_BUNDLE;
			case BLUE -> Items.BLUE_BUNDLE;
			case BROWN -> Items.BROWN_BUNDLE;
			case GREEN -> Items.GREEN_BUNDLE;
			case RED -> Items.RED_BUNDLE;
			case BLACK -> Items.BLACK_BUNDLE;
			case PURPLE -> Items.PURPLE_BUNDLE;
		};
	}

	private static void playRemoveOneSound(Entity entity) {
		entity.playSound(
			SoundEvents.ITEM_BUNDLE_REMOVE_ONE,
			0.8F,
			0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F
		);
	}

	private static void playInsertSound(Entity entity) {
		entity.playSound(
			SoundEvents.ITEM_BUNDLE_INSERT,
			0.8F,
			0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F
		);
	}

	private static void playInsertFailSound(Entity entity) {
		entity.playSound(SoundEvents.ITEM_BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
	}

	private static void playDropContentsSound(World world, Entity entity) {
		world.playSound(
			null,
			entity.getBlockPos(),
			SoundEvents.ITEM_BUNDLE_DROP_CONTENTS,
			SoundCategory.PLAYERS,
			0.8F,
			0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F
		);
	}

	private void onContentChanged(PlayerEntity user) {
		ScreenHandler screenHandler = user.currentScreenHandler;

		if (screenHandler != null) {
			screenHandler.onContentChanged(user.getInventory());
		}
	}
}
