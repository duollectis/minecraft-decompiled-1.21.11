package net.minecraft.resource;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.dynamic.Range;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Версия пака в формате {@code major.minor}.
 * Используется для проверки совместимости ресурс-паков с текущей версией игры.
 */
public record PackVersion(int major, int minor) implements Comparable<PackVersion> {

	private static final Logger LOGGER = LogUtils.getLogger();
	/** Минимальная версия формата, начиная с которой поддерживаются мультиверсионные паки. */
	private static final int MIN_MULTI_VERSION_FORMAT = 15;

	public static final Codec<PackVersion> CODEC = createCodec(0);
	public static final Codec<PackVersion> ANY_CODEC = createCodec(Integer.MAX_VALUE);

	private static Codec<PackVersion> createCodec(int impliedMinorVersion) {
		return Codecs.listOrSingle(Codecs.NON_NEGATIVE_INT, Codecs.NON_NEGATIVE_INT.listOf(1, 256))
			.xmap(
				list -> list.size() > 1
					? of(list.getFirst(), list.get(1))
					: of(list.getFirst(), impliedMinorVersion),
				version -> version.minor != impliedMinorVersion
					? List.of(version.major(), version.minor())
					: List.of(version.major())
			);
	}

	/**
	 * Валидирует список держателей форматов и преобразует их в результирующие объекты.
	 * Возвращает ошибку, если хотя бы один держатель не прошёл валидацию.
	 *
	 * @param holders            список держателей форматов
	 * @param lastOldPackVersion последняя версия старого формата пака
	 * @param toResult           функция преобразования держателя в результат
	 * @return список результатов или ошибка
	 */
	public static <ResultType, HolderType extends PackVersion.FormatHolder> DataResult<List<ResultType>> validate(
		List<HolderType> holders,
		int lastOldPackVersion,
		BiFunction<HolderType, Range<PackVersion>, ResultType> toResult
	) {
		int minMajor = holders.stream()
			.map(PackVersion.FormatHolder::format)
			.mapToInt(PackVersion.Format::minMajor)
			.min()
			.orElse(Integer.MAX_VALUE);

		List<ResultType> results = new ArrayList<>(holders.size());

		for (HolderType holder : holders) {
			PackVersion.Format format = holder.format();
			if (format.min().isEmpty() && format.max().isEmpty() && format.supported().isEmpty()) {
				LOGGER.warn("Unknown or broken overlay entry {}", holder);
				continue;
			}

			DataResult<Range<PackVersion>> dataResult = format.validate(
				lastOldPackVersion,
				false,
				minMajor <= lastOldPackVersion,
				"Overlay \"" + holder + "\"",
				"formats"
			);

			if (!dataResult.isSuccess()) {
				return DataResult.error(((Error<?>) dataResult.error().get())::message);
			}

			results.add(toResult.apply(holder, dataResult.getOrThrow()));
		}

		return DataResult.success(List.copyOf(results));
	}

	@VisibleForTesting
	public static int getLastOldPackVersion(ResourceType type) {
		return switch (type) {
			case CLIENT_RESOURCES -> 64;
			case SERVER_DATA -> 81;
		};
	}

	/**
	 * Создаёт кодек для диапазона версий пака указанного типа.
	 *
	 * @param type тип ресурса
	 * @return кодек диапазона версий
	 */
	public static MapCodec<Range<PackVersion>> createRangeCodec(ResourceType type) {
		int lastOldVersion = getLastOldPackVersion(type);
		return PackVersion.Format.PACK_CODEC.flatXmap(
			format -> format.validate(lastOldVersion, true, false, "Pack", "supported_formats"),
			range -> DataResult.success(PackVersion.Format.ofRange(range, lastOldVersion))
		);
	}

	public static PackVersion of(int major, int minor) {
		return new PackVersion(major, minor);
	}

	public static PackVersion of(int major) {
		return new PackVersion(major, 0);
	}

	/**
	 * Возвращает диапазон, охватывающий все минорные версии данного мажора.
	 *
	 * @return диапазон {@code [major.0, major.MAX_VALUE]}
	 */
	public Range<PackVersion> majorRange() {
		return new Range<>(this, of(major, Integer.MAX_VALUE));
	}

	@Override
	public int compareTo(PackVersion other) {
		int majorCompare = Integer.compare(major(), other.major());
		return majorCompare != 0 ? majorCompare : Integer.compare(minor(), other.minor());
	}

	@Override
	public String toString() {
		return minor == Integer.MAX_VALUE
			? String.format(Locale.ROOT, "%d.*", major())
			: String.format(Locale.ROOT, "%d.%d", major(), minor());
	}

	/**
	 * Описание формата версии пака: диапазон поддерживаемых версий в различных представлениях.
	 * Используется при разборе {@code pack.mcmeta}.
	 */
	public record Format(
		Optional<PackVersion> min,
		Optional<PackVersion> max,
		Optional<Integer> format,
		Optional<Range<Integer>> supported
	) {

		static final MapCodec<PackVersion.Format> PACK_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				PackVersion.CODEC.optionalFieldOf("min_format").forGetter(PackVersion.Format::min),
				PackVersion.ANY_CODEC.optionalFieldOf("max_format").forGetter(PackVersion.Format::max),
				Codec.INT.optionalFieldOf("pack_format").forGetter(PackVersion.Format::format),
				Range.createCodec(Codec.INT).optionalFieldOf("supported_formats").forGetter(PackVersion.Format::supported)
			).apply(instance, PackVersion.Format::new)
		);

		public static final MapCodec<PackVersion.Format> OVERLAY_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				PackVersion.CODEC.optionalFieldOf("min_format").forGetter(PackVersion.Format::min),
				PackVersion.ANY_CODEC.optionalFieldOf("max_format").forGetter(PackVersion.Format::max),
				Range.createCodec(Codec.INT).optionalFieldOf("formats").forGetter(PackVersion.Format::supported)
			).apply(
				instance,
				(min, max, supported) -> new PackVersion.Format(min, max, min.map(PackVersion::major), supported)
			)
		);

		/**
		 * Создаёт описание формата из диапазона версий.
		 *
		 * @param range              диапазон версий
		 * @param lastOldPackVersion последняя версия старого формата
		 * @return описание формата
		 */
		public static PackVersion.Format ofRange(Range<PackVersion> range, int lastOldPackVersion) {
			Range<Integer> majorRange = range.map(PackVersion::major);
			boolean containsOld = majorRange.contains(lastOldPackVersion);
			return new PackVersion.Format(
				Optional.of(range.minInclusive()),
				Optional.of(range.maxInclusive()),
				containsOld ? Optional.of(majorRange.minInclusive()) : Optional.empty(),
				containsOld ? Optional.of(new Range<>(majorRange.minInclusive(), majorRange.maxInclusive()))
				            : Optional.empty()
			);
		}

		/**
		 * Возвращает минимальный мажор из всех представлений версии в этом формате.
		 *
		 * @return минимальный мажор или {@link Integer#MAX_VALUE}, если версия не задана
		 */
		public int minMajor() {
			if (min.isPresent()) {
				return supported.isPresent()
					? Math.min(min.get().major(), supported.get().minInclusive())
					: min.get().major();
			}

			return supported.isPresent() ? supported.get().minInclusive() : Integer.MAX_VALUE;
		}

		/**
		 * Валидирует описание формата и возвращает диапазон версий или ошибку.
		 *
		 * @param lastOldPackVersion последняя версия старого формата
		 * @param pack               {@code true}, если это основной пак (не оверлей)
		 * @param supportsOld        {@code true}, если пак должен поддерживать старые версии
		 * @param packDescriptor     описание пака для сообщений об ошибках
		 * @param supportedFormatsKey ключ поля поддерживаемых форматов
		 * @return диапазон версий или ошибка
		 */
		public DataResult<Range<PackVersion>> validate(
			int lastOldPackVersion,
			boolean pack,
			boolean supportsOld,
			String packDescriptor,
			String supportedFormatsKey
		) {
			if (min.isPresent() != max.isPresent()) {
				return DataResult.error(
					() -> packDescriptor + " missing field, must declare both min_format and max_format"
				);
			}

			if (supportsOld && supported.isEmpty()) {
				return DataResult.error(
					() -> packDescriptor
						+ " missing required field "
						+ supportedFormatsKey
						+ ", must be present in all overlays for any overlays to work across game versions"
				);
			}

			if (min.isPresent()) {
				return validateVersions(lastOldPackVersion, pack, supportsOld, packDescriptor, supportedFormatsKey);
			}

			if (supported.isPresent()) {
				return validateSupportedFormats(lastOldPackVersion, pack, packDescriptor, supportedFormatsKey);
			}

			if (pack && format.isPresent()) {
				int packFormat = format.get();
				return packFormat > lastOldPackVersion
					? DataResult.error(
						() -> packDescriptor
							+ " declares support for version newer than "
							+ lastOldPackVersion
							+ ", but is missing mandatory fields min_format and max_format"
					)
					: DataResult.success(new Range<>(PackVersion.of(packFormat)));
			}

			return DataResult.error(
				() -> packDescriptor + " could not be parsed, missing format version information"
			);
		}

		private DataResult<Range<PackVersion>> validateVersions(
			int lastOldPackVersion,
			boolean pack,
			boolean supportsOld,
			String packDescriptor,
			String supportedFormatsKey
		) {
			int minMajorVal = min.get().major();
			int maxMajorVal = max.get().major();

			if (min.get().compareTo(max.get()) > 0) {
				return DataResult.error(
					() -> packDescriptor + " min_format (" + min.get()
						+ ") is greater than max_format (" + max.get() + ")"
				);
			}

			if (minMajorVal > lastOldPackVersion && !supportsOld) {
				if (supported.isPresent()) {
					return DataResult.error(
						() -> packDescriptor
							+ " key "
							+ supportedFormatsKey
							+ " is deprecated starting from pack format "
							+ (lastOldPackVersion + 1)
							+ ". Remove "
							+ supportedFormatsKey
							+ " from your pack.mcmeta."
					);
				}

				if (pack && format.isPresent()) {
					String error = validateMainFormat(minMajorVal, maxMajorVal);
					if (error != null) {
						return DataResult.error(() -> error);
					}
				}
			} else {
				if (supported.isEmpty()) {
					return DataResult.error(
						() -> packDescriptor
							+ " declares support for format "
							+ minMajorVal
							+ ", but game versions supporting formats 17 to "
							+ lastOldPackVersion
							+ " require a "
							+ supportedFormatsKey
							+ " field. Add \""
							+ supportedFormatsKey
							+ "\": ["
							+ minMajorVal
							+ ", "
							+ lastOldPackVersion
							+ "] or require a version greater or equal to "
							+ (lastOldPackVersion + 1)
							+ ".0."
					);
				}

				Range<Integer> supportedRange = supported.get();
				if (supportedRange.minInclusive() != minMajorVal) {
					return DataResult.error(
						() -> packDescriptor
							+ " version declaration mismatch between "
							+ supportedFormatsKey
							+ " (from "
							+ supportedRange.minInclusive()
							+ ") and min_format ("
							+ min.get()
							+ ")"
					);
				}

				if (supportedRange.maxInclusive() != maxMajorVal
					&& supportedRange.maxInclusive() != lastOldPackVersion) {
					return DataResult.error(
						() -> packDescriptor
							+ " version declaration mismatch between "
							+ supportedFormatsKey
							+ " (up to "
							+ supportedRange.maxInclusive()
							+ ") and max_format ("
							+ max.get()
							+ ")"
					);
				}

				if (pack) {
					if (format.isEmpty()) {
						return DataResult.error(
							() -> packDescriptor
								+ " declares support for formats up to "
								+ lastOldPackVersion
								+ ", but game versions supporting formats 17 to "
								+ lastOldPackVersion
								+ " require a pack_format field. Add \"pack_format\": "
								+ minMajorVal
								+ " or require a version greater or equal to "
								+ (lastOldPackVersion + 1)
								+ ".0."
						);
					}

					String error = validateMainFormat(minMajorVal, maxMajorVal);
					if (error != null) {
						return DataResult.error(() -> error);
					}
				}
			}

			return DataResult.success(new Range<>(min.get(), max.get()));
		}

		private DataResult<Range<PackVersion>> validateSupportedFormats(
			int lastOldPackVersion,
			boolean pack,
			String packDescriptor,
			String supportedFormatsKey
		) {
			Range<Integer> supportedRange = supported.get();
			int minVal = supportedRange.minInclusive();
			int maxVal = supportedRange.maxInclusive();

			if (maxVal > lastOldPackVersion) {
				return DataResult.error(
					() -> packDescriptor
						+ " declares support for version newer than "
						+ lastOldPackVersion
						+ ", but is missing mandatory fields min_format and max_format"
				);
			}

			if (pack) {
				if (format.isEmpty()) {
					return DataResult.error(
						() -> packDescriptor
							+ " declares support for formats up to "
							+ lastOldPackVersion
							+ ", but game versions supporting formats 17 to "
							+ lastOldPackVersion
							+ " require a pack_format field. Add \"pack_format\": "
							+ minVal
							+ " or require a version greater or equal to "
							+ (lastOldPackVersion + 1)
							+ ".0."
					);
				}

				String error = validateMainFormat(minVal, maxVal);
				if (error != null) {
					return DataResult.error(() -> error);
				}
			}

			return DataResult.success(new Range<>(minVal, maxVal).map(PackVersion::of));
		}

		private @Nullable String validateMainFormat(int min, int max) {
			int packFormat = format.get();
			if (packFormat < min || packFormat > max) {
				return "Pack declared support for versions " + min + " to " + max
					+ " but declared main format is " + packFormat;
			}

			return packFormat < MIN_MULTI_VERSION_FORMAT
				? "Multi-version packs cannot support minimum version of less than "
					+ MIN_MULTI_VERSION_FORMAT
					+ ", since this will leave versions in range unable to load pack."
				: null;
		}
	}

	/**
	 * Держатель формата версии пака. Реализуется записями, хранящими метаданные оверлея.
	 */
	public interface FormatHolder {

		PackVersion.Format format();
	}
}
