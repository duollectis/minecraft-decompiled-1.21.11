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

/**
 * Рендерер предмета-головы (черепа) — делегирует отрисовку {@link SkullBlockEntityRenderer}.
 * Поддерживает анимацию (например, вращение нижней челюсти у дракона) через параметр {@code animation}.
 */
@Environment(EnvType.CLIENT)
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
			int seed
	) {
		SkullBlockEntityRenderer.render(
				null,
				180.0F,
				animation,
				matrices,
				queue,
				light,
				model,
				renderLayer,
				seed,
				null
		);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrices = new MatrixStack();
		matrices.translate(0.5F, 0.0F, 0.5F);
		matrices.scale(-1.0F, -1.0F, 1.0F);

		SkullBlockEntityModel.SkullModelState skullState = new SkullBlockEntityModel.SkullModelState();
		skullState.poweredTicks = animation;
		skullState.yaw = 180.0F;
		model.setAngles(skullState);
		model.getRootPart().collectVertices(matrices, consumer);
	}

	/**
	 * Несериализованный дескриптор рендерера головы.
	 * Хранит тип черепа, опциональный путь к текстуре и значение анимации (например, угол открытия челюсти).
	 */
	@Environment(EnvType.CLIENT)
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
			SkullBlockEntityModel skullModel = SkullBlockEntityRenderer.getModels(context.entityModelSet(), kind);

			if (skullModel == null) {
				return null;
			}

			Identifier textureId = textureOverride
					.<Identifier>map(id -> id.withPath(texture -> "textures/entity/" + texture + ".png"))
					.orElse(null);
			RenderLayer layer = SkullBlockEntityRenderer.getCutoutRenderLayer(kind, textureId);

			return new HeadModelRenderer(skullModel, animation, layer);
		}
	}
}
