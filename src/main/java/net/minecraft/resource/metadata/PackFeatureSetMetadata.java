package net.minecraft.resource.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;

/**
 * Метаданные секции {@code "features"} файла {@code pack.mcmeta}.
 *
 * <p>Объявляет набор экспериментальных флагов функций, которые требуются для работы пакета.
 * При загрузке мира эти флаги объединяются с флагами из других активных пакетов.</p>
 *
 * @param flags набор флагов функций, объявленных пакетом
 */
public record PackFeatureSetMetadata(FeatureSet flags) {

	private static final Codec<PackFeatureSetMetadata> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(FeatureFlags.CODEC.fieldOf("enabled").forGetter(PackFeatureSetMetadata::flags))
					.apply(instance, PackFeatureSetMetadata::new)
	);

	/** Сериализатор для секции {@code "features"} в {@code pack.mcmeta}. */
	public static final ResourceMetadataSerializer<PackFeatureSetMetadata>
			SERIALIZER = new ResourceMetadataSerializer<>("features", CODEC);
}
