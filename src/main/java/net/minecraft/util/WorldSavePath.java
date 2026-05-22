package net.minecraft.util;

/**
 * Именованные пути к стандартным директориям и файлам внутри папки сохранения мира.
 */
public class WorldSavePath {

	public static final WorldSavePath ADVANCEMENTS = new WorldSavePath("advancements");
	public static final WorldSavePath STATS = new WorldSavePath("stats");
	public static final WorldSavePath PLAYERDATA = new WorldSavePath("playerdata");
	public static final WorldSavePath PLAYERS = new WorldSavePath("players");
	public static final WorldSavePath LEVEL_DAT = new WorldSavePath("level.dat");
	public static final WorldSavePath LEVEL_DAT_OLD = new WorldSavePath("level.dat_old");
	public static final WorldSavePath ICON_PNG = new WorldSavePath("icon.png");
	public static final WorldSavePath SESSION_LOCK = new WorldSavePath("session.lock");
	public static final WorldSavePath GENERATED = new WorldSavePath("generated");
	public static final WorldSavePath DATAPACKS = new WorldSavePath("datapacks");
	public static final WorldSavePath RESOURCES_ZIP = new WorldSavePath("resources.zip");
	public static final WorldSavePath ROOT = new WorldSavePath(".");

	private final String relativePath;

	private WorldSavePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public String getRelativePath() {
		return relativePath;
	}

	@Override
	public String toString() {
		return "/" + relativePath;
	}
}
