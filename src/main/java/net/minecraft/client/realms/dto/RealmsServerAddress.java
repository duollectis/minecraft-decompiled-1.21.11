package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.CheckedGson;
import net.minecraft.client.realms.RealmsSerializable;
import net.minecraft.client.realms.ServiceQuality;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * DTO адреса сервера Realms для подключения.
 * Содержит адрес, опциональный ресурс-пак и данные региона текущей сессии.
 */
@Environment(EnvType.CLIENT)
public record RealmsServerAddress(
		@SerializedName("address") @Nullable String address,
		@SerializedName("resourcePackUrl") @Nullable String resourcePackUrl,
		@SerializedName("resourcePackHash") @Nullable String resourcePackHash,
		@SerializedName("sessionRegionData") RealmsServerAddress.@Nullable RegionData regionData
) implements RealmsSerializable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final RealmsServerAddress NULL = new RealmsServerAddress(null, null, null, null);

	/**
	 * Парсит адрес сервера из JSON-строки ответа Realms.
	 * При ошибке возвращает объект с null-полями вместо выброса исключения.
	 *
	 * @param gson настроенный экземпляр Gson
	 * @param json JSON-строка с данными адреса
	 * @return распарсенный адрес или {@link #NULL} при ошибке
	 */
	public static RealmsServerAddress parse(CheckedGson gson, String json) {
		try {
			RealmsServerAddress address = gson.fromJson(json, RealmsServerAddress.class);

			if (address == null) {
				LOGGER.error("Could not parse RealmsServerAddress: {}", json);
				return NULL;
			}

			return address;
		} catch (Exception ex) {
			LOGGER.error("Could not parse RealmsServerAddress", ex);
			return NULL;
		}
	}

	/**
	 * Данные региона текущей игровой сессии Realms.
	 */
	@Environment(EnvType.CLIENT)
	public record RegionData(
			@SerializedName("regionName") @Nullable RealmsRegion region,
			@SerializedName("serviceQuality") @Nullable ServiceQuality serviceQuality
	) implements RealmsSerializable {
	}
}
