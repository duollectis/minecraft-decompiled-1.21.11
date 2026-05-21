package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code HeadModelRenderer}.
 */
public class HeadModelRenderer implements SimpleSpecialModelRenderer {

	private final SkullBlockEntityModel model;
	private final float animation;
	private final RenderLayer renderLayer;

	public HeadModelRenderer(SkullBlockEntityModel model, float animation, RenderLayer renderLayer) {
		this.model = model;
		this.animation = animation;
		this.renderLayer = renderLayer;
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
		SkullBlockEntityRenderer.render(
				null,
				180.0F,
				this.animation,
				matrices,
				queue,
				light,
				this.model,
				this.renderLayer,
				i,
				null
		);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrixStack = new MatrixStack();
		matrixStack.translate(0.5F, 0.0F, 0.5F);
		matrixStack.scale(-1.0F, -1.0F, 1.0F);
		SkullBlockEntityModel.SkullModelState skullModelState = new SkullBlockEntityModel.SkullModelState();
		skullModelState.poweredTicks = this.animation;
		skullModelState.yaw = 180.0F;
		this.model.setAngles(skullModelState);
		this.model.getRootPart().collectVertices(matrixStack, consumer);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked(
			SkullBlock.SkullType kind,
			Optional<Identifier> textureOverride,
			float animation
	) implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<HeadModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    SkullBlock.SkullType.CODEC.fieldOf("kind").forGetter(HeadModelRenderer.Unbaked::kind),
						                    Identifier.CODEC
								                    .optionalFieldOf("texture")
								                    .forGetter(HeadModelRenderer.Unbaked::textureOverride),
						                    Codec.FLOAT.optionalFieldOf("animation", 0.0F).forGetter(HeadModelRenderer.Unbaked::animation)
				                    )
				                    .apply(instance, HeadModelRenderer.Unbaked::new)
		);

		public Unbaked(SkullBlock.SkullType kind) {
			this(kind, Optional.empty(), 0.0F);
		}

		@Override
		public MapCodec<HeadModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public @Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			SkullBlockEntityModel
					skullBlockEntityModel =
					SkullBlockEntityRenderer.getModels(context.entityModelSet(), this.kind);
			Identifier
					identifier =
					this.textureOverride
							.<Identifier>map(id -> id.withPath(texture -> "textures/entity/" + texture + ".png"))
							.orElse(null);
			if (skullBlockEntityModel == null) {
				return null;
			}
			else {
				RenderLayer renderLayer = SkullBlockEntityRenderer.getCutoutRenderLayer(this.kind, identifier);
				return new HeadModelRenderer(skullBlockEntityModel, this.animation, renderLayer);
			}
		}
	}
}
