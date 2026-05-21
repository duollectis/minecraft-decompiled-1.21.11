package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsRegionDataList}.
 */
public record RealmsRegionDataList(@SerializedName("regionDataList") List<RegionData> regionData) implements RealmsSerializable {

	/**
	 * Empty.
	 *
	 * @return RealmsRegionDataList — результат операции
	 */
	public static RealmsRegionDataList empty() {
		return new RealmsRegionDataList(List.of());
	}
}
