package net.minecraft.client.render.gizmo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawer;
import net.minecraft.world.debug.gizmo.TextGizmo;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Клиентская реализация {@link GizmoDrawer}, накапливающая геометрические примитивы
 * (точки, линии, полигоны, квады, текст) и отрисовывающая их за один проход.
 * <p>
 * Примитивы разделяются на два бакета: {@code opaque} (alpha == 255) и {@code transparent}
 * (alpha < 255), чтобы гарантировать корректный порядок смешивания.
 */
@Environment(EnvType.CLIENT)
public class GizmoDrawerImpl implements GizmoDrawer {

	private static final int FULL_BRIGHT = 15728880;

	private final GizmoDrawerImpl.Division opaque = new GizmoDrawerImpl.Division(true);
	private final GizmoDrawerImpl.Division transparent = new GizmoDrawerImpl.Division(false);
	private boolean empty = true;

	private GizmoDrawerImpl.Division getDivision(int color) {
		return ColorHelper.getAlpha(color) < 255 ? transparent : opaque;
	}

	@Override
	public void addPoint(Vec3d pos, int color, float size) {
		getDivision(color).points.add(new GizmoDrawerImpl.Point(pos, color, size));
		empty = false;
	}

	@Override
	public void addLine(Vec3d start, Vec3d end, int color, float width) {
		getDivision(color).lines.add(new GizmoDrawerImpl.Line(start, end, color, width));
		empty = false;
	}

	@Override
	public void addPolygon(Vec3d[] vertices, int color) {
		getDivision(color).triangleFans.add(new GizmoDrawerImpl.Polygon(vertices, color));
		empty = false;
	}

	@Override
	public void addQuad(Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color) {
		getDivision(color).quads.add(new GizmoDrawerImpl.Quad(a, b, c, d, color));
		empty = false;
	}

	@Override
	public void addText(Vec3d pos, String text, TextGizmo.Style style) {
		getDivision(style.color()).texts.add(new GizmoDrawerImpl.Text(pos, text, style));
		empty = false;
	}

	/**
	 * Отрисовывает все накопленные примитивы: сначала непрозрачные, затем прозрачные.
	 *
	 * @param matrices матрица трансформаций
	 * @param vertexConsumers провайдер вершинных буферов
	 * @param cameraRenderState состояние камеры (позиция, ориентация)
	 * @param posMatrix матрица позиционирования для клиппинга линий
	 */
	public void draw(
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			CameraRenderState cameraRenderState,
			Matrix4f posMatrix
	) {
		opaque.draw(matrices, vertexConsumers, cameraRenderState, posMatrix);
		transparent.draw(matrices, vertexConsumers, cameraRenderState, posMatrix);
	}

	public boolean isEmpty() {
		return empty;
	}

	/**
	 * Бакет примитивов одного типа прозрачности. Хранит все геометрические примитивы
	 * и отрисовывает их в правильном порядке: квады → полигоны → линии → текст → точки.
	 */
	@Environment(EnvType.CLIENT)
	record Division(
			boolean opaque,
			List<GizmoDrawerImpl.Line> lines,
			List<GizmoDrawerImpl.Quad> quads,
			List<GizmoDrawerImpl.Polygon> triangleFans,
			List<GizmoDrawerImpl.Text> texts,
			List<GizmoDrawerImpl.Point> points
	) {

		Division(boolean opaque) {
			this(opaque, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		}

		public void draw(
				MatrixStack matrices,
				VertexConsumerProvider vertexConsumers,
				CameraRenderState cameraRenderState,
				Matrix4f posMatrix
		) {
			this.drawQuads(matrices, vertexConsumers, cameraRenderState);
			this.drawTriangleFans(matrices, vertexConsumers, cameraRenderState);
			this.drawLines(matrices, vertexConsumers, cameraRenderState, posMatrix);
			this.drawText(matrices, vertexConsumers, cameraRenderState);
			this.drawPoints(matrices, vertexConsumers, cameraRenderState);
		}

		private void drawText(
				MatrixStack matrices,
				VertexConsumerProvider vertexConsumers,
				CameraRenderState cameraRenderState
		) {
			if (!cameraRenderState.initialized) {
				return;
			}

			TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
			double camX = cameraRenderState.pos.getX();
			double camY = cameraRenderState.pos.getY();
			double camZ = cameraRenderState.pos.getZ();

			for (GizmoDrawerImpl.Text text : this.texts) {
				matrices.push();
				matrices.translate(
						(float) (text.pos().getX() - camX),
						(float) (text.pos().getY() - camY),
						(float) (text.pos().getZ() - camZ)
				);
				matrices.multiply(cameraRenderState.orientation);
				matrices.scale(text.style.scale() / 16.0F, -text.style.scale() / 16.0F, text.style.scale() / 16.0F);

				float xOffset = text.style.adjustLeft().isEmpty()
						? -textRenderer.getWidth(text.text) / 2.0F
						: (float) (-text.style.adjustLeft().getAsDouble()) / text.style.scale();

				textRenderer.draw(
						text.text,
						xOffset,
						0.0F,
						text.style.color(),
						false,
						matrices.peek().getPositionMatrix(),
						vertexConsumers,
						TextRenderer.TextLayerType.NORMAL,
						0,
						FULL_BRIGHT
				);
				matrices.pop();
			}
		}

		private void drawLines(
				MatrixStack matrices,
				VertexConsumerProvider vertexConsumers,
				CameraRenderState cameraRenderState,
				Matrix4f posMatrix
		) {
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(
					this.opaque ? RenderLayers.lines() : RenderLayers.linesTranslucent()
			);
			MatrixStack.Entry entry = matrices.peek();
			Vector4f startLocal = new Vector4f();
			Vector4f endLocal = new Vector4f();
			Vector4f startClip = new Vector4f();
			Vector4f endClip = new Vector4f();
			Vector4f clipped = new Vector4f();
			double camX = cameraRenderState.pos.getX();
			double camY = cameraRenderState.pos.getY();
			double camZ = cameraRenderState.pos.getZ();

			for (GizmoDrawerImpl.Line line : this.lines) {
				startLocal.set(line.start().getX() - camX, line.start().getY() - camY, line.start().getZ() - camZ, 1.0);
				endLocal.set(line.end().getX() - camX, line.end().getY() - camY, line.end().getZ() - camZ, 1.0);
				startLocal.mul(posMatrix, startClip);
				endLocal.mul(posMatrix, endClip);
				boolean startVisible = startClip.z > -0.05F;
				boolean endVisible = endClip.z > -0.05F;

				if (startVisible && endVisible) {
					continue;
				}

				if (startVisible || endVisible) {
					float zDelta = endClip.z - startClip.z;
					if (Math.abs(zDelta) < 1.0E-9F) {
						continue;
					}

					float clipT = MathHelper.clamp((-0.05F - startClip.z) / zDelta, 0.0F, 1.0F);
					startLocal.lerp(endLocal, clipT, clipped);

					if (startVisible) {
						startLocal.set(clipped);
					}
					else {
						endLocal.set(clipped);
					}
				}

				float nx = endLocal.x - startLocal.x;
				float ny = endLocal.y - startLocal.y;
				float nz = endLocal.z - startLocal.z;

				vertexConsumer.vertex(entry, startLocal.x, startLocal.y, startLocal.z)
				              .normal(entry, nx, ny, nz)
				              .color(line.color())
				              .lineWidth(line.width());
				vertexConsumer.vertex(entry, endLocal.x, endLocal.y, endLocal.z)
				              .normal(entry, nx, ny, nz)
				              .color(line.color())
				              .lineWidth(line.width());
			}
		}

		private void drawTriangleFans(
				MatrixStack matrices,
				VertexConsumerProvider vertexConsumers,
				CameraRenderState cameraRenderState
		) {
			MatrixStack.Entry entry = matrices.peek();
			double camX = cameraRenderState.pos.getX();
			double camY = cameraRenderState.pos.getY();
			double camZ = cameraRenderState.pos.getZ();

			for (GizmoDrawerImpl.Polygon polygon : this.triangleFans) {
				VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.debugTriangleFan());

				for (Vec3d vertex : polygon.points()) {
					vertexConsumer
							.vertex(
									entry,
									(float) (vertex.getX() - camX),
									(float) (vertex.getY() - camY),
									(float) (vertex.getZ() - camZ)
							)
							.color(polygon.color());
				}
			}
		}

		private void drawQuads(
				MatrixStack matrices,
				VertexConsumerProvider vertexConsumers,
				CameraRenderState cameraRenderState
		) {
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.debugFilledBox());
			MatrixStack.Entry entry = matrices.peek();
			double camX = cameraRenderState.pos.getX();
			double camY = cameraRenderState.pos.getY();
			double camZ = cameraRenderState.pos.getZ();

			for (GizmoDrawerImpl.Quad quad : this.quads) {
				vertexConsumer
						.vertex(entry, (float) (quad.a().getX() - camX), (float) (quad.a().getY() - camY), (float) (quad.a().getZ() - camZ))
						.color(quad.color());
				vertexConsumer
						.vertex(entry, (float) (quad.b().getX() - camX), (float) (quad.b().getY() - camY), (float) (quad.b().getZ() - camZ))
						.color(quad.color());
				vertexConsumer
						.vertex(entry, (float) (quad.c().getX() - camX), (float) (quad.c().getY() - camY), (float) (quad.c().getZ() - camZ))
						.color(quad.color());
				vertexConsumer
						.vertex(entry, (float) (quad.d().getX() - camX), (float) (quad.d().getY() - camY), (float) (quad.d().getZ() - camZ))
						.color(quad.color());
			}
		}

		private void drawPoints(
				MatrixStack matrices,
				VertexConsumerProvider vertexConsumers,
				CameraRenderState cameraRenderState
		) {
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.debugPoint());
			MatrixStack.Entry entry = matrices.peek();
			double camX = cameraRenderState.pos.getX();
			double camY = cameraRenderState.pos.getY();
			double camZ = cameraRenderState.pos.getZ();

			for (GizmoDrawerImpl.Point point : this.points) {
				vertexConsumer
						.vertex(
								entry,
								(float) (point.pos.getX() - camX),
								(float) (point.pos.getY() - camY),
								(float) (point.pos.getZ() - camZ)
						)
						.color(point.color())
						.lineWidth(point.size());
			}
		}
	}

	/** Отрезок линии с цветом и шириной. */
	@Environment(EnvType.CLIENT)
	record Line(Vec3d start, Vec3d end, int color, float width) {
	}

	/** Точка с цветом и размером. */
	@Environment(EnvType.CLIENT)
	record Point(Vec3d pos, int color, float size) {
	}

	/** Полигон (triangle fan) с набором вершин и цветом. */
	@Environment(EnvType.CLIENT)
	record Polygon(Vec3d[] points, int color) {
	}

	/** Четырёхугольник с четырьмя вершинами и цветом. */
	@Environment(EnvType.CLIENT)
	record Quad(Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color) {
	}

	/** Текстовая метка с позицией и стилем отображения. */
	@Environment(EnvType.CLIENT)
	record Text(Vec3d pos, String text, TextGizmo.Style style) {
	}
}
