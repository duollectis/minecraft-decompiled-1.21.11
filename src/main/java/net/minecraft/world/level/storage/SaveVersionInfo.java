package net.minecraft.world.level.storage;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.SaveVersion;
import net.minecraft.SharedConstants;

/**
 * {@code SaveVersionInfo}.
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
	 * From dynamic.
	 *
	 * @param dynamic dynamic
	 *
	 * @return SaveVersionInfo — результат операции
	 */
	public static SaveVersionInfo fromDynamic(Dynamic<?> dynamic) {
		int i = dynamic.get("version").asInt(0);
		long l = dynamic.get("LastPlayed").asLong(0L);
		OptionalDynamic<?> optionalDynamic = dynamic.get("Version");
		return optionalDynamic.result().isPresent()
		       ? new SaveVersionInfo(
				i,
				l,
				optionalDynamic.get("Name").asString(SharedConstants.getGameVersion().name()),
				optionalDynamic.get("Id").asInt(SharedConstants.getGameVersion().dataVersion().id()),
				optionalDynamic.get("Series").asString("main"),
				optionalDynamic.get("Snapshot").asBoolean(!SharedConstants.getGameVersion().stable())
		)
		       : new SaveVersionInfo(i, l, "", 0, "main", false);
	}

	public int getLevelFormatVersion() {
		return this.levelFormatVersion;
	}

	public long getLastPlayed() {
		return this.lastPlayed;
	}

	public String getVersionName() {
		return this.versionName;
	}

	public SaveVersion getVersion() {
		return this.version;
	}

	public boolean isStable() {
		return this.stable;
	}
}
