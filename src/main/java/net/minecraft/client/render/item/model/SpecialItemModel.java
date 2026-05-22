package net.minecraft.client.render.item.model;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.client.render.model.BakedSimpleModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelSettings;
import net.minecraft.client.render.model.ModelTextures;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Модель предмета, делегирующая рендер специализированному {@link SpecialModelRenderer}.
 * Используется для предметов с нетривиальной геометрией (щиты, головы, сундуки и т.д.),
 * которые не могут быть представлены стандартной блочной моделью.
 * Вершины модели кэшируются лениво через {@link Suppliers#memoize} для оптимизации.
 *
 * @param <T> тип данных, извлекаемых из стека предмета для рендера
 */
@Environment(EnvType.CLIENT)
public class SpecialItemModel<T> implements ItemModel {

	private final SpecialModelRenderer<T> specialModelType;
	private final ModelSettings settings;
	private final Supplier<Vector3fc[]> verticesSupplier;

	public SpecialItemModel(SpecialModelRenderer<T> specialModelType, ModelSettings settings) {
		this.specialModelType = specialModelType;
		this.settings = settings;
		this.verticesSupplier = Suppliers.memoize(() -> {
			Set<Vector3fc> vertices = new HashSet<>();
			specialModelType.collectVertices(vertices::add);
			return vertices.toArray(new Vector3fc[0]);
		});
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

		ItemRenderState.LayerRenderState layer = state.newLayer();

		if (stack.hasGlint()) {
			ItemRenderState.Glint glint = ItemRenderState.Glint.STANDARD;
			layer.setGlint(glint);
			state.markAnimated();
			state.addModelKey(glint);
		}

		T data = specialModelType.getData(stack);
		layer.setVertices(verticesSupplier);
		layer.setSpecialModel(specialModelType, data);

		if (data != null) {
			state.addModelKey(data);
		}

		settings.addSettings(layer, displayContext);
	}

	/**
	 * Несериализованная форма специальной модели предмета.
	 * Содержит идентификатор базовой блочной модели (для настроек трансформаций)
	 * и несериализованный специализированный рендерер.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked(Identifier base, SpecialModelRenderer.Unbaked specialModel) implements ItemModel.Unbaked {

		public static final MapCodec<SpecialItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Identifier.CODEC.fieldOf("base").forGetter(SpecialItemModel.Unbaked::base),
						SpecialModelTypes.CODEC.fieldOf("model").forGetter(SpecialItemModel.Unbaked::specialModel)
				).apply(instance, SpecialItemModel.Unbaked::new)
		);

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			resolver.markDependency(base);
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			SpecialModelRenderer<?> renderer = specialModel.bake(context);

			if (renderer == null) {
				return context.missingItemModel();
			}

			ModelSettings modelSettings = resolveSettings(context);
			return new SpecialItemModel<>(renderer, modelSettings);
		}

		private ModelSettings resolveSettings(ItemModel.BakeContext context) {
			Baker baker = context.blockModelBaker();
			BakedSimpleModel bakedModel = baker.getModel(base);
			ModelTextures textures = bakedModel.getTextures();
			return ModelSettings.resolveSettings(baker, bakedModel, textures);
		}

		@Override
		public MapCodec<SpecialItemModel.Unbaked> getCodec() {
			return CODEC;
		}
	}
}
