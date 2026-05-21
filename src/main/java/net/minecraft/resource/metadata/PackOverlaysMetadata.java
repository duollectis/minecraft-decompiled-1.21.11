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
 * {@code PackOverlaysMetadata}.
 */
public record PackOverlaysMetadata(List<PackOverlaysMetadata.Entry> overlays) {

	private static final Pattern DIRECTORY_NAME_PATTERN = Pattern.compile("[-_a-zA-Z0-9.]+");
	public static final ResourceMetadataSerializer<PackOverlaysMetadata>
			CLIENT_RESOURCES_SERIALIZER =
			new ResourceMetadataSerializer<>(
					"overlays", createCodec(ResourceType.CLIENT_RESOURCES)
			);
	public static final ResourceMetadataSerializer<PackOverlaysMetadata>
			SERVER_DATA_SERIALIZER =
			new ResourceMetadataSerializer<>(
					"overlays", createCodec(ResourceType.SERVER_DATA)
			);

	private static DataResult<String> validate(String directoryName) {
		return !DIRECTORY_NAME_PATTERN.matcher(directoryName).matches()
		       ? DataResult.error(() -> directoryName + " is not accepted directory name")
		       : DataResult.success(directoryName);
	}

	@VisibleForTesting
	public static Codec<PackOverlaysMetadata> createCodec(ResourceType type) {
		return RecordCodecBuilder.create(
				instance -> instance
						.group(PackOverlaysMetadata.Entry
								.createCodec(type)
								.fieldOf("entries")
								.forGetter(PackOverlaysMetadata::overlays))
						.apply(instance, PackOverlaysMetadata::new)
		);
	}

	public static ResourceMetadataSerializer<PackOverlaysMetadata> getSerializerFor(ResourceType type) {
		return switch (type) {
			case CLIENT_RESOURCES -> CLIENT_RESOURCES_SERIALIZER;
			case SERVER_DATA -> SERVER_DATA_SERIALIZER;
		};
	}

	public List<String> getAppliedOverlays(PackVersion version) {
		return this.overlays
				.stream()
				.filter(overlay -> overlay.isValid(version))
				.map(PackOverlaysMetadata.Entry::overlay)
				.toList();
	}

	/**
	 * {@code Entry}.
	 */
	public record Entry(Range<PackVersion> format, String overlay) {

		static Codec<List<PackOverlaysMetadata.Entry>> createCodec(ResourceType type) {
			int i = PackVersion.getLastOldPackVersion(type);
			return PackOverlaysMetadata.Entry.Holder.CODEC
					.listOf()
					.flatXmap(
							holders -> PackVersion.validate(
									holders,
									i,
									(holder, versionRange) -> new PackOverlaysMetadata.Entry(
											versionRange,
											holder.overlay()
									)
							),
							entries -> DataResult.success(
									entries
											.stream()
											.map(entry -> new PackOverlaysMetadata.Entry.Holder(
													PackVersion.Format.ofRange(
															entry.format(),
															i
													), entry.overlay()
											))
											.toList()
							)
					);
		}

		public boolean isValid(PackVersion version) {
			return this.format.contains(version);
		}

		/**
		 * {@code Holder}.
		 */
		record Holder(PackVersion.Format format, String overlay) implements PackVersion.FormatHolder {

			static final Codec<PackOverlaysMetadata.Entry.Holder> CODEC = RecordCodecBuilder.create(
					instance -> instance.group(
							                    PackVersion.Format.OVERLAY_CODEC.forGetter(PackOverlaysMetadata.Entry.Holder::format),
							                    Codec.STRING
									                    .validate(PackOverlaysMetadata::validate)
									                    .fieldOf("directory")
									                    .forGetter(PackOverlaysMetadata.Entry.Holder::overlay)
					                    )
					                    .apply(instance, PackOverlaysMetadata.Entry.Holder::new)
			);

			@Override
			public String toString() {
				return this.overlay;
			}
		}
	}
}
