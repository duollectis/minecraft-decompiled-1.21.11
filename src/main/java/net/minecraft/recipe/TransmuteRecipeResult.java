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
 * {@code TransmuteRecipeResult}.
 */
public record TransmuteRecipeResult(RegistryEntry<Item> itemEntry, int count, ComponentChanges components) {

	private static final Codec<TransmuteRecipeResult> BASE_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Item.ENTRY_CODEC.fieldOf("id").forGetter(TransmuteRecipeResult::itemEntry),
					                    Codecs.rangedInt(1, 99).optionalFieldOf("count", 1).forGetter(TransmuteRecipeResult::count),
					                    ComponentChanges.CODEC
							                    .optionalFieldOf("components", ComponentChanges.EMPTY)
							                    .forGetter(TransmuteRecipeResult::components)
			                    )
			                    .apply(instance, TransmuteRecipeResult::new)
	);
	public static final Codec<TransmuteRecipeResult> CODEC = Codec.withAlternative(
			                                                              BASE_CODEC, Item.ENTRY_CODEC, itemEntry -> new TransmuteRecipeResult((Item) itemEntry.value())
	                                                              )
	                                                              .validate(TransmuteRecipeResult::validate);
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

	public ItemStack apply(ItemStack stack) {
		ItemStack itemStack = stack.copyComponentsToNewStack(this.itemEntry.value(), this.count);
		itemStack.applyUnvalidatedChanges(this.components);
		return itemStack;
	}

	public boolean isEqualToResult(ItemStack stack) {
		ItemStack itemStack = this.apply(stack);
		return itemStack.getCount() == 1 && ItemStack.areItemsAndComponentsEqual(stack, itemStack);
	}

	public SlotDisplay createSlotDisplay() {
		return new SlotDisplay.StackSlotDisplay(new ItemStack(this.itemEntry, this.count, this.components));
	}
}
