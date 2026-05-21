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

@Environment(EnvType.CLIENT)
/**
 * {@code SelectItemModel}.
 */
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
		T
				object =
				this.property.getValue(
						stack,
						world,
						heldItemContext == null ? null : heldItemContext.getEntity(),
						seed,
						displayContext
				);
		ItemModel itemModel = this.selector.get(object, world);
		if (itemModel != null) {
			itemModel.update(state, stack, resolver, displayContext, world, heldItemContext, seed);
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code ModelSelector}.
	 */
	public interface ModelSelector<T> {

		@Nullable ItemModel get(@Nullable T propertyValue, @Nullable ClientWorld world);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SwitchCase}.
	 */
	public record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {

		public static <T> Codec<SelectItemModel.SwitchCase<T>> createCodec(Codec<T> conditionCodec) {
			return RecordCodecBuilder.create(
					instance -> instance.group(
							                    Codecs
									                    .nonEmptyList(Codecs.listOrSingle(conditionCodec))
									                    .fieldOf("when")
									                    .forGetter(SelectItemModel.SwitchCase::values),
							                    ItemModelTypes.CODEC.fieldOf("model").forGetter(SelectItemModel.SwitchCase::model)
					                    )
					                    .apply(instance, SelectItemModel.SwitchCase::new)
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked(
			SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch,
			Optional<ItemModel.Unbaked> fallback
	) implements ItemModel.Unbaked {

		public static final MapCodec<SelectItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    SelectItemModel.UnbakedSwitch.CODEC.forGetter(SelectItemModel.Unbaked::unbakedSwitch),
						                    ItemModelTypes.CODEC.optionalFieldOf("fallback").forGetter(SelectItemModel.Unbaked::fallback)
				                    )
				                    .apply(instance, SelectItemModel.Unbaked::new)
		);

		@Override
		public MapCodec<SelectItemModel.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			ItemModel
					itemModel =
					this.fallback.<ItemModel>map(model -> model.bake(context)).orElse(context.missingItemModel());
			return this.unbakedSwitch.bake(context, itemModel);
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			this.unbakedSwitch.resolveCases(resolver);
			this.fallback.ifPresent(model -> model.resolve(resolver));
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code UnbakedSwitch}.
	 */
	public record UnbakedSwitch<P extends SelectProperty<T>, T>(P property, List<SelectItemModel.SwitchCase<T>> cases) {

		public static final MapCodec<SelectItemModel.UnbakedSwitch<?, ?>> CODEC = SelectProperties.CODEC
				.dispatchMap(
						"property",
						unbakedSwitch -> unbakedSwitch.property().getType(),
						SelectProperty.Type::switchCodec
				);

		public ItemModel bake(ItemModel.BakeContext context, ItemModel fallback) {
			Object2ObjectMap<T, ItemModel> object2ObjectMap = new Object2ObjectOpenHashMap();

			for (SelectItemModel.SwitchCase<T> switchCase : this.cases) {
				ItemModel.Unbaked unbaked = switchCase.model;
				ItemModel itemModel = unbaked.bake(context);

				for (T object : switchCase.values) {
					object2ObjectMap.put(object, itemModel);
				}
			}

			object2ObjectMap.defaultReturnValue(fallback);
			return new SelectItemModel<>(
					this.property,
					this.buildModelSelector(object2ObjectMap, context.contextSwapper())
			);
		}

		private SelectItemModel.ModelSelector<T> buildModelSelector(
				Object2ObjectMap<T, ItemModel> models,
				@Nullable ContextSwapper contextSwapper
		) {
			if (contextSwapper == null) {
				return (value, world) -> (ItemModel) models.get(value);
			}
			else {
				ItemModel itemModel = (ItemModel) models.defaultReturnValue();
				DataCache<ClientWorld, Object2ObjectMap<T, ItemModel>> dataCache = new DataCache<>(
						world -> {
							Object2ObjectMap<T, ItemModel>
									object2ObjectMap2 =
									new Object2ObjectOpenHashMap(models.size());
							object2ObjectMap2.defaultReturnValue(itemModel);
							models.forEach(
									(value, worldx) -> contextSwapper
											.swapContext(this.property.valueCodec(), value, world.getRegistryManager())
											.ifSuccess(swappedValue -> object2ObjectMap2.put(swappedValue, worldx))
							);
							return object2ObjectMap2;
						}
				);
				return (value, world) -> {
					if (world == null) {
						return (ItemModel) models.get(value);
					}
					else {
						return value == null ? itemModel : (ItemModel) dataCache.compute(world).get(value);
					}
				};
			}
		}

		public void resolveCases(ResolvableModel.Resolver resolver) {
			for (SelectItemModel.SwitchCase<?> switchCase : this.cases) {
				switchCase.model.resolve(resolver);
			}
		}
	}
}
