package net.minecraft.client.render.entity.animation;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Декларативное описание анимации: длительность, режим зацикливания
 * и набор трансформаций по именам костей.
 * <p>
 * Создаётся через {@link Builder} и затем привязывается к конкретному
 * дереву {@link ModelPart} вызовом {@link #createAnimation(ModelPart)},
 * который разрешает имена костей в реальные ссылки на части модели.
 */
@Environment(EnvType.CLIENT)
public record AnimationDefinition(
		float lengthInSeconds,
		boolean looping,
		Map<String, List<Transformation>> boneAnimations
) {

	/**
	 * Создаёт исполняемую {@link Animation}, привязывая кости по имени
	 * к частям переданного дерева модели.
	 *
	 * @param root корневая часть модели, содержащая все именованные кости
	 * @return готовая к воспроизведению анимация
	 * @throws IllegalArgumentException если кость из определения не найдена в модели
	 */
	public Animation createAnimation(ModelPart root) {
		return Animation.of(root, this);
	}

	/** Строитель для пошагового конструирования {@link AnimationDefinition}. */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final float lengthInSeconds;
		private final Map<String, List<Transformation>> transformations = Maps.newHashMap();
		private boolean looping;

		public static AnimationDefinition.Builder create(float lengthInSeconds) {
			return new AnimationDefinition.Builder(lengthInSeconds);
		}

		private Builder(float lengthInSeconds) {
			this.lengthInSeconds = lengthInSeconds;
		}

		public AnimationDefinition.Builder looping() {
			looping = true;
			return this;
		}

		public AnimationDefinition.Builder addBoneAnimation(String name, Transformation transformation) {
			transformations.computeIfAbsent(name, key -> new ArrayList<>()).add(transformation);
			return this;
		}

		public AnimationDefinition build() {
			return new AnimationDefinition(lengthInSeconds, looping, transformations);
		}
	}
}
