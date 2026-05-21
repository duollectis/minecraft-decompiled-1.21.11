package net.minecraft.client.render.item.model;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.tint.TintSource;
import net.minecraft.client.render.item.tint.TintSourceTypes;
import net.minecraft.client.render.model.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code BasicItemModel}.
 */
public class BasicItemModel implements ItemModel {

	private static final Function<ItemStack, RenderLayer>
			ITEMS_RENDER_LAYER_FUNCTION =
			itemStack -> TexturedRenderLayers.getItemTranslucentCull();
	private static final Function<ItemStack, RenderLayer> BLOCK_RENDER_LAYER_FUNCTION = itemStack -> {
		if (itemStack.getItem() instanceof BlockItem blockItem) {
			BlockRenderLayer blockRenderLayer = BlockRenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
			if (blockRenderLayer != BlockRenderLayer.TRANSLUCENT) {
				return TexturedRenderLayers.getEntityCutout();
			}
		}

		return TexturedRenderLayers.getBlockTranslucentCull();
	};
	private final List<TintSource> tints;
	private final List<BakedQuad> quads;
	private final Supplier<Vector3fc[]> vector;
	private final ModelSettings settings;
	private final boolean animated;
	private final Function<ItemStack, RenderLayer> renderLayerFunction;

	BasicItemModel(
			List<TintSource> tints,
			List<BakedQuad> quads,
			ModelSettings settings,
			Function<ItemStack, RenderLayer> function
	) {
		this.tints = tints;
		this.quads = quads;
		this.settings = settings;
		this.renderLayerFunction = function;
		this.vector = Suppliers.memoize(() -> bakeQuads(this.quads));
		boolean bl = false;

		for (BakedQuad bakedQuad : quads) {
			if (bakedQuad.sprite().getContents().isAnimated()) {
				bl = true;
				break;
			}
		}

		this.animated = bl;
	}

	/**
	 * Bake quads.
	 *
	 * @param quads quads
	 *
	 * @return Vector3fc[] — результат операции
	 */
	public static Vector3fc[] bakeQuads(List<BakedQuad> quads) {
		Set<Vector3fc> set = new HashSet<>();

		for (BakedQuad bakedQuad : quads) {
			for (int i = 0; i < 4; i++) {
				set.add(bakedQuad.getPosition(i));
			}
		}

		return set.toArray(Vector3fc[]::new);
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
			ItemRenderState.Glint
					glint =
					shouldUseSpecialGlint(stack) ? ItemRenderState.Glint.SPECIAL : ItemRenderState.Glint.STANDARD;
			layerRenderState.setGlint(glint);
			state.markAnimated();
			state.addModelKey(glint);
		}

		int i = this.tints.size();
		int[] is = layerRenderState.initTints(i);

		for (int j = 0; j < i; j++) {
			int
					k =
					this.tints
							.get(j)
							.getTint(stack, world, heldItemContext == null ? null : heldItemContext.getEntity());
			is[j] = k;
			state.addModelKey(k);
		}

		layerRenderState.setVertices(this.vector);
		layerRenderState.setRenderLayer(this.renderLayerFunction.apply(stack));
		this.settings.addSettings(layerRenderState, displayContext);
		layerRenderState.getQuads().addAll(this.quads);
		if (this.animated) {
			state.markAnimated();
		}
	}

	static Function<ItemStack, RenderLayer> resolveRenderLayerFunction(List<BakedQuad> list) {
		Iterator<BakedQuad> iterator = list.iterator();
		if (!iterator.hasNext()) {
			return ITEMS_RENDER_LAYER_FUNCTION;
		}
		else {
			Identifier identifier = iterator.next().sprite().getAtlasId();

			while (iterator.hasNext()) {
				BakedQuad bakedQuad = iterator.next();
				Identifier identifier2 = bakedQuad.sprite().getAtlasId();
				if (!identifier2.equals(identifier)) {
					throw new IllegalStateException(
							"Multiple atlases used in model, expected " + identifier + ", but also got " + identifier2);
				}
			}

			if (identifier.equals(SpriteAtlasTexture.ITEMS_ATLAS_TEXTURE)) {
				return ITEMS_RENDER_LAYER_FUNCTION;
			}
			else if (identifier.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)) {
				return BLOCK_RENDER_LAYER_FUNCTION;
			}
			else {
				throw new IllegalArgumentException("Atlas " + identifier + " can't be usef for item models");
			}
		}
	}

	private static boolean shouldUseSpecialGlint(ItemStack stack) {
		return stack.isIn(ItemTags.COMPASSES) || stack.isOf(Items.CLOCK);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked(Identifier model, List<TintSource> tints) implements ItemModel.Unbaked {

		public static final MapCodec<BasicItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Identifier.CODEC.fieldOf("model").forGetter(BasicItemModel.Unbaked::model),
						                    TintSourceTypes.CODEC
								                    .listOf()
								                    .optionalFieldOf("tints", List.of())
								                    .forGetter(BasicItemModel.Unbaked::tints)
				                    )
				                    .apply(instance, BasicItemModel.Unbaked::new)
		);

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			resolver.markDependency(this.model);
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			Baker baker = context.blockModelBaker();
			BakedSimpleModel bakedSimpleModel = baker.getModel(this.model);
			ModelTextures modelTextures = bakedSimpleModel.getTextures();
			List<BakedQuad>
					list =
					bakedSimpleModel.bakeGeometry(modelTextures, baker, ModelRotation.IDENTITY).getAllQuads();
			ModelSettings modelSettings = ModelSettings.resolveSettings(baker, bakedSimpleModel, modelTextures);
			Function<ItemStack, RenderLayer> function = BasicItemModel.resolveRenderLayerFunction(list);
			return new BasicItemModel(this.tints, list, modelSettings, function);
		}

		@Override
		public MapCodec<BasicItemModel.Unbaked> getCodec() {
			return CODEC;
		}
	}
}
