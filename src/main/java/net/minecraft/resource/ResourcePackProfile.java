package net.minecraft.resource;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.resource.metadata.PackFeatureSetMetadata;
import net.minecraft.resource.metadata.PackOverlaysMetadata;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Function;

/**
 * Профиль ресурс-пака: объединяет метаданные, фабрику и позицию пака в списке.
 * Создаётся через {@link #create} с автоматическим чтением метаданных из {@code pack.mcmeta}.
 */
public class ResourcePackProfile {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final ResourcePackInfo info;
	private final ResourcePackProfile.PackFactory packFactory;
	private final ResourcePackProfile.Metadata metaData;
	private final ResourcePackPosition position;

	/**
	 * Создаёт профиль пака, читая метаданные через фабрику.
	 * Возвращает {@code null}, если метаданные не удалось прочитать.
	 *
	 * @param info        информация о паке
	 * @param packFactory фабрика для открытия пака
	 * @param type        тип ресурса
	 * @param position    позиция пака в списке
	 * @return профиль пака или {@code null}
	 */
	public static @Nullable ResourcePackProfile create(
		ResourcePackInfo info,
		ResourcePackProfile.PackFactory packFactory,
		ResourceType type,
		ResourcePackPosition position
	) {
		PackVersion packVersion = SharedConstants.getGameVersion().packVersion(type);
		ResourcePackProfile.Metadata metadata = loadMetadata(info, packFactory, packVersion, type);
		return metadata != null ? new ResourcePackProfile(info, packFactory, metadata, position) : null;
	}

	public ResourcePackProfile(
		ResourcePackInfo info,
		ResourcePackProfile.PackFactory packFactory,
		ResourcePackProfile.Metadata metaData,
		ResourcePackPosition position
	) {
		this.info = info;
		this.packFactory = packFactory;
		this.metaData = metaData;
		this.position = position;
	}

	/**
	 * Загружает метаданные пака из {@code pack.mcmeta} через фабрику.
	 * Пробует сначала типизированный сериализатор, затем универсальный (только описание).
	 *
	 * @param info        информация о паке
	 * @param packFactory фабрика для открытия пака
	 * @param version     текущая версия пака игры
	 * @param type        тип ресурса
	 * @return метаданные или {@code null} при ошибке
	 */
	public static ResourcePackProfile.@Nullable Metadata loadMetadata(
		ResourcePackInfo info,
		ResourcePackProfile.PackFactory packFactory,
		PackVersion version,
		ResourceType type
	) {
		try (ResourcePack resourcePack = packFactory.open(info)) {
			PackResourceMetadata packMeta = resourcePack.parseMetadata(PackResourceMetadata.getSerializerFor(type));
			if (packMeta == null) {
				packMeta = resourcePack.parseMetadata(PackResourceMetadata.DESCRIPTION_SERIALIZER);
			}

			if (packMeta == null) {
				LOGGER.warn("Missing metadata in pack {}", info.id());
				return null;
			}

			PackFeatureSetMetadata featureSetMeta = resourcePack.parseMetadata(PackFeatureSetMetadata.SERIALIZER);
			FeatureSet featureSet = featureSetMeta != null ? featureSetMeta.flags() : FeatureSet.empty();
			ResourcePackCompatibility compatibility = ResourcePackCompatibility.from(
				packMeta.supportedFormats(),
				version
			);
			PackOverlaysMetadata overlaysMeta = resourcePack.parseMetadata(PackOverlaysMetadata.getSerializerFor(type));
			List<String> overlays = overlaysMeta != null ? overlaysMeta.getAppliedOverlays(version) : List.of();

			return new ResourcePackProfile.Metadata(
				packMeta.description(),
				compatibility,
				featureSet,
				overlays
			);
		} catch (Exception exception) {
			LOGGER.warn("Failed to read pack {} metadata", info.id(), exception);
			return null;
		}
	}

	public ResourcePackInfo getInfo() {
		return info;
	}

	public Text getDisplayName() {
		return info.title();
	}

	public Text getDescription() {
		return metaData.description();
	}

	public Text getInformationText(boolean enabled) {
		return info.getInformationText(enabled, metaData.description);
	}

	public ResourcePackCompatibility getCompatibility() {
		return metaData.compatibility();
	}

	public FeatureSet getRequestedFeatures() {
		return metaData.requestedFeatures();
	}

	public ResourcePack createResourcePack() {
		return packFactory.openWithOverlays(info, metaData);
	}

	public String getId() {
		return info.id();
	}

	public ResourcePackPosition getPosition() {
		return position;
	}

	public boolean isRequired() {
		return position.required();
	}

	public boolean isPinned() {
		return position.fixedPosition();
	}

	public ResourcePackProfile.InsertionPosition getInitialPosition() {
		return position.defaultPosition();
	}

	public ResourcePackSource getSource() {
		return info.source();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof ResourcePackProfile other && info.equals(other.info);
	}

	@Override
	public int hashCode() {
		return info.hashCode();
	}

	/**
	 * Позиция вставки пака в список: сверху или снизу.
	 * Учитывает закреплённые паки при определении индекса вставки.
	 */
	public enum InsertionPosition {
		TOP,
		BOTTOM;

		/**
		 * Вставляет элемент в список с учётом закреплённых паков и возможной инверсии.
		 *
		 * @param items          список паков
		 * @param item           вставляемый элемент
		 * @param profileGetter  функция получения позиции из элемента
		 * @param listInverted   инвертировать ли позицию вставки
		 * @return индекс вставки
		 */
		public <T> int insert(
			List<T> items,
			T item,
			Function<T, ResourcePackPosition> profileGetter,
			boolean listInverted
		) {
			InsertionPosition effectivePosition = listInverted ? inverse() : this;
			int insertIndex;

			if (effectivePosition == BOTTOM) {
				for (insertIndex = 0; insertIndex < items.size(); insertIndex++) {
					ResourcePackPosition pos = profileGetter.apply(items.get(insertIndex));
					if (!pos.fixedPosition() || pos.defaultPosition() != this) {
						break;
					}
				}
			} else {
				for (insertIndex = items.size() - 1; insertIndex >= 0; insertIndex--) {
					ResourcePackPosition pos = profileGetter.apply(items.get(insertIndex));
					if (!pos.fixedPosition() || pos.defaultPosition() != this) {
						break;
					}
				}

				insertIndex++;
			}

			items.add(insertIndex, item);
			return insertIndex;
		}

		public InsertionPosition inverse() {
			return this == TOP ? BOTTOM : TOP;
		}
	}

	/**
	 * Метаданные профиля пака: описание, совместимость, фичи и список оверлеев.
	 */
	public record Metadata(
		Text description,
		ResourcePackCompatibility compatibility,
		FeatureSet requestedFeatures,
		List<String> overlays
	) {}

	/**
	 * Фабрика для открытия ресурс-пака с поддержкой оверлеев.
	 */
	public interface PackFactory {

		ResourcePack open(ResourcePackInfo info);

		ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata);
	}
}
