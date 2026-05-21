package net.minecraft.client.realms;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;

@Environment(EnvType.CLIENT)
/**
 * {@code ServiceQuality}.
 */
public enum ServiceQuality {
	GREAT(1, "icon/ping_5"),
	GOOD(2, "icon/ping_4"),
	OKAY(3, "icon/ping_3"),
	POOR(4, "icon/ping_2"),
	UNKNOWN(5, "icon/ping_unknown");

	final int index;
	private final Identifier icon;

	private ServiceQuality(final int index, final String icon) {
		this.index = index;
		this.icon = Identifier.ofVanilla(icon);
	}

	public static @Nullable ServiceQuality byIndex(int index) {
		for (ServiceQuality serviceQuality : values()) {
			if (serviceQuality.getIndex() == index) {
				return serviceQuality;
			}
		}

		return null;
	}

	public int getIndex() {
		return this.index;
	}

	public Identifier getIcon() {
		return this.icon;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ServiceQualityTypeAdapter}.
	 */
	public static class ServiceQualityTypeAdapter extends TypeAdapter<ServiceQuality> {

		private static final Logger LOGGER = LogUtils.getLogger();

		public void write(JsonWriter jsonWriter, ServiceQuality serviceQuality) throws IOException {
			jsonWriter.value(serviceQuality.index);
		}

		public ServiceQuality read(JsonReader jsonReader) throws IOException {
			int i = jsonReader.nextInt();
			ServiceQuality serviceQuality = ServiceQuality.byIndex(i);
			if (serviceQuality == null) {
				LOGGER.warn("Unsupported ServiceQuality {}", i);
				return ServiceQuality.UNKNOWN;
			}
			else {
				return serviceQuality;
			}
		}
	}
}
