package net.minecraft.component.type;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
	 * Компонент контейнера предмета (сундук, шалкер-бокс и т.д.).
	 * Хранит список стеков предметов по слотам. Пустые слоты в конце не сохраняются.
	 */
public final class ContainerComponent implements TooltipAppender {

	private static final int NO_ITEMS = -1;
	private static final int MAX_SLOTS = 256;
	private static final int MAX_TOOLTIP_ITEMS = 4;
	public static final ContainerComponent DEFAULT = new ContainerComponent(DefaultedList.of());
	public static final Codec<ContainerComponent> CODEC = ContainerComponent.Slot.CODEC
			.sizeLimitedListOf(MAX_SLOTS)
			.xmap(ContainerComponent::fromSlots, ContainerComponent::collectSlots);
	public static final PacketCodec<RegistryByteBuf, ContainerComponent> PACKET_CODEC = ItemStack.OPTIONAL_PACKET_CODEC
			.collect(PacketCodecs.toList(MAX_SLOTS))
			.xmap(ContainerComponent::new, component -> component.stacks);
	public final DefaultedList<ItemStack> stacks;
	private final int hashCode;

	private ContainerComponent(DefaultedList<ItemStack> stacks) {
		if (stacks.size() > MAX_SLOTS) {
			throw new IllegalArgumentException("Got " + stacks.size() + " items, but maximum is " + MAX_SLOTS);
		}

		this.stacks = stacks;
		this.hashCode = ItemStack.listHashCode(stacks);
	}

	private ContainerComponent(int size) {
		this(DefaultedList.ofSize(size, ItemStack.EMPTY));
	}

	private ContainerComponent(List<ItemStack> stacks) {
		this(stacks.size());

		for (int i = 0; i < stacks.size(); i++) {
			this.stacks.set(i, stacks.get(i));
		}
	}

	private static ContainerComponent fromSlots(List<ContainerComponent.Slot> slots) {
		OptionalInt maxIndex = slots.stream().mapToInt(ContainerComponent.Slot::index).max();
		if (maxIndex.isEmpty()) {
			return DEFAULT;
		}

		ContainerComponent result = new ContainerComponent(maxIndex.getAsInt() + 1);

		for (ContainerComponent.Slot slot : slots) {
			result.stacks.set(slot.index(), slot.item());
		}

		return result;
	}

	public static ContainerComponent fromStacks(List<ItemStack> stacks) {
		int lastNonEmpty = findLastNonEmptyIndex(stacks);
		if (lastNonEmpty == NO_ITEMS) {
			return DEFAULT;
		}

		ContainerComponent result = new ContainerComponent(lastNonEmpty + 1);

		for (int i = 0; i <= lastNonEmpty; i++) {
			result.stacks.set(i, stacks.get(i).copy());
		}

		return result;
	}

	private static int findLastNonEmptyIndex(List<ItemStack> stacks) {
		for (int i = stacks.size() - 1; i >= 0; i--) {
			if (!stacks.get(i).isEmpty()) {
				return i;
			}
		}

		return NO_ITEMS;
	}

	private List<ContainerComponent.Slot> collectSlots() {
		List<ContainerComponent.Slot> slots = new ArrayList<>();

		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i);
			if (!stack.isEmpty()) {
				slots.add(new ContainerComponent.Slot(i, stack));
			}
		}

		return slots;
	}

	public void copyTo(DefaultedList<ItemStack> target) {
		for (int i = 0; i < target.size(); i++) {
			ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
			target.set(i, stack.copy());
		}
	}

	public ItemStack copyFirstStack() {
		return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0).copy();
	}

	public Stream<ItemStack> stream() {
		return stacks.stream().map(ItemStack::copy);
	}

	public Stream<ItemStack> streamNonEmpty() {
		return stacks.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy);
	}

	public Iterable<ItemStack> iterateNonEmpty() {
		return Iterables.filter(stacks, stack -> !stack.isEmpty());
	}

	public Iterable<ItemStack> iterateNonEmptyCopy() {
		return Iterables.transform(iterateNonEmpty(), ItemStack::copy);
	}

	@Override
	public boolean equals(Object o) {
		return this == o
			? true
			: o instanceof ContainerComponent other && ItemStack.stacksEqual(stacks, other.stacks);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		int shown = 0;
		int total = 0;

		for (ItemStack stack : iterateNonEmpty()) {
			total++;
			if (shown < MAX_TOOLTIP_ITEMS) {
				shown++;
				textConsumer.accept(Text.translatable("item.container.item_count", stack.getName(), stack.getCount()));
			}
		}

		int hidden = total - shown;
		if (hidden > 0) {
			textConsumer.accept(Text.translatable("item.container.more_items", hidden).formatted(Formatting.ITALIC));
		}
	}

	record Slot(int index, ItemStack item) {

		public static final Codec<ContainerComponent.Slot> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
											Codec.intRange(0, MAX_SLOTS - 1).fieldOf("slot").forGetter(ContainerComponent.Slot::index),
											ItemStack.CODEC.fieldOf("item").forGetter(ContainerComponent.Slot::item)
									)
									.apply(instance, ContainerComponent.Slot::new)
		);
	}
}
