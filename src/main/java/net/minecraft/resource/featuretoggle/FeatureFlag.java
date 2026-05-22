package net.minecraft.resource.featuretoggle;

/**
 * Флаг экспериментальной фичи.
 * Принадлежит конкретной {@link FeatureUniverse} и идентифицируется битовой маской.
 * Создаётся только через {@link FeatureManager.Builder}.
 */
public class FeatureFlag {

	final FeatureUniverse universe;
	final long mask;

	FeatureFlag(FeatureUniverse universe, int id) {
		this.universe = universe;
		mask = 1L << id;
	}
}
