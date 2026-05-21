package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.WoodType;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;

import java.util.Optional;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code SignModelRenderer}.
 */
public class SignModelRenderer implements SimpleSpecialModelRenderer {

	private final SpriteHolder spriteHolder;
	private final Model.SinglePartModel model;
	private final SpriteIdentifier texture;

	public SignModelRenderer(SpriteHolder spriteHolder, Model.SinglePartModel model, SpriteIdentifier texture) {
		this.spriteHolder = spriteHolder;
		this.model = model;
		this.texture = texture;
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
		SignBlockEntityRenderer.renderAsItem(
				this.spriteHolder,
				matrices,
				queue,
				light,
				overlay,
				this.model,
				this.texture
		);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrixStack = new MatrixStack();
		SignBlockEntityRenderer.setTransformsForItem(matrixStack);
		this.model.getRootPart().collectVertices(matrixStack, consumer);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked(WoodType woodType, Optional<Identifier> texture) implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<SignModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    WoodType.CODEC.fieldOf("wood_type").forGetter(SignModelRenderer.Unbaked::woodType),
						                    Identifier.CODEC.optionalFieldOf("texture").forGetter(SignModelRenderer.Unbaked::texture)
				                    )
				                    .apply(instance, SignModelRenderer.Unbaked::new)
		);

		public Unbaked(WoodType woodType) {
			this(woodType, Optional.empty());
		}

		@Override
		public MapCodec<SignModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			Model.SinglePartModel
					singlePartModel =
					SignBlockEntityRenderer.createSignModel(context.entityModelSet(), this.woodType, true);
			SpriteIdentifier spriteIdentifier = this.texture
					.map(TexturedRenderLayers.SIGN_SPRITE_MAPPER::map)
					.orElseGet(() -> TexturedRenderLayers.getSignTextureId(this.woodType));
			return new SignModelRenderer(context.spriteHolder(), singlePartModel, spriteIdentifier);
		}
	}
}
