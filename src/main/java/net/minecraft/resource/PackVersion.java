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
 * {@code PackVersion}.
 */
public record PackVersion(int major, int minor) implements Comparable<PackVersion> {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Codec<PackVersion> CODEC = createCodec(0);
	public static final Codec<PackVersion> ANY_CODEC = createCodec(Integer.MAX_VALUE);

	private static Codec<PackVersion> createCodec(int impliedMinorVersion) {
		return Codecs.listOrSingle(Codecs.NON_NEGATIVE_INT, Codecs.NON_NEGATIVE_INT.listOf(1, 256))
		             .xmap(
				             list -> list.size() > 1 ? of((Integer) list.getFirst(), (Integer) list.get(1))
				                                     : of((Integer) list.getFirst(), impliedMinorVersion),
				             version -> version.minor != impliedMinorVersion ? List.of(version.major(), version.minor())
				                                                             : List.of(version.major())
		             );
	}

	public static <ResultType, HolderType extends PackVersion.FormatHolder> DataResult<List<ResultType>> validate(
			List<HolderType> holders,
			int lastOldPackVersion,
			BiFunction<HolderType, Range<PackVersion>, ResultType> toResult
	) {
		int
				i =
				holders
						.stream()
						.map(PackVersion.FormatHolder::format)
						.mapToInt(PackVersion.Format::minMajor)
						.min()
						.orElse(Integer.MAX_VALUE);
		List<ResultType> list = new ArrayList<>(holders.size());

		for (HolderType formatHolder : holders) {
			PackVersion.Format format = formatHolder.format();
			if (format.min().isEmpty() && format.max().isEmpty() && format.supported().isEmpty()) {
				LOGGER.warn("Unknown or broken overlay entry {}", formatHolder);
			}
			else {
				DataResult<Range<PackVersion>> dataResult = format.validate(
						lastOldPackVersion,
						false,
						i <= lastOldPackVersion,
						"Overlay \"" + formatHolder + "\"",
						"formats"
				);
				if (!dataResult.isSuccess()) {
					return DataResult.error(((Error) dataResult.error().get())::message);
				}

				list.add(toResult.apply(formatHolder, (Range<PackVersion>) dataResult.getOrThrow()));
			}
		}

		return DataResult.success(List.copyOf(list));
	}

	@VisibleForTesting
	public static int getLastOldPackVersion(ResourceType type) {
		return switch (type) {
			case CLIENT_RESOURCES -> 64;
			case SERVER_DATA -> 81;
		};
	}

	public static MapCodec<Range<PackVersion>> createRangeCodec(ResourceType type) {
		int i = getLastOldPackVersion(type);
		return PackVersion.Format.PACK_CODEC
				.flatXmap(
						format -> format.validate(i, true, false, "Pack", "supported_formats"),
						range -> DataResult.success(PackVersion.Format.ofRange(range, i))
				);
	}

	public static PackVersion of(int major, int minor) {
		return new PackVersion(major, minor);
	}

	public static PackVersion of(int major) {
		return new PackVersion(major, 0);
	}

	public Range<PackVersion> majorRange() {
		return new Range<>(this, of(this.major, Integer.MAX_VALUE));
	}

	public int compareTo(PackVersion packVersion) {
		int i = Integer.compare(this.major(), packVersion.major());
		return i != 0 ? i : Integer.compare(this.minor(), packVersion.minor());
	}

	@Override
	public String toString() {
		return this.minor == Integer.MAX_VALUE
		       ? String.format(Locale.ROOT, "%d.*", this.major())
		       : String.format(Locale.ROOT, "%d.%d", this.major(), this.minor());
	}

	/**
	 * {@code Format}.
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
						                    Range
								                    .createCodec(Codec.INT)
								                    .optionalFieldOf("supported_formats")
								                    .forGetter(PackVersion.Format::supported)
				                    )
				                    .apply(instance, PackVersion.Format::new)
		);
		public static final MapCodec<PackVersion.Format> OVERLAY_CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    PackVersion.CODEC.optionalFieldOf("min_format").forGetter(PackVersion.Format::min),
						                    PackVersion.ANY_CODEC.optionalFieldOf("max_format").forGetter(PackVersion.Format::max),
						                    Range.createCodec(Codec.INT).optionalFieldOf("formats").forGetter(PackVersion.Format::supported)
				                    )
				                    .apply(
						                    instance,
						                    (min, max, supported) -> new PackVersion.Format(
								                    min,
								                    max,
								                    min.map(PackVersion::major),
								                    supported
						                    )
				                    )
		);

		public static PackVersion.Format ofRange(Range<PackVersion> range, int lastOldPackVersion) {
			Range<Integer> range2 = range.map(PackVersion::major);
			return new PackVersion.Format(
					Optional.of(range.minInclusive()),
					Optional.of(range.maxInclusive()),
					range2.contains(lastOldPackVersion) ? Optional.of(range2.minInclusive()) : Optional.empty(),
					range2.contains(lastOldPackVersion) ? Optional.of(new Range<>(
							range2.minInclusive(),
							range2.maxInclusive()
					)) : Optional.empty()
			);
		}

		public int minMajor() {
			if (this.min.isPresent()) {
				return this.supported.isPresent() ? Math.min(
						this.min.get().major(),
						this.supported.get().minInclusive()
				) : this.min.get().major();
			}
			else {
				return this.supported.isPresent() ? this.supported.get().minInclusive() : Integer.MAX_VALUE;
			}
		}

		public DataResult<Range<PackVersion>> validate(
				int lastOldPackVersion,
				boolean pack,
				boolean supportsOld,
				String packDescriptor,
				String supportedFormatsKey
		) {
			if (this.min.isPresent() != this.max.isPresent()) {
				return DataResult.error(() -> packDescriptor
						+ " missing field, must declare both min_format and max_format");
			}
			else if (supportsOld && this.supported.isEmpty()) {
				return DataResult.error(
						() -> packDescriptor
								+ " missing required field "
								+ supportedFormatsKey
								+ ", must be present in all overlays for any overlays to work across game versions"
				);
			}
			else if (this.min.isPresent()) {
				return this.validateVersions(
						lastOldPackVersion,
						pack,
						supportsOld,
						packDescriptor,
						supportedFormatsKey
				);
			}
			else if (this.supported.isPresent()) {
				return this.validateSupportedFormats(lastOldPackVersion, pack, packDescriptor, supportedFormatsKey);
			}
			else if (pack && this.format.isPresent()) {
				int i = this.format.get();
				return i > lastOldPackVersion
				       ? DataResult.error(
						() -> packDescriptor
						      + " declares support for version newer than "
						      + lastOldPackVersion
						      + ", but is missing mandatory fields min_format and max_format"
				)
				       : DataResult.success(new Range<>(PackVersion.of(i)));
			}
			else {
				return DataResult.error(() -> packDescriptor
						+ " could not be parsed, missing format version information");
			}
		}

		private DataResult<Range<PackVersion>> validateVersions(
				int lastOldPackVersion,
				boolean pack,
				boolean supportsOld,
				String packDescriptor,
				String supportedFormatsKey
		) {
			int i = this.min.get().major();
			int j = this.max.get().major();
			if (this.min.get().compareTo(this.max.get()) > 0) {
				return DataResult.error(() -> packDescriptor + " min_format (" + this.min.get()
						+ ") is greater than max_format (" + this.max.get() + ")");
			}
			else {
				if (i > lastOldPackVersion && !supportsOld) {
					if (this.supported.isPresent()) {
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

					if (pack && this.format.isPresent()) {
						String string = this.validateMainFormat(i, j);
						if (string != null) {
							return DataResult.error(() -> string);
						}
					}
				}
				else {
					if (!this.supported.isPresent()) {
						return DataResult.error(
								() -> packDescriptor
										+ " declares support for format "
										+ i
										+ ", but game versions supporting formats 17 to "
										+ lastOldPackVersion
										+ " require a "
										+ supportedFormatsKey
										+ " field. Add \""
										+ supportedFormatsKey
										+ "\": ["
										+ i
										+ ", "
										+ lastOldPackVersion
										+ "] or require a version greater or equal to "
										+ (lastOldPackVersion + 1)
										+ ".0."
						);
					}

					Range<Integer> range = this.supported.get();
					if (range.minInclusive() != i) {
						return DataResult.error(
								() -> packDescriptor
										+ " version declaration mismatch between "
										+ supportedFormatsKey
										+ " (from "
										+ range.minInclusive()
										+ ") and min_format ("
										+ this.min.get()
										+ ")"
						);
					}

					if (range.maxInclusive() != j && range.maxInclusive() != lastOldPackVersion) {
						return DataResult.error(
								() -> packDescriptor
										+ " version declaration mismatch between "
										+ supportedFormatsKey
										+ " (up to "
										+ range.maxInclusive()
										+ ") and max_format ("
										+ this.max.get()
										+ ")"
						);
					}

					if (pack) {
						if (!this.format.isPresent()) {
							return DataResult.error(
									() -> packDescriptor
											+ " declares support for formats up to "
											+ lastOldPackVersion
											+ ", but game versions supporting formats 17 to "
											+ lastOldPackVersion
											+ " require a pack_format field. Add \"pack_format\": "
											+ i
											+ " or require a version greater or equal to "
											+ (lastOldPackVersion + 1)
											+ ".0."
							);
						}

						String string = this.validateMainFormat(i, j);
						if (string != null) {
							return DataResult.error(() -> string);
						}
					}
				}

				return DataResult.success(new Range<>(this.min.get(), this.max.get()));
			}
		}

		private DataResult<Range<PackVersion>> validateSupportedFormats(
				int lastOldPackVersion,
				boolean pack,
				String packDescriptor,
				String supportedFormatsKey
		) {
			Range<Integer> range = this.supported.get();
			int i = range.minInclusive();
			int j = range.maxInclusive();
			if (j > lastOldPackVersion) {
				return DataResult.error(
						() -> packDescriptor
								+ " declares support for version newer than "
								+ lastOldPackVersion
								+ ", but is missing mandatory fields min_format and max_format"
				);
			}
			else {
				if (pack) {
					if (!this.format.isPresent()) {
						return DataResult.error(
								() -> packDescriptor
										+ " declares support for formats up to "
										+ lastOldPackVersion
										+ ", but game versions supporting formats 17 to "
										+ lastOldPackVersion
										+ " require a pack_format field. Add \"pack_format\": "
										+ i
										+ " or require a version greater or equal to "
										+ (lastOldPackVersion + 1)
										+ ".0."
						);
					}

					String string = this.validateMainFormat(i, j);
					if (string != null) {
						return DataResult.error(() -> string);
					}
				}

				return DataResult.success(new Range<>(i, j).map(PackVersion::of));
			}
		}

		private @Nullable String validateMainFormat(int min, int max) {
			int i = this.format.get();
			if (i < min || i > max) {
				return "Pack declared support for versions " + min + " to " + max + " but declared main format is " + i;
			}
			else {
				return i < 15
				       ? "Multi-version packs cannot support minimum version of less than 15, since this will leave versions in range unable to load pack."
				       : null;
			}
		}
	}

	/**
	 * {@code FormatHolder}.
	 */
	public interface FormatHolder {

		PackVersion.Format format();
	}
}
