package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.Submittable;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Рендерер частиц подбора предметов. Вместо стандартного billboard-рендеринга
 * использует полноценный рендер сущности предмета, интерполируя его позицию
 * между текущим положением предмета и целевой точкой (игроком). Это создаёт
 * плавную анимацию «притягивания» предмета к игроку при подборе.
 */
@Environment(EnvType.CLIENT)
public class ItemPickupParticleRenderer extends ParticleRenderer<ItemPickupParticle> {

	public ItemPickupParticleRenderer(ParticleManager particleManager) {
		super(particleManager);
	}

	@Override
	public Submittable render(Frustum frustum, Camera camera, float tickProgress) {
		return new Result(
				this.particles
						.stream()
						.map(particle -> Instance.create(particle, camera, tickProgress))
						.toList()
		);
	}

	/**
	 * Снимок состояния одной частицы подбора для конкретного кадра рендеринга:
	 * содержит состояние рендера предмета и его смещение относительно камеры.
	 */
	@Environment(EnvType.CLIENT)
	record Instance(EntityRenderState itemRenderState, double xOffset, double yOffset, double zOffset) {

		/**
		 * Вычисляет интерполированную позицию частицы подбора для текущего кадра.
		 * Использует квадратичную интерполяцию: чем ближе к концу анимации, тем
		 * быстрее предмет движется к цели.
		 *
		 * @param particle    частица подбора предмета
		 * @param camera      текущая камера для вычисления смещения
		 * @param tickProgress прогресс текущего тика (0.0–1.0)
		 * @return снимок состояния для рендеринга
		 */
		public static Instance create(ItemPickupParticle particle, Camera camera, float tickProgress) {
			float progress = (particle.ticksExisted + tickProgress) / 3.0F;
			float progressSquared = progress * progress;

			double targetX = MathHelper.lerp((double) tickProgress, particle.lastTargetX, particle.targetX);
			double targetY = MathHelper.lerp((double) tickProgress, particle.lastTargetY, particle.targetY);
			double targetZ = MathHelper.lerp((double) tickProgress, particle.lastTargetZ, particle.targetZ);

			double renderX = MathHelper.lerp((double) progressSquared, particle.renderState.x, targetX);
			double renderY = MathHelper.lerp((double) progressSquared, particle.renderState.y, targetY);
			double renderZ = MathHelper.lerp((double) progressSquared, particle.renderState.z, targetZ);

			Vec3d cameraPos = camera.getCameraPos();
			return new Instance(
					particle.renderState,
					renderX - cameraPos.getX(),
					renderY - cameraPos.getY(),
					renderZ - cameraPos.getZ()
			);
		}
	}

	/**
	 * Результат рендеринга: список снимков состояний частиц подбора,
	 * готовых к отправке в очередь рендер-команд.
	 */
	@Environment(EnvType.CLIENT)
	record Result(List<Instance> instances) implements Submittable {

		@Override
		public void submit(OrderedRenderCommandQueue renderQueue, CameraRenderState cameraRenderState) {
			MatrixStack matrixStack = new MatrixStack();
			EntityRenderManager entityRenderManager = MinecraftClient.getInstance().getEntityRenderDispatcher();

			for (Instance instance : this.instances) {
				entityRenderManager.render(
						instance.itemRenderState(),
						cameraRenderState,
						instance.xOffset(),
						instance.yOffset(),
						instance.zOffset(),
						matrixStack,
						renderQueue
				);
			}
		}
	}
}
