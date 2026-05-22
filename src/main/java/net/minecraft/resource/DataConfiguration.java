package net.minecraft.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;

/**
 * Конфигурация данных мира: список активных датапаков и набор включённых фич.
 * Сериализуется в NBT-тег уровня мира.
 */
public record DataConfiguration(DataPackSettings dataPacks, FeatureSet enabledFeatures) {

	public static final String ENABLED_FEATURES_KEY = "enabled_features";

	public static final MapCodec<DataConfiguration> MAP_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			DataPackSettings.CODEC
				.lenientOptionalFieldOf("DataPacks", DataPackSettings.SAFE_MODE)
				.forGetter(DataConfiguration::dataPacks),
			FeatureFlags.CODEC
				.lenientOptionalFieldOf(ENABLED_FEATURES_KEY, FeatureFlags.DEFAULT_ENABLED_FEATURES)
				.forGetter(DataConfiguration::enabledFeatures)
		).apply(instance, DataConfiguration::new)
	);

	public static final Codec<DataConfiguration> CODEC = MAP_CODEC.codec();

	public static final DataConfiguration SAFE_MODE = new DataConfiguration(
		DataPackSettings.SAFE_MODE,
		FeatureFlags.DEFAULT_ENABLED_FEATURES
	);

	/**
	 * Возвращает новую конфигурацию с добавленными фичами из переданного набора.
	 *
	 * @param features набор фич для объединения с текущим
	 * @return новая конфигурация с расширенным набором фич
	 */
	public DataConfiguration withFeaturesAdded(FeatureSet features) {
		return new DataConfiguration(dataPacks, enabledFeatures.combine(features));
	}
}
