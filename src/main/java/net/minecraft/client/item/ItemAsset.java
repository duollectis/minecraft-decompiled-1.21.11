package net.minecraft.client.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.ItemModelTypes;
import net.minecraft.registry.ContextSwapper;
import org.jspecify.annotations.Nullable;

/**
 * Описание клиентского ассета предмета: модель, свойства анимации и опциональный
 * {@link ContextSwapper} для подмены реестровых контекстов при рендере.
 * Загружается из JSON-файлов в директории {@code items/} ресурс-пака.
 */
@Environment(EnvType.CLIENT)
public record ItemAsset(
		ItemModel.Unbaked model,
		ItemAsset.Properties properties,
		@Nullable ContextSwapper registrySwapper
) {

	public static final Codec<ItemAsset> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(
							ItemModelTypes.CODEC.fieldOf("model").forGetter(ItemAsset::model),
							ItemAsset.Properties.CODEC.forGetter(ItemAsset::properties)
					)
					.apply(instance, ItemAsset::new)
	);

	public ItemAsset(ItemModel.Unbaked model, ItemAsset.Properties properties) {
		this(model, properties, null);
	}

	/** Возвращает копию ассета с привязанным {@link ContextSwapper} для реестровых подстановок. */
	public ItemAsset withContextSwapper(ContextSwapper contextSwapper) {
		return new ItemAsset(model, properties, contextSwapper);
	}

	/** Свойства анимации предмета при смене в руке и отображении в GUI. */
	@Environment(EnvType.CLIENT)
	public record Properties(boolean handAnimationOnSwap, boolean oversizedInGui, float swapAnimationScale) {

		public static final ItemAsset.Properties DEFAULT = new ItemAsset.Properties(true, false, 1.0F);
		public static final MapCodec<ItemAsset.Properties> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
								Codec.BOOL
										.optionalFieldOf("hand_animation_on_swap", true)
										.forGetter(ItemAsset.Properties::handAnimationOnSwap),
								Codec.BOOL
										.optionalFieldOf("oversized_in_gui", false)
										.forGetter(ItemAsset.Properties::oversizedInGui),
								Codec.FLOAT
										.optionalFieldOf("swap_animation_scale", 1.0F)
										.forGetter(ItemAsset.Properties::swapAnimationScale)
						)
						.apply(instance, ItemAsset.Properties::new)
		);
	}
}
