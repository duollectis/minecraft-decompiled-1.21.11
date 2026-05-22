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

/**
 * Базовая реализация {@link ItemModel} для стандартных предметов на основе запечённой геометрии
 * ({@link BakedQuad}). Поддерживает тинты, блеск (glint) и анимированные текстуры.
 * <p>
 * Вершины кэшируются через {@link com.google.common.base.Suppliers#memoize} для избежания
 * повторного обхода квадов при каждом кадре.
 */
@Environment(EnvType.CLIENT)
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
			Function<ItemStack, RenderLayer> renderLayerFunction
	) {
		this.tints = tints;
		this.quads = quads;
		this.settings = settings;
		this.renderLayerFunction = renderLayerFunction;
		this.vector = Suppliers.memoize(() -> bakeQuads(this.quads));

		boolean hasAnimation = false;

		for (BakedQuad quad : quads) {
			if (quad.sprite().getContents().isAnimated()) {
				hasAnimation = true;
				break;
			}
		}

		this.animated = hasAnimation;
	}

	/**
	 * Собирает уникальные позиции вершин из всех квадов модели.
	 * Используется для построения AABB предмета без дублирования точек.
	 *
	 * @param quads список запечённых квадов модели
	 * @return массив уникальных позиций вершин
	 */
	public static Vector3fc[] bakeQuads(List<BakedQuad> quads) {
		Set<Vector3fc> positions = new HashSet<>();

		for (BakedQuad quad : quads) {
			for (int corner = 0; corner < 4; corner++) {
				positions.add(quad.getPosition(corner));
			}
		}

		return positions.toArray(Vector3fc[]::new);
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
			ItemRenderState.Glint glint = shouldUseSpecialGlint(stack)
					? ItemRenderState.Glint.SPECIAL
					: ItemRenderState.Glint.STANDARD;
			layer.setGlint(glint);
			state.markAnimated();
			state.addModelKey(glint);
		}

		int tintCount = tints.size();
		int[] tintValues = layer.initTints(tintCount);

		for (int tintIdx = 0; tintIdx < tintCount; tintIdx++) {
			int tint = tints
					.get(tintIdx)
					.getTint(stack, world, heldItemContext == null ? null : heldItemContext.getEntity());
			tintValues[tintIdx] = tint;
			state.addModelKey(tint);
		}

		layer.setVertices(vector);
		layer.setRenderLayer(renderLayerFunction.apply(stack));
		settings.addSettings(layer, displayContext);
		layer.getQuads().addAll(quads);

		if (animated) {
			state.markAnimated();
		}
	}

	/**
	 * Определяет функцию выбора {@link RenderLayer} на основе атласа текстур квадов.
	 * Все квады обязаны принадлежать одному атласу — иначе выбрасывается исключение.
	 *
	 * @param quads список квадов модели
	 * @return функция, возвращающая нужный {@link RenderLayer} для данного {@link ItemStack}
	 * @throws IllegalStateException если квады принадлежат разным атласам
	 * @throws IllegalArgumentException если атлас не поддерживается для предметов
	 */
	static Function<ItemStack, RenderLayer> resolveRenderLayerFunction(List<BakedQuad> quads) {
		Iterator<BakedQuad> iterator = quads.iterator();

		if (!iterator.hasNext()) {
			return ITEMS_RENDER_LAYER_FUNCTION;
		}

		Identifier atlasId = iterator.next().sprite().getAtlasId();

		while (iterator.hasNext()) {
			Identifier nextAtlasId = iterator.next().sprite().getAtlasId();

			if (!nextAtlasId.equals(atlasId)) {
				throw new IllegalStateException(
						"Multiple atlases used in model, expected " + atlasId + ", but also got " + nextAtlasId
				);
			}
		}

		if (atlasId.equals(SpriteAtlasTexture.ITEMS_ATLAS_TEXTURE)) {
			return ITEMS_RENDER_LAYER_FUNCTION;
		}
		else if (atlasId.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)) {
			return BLOCK_RENDER_LAYER_FUNCTION;
		}
		else {
			// Атлас не поддерживается для предметных моделей
			throw new IllegalArgumentException("Atlas " + atlasId + " can't be used for item models");
		}
	}

	private static boolean shouldUseSpecialGlint(ItemStack stack) {
		return stack.isIn(ItemTags.COMPASSES) || stack.isOf(Items.CLOCK);
	}

	/**
	 * Незапечённое описание базовой модели предмета: идентификатор блок-модели и список тинтов.
	 * При запекании разрешает геометрию через {@link Baker} и создаёт {@link BasicItemModel}.
	 */
	@Environment(EnvType.CLIENT)
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
