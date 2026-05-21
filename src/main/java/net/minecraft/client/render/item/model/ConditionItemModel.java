package net.minecraft.client.render.item.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.property.PropertyTester;
import net.minecraft.client.render.item.property.bool.BooleanProperties;
import net.minecraft.client.render.item.property.bool.BooleanProperty;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.DataCache;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.ContextSwapper;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ConditionItemModel}.
 */
public class ConditionItemModel implements ItemModel {

	private final PropertyTester property;
	private final ItemModel onTrue;
	private final ItemModel onFalse;

	public ConditionItemModel(PropertyTester property, ItemModel onTrue, ItemModel onFalse) {
		this.property = property;
		this.onTrue = onTrue;
		this.onFalse = onFalse;
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
		(this.property.test(
				stack,
				world,
				heldItemContext == null ? null : heldItemContext.getEntity(),
				seed,
				displayContext
		) ? this.onTrue : this.onFalse
		)
				.update(state, stack, resolver, displayContext, world, heldItemContext, seed);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked(
			BooleanProperty property,
			ItemModel.Unbaked onTrue,
			ItemModel.Unbaked onFalse
	) implements ItemModel.Unbaked {

		public static final MapCodec<ConditionItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    BooleanProperties.CODEC.forGetter(ConditionItemModel.Unbaked::property),
						                    ItemModelTypes.CODEC.fieldOf("on_true").forGetter(ConditionItemModel.Unbaked::onTrue),
						                    ItemModelTypes.CODEC.fieldOf("on_false").forGetter(ConditionItemModel.Unbaked::onFalse)
				                    )
				                    .apply(instance, ConditionItemModel.Unbaked::new)
		);

		@Override
		public MapCodec<ConditionItemModel.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			return new ConditionItemModel(
					this.makeWorldIndependentProperty(this.property, context.contextSwapper()),
					this.onTrue.bake(context),
					this.onFalse.bake(context)
			);
		}

		private PropertyTester makeWorldIndependentProperty(
				BooleanProperty property,
				@Nullable ContextSwapper contextSwapper
		) {
			if (contextSwapper == null) {
				return property;
			}
			else {
				DataCache<ClientWorld, PropertyTester>
						dataCache =
						new DataCache<>(world -> ConditionItemModel.Unbaked.<BooleanProperty>swapContext(
								property,
								contextSwapper,
								world
						));
				return (stack, world, entity, seed, transformationMode) -> {
					PropertyTester
							propertyTester =
							(PropertyTester) (world == null ? property : dataCache.compute(world));
					return propertyTester.test(stack, world, entity, seed, transformationMode);
				};
			}
		}

		@SuppressWarnings("unchecked")
		private static <T extends BooleanProperty> T swapContext(
				T value,
				ContextSwapper contextSwapper,
				ClientWorld world
		) {
			return (T) contextSwapper
					.swapContext(
							(Codec<Object>) (Codec<?>) value.getCodec().codec(),
							(Object) value,
							world.getRegistryManager()
					)
					.result()
					.orElse(value);
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			this.onTrue.resolve(resolver);
			this.onFalse.resolve(resolver);
		}
	}
}
