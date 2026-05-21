package net.minecraft.client.realms.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

import java.io.IOException;

@Environment(EnvType.CLIENT)
/**
 * {@code RegionSelectionMethod}.
 */
public enum RegionSelectionMethod {
	AUTOMATIC_PLAYER(0, "realms.configuration.region_preference.automatic_player"),
	AUTOMATIC_OWNER(1, "realms.configuration.region_preference.automatic_owner"),
	MANUAL(2, "");

	public static final RegionSelectionMethod DEFAULT = AUTOMATIC_PLAYER;
	public final int index;
	public final String translationKey;

	private RegionSelectionMethod(final int index, final String translationKey) {
		this.index = index;
		this.translationKey = translationKey;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SelectionMethodTypeAdapter}.
	 */
	public static class SelectionMethodTypeAdapter extends TypeAdapter<RegionSelectionMethod> {

		private static final Logger LOGGER = LogUtils.getLogger();

		/**
		 * Write.
		 *
		 * @param jsonWriter json writer
		 * @param regionSelectionMethod region selection method
		 */
		public void write(JsonWriter jsonWriter, RegionSelectionMethod regionSelectionMethod) throws IOException {
			jsonWriter.value(regionSelectionMethod.index);
		}

		/**
		 * Read.
		 *
		 * @param jsonReader json reader
		 *
		 * @return RegionSelectionMethod — результат операции
		 */
		public RegionSelectionMethod read(JsonReader jsonReader) throws IOException {
			int i = jsonReader.nextInt();

			for (RegionSelectionMethod regionSelectionMethod : RegionSelectionMethod.values()) {
				if (regionSelectionMethod.index == i) {
					return regionSelectionMethod;
				}
			}

			LOGGER.warn("Unsupported RegionSelectionPreference {}", i);
			return RegionSelectionMethod.DEFAULT;
		}
	}
}
