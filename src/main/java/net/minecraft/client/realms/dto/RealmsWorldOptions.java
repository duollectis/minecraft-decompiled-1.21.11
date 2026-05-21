package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.CheckedGson;
import net.minecraft.client.realms.RealmsSerializable;
import net.minecraft.client.realms.util.DontSerialize;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.StringHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.level.LevelInfo;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsWorldOptions}.
 */
public class RealmsWorldOptions extends ValueObject implements RealmsSerializable {

	@SerializedName("spawnProtection")
	public int spawnProtection = 0;
	@SerializedName("forceGameMode")
	public boolean forceGameMode = false;
	@SerializedName("difficulty")
	public int difficulty = 2;
	@SerializedName("gameMode")
	public int gameMode = 0;
	@SerializedName("slotName")
	private String slotName = "";
	@SerializedName("version")
	public String version = "";
	@SerializedName("compatibility")
	public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
	@SerializedName("worldTemplateId")
	public long templateId = -1L;
	@SerializedName("worldTemplateImage")
	public @Nullable String templateImage = null;
	@DontSerialize
	public boolean empty;

	private RealmsWorldOptions() {
	}

	public RealmsWorldOptions(
			int i,
			int j,
			int spawnProtection,
			boolean bl,
			String string,
			String string2,
			RealmsServer.Compatibility compatibility
	) {
		this.spawnProtection = i;
		this.difficulty = j;
		this.gameMode = spawnProtection;
		this.forceGameMode = bl;
		this.slotName = string;
		this.version = string2;
		this.compatibility = compatibility;
	}

	public static RealmsWorldOptions getDefaults() {
		return new RealmsWorldOptions();
	}

	public static RealmsWorldOptions create(
			GameMode gameMode,
			Difficulty difficulty,
			boolean bl,
			String string,
			String string2
	) {
		RealmsWorldOptions realmsWorldOptions = getDefaults();
		realmsWorldOptions.difficulty = difficulty.getId();
		realmsWorldOptions.gameMode = gameMode.getIndex();
		realmsWorldOptions.slotName = string2;
		realmsWorldOptions.version = string;
		return realmsWorldOptions;
	}

	/**
	 * Create.
	 *
	 * @param levelInfo level info
	 * @param string string
	 *
	 * @return RealmsWorldOptions — результат операции
	 */
	public static RealmsWorldOptions create(LevelInfo levelInfo, String string) {
		return create(
				levelInfo.getGameMode(),
				levelInfo.getDifficulty(),
				levelInfo.isHardcore(),
				string,
				levelInfo.getLevelName()
		);
	}

	public static RealmsWorldOptions getEmptyDefaults() {
		RealmsWorldOptions realmsWorldOptions = getDefaults();
		realmsWorldOptions.setEmpty(true);
		return realmsWorldOptions;
	}

	public void setEmpty(boolean empty) {
		this.empty = empty;
	}

	/**
	 * From json.
	 *
	 * @param gson gson
	 * @param json json
	 *
	 * @return RealmsWorldOptions — результат операции
	 */
	public static RealmsWorldOptions fromJson(CheckedGson gson, String json) {
		RealmsWorldOptions realmsWorldOptions = gson.fromJson(json, RealmsWorldOptions.class);
		if (realmsWorldOptions == null) {
			return getDefaults();
		}
		else {
			replaceNullsWithDefaults(realmsWorldOptions);
			return realmsWorldOptions;
		}
	}

	private static void replaceNullsWithDefaults(RealmsWorldOptions options) {
		if (options.slotName == null) {
			options.slotName = "";
		}

		if (options.version == null) {
			options.version = "";
		}

		if (options.compatibility == null) {
			options.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
		}
	}

	public String getSlotName(int index) {
		if (StringHelper.isBlank(this.slotName)) {
			return this.empty ? I18n.translate("mco.configure.world.slot.empty") : this.getDefaultSlotName(index);
		}
		else {
			return this.slotName;
		}
	}

	public String getDefaultSlotName(int index) {
		return I18n.translate("mco.configure.world.slot", index);
	}

	/**
	 * Copy.
	 *
	 * @return RealmsWorldOptions — результат операции
	 */
	public RealmsWorldOptions copy() {
		return new RealmsWorldOptions(
				this.spawnProtection,
				this.difficulty,
				this.gameMode,
				this.forceGameMode,
				this.slotName,
				this.version,
				this.compatibility
		);
	}
}
