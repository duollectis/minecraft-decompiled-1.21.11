package net.minecraft.client.render.item.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.property.select.SelectProperties;
import net.minecraft.client.render.item.property.select.SelectProperty;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.DataCache;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.ContextSwapper;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Модель предмета с диспетчеризацией по перечислимому свойству (switch-логика).
 * Вычисляет значение {@link SelectProperty} и выбирает соответствующую дочернюю модель
 * из таблицы соответствий. Поддерживает контекстную замену значений через {@link ContextSwapper}
 * для корректной работы в разных реестровых контекстах.
 *
 * @param <T> тип значения свойства выбора
 */
@Environment(EnvType.CLIENT)
public class SelectItemModel<T> implements ItemModel {

	private final SelectProperty<T> property;
	private final SelectItemModel.ModelSelector<T> selector;

	public SelectItemModel(SelectProperty<T> property, SelectItemModel.ModelSelector<T> selector) {
		this.property = property;
		this.selector = selector;
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

		T value = property.getValue(
				stack,
				world,
				heldItemContext == null ? null : heldItemContext.getEntity(),
				seed,
				displayContext
		);

		ItemModel selected = selector.get(value, world);

		if (selected != null) {
			selected.update(state, stack, resolver, displayContext, world, heldItemContext, seed);
		}
	}

	/**
	 * Функциональный интерфейс для выбора модели по значению свойства и миру.
	 *
	 * @param <T> тип значения свойства
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface ModelSelector<T> {

		@Nullable ItemModel get(@Nullable T propertyValue, @Nullable ClientWorld world);
	}

	/**
	 * Один вариант switch-выражения: список значений, при которых активируется данная модель.
	 *
	 * @param <T> тип значения свойства
	 */
	@Environment(EnvType.CLIENT)
	public record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {

		public static <T> Codec<SelectItemModel.SwitchCase<T>> createCodec(Codec<T> conditionCodec) {
			return RecordCodecBuilder.create(
					instance -> instance.group(
							Codecs.nonEmptyList(Codecs.listOrSingle(conditionCodec))
									.fieldOf("when")
									.forGetter(SelectItemModel.SwitchCase::values),
							ItemModelTypes.CODEC.fieldOf("model").forGetter(SelectItemModel.SwitchCase::model)
					).apply(instance, SelectItemModel.SwitchCase::new)
			);
		}
	}

	/**
	 * Несериализованная форма модели с выбором по свойству.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked(
			SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch,
			Optional<ItemModel.Unbaked> fallback
	) implements ItemModel.Unbaked {

		public static final MapCodec<SelectItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						SelectItemModel.UnbakedSwitch.CODEC.forGetter(SelectItemModel.Unbaked::unbakedSwitch),
						ItemModelTypes.CODEC.optionalFieldOf("fallback").forGetter(SelectItemModel.Unbaked::fallback)
				).apply(instance, SelectItemModel.Unbaked::new)
		);

		@Override
		public MapCodec<SelectItemModel.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			ItemModel bakedFallback = fallback
					.<ItemModel>map(model -> model.bake(context))
					.orElse(context.missingItemModel());

			return unbakedSwitch.bake(context, bakedFallback);
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			unbakedSwitch.resolveCases(resolver);
			fallback.ifPresent(model -> model.resolve(resolver));
		}
	}

	/**
	 * Несериализованный switch-блок, связывающий свойство с набором вариантов.
	 * При запекании строит карту значений → моделей и при наличии {@link ContextSwapper}
	 * создаёт кэшированный селектор с поддержкой замены контекста реестра.
	 *
	 * @param <P> тип свойства выбора
	 * @param <T> тип значения свойства
	 */
	@Environment(EnvType.CLIENT)
	public record UnbakedSwitch<P extends SelectProperty<T>, T>(
			P property,
			List<SelectItemModel.SwitchCase<T>> cases
	) {

		public static final MapCodec<SelectItemModel.UnbakedSwitch<?, ?>> CODEC = SelectProperties.CODEC
				.dispatchMap(
						"property",
						unbakedSwitch -> unbakedSwitch.property().getType(),
						SelectProperty.Type::switchCodec
				);

		/**
		 * Запекает switch-блок в готовую {@link SelectItemModel}.
		 * Строит карту значений → запечённых моделей. Если задан {@link ContextSwapper},
		 * создаёт кэшированный по миру селектор с поддержкой замены контекста реестра.
		 *
		 * @param context  контекст запекания
		 * @param fallback модель-заглушка для неизвестных значений
		 * @return готовая модель с диспетчеризацией по значению свойства
		 */
		public ItemModel bake(ItemModel.BakeContext context, ItemModel fallback) {
			Object2ObjectMap<T, ItemModel> modelMap = new Object2ObjectOpenHashMap<>();

			for (SelectItemModel.SwitchCase<T> switchCase : cases) {
				ItemModel bakedModel = switchCase.model().bake(context);

				for (T value : switchCase.values()) {
					modelMap.put(value, bakedModel);
				}
			}

			modelMap.defaultReturnValue(fallback);

			return new SelectItemModel<>(property, buildModelSelector(modelMap, context.contextSwapper()));
		}

		private SelectItemModel.ModelSelector<T> buildModelSelector(
				Object2ObjectMap<T, ItemModel> models,
				@Nullable ContextSwapper contextSwapper
		) {
			if (contextSwapper == null) {
				return (value, world) -> models.get(value);
			}

			ItemModel defaultModel = models.defaultReturnValue();
			DataCache<ClientWorld, Object2ObjectMap<T, ItemModel>> dataCache = new DataCache<>(
					world -> {
						Object2ObjectMap<T, ItemModel> swapped = new Object2ObjectOpenHashMap<>(models.size());
						swapped.defaultReturnValue(defaultModel);
						models.forEach(
								(value, model) -> contextSwapper
										.swapContext(property.valueCodec(), value, world.getRegistryManager())
										.ifSuccess(swappedValue -> swapped.put(swappedValue, model))
						);
						return swapped;
					}
			);

			return (value, world) -> {
				if (world == null) {
					return models.get(value);
				}

				return value == null ? defaultModel : dataCache.compute(world).get(value);
			};
		}

		/**
		 * Разрешает зависимости всех вариантов switch-блока.
		 *
		 * @param resolver резолвер зависимостей моделей
		 */
		public void resolveCases(ResolvableModel.Resolver resolver) {
			for (SelectItemModel.SwitchCase<?> switchCase : cases) {
				switchCase.model().resolve(resolver);
			}
		}
	}
}
