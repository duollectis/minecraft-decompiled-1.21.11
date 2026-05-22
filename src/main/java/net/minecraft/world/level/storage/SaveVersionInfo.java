package net.minecraft.world.level.storage;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.SaveVersion;
import net.minecraft.SharedConstants;

/**
 * Метаданные версии сохранения: формат уровня, время последней игры,
 * имя версии игры и идентификатор датаверсии. Используется для определения
 * необходимости конвертации мира и отображения информации в списке миров.
 */
public class SaveVersionInfo {

	private final int levelFormatVersion;
	private final long lastPlayed;
	private final String versionName;
	private final SaveVersion version;
	private final boolean stable;

	private SaveVersionInfo(
		int levelFormatVersion,
		long lastPlayed,
		String versionName,
		int versionId,
		String series,
		boolean stable
	) {
		this.levelFormatVersion = levelFormatVersion;
		this.lastPlayed = lastPlayed;
		this.versionName = versionName;
		this.version = new SaveVersion(versionId, series);
		this.stable = stable;
	}

	/**
	 * Десериализует метаданные версии из NBT-совместимого {@link Dynamic}.
	 * Если блок {@code Version} отсутствует (очень старые сохранения),
	 * возвращает экземпляр с нулевым идентификатором версии и пустым именем.
	 *
	 * @param dynamic динамическое представление данных уровня
	 * @return десериализованный экземпляр {@code SaveVersionInfo}
	 */
	public static SaveVersionInfo fromDynamic(Dynamic<?> dynamic) {
		int levelFormatVersion = dynamic.get("version").asInt(0);
		long lastPlayed = dynamic.get("LastPlayed").asLong(0L);
		OptionalDynamic<?> versionDynamic = dynamic.get("Version");

		return versionDynamic.result().isPresent()
			? new SaveVersionInfo(
				levelFormatVersion,
				lastPlayed,
				versionDynamic.get("Name").asString(SharedConstants.getGameVersion().name()),
				versionDynamic.get("Id").asInt(SharedConstants.getGameVersion().dataVersion().id()),
				versionDynamic.get("Series").asString("main"),
				versionDynamic.get("Snapshot").asBoolean(!SharedConstants.getGameVersion().stable())
			)
			: new SaveVersionInfo(levelFormatVersion, lastPlayed, "", 0, "main", false);
	}

	public int getLevelFormatVersion() {
		return levelFormatVersion;
	}

	public long getLastPlayed() {
		return lastPlayed;
	}

	public String getVersionName() {
		return versionName;
	}

	public SaveVersion getVersion() {
		return version;
	}

	public boolean isStable() {
		return stable;
	}
}
