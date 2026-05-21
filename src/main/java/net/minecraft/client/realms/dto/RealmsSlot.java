package net.minecraft.client.realms.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.CheckedGson;
import net.minecraft.client.realms.RealmsSerializable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsSlot}.
 */
public final class RealmsSlot implements RealmsSerializable {

	@SerializedName("slotId")
	public int slotId;
	@SerializedName("options")
	@JsonAdapter(RealmsSlot.OptionsTypeAdapter.class)
	public RealmsWorldOptions options;
	@SerializedName("settings")
	public List<RealmsSettingDto> settings;

	public RealmsSlot(int slotId, RealmsWorldOptions options, List<RealmsSettingDto> settings) {
		this.slotId = slotId;
		this.options = options;
		this.settings = settings;
	}

	public static RealmsSlot create(int slotId) {
		return new RealmsSlot(
				slotId,
				RealmsWorldOptions.getEmptyDefaults(),
				List.of(RealmsSettingDto.ofHardcore(false))
		);
	}

	public RealmsSlot copy() {
		return new RealmsSlot(this.slotId, this.options.copy(), new ArrayList<>(this.settings));
	}

	public boolean isHardcore() {
		return RealmsSettingDto.isHardcore(this.settings);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code OptionsTypeAdapter}.
	 */
	static class OptionsTypeAdapter extends TypeAdapter<RealmsWorldOptions> {

		private OptionsTypeAdapter() {
		}

		public void write(JsonWriter jsonWriter, RealmsWorldOptions realmsWorldOptions) throws IOException {
			jsonWriter.jsonValue(new CheckedGson().toJson(realmsWorldOptions));
		}

		public RealmsWorldOptions read(JsonReader jsonReader) throws IOException {
			String string = jsonReader.nextString();
			return RealmsWorldOptions.fromJson(new CheckedGson(), string);
		}
	}
}
