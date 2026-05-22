package net.minecraft.client.render.entity.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.AnimationState;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Исполняемая анимация, привязанная к конкретному дереву модели.
 * <p>
 * Создаётся из {@link AnimationDefinition} через {@link #of(ModelPart, AnimationDefinition)},
 * который разрешает имена костей в реальные ссылки на {@link ModelPart}.
 * Поддерживает воспроизведение по времени в миллисекундах, по {@link AnimationState}
 * и по прогрессу ходьбы.
 */
@Environment(EnvType.CLIENT)
public class Animation {

	private final AnimationDefinition definition;
	private final List<Animation.TransformationEntry> entries;

	private Animation(AnimationDefinition definition, List<Animation.TransformationEntry> entries) {
		this.definition = definition;
		this.entries = entries;
	}

	/**
	 * Создаёт анимацию, разрешая имена костей из {@code definition} в части модели {@code root}.
	 *
	 * @param root       корневая часть модели
	 * @param definition декларативное описание анимации
	 * @return готовая к воспроизведению анимация
	 * @throws IllegalArgumentException если кость из определения не найдена в модели
	 */
	static Animation of(ModelPart root, AnimationDefinition definition) {
		List<Animation.TransformationEntry> result = new ArrayList<>();
		Function<String, ModelPart> partGetter = root.createPartGetter();

		for (Entry<String, List<Transformation>> entry : definition.boneAnimations().entrySet()) {
			String boneName = entry.getKey();
			ModelPart part = partGetter.apply(boneName);

			if (part == null) {
				throw new IllegalArgumentException("Cannot animate " + boneName + ", which does not exist in model");
			}

			for (Transformation transformation : entry.getValue()) {
				result.add(new Animation.TransformationEntry(
						part,
						transformation.target(),
						transformation.keyframes()
				));
			}
		}

		return new Animation(definition, List.copyOf(result));
	}

	/** Применяет анимацию в нулевой момент времени (статичная поза). */
	public void applyStatic() {
		apply(0L, 1.0F);
	}

	/**
	 * Применяет анимацию, синхронизированную с циклом ходьбы.
	 *
	 * @param limbSwingProgress  прогресс качания конечностей (фаза)
	 * @param limbSwingAmplitude амплитуда качания конечностей
	 * @param speedFactor        множитель скорости воспроизведения
	 * @param amplitudeScale     масштаб амплитуды (ограничивает силу анимации)
	 */
	public void applyWalking(
			float limbSwingProgress,
			float limbSwingAmplitude,
			float speedFactor,
			float amplitudeScale
	) {
		long timeMs = (long) (limbSwingProgress * 50.0F * speedFactor);
		float scale = Math.min(limbSwingAmplitude * amplitudeScale, 1.0F);
		apply(timeMs, scale);
	}

	/** Применяет анимацию по состоянию с масштабом 1.0. */
	public void apply(AnimationState animationState, float age) {
		apply(animationState, age, 1.0F);
	}

	/**
	 * Применяет анимацию по состоянию с заданным множителем скорости.
	 *
	 * @param animationState  состояние анимации сущности
	 * @param age             возраст сущности в тиках
	 * @param speedMultiplier множитель скорости воспроизведения
	 */
	public void apply(AnimationState animationState, float age, float speedMultiplier) {
		animationState.run(state -> apply(
				(long) ((float) state.getTimeInMilliseconds(age) * speedMultiplier),
				1.0F
		));
	}

	/**
	 * Применяет анимацию в заданный момент времени.
	 *
	 * @param timeInMilliseconds время воспроизведения в миллисекундах
	 * @param scale              общий масштаб трансформаций
	 */
	public void apply(long timeInMilliseconds, float scale) {
		float runningSeconds = getRunningSeconds(timeInMilliseconds);
		Vector3f scratch = new Vector3f();

		for (Animation.TransformationEntry entry : entries) {
			entry.apply(runningSeconds, scale, scratch);
		}
	}

	/** Переводит время в секунды с учётом зацикливания. */
	private float getRunningSeconds(long timeInMilliseconds) {
		float seconds = (float) timeInMilliseconds / 1000.0F;
		return definition.looping() ? seconds % definition.lengthInSeconds() : seconds;
	}

	/**
	 * Привязка трансформации к конкретной части модели.
	 * <p>
	 * Хранит ссылку на {@link ModelPart}, цель трансформации и массив ключевых кадров.
	 * Метод {@link #apply} выполняет бинарный поиск нужного кадра и интерполирует значение.
	 */
	@Environment(EnvType.CLIENT)
	record TransformationEntry(ModelPart part, Transformation.Target target, Keyframe[] keyframes) {

		/**
		 * Вычисляет и применяет трансформацию для заданного момента времени.
		 * <p>
		 * Бинарным поиском находит пару соседних кадров, вычисляет дельту
		 * интерполяции и делегирует вычисление конкретной стратегии {@link Transformation.Interpolation}.
		 *
		 * @param runningSeconds текущее время анимации в секундах
		 * @param scale          масштаб трансформации
		 * @param scratch        переиспользуемый вектор для записи результата
		 */
		public void apply(float runningSeconds, float scale, Vector3f scratch) {
			int startIndex = Math.max(
					0,
					MathHelper.binarySearch(
							0,
							keyframes.length,
							index -> runningSeconds <= keyframes[index].timestamp()
					) - 1
			);
			int endIndex = Math.min(keyframes.length - 1, startIndex + 1);

			Keyframe startFrame = keyframes[startIndex];
			Keyframe endFrame = keyframes[endIndex];

			float elapsed = runningSeconds - startFrame.timestamp();
			float delta = endIndex != startIndex
					? MathHelper.clamp(elapsed / (endFrame.timestamp() - startFrame.timestamp()), 0.0F, 1.0F)
					: 0.0F;

			endFrame.interpolation().apply(scratch, delta, keyframes, startIndex, endIndex, scale);
			target.apply(part, scratch);
		}
	}
}
