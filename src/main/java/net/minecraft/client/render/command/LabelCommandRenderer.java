package net.minecraft.client.render.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Рендерит текстовые метки над сущностями (имена игроков, кастомные имена).
 * Метки делятся на два типа: обычные (перекрываются геометрией) и сквозные (видны сквозь блоки).
 * Сквозные метки сортируются по убыванию расстояния до камеры для корректного альфа-блендинга.
 */
@Environment(EnvType.CLIENT)
public class LabelCommandRenderer {

	public void render(
			BatchingRenderCommandQueue queue,
			VertexConsumerProvider.Immediate vertexConsumers,
			TextRenderer renderer
	) {
		LabelCommandRenderer.Commands commands = queue.getLabelCommands();
		commands.seethroughLabels.sort(Comparator
				.comparing(OrderedRenderCommandQueueImpl.LabelCommand::distanceToCameraSq)
				.reversed());

		for (OrderedRenderCommandQueueImpl.LabelCommand labelCommand : commands.seethroughLabels) {
			renderer.draw(
					labelCommand.text(),
					labelCommand.x(),
					labelCommand.y(),
					labelCommand.color(),
					false,
					labelCommand.matricesEntry(),
					vertexConsumers,
					TextRenderer.TextLayerType.SEE_THROUGH,
					labelCommand.backgroundColor(),
					labelCommand.lightCoords()
			);
		}

		for (OrderedRenderCommandQueueImpl.LabelCommand labelCommand : commands.normalLabels) {
			renderer.draw(
					labelCommand.text(),
					labelCommand.x(),
					labelCommand.y(),
					labelCommand.color(),
					false,
					labelCommand.matricesEntry(),
					vertexConsumers,
					TextRenderer.TextLayerType.NORMAL,
					labelCommand.backgroundColor(),
					labelCommand.lightCoords()
			);
		}
	}

	/** Накопитель команд рисования меток, разделённых на обычные и сквозные. */
	@Environment(EnvType.CLIENT)
	public static class Commands {

		// 0x7F000000 — полупрозрачный чёрный фон для сквозных меток (alpha=127)
		private static final int SEETHROUGH_COLOR = -2130706433;
		// Масштаб текста в мировом пространстве (1/40 блока)
		private static final float LABEL_SCALE = 0.025F;
		// Смещение метки по Y над позицией сущности
		private static final double LABEL_Y_OFFSET = 0.5;
		// Уровень эмиссии для нормальных меток (не крадущихся)
		private static final int NORMAL_LABEL_EMISSION = 2;

		final List<OrderedRenderCommandQueueImpl.LabelCommand> seethroughLabels = new ArrayList<>();
		final List<OrderedRenderCommandQueueImpl.LabelCommand> normalLabels = new ArrayList<>();

		public void add(
				MatrixStack matrices,
				@Nullable Vec3d pos,
				int y,
				Text label,
				boolean notSneaking,
				int light,
				double squaredDistanceToCamera,
				CameraRenderState cameraState
		) {
			if (pos == null) {
				return;
			}

			MinecraftClient client = MinecraftClient.getInstance();
			matrices.push();
			matrices.translate(pos.x, pos.y + LABEL_Y_OFFSET, pos.z);
			matrices.multiply(cameraState.orientation);
			matrices.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
			Matrix4f posMatrix = new Matrix4f(matrices.peek().getPositionMatrix());
			float textX = -client.textRenderer.getWidth(label) / 2.0F;
			int bgColor = (int) (client.options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;

			if (notSneaking) {
				normalLabels.add(new OrderedRenderCommandQueueImpl.LabelCommand(
						posMatrix, textX, y, label,
						LightmapTextureManager.applyEmission(light, NORMAL_LABEL_EMISSION),
						-1, 0, squaredDistanceToCamera
				));
				seethroughLabels.add(new OrderedRenderCommandQueueImpl.LabelCommand(
						posMatrix, textX, y, label,
						light, SEETHROUGH_COLOR, bgColor, squaredDistanceToCamera
				));
			}
			else {
				normalLabels.add(new OrderedRenderCommandQueueImpl.LabelCommand(
						posMatrix, textX, y, label,
						light, SEETHROUGH_COLOR, bgColor, squaredDistanceToCamera
				));
			}

			matrices.pop();
		}

		public void clear() {
			normalLabels.clear();
			seethroughLabels.clear();
		}
	}
}
