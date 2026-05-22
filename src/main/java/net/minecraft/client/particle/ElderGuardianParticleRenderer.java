package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Unit;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

/**
 * Рендерер частицы проклятия Старшего Стража. Отображает 3D-модель существа
 * поверх игрока с синусоидальной анимацией прозрачности и вращением вокруг
 * оси X. Использует полную яркость (full bright) для видимости в темноте.
 */
@Environment(EnvType.CLIENT)
public class ElderGuardianParticleRenderer extends ParticleRenderer<ElderGuardianParticle> {

	public ElderGuardianParticleRenderer(ParticleManager particleManager) {
		super(particleManager);
	}

	@Override
	public Submittable render(Frustum frustum, Camera camera, float tickProgress) {
		return new Result(
				this.particles
						.stream()
						.map(particle -> State.create(particle, camera, tickProgress))
						.toList()
		);
	}

	/**
	 * Результат рендеринга — список снимков состояния всех активных частиц.
	 * Отправляет каждую модель в очередь рендеринга с полной яркостью.
	 */
	@Environment(EnvType.CLIENT)
	record Result(List<State> states) implements Submittable {

		@Override
		public void submit(OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState) {
			for (State state : this.states) {
				queue.submitModel(
						state.model(),
						Unit.INSTANCE,
						state.matrices(),
						state.renderLayer(),
						State.FULL_BRIGHTNESS,
						OverlayTexture.DEFAULT_UV,
						state.color(),
						null,
						0,
						null
				);
			}
		}
	}

	/**
	 * Снимок состояния одной частицы для рендеринга: модель, матрица трансформации,
	 * слой рендеринга и цвет с альфой.
	 */
	@Environment(EnvType.CLIENT)
	record State(Model<Unit> model, MatrixStack matrices, RenderLayer renderLayer, int color) {

		// 0xF000F0 — максимальная яркость блока и неба в упакованном формате
		static final int FULL_BRIGHTNESS = 15728880;
		// Масштаб модели: 1/2.35 ≈ 0.4255, инвертирован по Y и Z для корректной ориентации
		private static final float MODEL_SCALE = 0.42553192F;
		// Начальный угол наклона (градусы), конечный угол при смерти частицы
		private static final float ROTATION_START_DEGREES = 60.0F;
		private static final float ROTATION_END_DEGREES = 150.0F;
		private static final float TRANSLATE_Y = -0.56F;
		private static final float TRANSLATE_Z = 3.5F;

		/**
		 * Создаёт снимок состояния частицы для текущего кадра.
		 * Альфа-канал анимируется по синусоиде: 0.05 в начале и конце жизни, 0.55 в середине.
		 */
		public static State create(ElderGuardianParticle particle, Camera camera, float tickProgress) {
			float lifeRatio = (particle.age + tickProgress) / particle.maxAge;
			float alpha = 0.05F + 0.5F * MathHelper.sin(lifeRatio * (float) Math.PI);
			int color = ColorHelper.fromFloats(alpha, 1.0F, 1.0F, 1.0F);

			MatrixStack matrices = new MatrixStack();
			matrices.push();
			matrices.multiply(camera.getRotation());
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ROTATION_START_DEGREES - ROTATION_END_DEGREES * lifeRatio));
			matrices.scale(MODEL_SCALE, -MODEL_SCALE, -MODEL_SCALE);
			matrices.translate(0.0F, TRANSLATE_Y, TRANSLATE_Z);

			return new State(particle.model, matrices, particle.renderLayer, color);
		}
	}
}
