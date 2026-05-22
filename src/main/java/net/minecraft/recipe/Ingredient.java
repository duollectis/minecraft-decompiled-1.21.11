package net.minecraft.recipe;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryListCodec;
import net.minecraft.util.dynamic.Codecs;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Описывает допустимые предметы для одного слота рецепта.
 * Может быть задан через тег предметов или явным списком предметов.
 * Пустой ингредиент (без предметов) недопустим — выбрасывает исключение при создании.
 */
public final class Ingredient implements RecipeMatcher.RawIngredient<RegistryEntry<Item>>, Predicate<ItemStack>, FabricIngredient {

	public static final PacketCodec<RegistryByteBuf, Ingredient> PACKET_CODEC =
		PacketCodecs.registryEntryList(RegistryKeys.ITEM)
			.xmap(Ingredient::new, ingredient -> ingredient.entries);

	public static final PacketCodec<RegistryByteBuf, Optional<Ingredient>> OPTIONAL_PACKET_CODEC =
		PacketCodecs.registryEntryList(RegistryKeys.ITEM)
			.xmap(
				entries -> entries.size() == 0
					? Optional.empty()
					: Optional.of(new Ingredient((RegistryEntryList<Item>) entries)),
				optional -> optional
					.map(ingredient -> (RegistryEntryList<Item>) ingredient.entries)
					.orElse(RegistryEntryList.of())
			);

	public static final Codec<RegistryEntryList<Item>> ENTRIES_CODEC =
		RegistryEntryListCodec.create(RegistryKeys.ITEM, Item.ENTRY_CODEC, false);

	public static final Codec<Ingredient> CODEC =
		Codecs.nonEmptyEntryList(ENTRIES_CODEC).xmap(Ingredient::new, ingredient -> ingredient.entries);

	private final RegistryEntryList<Item> entries;

	private Ingredient(RegistryEntryList<Item> entries) {
		entries.getStorage().ifRight(list -> {
			if (list.isEmpty()) {
				throw new UnsupportedOperationException("Ingredients can't be empty");
			}

			if (list.contains(Items.AIR.getRegistryEntry())) {
				throw new UnsupportedOperationException("Ingredient can't contain air");
			}
		});

		this.entries = entries;
	}

	/**
	 * Проверяет совпадение опционального ингредиента со стеком.
	 * Если ингредиент отсутствует — считается совпадением только для пустого стека.
	 */
	public static boolean matches(Optional<Ingredient> ingredient, ItemStack stack) {
		return ingredient.<Boolean>map(ing -> ing.test(stack)).orElseGet(stack::isEmpty);
	}

	@Deprecated
	public Stream<RegistryEntry<Item>> getMatchingItems() {
		return entries.stream();
	}

	public boolean isEmpty() {
		return entries.size() == 0;
	}

	@Override
	public boolean test(ItemStack stack) {
		return stack.isIn(entries);
	}

	@Override
	public boolean acceptsItem(RegistryEntry<Item> registryEntry) {
		return entries.contains(registryEntry);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Ingredient other ? Objects.equals(entries, other.entries) : false;
	}

	public static Ingredient ofItem(ItemConvertible item) {
		return new Ingredient(RegistryEntryList.of(item.asItem().getRegistryEntry()));
	}

	public static Ingredient ofItems(ItemConvertible... items) {
		return ofItems(Arrays.stream(items));
	}

	public static Ingredient ofItems(Stream<? extends ItemConvertible> items) {
		return new Ingredient(RegistryEntryList.of(items.map(item -> item.asItem().getRegistryEntry()).toList()));
	}

	public static Ingredient ofTag(RegistryEntryList<Item> tag) {
		return new Ingredient(tag);
	}

	/**
	 * Создаёт {@link SlotDisplay} для отображения ингредиента в интерфейсе.
	 * Тег отображается как {@link SlotDisplay.TagSlotDisplay}, список предметов —
	 * как {@link SlotDisplay.CompositeSlotDisplay} с учётом остатков крафта.
	 */
	public SlotDisplay toDisplay() {
		return (SlotDisplay) entries
			.getStorage()
			.map(
				SlotDisplay.TagSlotDisplay::new,
				items -> new SlotDisplay.CompositeSlotDisplay(
					items.stream()
						.map(Ingredient::createDisplayWithRemainder)
						.toList()
				)
			);
	}

	public static SlotDisplay toDisplay(Optional<Ingredient> ingredient) {
		return ingredient.<SlotDisplay>map(Ingredient::toDisplay).orElse(SlotDisplay.EmptySlotDisplay.INSTANCE);
	}

	private static SlotDisplay createDisplayWithRemainder(RegistryEntry<Item> displayedItem) {
		SlotDisplay itemDisplay = new SlotDisplay.ItemSlotDisplay(displayedItem);
		ItemStack remainder = displayedItem.value().getRecipeRemainder();

		if (remainder.isEmpty()) {
			return itemDisplay;
		}

		return new SlotDisplay.WithRemainderSlotDisplay(itemDisplay, new SlotDisplay.StackSlotDisplay(remainder));
	}
}
