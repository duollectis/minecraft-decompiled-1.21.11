package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.MapTextureManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Atlases;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
/**
 * {@code MapRenderer}.
 */
public class MapRenderer {

	private static final float MAP_Z_OFFSET = -0.01F;
	private static final float CURSOR_Z_OFFSET = -0.001F;
	public static final int DEFAULT_IMAGE_WIDTH = 128;
	public static final int DEFAULT_IMAGE_HEIGHT = 128;
	private final SpriteAtlasTexture decorationsAtlasManager;
	private final MapTextureManager textureManager;

	public MapRenderer(AtlasManager decorationsAtlasManager, MapTextureManager textureManager) {
		this.decorationsAtlasManager = decorationsAtlasManager.getAtlasTexture(Atlases.MAP_DECORATIONS);
		this.textureManager = textureManager;
	}

	public void draw(
			MapRenderState state,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			boolean renderDecorations,
			int light
	) {
		queue.submitCustom(
				matrices, RenderLayers.text(state.texture), (matrix, vertexConsumer) -> {
					vertexConsumer.vertex(matrix, 0.0F, 128.0F, -0.01F).color(-1).texture(0.0F, 1.0F).light(light);
					vertexConsumer.vertex(matrix, 128.0F, 128.0F, -0.01F).color(-1).texture(1.0F, 1.0F).light(light);
					vertexConsumer.vertex(matrix, 128.0F, 0.0F, -0.01F).color(-1).texture(1.0F, 0.0F).light(light);
					vertexConsumer.vertex(matrix, 0.0F, 0.0F, -0.01F).color(-1).texture(0.0F, 0.0F).light(light);
				}
		);
		int i = 0;

		for (MapRenderState.Decoration decoration : state.decorations) {
			if (!renderDecorations || decoration.alwaysRendered) {
				matrices.push();
				matrices.translate(decoration.x / 2.0F + 64.0F, decoration.z / 2.0F + 64.0F, -0.02F);
				matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(decoration.rotation * 360 / 16.0F));
				matrices.scale(4.0F, 4.0F, 3.0F);
				matrices.translate(-0.125F, 0.125F, 0.0F);
				Sprite sprite = decoration.sprite;
				if (sprite != null) {
					float f = i * -0.001F;
					queue.submitCustom(
							matrices, RenderLayers.text(sprite.getAtlasId()), (matrix, vertexConsumer) -> {
								vertexConsumer
										.vertex(matrix, -1.0F, 1.0F, f)
										.color(-1)
										.texture(sprite.getMinU(), sprite.getMinV())
										.light(light);
								vertexConsumer
										.vertex(matrix, 1.0F, 1.0F, f)
										.color(-1)
										.texture(sprite.getMaxU(), sprite.getMinV())
										.light(light);
								vertexConsumer
										.vertex(matrix, 1.0F, -1.0F, f)
										.color(-1)
										.texture(sprite.getMaxU(), sprite.getMaxV())
										.light(light);
								vertexConsumer
										.vertex(matrix, -1.0F, -1.0F, f)
										.color(-1)
										.texture(sprite.getMinU(), sprite.getMaxV())
										.light(light);
							}
					);
					matrices.pop();
				}

				if (decoration.name != null) {
					TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
					float g = textRenderer.getWidth(decoration.name);
					float h = MathHelper.clamp(25.0F / g, 0.0F, 6.0F / 9.0F);
					matrices.push();
					matrices.translate(
							decoration.x / 2.0F + 64.0F - g * h / 2.0F,
							decoration.z / 2.0F + 64.0F + 4.0F,
							-0.025F
					);
					matrices.scale(h, h, -1.0F);
					matrices.translate(0.0F, 0.0F, 0.1F);
					queue.getBatchingQueue(1)
					     .submitText(
							     matrices,
							     0.0F,
							     0.0F,
							     decoration.name.asOrderedText(),
							     false,
							     TextRenderer.TextLayerType.NORMAL,
							     light,
							     -1,
							     Integer.MIN_VALUE,
							     0
					     );
					matrices.pop();
				}

				i++;
			}
		}
	}

	/**
	 * Update.
	 *
	 * @param mapId map id
	 * @param mapState map state
	 * @param renderState render state
	 */
	public void update(MapIdComponent mapId, MapState mapState, MapRenderState renderState) {
		renderState.texture = this.textureManager.getTextureId(mapId, mapState);
		renderState.decorations.clear();

		for (MapDecoration mapDecoration : mapState.getDecorations()) {
			renderState.decorations.add(this.createDecoration(mapDecoration));
		}
	}

	private MapRenderState.Decoration createDecoration(MapDecoration decoration) {
		MapRenderState.Decoration decoration2 = new MapRenderState.Decoration();
		decoration2.sprite = this.decorationsAtlasManager.getSprite(decoration.getAssetId());
		decoration2.x = decoration.x();
		decoration2.z = decoration.z();
		decoration2.rotation = decoration.rotation();
		decoration2.name = decoration.name().orElse(null);
		decoration2.alwaysRendered = decoration.isAlwaysRendered();
		return decoration2;
	}
}
