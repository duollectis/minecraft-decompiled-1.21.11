package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Составная модель предмета, объединяющая несколько дочерних {@link ItemModel} в один
 * визуальный результат. Каждая дочерняя модель добавляет свои слои в {@link ItemRenderState}.
 * <p>
 * Используется для предметов с несколькими независимыми визуальными компонентами,
 * например, предмет с основной геометрией и отдельным слоем блеска.
 */
@Environment(EnvType.CLIENT)
public class CompositeItemModel implements ItemModel {

	private final List<ItemModel> models;

	public CompositeItemModel(List<ItemModel> models) {
		this.models = models;
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
		state.addLayers(models.size());

		for (ItemModel model : models) {
			model.update(state, stack, resolver, displayContext, world, heldItemContext, seed);
		}
	}

	/** Незапечённый дескриптор составной модели: список дочерних незапечённых моделей. */
	@Environment(EnvType.CLIENT)
	public record Unbaked(List<ItemModel.Unbaked> models) implements ItemModel.Unbaked {

		public static final MapCodec<CompositeItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(ItemModelTypes.CODEC
								.listOf()
								.fieldOf("models")
								.forGetter(CompositeItemModel.Unbaked::models))
						.apply(instance, CompositeItemModel.Unbaked::new)
		);

		@Override
		public MapCodec<CompositeItemModel.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			for (ItemModel.Unbaked unbaked : this.models) {
				unbaked.resolve(resolver);
			}
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			return new CompositeItemModel(this.models.stream().map(model -> model.bake(context)).toList());
		}
	}
}
