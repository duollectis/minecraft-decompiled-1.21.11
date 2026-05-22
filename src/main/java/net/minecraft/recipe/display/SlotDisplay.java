package net.minecraft.recipe.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.FuelRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.SmithingTrimRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.stream.Stream;

/**
 * Интерфейс отображения одного слота рецепта на клиенте.
 * Каждая реализация описывает, какие {@link ItemStack} показывать в слоте.
 * Все реализации регистрируются в реестре {@code SLOT_DISPLAY}.
 */
public interface SlotDisplay {

	Codec<SlotDisplay> CODEC = Registries.SLOT_DISPLAY
			.getCodec()
			.dispatch(SlotDisplay::serializer, Serializer::codec);

	PacketCodec<RegistryByteBuf, SlotDisplay> PACKET_CODEC = PacketCodecs
			.registryValue(RegistryKeys.SLOT_DISPLAY)
			.dispatch(SlotDisplay::serializer, Serializer::streamCodec);

	<T> Stream<T> appendStacks(ContextParameterMap parameters, DisplayedItemFactory<T> factory);

	Serializer<? extends SlotDisplay> serializer();

	default boolean isEnabled(FeatureSet features) {
		return true;
	}

	default List<ItemStack> getStacks(ContextParameterMap parameters) {
		return appendStacks(parameters, NoopDisplayedItemFactory.INSTANCE).toList();
	}

	default ItemStack getFirst(ContextParameterMap context) {
		return appendStacks(context, NoopDisplayedItemFactory.INSTANCE)
				.findFirst()
				.orElse(ItemStack.EMPTY);
	}

	/**
	 * Отображение слота, принимающего любое топливо.
	 * Список предметов берётся из {@link FuelRegistry} контекста.
	 */
	class AnyFuelSlotDisplay implements SlotDisplay {

		public static final AnyFuelSlotDisplay INSTANCE = new AnyFuelSlotDisplay();
		public static final MapCodec<AnyFuelSlotDisplay> CODEC = MapCodec.unit(INSTANCE);
		public static final PacketCodec<RegistryByteBuf, AnyFuelSlotDisplay> PACKET_CODEC = PacketCodec.unit(INSTANCE);
		public static final Serializer<AnyFuelSlotDisplay> SERIALIZER = new Serializer<>(CODEC, PACKET_CODEC);

		private AnyFuelSlotDisplay() {
		}

		@Override
		public Serializer<AnyFuelSlotDisplay> serializer() {
			return SERIALIZER;
		}

		@Override
		public String toString() {
			return "<any fuel>";
		}

		@Override
		public <T> Stream<T> appendStacks(ContextParameterMap parameters, DisplayedItemFactory<T> factory) {
			if (factory instanceof DisplayedItemFactory.FromStack<T> fromStack) {
				FuelRegistry fuelRegistry = parameters.getNullable(SlotDisplayContexts.FUEL_REGISTRY);
				if (fuelRegistry != null) {
					return fuelRegistry.getFuelItems().stream().map(fromStack::toDisplayed);
				}
			}

			return Stream.empty();
		}
	}

	/**
	 * Составное отображение слота — объединяет несколько {@link SlotDisplay} в одно.
	 */
	record CompositeSlotDisplay(List<SlotDisplay> contents) implements SlotDisplay {

		public static final MapCodec<CompositeSlotDisplay> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(SlotDisplay.CODEC.listOf().fieldOf("contents").forGetter(CompositeSlotDisplay::contents))
						.apply(instance, CompositeSlotDisplay::new)
		);
		public static final PacketCodec<RegistryByteBuf, CompositeSlotDisplay> PACKET_CODEC = PacketCodec.tuple(
				SlotDisplay.PACKET_CODEC.collect(PacketCodecs.toList()),
				CompositeSlotDisplay::contents,
				CompositeSlotDisplay::new
		);
		public static final Serializer<CompositeSlotDisplay> SERIALIZER = new Serializer<>(CODEC, PACKET_CODEC);

		@Override
		public Serializer<CompositeSlotDisplay> serializer() {
			return SERIALIZER;
		}

		@Override
		public <T> Stream<T> appendStacks(ContextParameterMap parameters, DisplayedItemFactory<T> factory) {
			return contents.stream().flatMap(display -> display.appendStacks(parameters, factory));
		}

		@Override
		public boolean isEnabled(FeatureSet features) {
			return contents.stream().allMatch(child -> child.isEnabled(features));
		}
	}

	/**
	 * Отображение пустого слота — не показывает ничего.
	 */
	class EmptySlotDisplay implements SlotDisplay {

		public static final EmptySlotDisplay INSTANCE = new EmptySlotDisplay();
		public static final MapCodec<EmptySlotDisplay> CODEC = MapCodec.unit(INSTANCE);
		public static final PacketCodec<RegistryByteBuf, EmptySlotDisplay> PACKET_CODEC = PacketCodec.unit(INSTANCE);
		public static final Serializer<EmptySlotDisplay> SERIALIZER = new Serializer<>(CODEC, PACKET_CODEC);

		private EmptySlotDisplay() {
		}

		@Override
		public Serializer<EmptySlotDisplay> serializer() {
			return SERIALIZER;
		}

		@Override
		public String toString() {
			return "<empty>";
		}

		@Override
		public <T> Stream<T> appendStacks(ContextParameterMap parameters, DisplayedItemFactory<T> factory) {
			return Stream.empty();
		}
	}

	/**
	 * Отображение слота с конкретным предметом по его записи реестра.
	 */
	record ItemSlotDisplay(RegistryEntry<Item> item) implements SlotDisplay {

		public static final MapCodec<ItemSlotDisplay> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(Item.ENTRY_CODEC.fieldOf("item").forGetter(ItemSlotDisplay::item))
						.apply(instance, ItemSlotDisplay::new)
		);
		public static final PacketCodec<RegistryByteBuf, ItemSlotDisplay> PACKET_CODEC = PacketCodec.tuple(
				Item.ENTRY_PACKET_CODEC, ItemSlotDisplay::item, ItemSlotDisplay::new
		);
		public static final Serializer<ItemSlotDisplay> SERIALIZER = new Serializer<>(CODEC, PACKET_CODEC);

		public ItemSlotDisplay(Item item) {
			this(item.getRegistryEntry());
		}

		@Override
		public Serializer<ItemSlotDisplay> serializer() {
			return SERIALIZER;
		}

		@Override
		public <T> Stream<T> appendStacks(ContextParameterMap parameters, DisplayedItemFactory<T> factory) {
			return factory instanceof DisplayedItemFactory.FromStack<T> fromStack
					? Stream.of(fromStack.toDisplayed(item))
					: Stream.empty();
		}

		@Override
		public boolean isEnabled(FeatureSet features) {
			return item.value().isEnabled(features);
		}
	}

	/**
	 * Реализация {@link DisplayedItemFactory.FromStack} по умолчанию — возвращает стек без изменений.
	 */
	class NoopDisplayedItemFactory implements DisplayedItemFactory.FromStack<ItemStack> {

		public static final NoopDisplayedItemFactory INSTANCE = new NoopDisplayedItemFactory();

		@Override
		public ItemStack toDisplayed(ItemStack stack) {
			return stack;
		}
	}

	record Serializer<T extends SlotDisplay>(MapCodec<T> codec, PacketCodec<RegistryByteBuf, T> streamCodec) {
	}

	/**
	 * Отображение слота с наложением трима кузнечного стола.
	 * Генерирует случайные комбинации базы и материала с заданным паттерном трима.
	 */
	record SmithingTrimSlotDisplay(
			SlotDisplay base,
			SlotDisplay material,
			RegistryEntry<ArmorTrimPattern> pattern
	) implements SlotDisplay {

		public static final MapCodec<SmithingTrimSlotDisplay> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						SlotDisplay.CODEC.fieldOf("base").forGetter(SmithingTrimSlotDisplay::base),
						SlotDisplay.CODEC.fieldOf("material").forGetter(SmithingTrimSlotDisplay::material),
						ArmorTrimPattern.ENTRY_CODEC.fieldOf("pattern").forGetter(SmithingTrimSlotDisplay::pattern)
				).apply(instance, SmithingTrimSlotDisplay::new)
		);
		public static final PacketCodec<RegistryByteBuf, SmithingTrimSlotDisplay> PACKET_CODEC = PacketCodec.tuple(
				SlotDisplay.PACKET_CODEC, SmithingTrimSlotDisplay::base,
				SlotDisplay.PACKET_CODEC, SmithingTrimSlotDisplay::material,
				ArmorTrimPattern.ENTRY_PACKET_CODEC, SmithingTrimSlotDisplay::pattern,
				SmithingTrimSlotDisplay::new
		);
		public static final Serializer<SmithingTrimSlotDisplay> SERIALIZER = new Serializer<>(CODEC, PACKET_CODEC);

		private static final int MAX_GENERATION_ATTEMPTS = 256;
		private static final int MAX_DISPLAY_RESULTS = 16;

		@Override
		public Serializer<SmithingTrimSlotDisplay> serializer() {
			return SERIALIZER;
		}

		@Override
		public <T> Stream<T> appendStacks(ContextParameterMap parameters, DisplayedItemFactory<T> factory) {
			if (factory instanceof DisplayedItemFactory.FromStack<T> fromStack) {
				RegistryWrapper.WrapperLookup registries = parameters.getNullable(SlotDisplayContexts.REGISTRIES);
				if (registries != null) {
					Random random = Random.create(System.identityHashCode(this));
					List<ItemStack> baseStacks = base.getStacks(parameters);
					if (baseStacks.isEmpty()) {
						return Stream.empty();
					}

					List<ItemStack> materialStacks = material.getStacks(parameters);
					if (materialStacks.isEmpty()) {
						return Stream.empty();
					}

					return Stream.<ItemStack>generate(() -> {
						ItemStack baseStack = Util.getRandom(baseStacks, random);
						ItemStack materialStack = Util.getRandom(materialStacks, random);
						return SmithingTrimRecipe.craft(registries, baseStack, materialStack, pattern);
					})
							.limit(MAX_GENERATION_ATTEMPTS)
							.filter(stack -> !stack.isEmpty())
							.limit(MAX_DISPLAY_RESULTS)
							.map(fromStack::toDisplayed);
				}
			}

			return Stream.empty();
		}
	}

	/**
	 * Отображение слота с конкретным {@link ItemStack} (включая компоненты).
	 */
	record StackSlotDisplay(ItemStack stack) implements SlotDisplay {

		public static final MapCodec<StackSlotDisplay> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(ItemStack.VALIDATED_CODEC.fieldOf("item").forGetter(StackSlotDisplay::stack))
						.apply(instance, StackSlotDisplay::new)
		);
		public static final PacketCodec<RegistryByteBuf, StackSlotDisplay> PACKET_CODEC = PacketCodec.tuple(
				ItemStack.PACKET_CODEC, StackSlotDisplay::stack, StackSlotDisplay::new
		);
		public static final Serializer<StackSlotDisplay> SERIALIZER = new Serializer<>(CODEC, PACKET_CODEC);

		@Override
		public Serializer<StackSlotDisplay> serializer() {
			return SERIALIZER;
		}

		@Override
		public <T> Stream<T> appendStacks(ContextParameterMap parameters, DisplayedItemFactory<T> factory) {
			return factory instanceof DisplayedItemFactory.FromStack<T> fromStack
					? Stream.of(fromStack.toDisplayed(stack))
					: Stream.empty();
		}

		@Override
		public boolean equals(Object o) {
			return this == o
					|| o instanceof StackSlotDisplay other && ItemStack.areEqual(stack, other.stack);
		}

		@Override
		public boolean isEnabled(FeatureSet features) {
			return stack.getItem().isEnabled(features);
		}
	}

	/**
	 * Отображение слота со всеми предметами из тега.
	 */
	record TagSlotDisplay(TagKey<Item> tag) implements SlotDisplay {

		public static final MapCodec<TagSlotDisplay> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(TagKey.unprefixedCodec(RegistryKeys.ITEM).fieldOf("tag").forGetter(TagSlotDisplay::tag))
						.apply(instance, TagSlotDisplay::new)
		);
		public static final PacketCodec<RegistryByteBuf, TagSlotDisplay> PACKET_CODEC = PacketCodec.tuple(
				TagKey.packetCodec(RegistryKeys.ITEM), TagSlotDisplay::tag, TagSlotDisplay::new
		);
		public static final Serializer<TagSlotDisplay> SERIALIZER = new Serializer<>(CODEC, PACKET_CODEC);

		@Override
		public Serializer<TagSlotDisplay> serializer() {
			return SERIALIZER;
		}

		@Override
		public <T> Stream<T> appendStacks(ContextParameterMap parameters, DisplayedItemFactory<T> factory) {
			if (factory instanceof DisplayedItemFactory.FromStack<T> fromStack) {
				RegistryWrapper.WrapperLookup registries = parameters.getNullable(SlotDisplayContexts.REGISTRIES);
				if (registries != null) {
					return registries.getOrThrow(RegistryKeys.ITEM)
							.getOptional(tag)
							.map(tagEntries -> tagEntries.stream().map(fromStack::toDisplayed))
							.stream()
							.flatMap(values -> values);
				}
			}

			return Stream.empty();
		}
	}

	/**
	 * Отображение слота с предметом и его остатком (remainder) после использования.
	 */
	record WithRemainderSlotDisplay(SlotDisplay input, SlotDisplay remainder) implements SlotDisplay {

		public static final MapCodec<WithRemainderSlotDisplay> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						SlotDisplay.CODEC.fieldOf("input").forGetter(WithRemainderSlotDisplay::input),
						SlotDisplay.CODEC.fieldOf("remainder").forGetter(WithRemainderSlotDisplay::remainder)
				).apply(instance, WithRemainderSlotDisplay::new)
		);
		public static final PacketCodec<RegistryByteBuf, WithRemainderSlotDisplay> PACKET_CODEC = PacketCodec.tuple(
				SlotDisplay.PACKET_CODEC, WithRemainderSlotDisplay::input,
				SlotDisplay.PACKET_CODEC, WithRemainderSlotDisplay::remainder,
				WithRemainderSlotDisplay::new
		);
		public static final Serializer<WithRemainderSlotDisplay> SERIALIZER = new Serializer<>(CODEC, PACKET_CODEC);

		@Override
		public Serializer<WithRemainderSlotDisplay> serializer() {
			return SERIALIZER;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Stream<T> appendStacks(ContextParameterMap parameters, DisplayedItemFactory<T> factory) {
			if (factory instanceof DisplayedItemFactory.FromRemainder<T> fromRemainder) {
				List<T> remainders = remainder.appendStacks(parameters, factory).toList();
				return input.appendStacks(parameters, factory)
						.map(inputItem -> fromRemainder.toDisplayed((T) inputItem, remainders));
			}

			return input.appendStacks(parameters, factory);
		}

		@Override
		public boolean isEnabled(FeatureSet features) {
			return input.isEnabled(features) && remainder.isEnabled(features);
		}
	}
}
