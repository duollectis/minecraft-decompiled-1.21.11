package net.minecraft.resource.metadata;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resource.PackVersion;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.dynamic.Range;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Метаданные секции {@code "overlays"} файла {@code pack.mcmeta}.
 *
 * <p>Описывает список оверлейных директорий пакета, каждая из которых активируется
 * при определённом диапазоне версий формата. Оверлеи позволяют одному пакету
 * поддерживать несколько версий формата одновременно.</p>
 *
 * @param overlays список записей оверлеев
 */
public record PackOverlaysMetadata(List<Entry> overlays) {

	/** Паттерн допустимых имён директорий оверлеев. */
	private static final Pattern DIRECTORY_NAME_PATTERN = Pattern.compile("[-_a-zA-Z0-9.]+");

	/** Сериализатор для клиентских ресурсных пакетов. */
	public static final ResourceMetadataSerializer<PackOverlaysMetadata>
			CLIENT_RESOURCES_SERIALIZER = new ResourceMetadataSerializer<>(
					"overlays", createCodec(ResourceType.CLIENT_RESOURCES)
			);

	/** Сериализатор для серверных пакетов данных. */
	public static final ResourceMetadataSerializer<PackOverlaysMetadata>
			SERVER_DATA_SERIALIZER = new ResourceMetadataSerializer<>(
					"overlays", createCodec(ResourceType.SERVER_DATA)
			);

	private static DataResult<String> validate(String directoryName) {
		return DIRECTORY_NAME_PATTERN.matcher(directoryName).matches()
				? DataResult.success(directoryName)
				: DataResult.error(() -> directoryName + " is not accepted directory name");
	}

	@VisibleForTesting
	public static Codec<PackOverlaysMetadata> createCodec(ResourceType type) {
		return RecordCodecBuilder.create(
				instance -> instance
						.group(Entry.createCodec(type).fieldOf("entries").forGetter(PackOverlaysMetadata::overlays))
						.apply(instance, PackOverlaysMetadata::new)
		);
	}

	/**
	 * Возвращает сериализатор для заданного типа ресурсов.
	 *
	 * @param type тип ресурсов пакета
	 * @return соответствующий сериализатор
	 */
	public static ResourceMetadataSerializer<PackOverlaysMetadata> getSerializerFor(ResourceType type) {
		return switch (type) {
			case CLIENT_RESOURCES -> CLIENT_RESOURCES_SERIALIZER;
			case SERVER_DATA -> SERVER_DATA_SERIALIZER;
		};
	}

	/**
	 * Возвращает список имён директорий оверлеев, активных для заданной версии формата.
	 *
	 * @param version текущая версия формата пакета
	 * @return список имён директорий оверлеев, чей диапазон версий содержит {@code version}
	 */
	public List<String> getAppliedOverlays(PackVersion version) {
		return overlays
				.stream()
				.filter(overlay -> overlay.isValid(version))
				.map(Entry::overlay)
				.toList();
	}

	/**
	 * Запись оверлея: связывает диапазон версий формата с именем директории.
	 *
	 * @param format диапазон версий формата, при которых оверлей активен
	 * @param overlay имя директории оверлея внутри пакета
	 */
	public record Entry(Range<PackVersion> format, String overlay) {

		static Codec<List<Entry>> createCodec(ResourceType type) {
			int lastOldVersion = PackVersion.getLastOldPackVersion(type);

			return Holder.CODEC
					.listOf()
					.flatXmap(
							holders -> PackVersion.validate(
									holders,
									lastOldVersion,
									(holder, versionRange) -> new Entry(versionRange, holder.overlay())
							),
							entries -> DataResult.success(
									entries.stream()
											.map(entry -> new Holder(
													PackVersion.Format.ofRange(entry.format(), lastOldVersion),
													entry.overlay()
											))
											.toList()
							)
					);
		}

		public boolean isValid(PackVersion version) {
			return format.contains(version);
		}

		/**
		 * Промежуточный держатель для сериализации оверлея через {@link PackVersion.Format}.
		 *
		 * <p>Используется при кодировании/декодировании, поскольку формат версии в JSON
		 * может быть представлен как одним числом, так и диапазоном.</p>
		 *
		 * @param format формат версии пакета
		 * @param overlay имя директории оверлея
		 */
		record Holder(PackVersion.Format format, String overlay) implements PackVersion.FormatHolder {

			static final Codec<Holder> CODEC = RecordCodecBuilder.create(
					instance -> instance.group(
							PackVersion.Format.OVERLAY_CODEC.forGetter(Holder::format),
							Codec.STRING
									.validate(PackOverlaysMetadata::validate)
									.fieldOf("directory")
									.forGetter(Holder::overlay)
					).apply(instance, Holder::new)
			);

			@Override
			public String toString() {
				return overlay;
			}
		}
	}
}
