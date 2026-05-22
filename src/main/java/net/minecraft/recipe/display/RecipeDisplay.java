package net.minecraft.recipe.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.featuretoggle.FeatureSet;

/**
 * Интерфейс отображения рецепта на клиенте. Описывает, что показывать
 * в слотах результата и станка. Все реализации регистрируются в реестре
 * {@code RECIPE_DISPLAY} и сериализуются через {@link Serializer}.
 */
public interface RecipeDisplay {

	Codec<RecipeDisplay> CODEC = Registries.RECIPE_DISPLAY
			.getCodec()
			.dispatch(RecipeDisplay::serializer, Serializer::codec);

	PacketCodec<RegistryByteBuf, RecipeDisplay> PACKET_CODEC = PacketCodecs
			.registryValue(RegistryKeys.RECIPE_DISPLAY)
			.dispatch(RecipeDisplay::serializer, Serializer::streamCodec);

	SlotDisplay result();

	SlotDisplay craftingStation();

	Serializer<? extends RecipeDisplay> serializer();

	default boolean isEnabled(FeatureSet features) {
		return result().isEnabled(features) && craftingStation().isEnabled(features);
	}

	record Serializer<T extends RecipeDisplay>(MapCodec<T> codec, PacketCodec<RegistryByteBuf, T> streamCodec) {
	}
}
