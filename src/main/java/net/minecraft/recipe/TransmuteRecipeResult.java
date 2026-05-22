package net.minecraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.dynamic.Codecs;

/**
 * Описывает результат рецепта трансмутации: целевой предмет, количество и изменения компонентов.
 * При применении копирует компоненты исходного предмета и накладывает поверх {@code components}.
 */
public record TransmuteRecipeResult(RegistryEntry<Item> itemEntry, int count, ComponentChanges components) {

	private static final Codec<TransmuteRecipeResult> BASE_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Item.ENTRY_CODEC.fieldOf("id").forGetter(TransmuteRecipeResult::itemEntry),
			Codecs.rangedInt(1, 99).optionalFieldOf("count", 1).forGetter(TransmuteRecipeResult::count),
			ComponentChanges.CODEC
				.optionalFieldOf("components", ComponentChanges.EMPTY)
				.forGetter(TransmuteRecipeResult::components)
		).apply(instance, TransmuteRecipeResult::new)
	);

	public static final Codec<TransmuteRecipeResult> CODEC = Codec.withAlternative(
		BASE_CODEC,
		Item.ENTRY_CODEC,
		itemEntry -> new TransmuteRecipeResult(itemEntry.value())
	).validate(TransmuteRecipeResult::validate);

	public static final PacketCodec<RegistryByteBuf, TransmuteRecipeResult> PACKET_CODEC = PacketCodec.tuple(
		Item.ENTRY_PACKET_CODEC,
		TransmuteRecipeResult::itemEntry,
		PacketCodecs.VAR_INT,
		TransmuteRecipeResult::count,
		ComponentChanges.PACKET_CODEC,
		TransmuteRecipeResult::components,
		TransmuteRecipeResult::new
	);

	public TransmuteRecipeResult(Item item) {
		this(item.getRegistryEntry(), 1, ComponentChanges.EMPTY);
	}

	private static DataResult<TransmuteRecipeResult> validate(TransmuteRecipeResult result) {
		return ItemStack
			.validate(new ItemStack(result.itemEntry, result.count, result.components))
			.map(stack -> result);
	}

	/**
	 * Применяет трансмутацию к стеку: копирует компоненты исходного предмета
	 * в новый предмет типа {@code itemEntry}, затем накладывает {@code components}.
	 */
	public ItemStack apply(ItemStack stack) {
		ItemStack transmuted = stack.copyComponentsToNewStack(itemEntry.value(), count);
		transmuted.applyUnvalidatedChanges(components);

		return transmuted;
	}

	public boolean isEqualToResult(ItemStack stack) {
		ItemStack applied = apply(stack);

		return applied.getCount() == 1 && ItemStack.areItemsAndComponentsEqual(stack, applied);
	}

	public SlotDisplay createSlotDisplay() {
		return new SlotDisplay.StackSlotDisplay(new ItemStack(itemEntry, count, components));
	}
}
