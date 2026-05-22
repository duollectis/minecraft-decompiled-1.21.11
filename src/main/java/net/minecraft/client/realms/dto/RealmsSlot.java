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

/**
 * DTO слота мира на сервере Realms.
 * Каждый сервер имеет до 3 слотов; слот содержит настройки мира и список параметров.
 * Поле {@code options} сериализуется через вложенный JSON-объект посредством {@link OptionsTypeAdapter}.
 */
@Environment(EnvType.CLIENT)
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
		return new RealmsSlot(slotId, options.copy(), new ArrayList<>(settings));
	}

	public boolean isHardcore() {
		return RealmsSettingDto.isHardcore(settings);
	}

	/**
	 * Адаптер для сериализации {@link RealmsWorldOptions} как вложенной JSON-строки.
	 * Realms API передаёт options как экранированный JSON внутри JSON.
	 */
	@Environment(EnvType.CLIENT)
	static class OptionsTypeAdapter extends TypeAdapter<RealmsWorldOptions> {

		private OptionsTypeAdapter() {
		}

		@Override
		public void write(JsonWriter writer, RealmsWorldOptions options) throws IOException {
			writer.jsonValue(new CheckedGson().toJson(options));
		}

		@Override
		public RealmsWorldOptions read(JsonReader reader) throws IOException {
			String json = reader.nextString();
			return RealmsWorldOptions.fromJson(new CheckedGson(), json);
		}
	}
}
