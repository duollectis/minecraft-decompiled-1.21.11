package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.model.ChestBlockModel;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code ChestModelRenderer}.
 */
public class ChestModelRenderer implements SimpleSpecialModelRenderer {

	public static final Identifier CHRISTMAS_ID = Identifier.ofVanilla("christmas");
	public static final Identifier NORMAL_ID = Identifier.ofVanilla("normal");
	public static final Identifier TRAPPED_ID = Identifier.ofVanilla("trapped");
	public static final Identifier ENDER_ID = Identifier.ofVanilla("ender");
	public static final Identifier COPPER_ID = Identifier.ofVanilla("copper");
	public static final Identifier EXPOSED_COPPER_ID = Identifier.ofVanilla("copper_exposed");
	public static final Identifier WEATHERED_COPPER_ID = Identifier.ofVanilla("copper_weathered");
	public static final Identifier OXIDIZED_COPPER_ID = Identifier.ofVanilla("copper_oxidized");
	private final SpriteHolder spriteHolder;
	private final ChestBlockModel model;
	private final SpriteIdentifier textureId;
	private final float openness;

	public ChestModelRenderer(
			SpriteHolder spriteHolder,
			ChestBlockModel model,
			SpriteIdentifier textureId,
			float openness
	) {
		this.spriteHolder = spriteHolder;
		this.model = model;
		this.textureId = textureId;
		this.openness = openness;
	}

	@Override
	public void render(
			ItemDisplayContext displayContext,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			boolean glint,
			int i
	) {
		queue.submitModel(
				this.model,
				this.openness,
				matrices,
				this.textureId.getRenderLayer(RenderLayers::entitySolid),
				light,
				overlay,
				-1,
				this.spriteHolder.getSprite(this.textureId),
				i,
				null
		);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrixStack = new MatrixStack();
		this.model.setAngles(this.openness);
		this.model.getRootPart().collectVertices(matrixStack, consumer);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked(Identifier texture, float openness) implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<ChestModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Identifier.CODEC.fieldOf("texture").forGetter(ChestModelRenderer.Unbaked::texture),
						                    Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(ChestModelRenderer.Unbaked::openness)
				                    )
				                    .apply(instance, ChestModelRenderer.Unbaked::new)
		);

		public Unbaked(Identifier texture) {
			this(texture, 0.0F);
		}

		@Override
		public MapCodec<ChestModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			ChestBlockModel
					chestBlockModel =
					new ChestBlockModel(context.entityModelSet().getModelPart(EntityModelLayers.CHEST));
			SpriteIdentifier spriteIdentifier = TexturedRenderLayers.CHEST_SPRITE_MAPPER.map(this.texture);
			return new ChestModelRenderer(context.spriteHolder(), chestBlockModel, spriteIdentifier, this.openness);
		}
	}
}
