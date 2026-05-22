package net.minecraft.component.type;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.screen.slot.Slot;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
	 * Компонент содержимого сумки (bundle). Хранит список стеков предметов и их суммарную занятость.
	 * Занятость 1/1 означает полную сумку; вложенные сумки занимают 1/16 плюс их собственная занятость.
	 */
public final class BundleContentsComponent implements TooltipData {

	private static final Fraction NESTED_BUNDLE_OCCUPANCY = Fraction.getFraction(1, 16);

	/** Индекс, означающий отсутствие выбранного слота или необходимость создать новый. */
	public static final int NO_SELECTED_SLOT = -1;

	public static final BundleContentsComponent DEFAULT = new BundleContentsComponent(List.of());
	public static final Codec<BundleContentsComponent> CODEC = ItemStack.CODEC
			.listOf()
			.flatXmap(BundleContentsComponent::validateOccupancy, component -> DataResult.success(component.stacks));
	public static final PacketCodec<RegistryByteBuf, BundleContentsComponent> PACKET_CODEC = ItemStack.PACKET_CODEC
			.collect(PacketCodecs.toList())
			.xmap(BundleContentsComponent::new, component -> component.stacks);
	final List<ItemStack> stacks;
	final Fraction occupancy;
	final int selectedStackIndex;

	BundleContentsComponent(List<ItemStack> stacks, Fraction occupancy, int selectedStackIndex) {
		this.stacks = stacks;
		this.occupancy = occupancy;
		this.selectedStackIndex = selectedStackIndex;
	}

	private static DataResult<BundleContentsComponent> validateOccupancy(List<ItemStack> stacks) {
		try {
			Fraction fraction = calculateOccupancy(stacks);
			return DataResult.success(new BundleContentsComponent(stacks, fraction, NO_SELECTED_SLOT));
		} catch (ArithmeticException e) {
			return DataResult.error(() -> "Excessive total bundle weight");
		}
	}

	public BundleContentsComponent(List<ItemStack> stacks) {
		this(stacks, calculateOccupancy(stacks), NO_SELECTED_SLOT);
	}

	private static Fraction calculateOccupancy(List<ItemStack> stacks) {
		Fraction fraction = Fraction.ZERO;

		for (ItemStack itemStack : stacks) {
			fraction = fraction.add(getOccupancy(itemStack).multiplyBy(Fraction.getFraction(itemStack.getCount(), 1)));
		}

		return fraction;
	}

	static Fraction getOccupancy(ItemStack stack) {
		BundleContentsComponent bundleContentsComponent = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
		if (bundleContentsComponent != null) {
			return NESTED_BUNDLE_OCCUPANCY.add(bundleContentsComponent.getOccupancy());
		}
		else {
			List<BeehiveBlockEntity.BeeData>
					list =
					stack.getOrDefault(DataComponentTypes.BEES, BeesComponent.DEFAULT).bees();
			return !list.isEmpty() ? Fraction.ONE : Fraction.getFraction(1, stack.getMaxCount());
		}
	}

	public static boolean canBeBundled(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem().canBeNested();
	}

	public int getNumberOfStacksShown() {
		int total = size();
		int maxVisible = total > 12 ? 11 : 12;
		int remainder = total % 4;
		int padding = remainder == 0 ? 0 : 4 - remainder;
		return Math.min(total, maxVisible - padding);
	}

	public ItemStack get(int index) {
		return stacks.get(index);
	}

	public Stream<ItemStack> stream() {
		return stacks.stream().map(ItemStack::copy);
	}

	public Iterable<ItemStack> iterate() {
		return stacks;
	}

	public Iterable<ItemStack> iterateCopy() {
		return Lists.transform(stacks, ItemStack::copy);
	}

	public int size() {
		return stacks.size();
	}

	public Fraction getOccupancy() {
		return occupancy;
	}

	public boolean isEmpty() {
		return stacks.isEmpty();
	}

	public int getSelectedStackIndex() {
		return selectedStackIndex;
	}

	public boolean hasSelectedStack() {
		return selectedStackIndex != NO_SELECTED_SLOT;
	}

	@Override
	public boolean equals(Object o) {
		return this == o
			? true
			: o instanceof BundleContentsComponent other
				&& occupancy.equals(other.occupancy)
				&& ItemStack.stacksEqual(stacks, other.stacks);
	}

	@Override
	public int hashCode() {
		return ItemStack.listHashCode(stacks);
	}

	@Override
	public String toString() {
		return "BundleContents" + stacks;
	}

	public static class Builder {

		private final List<ItemStack> stacks;
		private Fraction occupancy;
		private int selectedStackIndex;

		public Builder(BundleContentsComponent base) {
			stacks = new ArrayList<>(base.stacks);
			occupancy = base.occupancy;
			selectedStackIndex = base.selectedStackIndex;
		}

		public BundleContentsComponent.Builder clear() {
			stacks.clear();
			occupancy = Fraction.ZERO;
			selectedStackIndex = NO_SELECTED_SLOT;
			return this;
		}

		private int getInsertionIndex(ItemStack stack) {
			if (!stack.isStackable()) {
				return NO_SELECTED_SLOT;
			}

			for (int i = 0; i < stacks.size(); i++) {
				if (ItemStack.areItemsAndComponentsEqual(stacks.get(i), stack)) {
					return i;
				}
			}

			return NO_SELECTED_SLOT;
		}

		private int getMaxAllowed(ItemStack stack) {
			Fraction remaining = Fraction.ONE.subtract(occupancy);
			return Math.max(remaining.divideBy(BundleContentsComponent.getOccupancy(stack)).intValue(), 0);
		}

		public int add(ItemStack stack) {
			if (!BundleContentsComponent.canBeBundled(stack)) {
				return 0;
			}

			int toAdd = Math.min(stack.getCount(), getMaxAllowed(stack));
			if (toAdd == 0) {
				return 0;
			}

			occupancy = occupancy.add(BundleContentsComponent.getOccupancy(stack).multiplyBy(Fraction.getFraction(toAdd, 1)));
			int insertionIndex = getInsertionIndex(stack);

			if (insertionIndex != NO_SELECTED_SLOT) {
				ItemStack existing = stacks.remove(insertionIndex);
				ItemStack merged = existing.copyWithCount(existing.getCount() + toAdd);
				stack.decrement(toAdd);
				stacks.add(0, merged);
			} else {
				stacks.add(0, stack.split(toAdd));
			}

			return toAdd;
		}

		public int add(Slot slot, PlayerEntity player) {
			ItemStack slotStack = slot.getStack();
			int maxAllowed = getMaxAllowed(slotStack);
			return BundleContentsComponent.canBeBundled(slotStack)
				? add(slot.takeStackRange(slotStack.getCount(), maxAllowed, player))
				: 0;
		}

		public void setSelectedStackIndex(int index) {
			selectedStackIndex = selectedStackIndex != index && !isOutOfBounds(index)
				? index
				: NO_SELECTED_SLOT;
		}

		private boolean isOutOfBounds(int index) {
			return index < 0 || index >= stacks.size();
		}

		public @Nullable ItemStack removeSelected() {
			if (stacks.isEmpty()) {
				return null;
			}

			int removeIndex = isOutOfBounds(selectedStackIndex) ? 0 : selectedStackIndex;
			ItemStack removed = stacks.remove(removeIndex).copy();
			occupancy = occupancy.subtract(
					BundleContentsComponent.getOccupancy(removed).multiplyBy(Fraction.getFraction(removed.getCount(), 1))
			);
			setSelectedStackIndex(NO_SELECTED_SLOT);
			return removed;
		}

		public Fraction getOccupancy() {
			return occupancy;
		}

		public BundleContentsComponent build() {
			return new BundleContentsComponent(List.copyOf(stacks), occupancy, selectedStackIndex);
		}
	}
}
