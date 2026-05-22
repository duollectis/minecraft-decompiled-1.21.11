package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.CopperGolemStatueBlock;
import net.minecraft.block.Oxidizable;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.model.CopperGolemStatueModel;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.CopperGolemOxidationLevels;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Специализированный рендерер статуи медного голема как предмета.
 * Поддерживает 4 позы голема (стоя, сидя, звезда, бег) и 4 степени окисления меди.
 * Трансформация матриц переворачивает модель для корректного отображения в инвентаре:
 * смещение в (0.5, 1.5, 0.5) и инверсия осей Y и X.
 */
@Environment(EnvType.CLIENT)
public class CopperGolemStatueModelRenderer implements SimpleSpecialModelRenderer {

	private static final Direction DEFAULT_FACING = Direction.SOUTH;
	private static final float TRANSLATE_X = 0.5F;
	private static final float TRANSLATE_Y = 1.5F;
	private static final float TRANSLATE_Z = 0.5F;
	private static final float SCALE_FLIP = -1.0F;

	private final CopperGolemStatueModel model;
	private final Identifier texture;

	public CopperGolemStatueModelRenderer(CopperGolemStatueModel model, Identifier texture) {
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
			int seed
	) {
		applyItemTransform(matrices);

		queue.submitModel(
				model,
				Direction.SOUTH,
				matrices,
				RenderLayers.entityCutoutNoCull(texture),
				light,
				overlay,
				-1,
				null,
				seed,
				null
		);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrices = new MatrixStack();
		applyItemTransform(matrices);
		model.setAngles(DEFAULT_FACING);
		model.getRootPart().collectVertices(matrices, consumer);
	}

	/**
	 * Применяет стандартную трансформацию для отображения статуи в инвентаре.
	 * Переворачивает модель по осям Y и X, чтобы она смотрела «на игрока».
	 */
	private static void applyItemTransform(MatrixStack matrices) {
		matrices.translate(TRANSLATE_X, TRANSLATE_Y, TRANSLATE_Z);
		matrices.scale(SCALE_FLIP, SCALE_FLIP, 1.0F);
	}

	/**
	 * Несериализованная форма рендерера статуи медного голема.
	 * Хранит текстуру (определяется степенью окисления) и позу голема.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked(
			Identifier texture,
			CopperGolemStatueBlock.Pose pose
	) implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<CopperGolemStatueModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Identifier.CODEC.fieldOf("texture")
								.forGetter(CopperGolemStatueModelRenderer.Unbaked::texture),
						CopperGolemStatueBlock.Pose.CODEC.fieldOf("pose")
								.forGetter(CopperGolemStatueModelRenderer.Unbaked::pose)
				).apply(instance, CopperGolemStatueModelRenderer.Unbaked::new)
		);

		public Unbaked(Oxidizable.OxidationLevel oxidationLevel, CopperGolemStatueBlock.Pose pose) {
			this(CopperGolemOxidationLevels.get(oxidationLevel).texture(), pose);
		}

		@Override
		public MapCodec<CopperGolemStatueModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			CopperGolemStatueModel bakedModel = new CopperGolemStatueModel(
					context.entityModelSet().getModelPart(getLayer(pose))
			);
			return new CopperGolemStatueModelRenderer(bakedModel, texture);
		}

		private static EntityModelLayer getLayer(CopperGolemStatueBlock.Pose pose) {
			return switch (pose) {
				case STANDING -> EntityModelLayers.COPPER_GOLEM;
				case SITTING -> EntityModelLayers.COPPER_GOLEM_SITTING;
				case STAR -> EntityModelLayers.COPPER_GOLEM_STAR;
				case RUNNING -> EntityModelLayers.COPPER_GOLEM_RUNNING;
			};
		}
	}
}
