package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;

import java.util.List;

/**
 * DTO результата пинга регионов Realms.
 * Содержит список результатов по регионам и идентификаторы миров для маршрутизации.
 */
@Environment(EnvType.CLIENT)
public record PingResult(
		@SerializedName("pingResults") List<RegionPingResult> pingResults,
		@SerializedName("worldIds") List<Long> worldIds
) implements RealmsSerializable {
}
