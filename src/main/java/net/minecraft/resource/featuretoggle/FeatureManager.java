package net.minecraft.resource.featuretoggle;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;

/**
 * Менеджер флагов функций (feature flags) для конкретной вселенной флагов.
 *
 * <p>Хранит полный реестр всех зарегистрированных флагов и предоставляет API
 * для преобразования между {@link Identifier}-идентификаторами и {@link FeatureSet}-наборами.
 * Создаётся через вложенный {@link Builder}.</p>
 */
public class FeatureManager {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final FeatureUniverse universe;
	private final Map<Identifier, FeatureFlag> featureFlags;
	private final FeatureSet featureSet;

	FeatureManager(FeatureUniverse universe, FeatureSet featureSet, Map<Identifier, FeatureFlag> featureFlags) {
		this.universe = universe;
		this.featureFlags = featureFlags;
		this.featureSet = featureSet;
	}

	/**
	 * Проверяет, является ли переданный набор флагов подмножеством активных флагов этого менеджера.
	 *
	 * @param features набор флагов для проверки
	 * @return {@code true}, если все флаги из {@code features} присутствуют в активном наборе
	 */
	public boolean contains(FeatureSet features) {
		return features.isSubsetOf(featureSet);
	}

	public FeatureSet getFeatureSet() {
		return featureSet;
	}

	/**
	 * Строит {@link FeatureSet} из коллекции идентификаторов, логируя предупреждение для неизвестных.
	 *
	 * @param features идентификаторы флагов
	 * @return набор флагов, соответствующих известным идентификаторам
	 */
	public FeatureSet featureSetOf(Iterable<Identifier> features) {
		return featureSetOf(features, feature -> LOGGER.warn("Unknown feature flag: {}", feature));
	}

	public FeatureSet featureSetOf(FeatureFlag... features) {
		return FeatureSet.of(universe, Arrays.asList(features));
	}

	/**
	 * Строит {@link FeatureSet} из коллекции идентификаторов с пользовательским обработчиком неизвестных флагов.
	 *
	 * @param features идентификаторы флагов
	 * @param unknownFlagConsumer вызывается для каждого идентификатора, не найденного в реестре
	 * @return набор флагов, соответствующих известным идентификаторам
	 */
	public FeatureSet featureSetOf(Iterable<Identifier> features, Consumer<Identifier> unknownFlagConsumer) {
		Set<FeatureFlag> resolved = Sets.newIdentityHashSet();

		for (Identifier identifier : features) {
			FeatureFlag featureFlag = featureFlags.get(identifier);

			if (featureFlag == null) {
				unknownFlagConsumer.accept(identifier);
			} else {
				resolved.add(featureFlag);
			}
		}

		return FeatureSet.of(universe, resolved);
	}

	/**
	 * Преобразует {@link FeatureSet} обратно в набор идентификаторов.
	 *
	 * @param features набор флагов для преобразования
	 * @return множество идентификаторов флагов, входящих в переданный набор
	 */
	public Set<Identifier> toId(FeatureSet features) {
		Set<Identifier> ids = new HashSet<>();

		featureFlags.forEach((identifier, featureFlag) -> {
			if (features.contains(featureFlag)) {
				ids.add(identifier);
			}
		});

		return ids;
	}

	/**
	 * Создаёт {@link Codec} для сериализации/десериализации {@link FeatureSet} через список идентификаторов.
	 *
	 * <p>При десериализации неизвестные идентификаторы не вызывают ошибку парсинга,
	 * но возвращают {@link DataResult#error} с частичным результатом.</p>
	 *
	 * @return кодек для {@link FeatureSet}
	 */
	public Codec<FeatureSet> getCodec() {
		return Identifier.CODEC.listOf().comapFlatMap(
				featureIds -> {
					Set<Identifier> unknown = new HashSet<>();
					FeatureSet resolved = featureSetOf(featureIds, unknown::add);

					return unknown.isEmpty()
							? DataResult.success(resolved)
							: DataResult.error(() -> "Unknown feature ids: " + unknown, resolved);
				},
				features -> List.copyOf(toId(features))
		);
	}

	/**
	 * Строитель {@link FeatureManager}.
	 *
	 * <p>Регистрирует флаги в порядке вызова {@link #addFlag}/{@link #addVanillaFlag}.
	 * Максимальное количество флагов ограничено 64 (размер {@code long}-маски).</p>
	 */
	public static class Builder {

		private final FeatureUniverse universe;
		private int id;
		private final Map<Identifier, FeatureFlag> featureFlags = new LinkedHashMap<>();

		public Builder(String universe) {
			this.universe = new FeatureUniverse(universe);
		}

		public FeatureFlag addVanillaFlag(String feature) {
			return addFlag(Identifier.ofVanilla(feature));
		}

		/**
		 * Регистрирует новый флаг с заданным идентификатором.
		 *
		 * @param feature идентификатор флага
		 * @return созданный {@link FeatureFlag}
		 * @throws IllegalStateException если превышен лимит в 64 флага или флаг уже зарегистрирован
		 */
		public FeatureFlag addFlag(Identifier feature) {
			if (id >= 64) {
				throw new IllegalStateException("Too many feature flags");
			}

			FeatureFlag featureFlag = new FeatureFlag(universe, id++);
			FeatureFlag existing = featureFlags.put(feature, featureFlag);

			if (existing != null) {
				throw new IllegalStateException("Duplicate feature flag " + feature);
			}

			return featureFlag;
		}

		public FeatureManager build() {
			FeatureSet allFlags = FeatureSet.of(universe, featureFlags.values());
			return new FeatureManager(universe, allFlags, Map.copyOf(featureFlags));
		}
	}
}
