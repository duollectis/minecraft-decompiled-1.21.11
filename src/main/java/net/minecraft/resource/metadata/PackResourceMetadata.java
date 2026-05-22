package net.minecraft.resource.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resource.PackVersion;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Range;

/**
 * Метаданные секции {@code "pack"} файла {@code pack.mcmeta}.
 *
 * <p>Содержит описание пакета и диапазон поддерживаемых версий формата.
 * Для каждого типа ресурсов ({@link ResourceType#CLIENT_RESOURCES} и
 * {@link ResourceType#SERVER_DATA}) существует отдельный сериализатор,
 * поскольку поля версии формата различаются по имени.</p>
 *
 * @param description текстовое описание пакета (отображается в интерфейсе)
 * @param supportedFormats диапазон поддерживаемых версий формата пакета
 */
public record PackResourceMetadata(Text description, Range<PackVersion> supportedFormats) {

	/**
	 * Упрощённый кодек только с полем {@code "description"} — используется как запасной вариант
	 * для пакетов, не объявляющих поле версии формата.
	 */
	private static final Codec<PackResourceMetadata> DESCRIPTION_CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(TextCodecs.CODEC.fieldOf("description").forGetter(PackResourceMetadata::description))
					.apply(instance,
							description -> new PackResourceMetadata(
									description,
									new Range<>(PackVersion.of(Integer.MAX_VALUE))
							)
					)
	);

	/** Сериализатор для клиентских ресурсных пакетов (секция {@code "pack"} в {@code pack.mcmeta}). */
	public static final ResourceMetadataSerializer<PackResourceMetadata>
			CLIENT_RESOURCES_SERIALIZER = new ResourceMetadataSerializer<>(
					"pack", createCodec(ResourceType.CLIENT_RESOURCES)
			);

	/** Сериализатор для серверных пакетов данных (секция {@code "pack"} в {@code pack.mcmeta}). */
	public static final ResourceMetadataSerializer<PackResourceMetadata>
			SERVER_DATA_SERIALIZER = new ResourceMetadataSerializer<>(
					"pack", createCodec(ResourceType.SERVER_DATA)
			);

	/** Запасной сериализатор без поля версии формата — для обратной совместимости. */
	public static final ResourceMetadataSerializer<PackResourceMetadata>
			DESCRIPTION_SERIALIZER = new ResourceMetadataSerializer<>("pack", DESCRIPTION_CODEC);

	private static Codec<PackResourceMetadata> createCodec(ResourceType type) {
		return RecordCodecBuilder.create(
				instance -> instance.group(
						TextCodecs.CODEC.fieldOf("description").forGetter(PackResourceMetadata::description),
						PackVersion.createRangeCodec(type).forGetter(PackResourceMetadata::supportedFormats)
				).apply(instance, PackResourceMetadata::new)
		);
	}

	/**
	 * Возвращает сериализатор для заданного типа ресурсов.
	 *
	 * @param type тип ресурсов пакета
	 * @return соответствующий сериализатор
	 */
	public static ResourceMetadataSerializer<PackResourceMetadata> getSerializerFor(ResourceType type) {
		return switch (type) {
			case CLIENT_RESOURCES -> CLIENT_RESOURCES_SERIALIZER;
			case SERVER_DATA -> SERVER_DATA_SERIALIZER;
		};
	}
}
