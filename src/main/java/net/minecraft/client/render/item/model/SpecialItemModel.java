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
import net.minecraft.client.render.model.*;
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

@Environment(EnvType.CLIENT)
/**
 * {@code SpecialItemModel}.
 */
public class SpecialItemModel<T> implements ItemModel {

	private final SpecialModelRenderer<T> specialModelType;
	private final ModelSettings settings;
	private final Supplier<Vector3fc[]> verticesSupplier;

	public SpecialItemModel(SpecialModelRenderer<T> specialModelType, ModelSettings settings) {
		this.specialModelType = specialModelType;
		this.settings = settings;
		this.verticesSupplier = Suppliers.memoize(() -> {
			Set<Vector3fc> set = new HashSet<>();
			specialModelType.collectVertices(set::add);
			return set.toArray(new Vector3fc[0]);
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
		ItemRenderState.LayerRenderState layerRenderState = state.newLayer();
		if (stack.hasGlint()) {
			ItemRenderState.Glint glint = ItemRenderState.Glint.STANDARD;
			layerRenderState.setGlint(glint);
			state.markAnimated();
			state.addModelKey(glint);
		}

		T object = this.specialModelType.getData(stack);
		layerRenderState.setVertices(this.verticesSupplier);
		layerRenderState.setSpecialModel(this.specialModelType, object);
		if (object != null) {
			state.addModelKey(object);
		}

		this.settings.addSettings(layerRenderState, displayContext);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked(Identifier base, SpecialModelRenderer.Unbaked specialModel) implements ItemModel.Unbaked {

		public static final MapCodec<SpecialItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Identifier.CODEC.fieldOf("base").forGetter(SpecialItemModel.Unbaked::base),
						                    SpecialModelTypes.CODEC.fieldOf("model").forGetter(SpecialItemModel.Unbaked::specialModel)
				                    )
				                    .apply(instance, SpecialItemModel.Unbaked::new)
		);

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			resolver.markDependency(this.base);
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			SpecialModelRenderer<?> specialModelRenderer = this.specialModel.bake(context);
			if (specialModelRenderer == null) {
				return context.missingItemModel();
			}
			else {
				ModelSettings modelSettings = this.getSettings(context);
				return new SpecialItemModel<>(specialModelRenderer, modelSettings);
			}
		}

		private ModelSettings getSettings(ItemModel.BakeContext context) {
			Baker baker = context.blockModelBaker();
			BakedSimpleModel bakedSimpleModel = baker.getModel(this.base);
			ModelTextures modelTextures = bakedSimpleModel.getTextures();
			return ModelSettings.resolveSettings(baker, bakedSimpleModel, modelTextures);
		}

		@Override
		public MapCodec<SpecialItemModel.Unbaked> getCodec() {
			return CODEC;
		}
	}
}
