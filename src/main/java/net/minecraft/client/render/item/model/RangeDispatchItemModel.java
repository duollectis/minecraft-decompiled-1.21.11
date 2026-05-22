package net.minecraft.client.render.item.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.property.numeric.NumericProperties;
import net.minecraft.client.render.item.property.numeric.NumericProperty;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Модель предмета с диспетчеризацией по числовому диапазону.
 * Вычисляет числовое свойство предмета, умножает на масштаб и выбирает
 * подходящую дочернюю модель по пороговым значениям.
 * При малом числе порогов используется линейный поиск, при большом — бинарный.
 */
@Environment(EnvType.CLIENT)
public class RangeDispatchItemModel implements ItemModel {

	private static final int BINARY_SEARCH_THRESHOLD = 16;

	private final NumericProperty property;
	private final float scale;
	private final float[] thresholds;
	private final ItemModel[] models;
	private final ItemModel fallback;

	RangeDispatchItemModel(
			NumericProperty property,
			float scale,
			float[] thresholds,
			ItemModel[] models,
			ItemModel fallback
	) {
		this.property = property;
		this.thresholds = thresholds;
		this.models = models;
		this.fallback = fallback;
		this.scale = scale;
	}

	/**
	 * Находит индекс модели для заданного значения по массиву порогов.
	 * Возвращает {@code -1}, если значение меньше первого порога.
	 *
	 * @param thresholds отсортированный массив пороговых значений
	 * @param value      вычисленное числовое значение свойства
	 * @return индекс подходящей модели или {@code -1} для fallback
	 */
	private static int getIndex(float[] thresholds, float value) {
		if (thresholds.length < BINARY_SEARCH_THRESHOLD) {
			for (int index = 0; index < thresholds.length; index++) {
				if (thresholds[index] > value) {
					return index - 1;
				}
			}

			return thresholds.length - 1;
		}

		int searchResult = Arrays.binarySearch(thresholds, value);

		return searchResult < 0
				? ~searchResult - 1
				: searchResult;
	}

	@Override
	public void update(
			ItemRenderState state,
			ItemStack stack,
			ItemModelManager resolver,
			ItemDisplayContext displayContext,
			@Nullable ClientWorld world,
			@Nullable HeldItemContext heldItemContext,
			int seed
	) {
		state.addModelKey(this);

		float scaledValue = property.getValue(stack, world, heldItemContext, seed) * scale;
		ItemModel selected;

		if (Float.isNaN(scaledValue)) {
			selected = fallback;
		} else {
			int index = getIndex(thresholds, scaledValue);
			selected = index == -1 ? fallback : models[index];
		}

		selected.update(state, stack, resolver, displayContext, world, heldItemContext, seed);
	}

	/**
	 * Запись, связывающая пороговое значение с несериализованной моделью.
	 */
	@Environment(EnvType.CLIENT)
	public record Entry(float threshold, ItemModel.Unbaked model) {

		public static final Codec<RangeDispatchItemModel.Entry> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.FLOAT.fieldOf("threshold").forGetter(RangeDispatchItemModel.Entry::threshold),
						ItemModelTypes.CODEC.fieldOf("model").forGetter(RangeDispatchItemModel.Entry::model)
				).apply(instance, RangeDispatchItemModel.Entry::new)
		);

		public static final Comparator<RangeDispatchItemModel.Entry> COMPARATOR =
				Comparator.comparingDouble(RangeDispatchItemModel.Entry::threshold);
	}

	/**
	 * Несериализованная форма модели с диспетчеризацией по диапазону.
	 * При запекании сортирует записи по порогу и строит параллельные массивы
	 * порогов и моделей для эффективного поиска во время рендера.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked(
			NumericProperty property,
			float scale,
			List<RangeDispatchItemModel.Entry> entries,
			Optional<ItemModel.Unbaked> fallback
	) implements ItemModel.Unbaked {

		public static final MapCodec<RangeDispatchItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						NumericProperties.CODEC.forGetter(RangeDispatchItemModel.Unbaked::property),
						Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(RangeDispatchItemModel.Unbaked::scale),
						RangeDispatchItemModel.Entry.CODEC
								.listOf()
								.fieldOf("entries")
								.forGetter(RangeDispatchItemModel.Unbaked::entries),
						ItemModelTypes.CODEC
								.optionalFieldOf("fallback")
								.forGetter(RangeDispatchItemModel.Unbaked::fallback)
				).apply(instance, RangeDispatchItemModel.Unbaked::new)
		);

		@Override
		public MapCodec<RangeDispatchItemModel.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			List<RangeDispatchItemModel.Entry> sorted = new ArrayList<>(entries);
			sorted.sort(RangeDispatchItemModel.Entry.COMPARATOR);

			float[] thresholds = new float[sorted.size()];
			ItemModel[] bakedModels = new ItemModel[sorted.size()];

			for (int index = 0; index < sorted.size(); index++) {
				RangeDispatchItemModel.Entry entry = sorted.get(index);
				thresholds[index] = entry.threshold();
				bakedModels[index] = entry.model().bake(context);
			}

			ItemModel bakedFallback = fallback
					.<ItemModel>map(model -> model.bake(context))
					.orElse(context.missingItemModel());

			return new RangeDispatchItemModel(property, scale, thresholds, bakedModels, bakedFallback);
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			fallback.ifPresent(model -> model.resolve(resolver));
			entries.forEach(entry -> entry.model().resolve(resolver));
		}
	}
}
