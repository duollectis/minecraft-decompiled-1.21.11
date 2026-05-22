package net.minecraft.resource.featuretoggle;

import com.mojang.serialization.Codec;
import net.minecraft.util.Identifier;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Реестр всех известных флагов экспериментальных фич игры.
 * Инициализируется статически через {@link FeatureManager.Builder}.
 */
public class FeatureFlags {

	public static final FeatureFlag VANILLA;
	public static final FeatureFlag TRADE_REBALANCE;
	public static final FeatureFlag REDSTONE_EXPERIMENTS;
	public static final FeatureFlag MINECART_IMPROVEMENTS;
	public static final FeatureManager FEATURE_MANAGER;
	public static final Codec<FeatureSet> CODEC;
	public static final FeatureSet VANILLA_FEATURES;
	/** Набор фич, включённых по умолчанию (только ванильные). */
	public static final FeatureSet DEFAULT_ENABLED_FEATURES;

	/**
	 * Формирует строку с перечислением флагов из {@code featuresToCheck},
	 * которые отсутствуют в {@code features}.
	 *
	 * @param featuresToCheck набор фич для проверки
	 * @param features        набор включённых фич
	 * @return строка с отсутствующими флагами через запятую
	 */
	public static String printMissingFlags(FeatureSet featuresToCheck, FeatureSet features) {
		return printMissingFlags(FEATURE_MANAGER, featuresToCheck, features);
	}

	/**
	 * Формирует строку с перечислением флагов из {@code featuresToCheck},
	 * которые отсутствуют в {@code features}, используя указанный менеджер.
	 *
	 * @param featureManager  менеджер для преобразования флагов в идентификаторы
	 * @param featuresToCheck набор фич для проверки
	 * @param features        набор включённых фич
	 * @return строка с отсутствующими флагами через запятую
	 */
	public static String printMissingFlags(
		FeatureManager featureManager,
		FeatureSet featuresToCheck,
		FeatureSet features
	) {
		Set<Identifier> required = featureManager.toId(featuresToCheck);
		Set<Identifier> enabled = featureManager.toId(features);
		return required.stream()
			.filter(id -> !enabled.contains(id))
			.map(Identifier::toString)
			.collect(Collectors.joining(", "));
	}

	/**
	 * Проверяет, содержит ли набор фич что-либо помимо ванильных.
	 *
	 * @param features проверяемый набор
	 * @return {@code true}, если набор выходит за рамки ванильных фич
	 */
	public static boolean isNotVanilla(FeatureSet features) {
		return !features.isSubsetOf(VANILLA_FEATURES);
	}

	static {
		FeatureManager.Builder builder = new FeatureManager.Builder("main");
		VANILLA = builder.addVanillaFlag("vanilla");
		TRADE_REBALANCE = builder.addVanillaFlag("trade_rebalance");
		REDSTONE_EXPERIMENTS = builder.addVanillaFlag("redstone_experiments");
		MINECART_IMPROVEMENTS = builder.addVanillaFlag("minecart_improvements");
		FEATURE_MANAGER = builder.build();
		CODEC = FEATURE_MANAGER.getCodec();
		VANILLA_FEATURES = FeatureSet.of(VANILLA);
		DEFAULT_ENABLED_FEATURES = VANILLA_FEATURES;
	}
}
