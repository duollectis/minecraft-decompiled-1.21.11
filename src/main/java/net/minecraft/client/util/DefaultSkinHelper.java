package net.minecraft.client.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

import java.util.UUID;

@Environment(EnvType.CLIENT)
/**
 * {@code DefaultSkinHelper}.
 */
public class DefaultSkinHelper {

	private static final SkinTextures[] SKINS = new SkinTextures[]{
			createSkinTextures("entity/player/slim/alex", PlayerSkinType.SLIM),
			createSkinTextures("entity/player/slim/ari", PlayerSkinType.SLIM),
			createSkinTextures("entity/player/slim/efe", PlayerSkinType.SLIM),
			createSkinTextures("entity/player/slim/kai", PlayerSkinType.SLIM),
			createSkinTextures("entity/player/slim/makena", PlayerSkinType.SLIM),
			createSkinTextures("entity/player/slim/noor", PlayerSkinType.SLIM),
			createSkinTextures("entity/player/slim/steve", PlayerSkinType.SLIM),
			createSkinTextures("entity/player/slim/sunny", PlayerSkinType.SLIM),
			createSkinTextures("entity/player/slim/zuri", PlayerSkinType.SLIM),
			createSkinTextures("entity/player/wide/alex", PlayerSkinType.WIDE),
			createSkinTextures("entity/player/wide/ari", PlayerSkinType.WIDE),
			createSkinTextures("entity/player/wide/efe", PlayerSkinType.WIDE),
			createSkinTextures("entity/player/wide/kai", PlayerSkinType.WIDE),
			createSkinTextures("entity/player/wide/makena", PlayerSkinType.WIDE),
			createSkinTextures("entity/player/wide/noor", PlayerSkinType.WIDE),
			createSkinTextures("entity/player/wide/steve", PlayerSkinType.WIDE),
			createSkinTextures("entity/player/wide/sunny", PlayerSkinType.WIDE),
			createSkinTextures("entity/player/wide/zuri", PlayerSkinType.WIDE)
	};

	public static Identifier getTexture() {
		return getSteve().body().texturePath();
	}

	public static SkinTextures getSteve() {
		return SKINS[6];
	}

	public static SkinTextures getSkinTextures(UUID uuid) {
		return SKINS[Math.floorMod(uuid.hashCode(), SKINS.length)];
	}

	public static SkinTextures getSkinTextures(GameProfile profile) {
		return getSkinTextures(profile.id());
	}

	private static SkinTextures createSkinTextures(String texture, PlayerSkinType type) {
		return new SkinTextures(new AssetInfo.TextureAssetInfo(Identifier.ofVanilla(texture)), null, null, type, true);
	}
}
