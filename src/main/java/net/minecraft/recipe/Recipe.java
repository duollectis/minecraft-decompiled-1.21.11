package net.minecraft.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.List;

/**
 * {@code Recipe}.
 */
public interface Recipe<T extends RecipeInput> {

	Codec<Recipe<?>>
			CODEC =
			Registries.RECIPE_SERIALIZER.getCodec().dispatch(Recipe::getSerializer, RecipeSerializer::codec);

	Codec<RegistryKey<Recipe<?>>> KEY_CODEC = RegistryKey.createCodec(RegistryKeys.RECIPE);

	PacketCodec<RegistryByteBuf, Recipe<?>> PACKET_CODEC = PacketCodecs.registryValue(RegistryKeys.RECIPE_SERIALIZER)
	                                                                   .dispatch(
			                                                                   Recipe::getSerializer,
			                                                                   RecipeSerializer::packetCodec
	                                                                   );

	boolean matches(T input, World world);

	ItemStack craft(T input, RegistryWrapper.WrapperLookup registries);

	default boolean isIgnoredInRecipeBook() {
		return false;
	}

	default boolean showNotification() {
		return true;
	}

	default String getGroup() {
		return "";
	}

	RecipeSerializer<? extends Recipe<T>> getSerializer();

	RecipeType<? extends Recipe<T>> getType();

	IngredientPlacement getIngredientPlacement();

	default List<RecipeDisplay> getDisplays() {
		return List.of();
	}

	RecipeBookCategory getRecipeBookCategory();
}
