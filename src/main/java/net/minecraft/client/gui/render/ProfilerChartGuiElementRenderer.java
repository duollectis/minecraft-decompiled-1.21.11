package net.minecraft.client.gui.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.render.state.special.ProfilerChartGuiElementRenderState;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.ProfilerTiming;
import org.joml.Matrix4f;

/**
 * Рендерер круговой диаграммы профайлера в GUI.
 * Строит диаграмму из треугольных секторов (fan) и боковых граней (quads),
 * создавая эффект объёмного «пирога». Боковые грани рисуются только для
 * секторов, чья средняя Y-координата неотрицательна (видимая сторона).
 */
@Environment(EnvType.CLIENT)
public class ProfilerChartGuiElementRenderer extends SpecialGuiElementRenderer<ProfilerChartGuiElementRenderState> {

	private static final float CHART_Y_OFFSET = -5.0F;
	private static final float CHART_RADIUS = 105.0F;
	private static final float CHART_DEPTH = 10.0F;
	private static final float CHART_Y_SCALE = 0.5F;
	private static final double FULL_CIRCLE_RADIANS = Math.PI * 2;
	private static final double PERCENT_TO_RADIANS = FULL_CIRCLE_RADIANS / 100.0;
	private static final int SECTOR_SUBDIVISIONS_BASE = 1;

	public ProfilerChartGuiElementRenderer(VertexConsumerProvider.Immediate immediate) {
		super(immediate);
	}

	@Override
	public Class<ProfilerChartGuiElementRenderState> getElementClass() {
		return ProfilerChartGuiElementRenderState.class;
	}

	/**
	 * Отрисовывает круговую диаграмму профайлера.
	 * Каждый сектор разбивается на {@code subdivisions} подсегментов для плавности дуги.
	 * Боковые грани (имитация толщины диска) рисуются только для видимых сегментов
	 * (средняя Y-координата ≥ 0), чтобы избежать артефактов на задней стороне.
	 */
	@Override
	protected void render(ProfilerChartGuiElementRenderState state, MatrixStack matrices) {
		double angleOffset = 0.0;
		matrices.translate(0.0F, CHART_Y_OFFSET, 0.0F);
		Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

		for (ProfilerTiming timing : state.chartData()) {
			int subdivisions = MathHelper.floor(timing.parentSectionUsagePercentage / 4.0) + SECTOR_SUBDIVISIONS_BASE;
			int color = ColorHelper.fullAlpha(timing.getColor());
			int sideColor = ColorHelper.mix(color, -8355712);

			renderSectorFan(positionMatrix, timing, subdivisions, color, angleOffset);
			renderSectorSides(positionMatrix, timing, subdivisions, sideColor, angleOffset);

			angleOffset += timing.parentSectionUsagePercentage;
		}
	}

	private void renderSectorFan(
			Matrix4f matrix,
			ProfilerTiming timing,
			int subdivisions,
			int color,
			double angleOffset
	) {
		VertexConsumer fan = vertexConsumers.getBuffer(RenderLayers.debugTriangleFan());
		fan.vertex(matrix, 0.0F, 0.0F, 0.0F).color(color);

		for (int step = subdivisions; step >= 0; step--) {
			float angle = sectorAngle(angleOffset, timing.parentSectionUsagePercentage, step, subdivisions);
			fan.vertex(matrix, MathHelper.sin(angle) * CHART_RADIUS, MathHelper.cos(angle) * CHART_RADIUS * CHART_Y_SCALE, 0.0F)
					.color(color);
		}
	}

	private void renderSectorSides(
			Matrix4f matrix,
			ProfilerTiming timing,
			int subdivisions,
			int sideColor,
			double angleOffset
	) {
		VertexConsumer quads = vertexConsumers.getBuffer(RenderLayers.debugQuads());

		for (int step = subdivisions; step > 0; step--) {
			float angleA = sectorAngle(angleOffset, timing.parentSectionUsagePercentage, step, subdivisions);
			float angleB = sectorAngle(angleOffset, timing.parentSectionUsagePercentage, step - 1, subdivisions);

			float xA = MathHelper.sin(angleA) * CHART_RADIUS;
			float yA = MathHelper.cos(angleA) * CHART_RADIUS * CHART_Y_SCALE;
			float xB = MathHelper.sin(angleB) * CHART_RADIUS;
			float yB = MathHelper.cos(angleB) * CHART_RADIUS * CHART_Y_SCALE;

			// Рисуем только видимую (переднюю) сторону диска
			if ((yA + yB) / 2.0F < 0.0F) {
				continue;
			}

			quads.vertex(matrix, xA, yA, 0.0F).color(sideColor);
			quads.vertex(matrix, xA, yA + CHART_DEPTH, 0.0F).color(sideColor);
			quads.vertex(matrix, xB, yB + CHART_DEPTH, 0.0F).color(sideColor);
			quads.vertex(matrix, xB, yB, 0.0F).color(sideColor);
		}
	}

	private static float sectorAngle(double offset, double percentage, int step, int subdivisions) {
		return (float) ((offset + percentage * step / subdivisions) * PERCENT_TO_RADIANS);
	}

	@Override
	protected float getYOffset(int height, int windowScaleFactor) {
		return height / 2.0F;
	}

	@Override
	protected String getName() {
		return "profiler chart";
	}
}
