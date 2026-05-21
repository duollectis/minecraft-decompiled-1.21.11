package net.minecraft.client.realms.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code UploadTokenCache}.
 */
public class UploadTokenCache {

	private static final Long2ObjectMap<String> TOKEN_CACHE = new Long2ObjectOpenHashMap();

	/**
	 * Get.
	 *
	 * @param worldId world id
	 *
	 * @return String — 
	 */
	public static String get(long worldId) {
		return (String) TOKEN_CACHE.get(worldId);
	}

	/**
	 * Invalidate.
	 *
	 * @param world world
	 */
	public static void invalidate(long world) {
		TOKEN_CACHE.remove(world);
	}

	/**
	 * Put.
	 *
	 * @param wid wid
	 * @param token token
	 */
	public static void put(long wid, @Nullable String token) {
		TOKEN_CACHE.put(wid, token);
	}
}
