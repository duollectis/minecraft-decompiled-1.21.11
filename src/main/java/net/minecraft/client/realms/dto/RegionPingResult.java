package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;

import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code RegionPingResult}.
 */
public record RegionPingResult(
		@SerializedName("regionName") String regionName,
		@SerializedName("ping") int ping
) implements RealmsSerializable {

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s --> %.2f ms", this.regionName, (float) this.ping);
	}
}
