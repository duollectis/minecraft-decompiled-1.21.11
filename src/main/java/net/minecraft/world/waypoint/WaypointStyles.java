package net.minecraft.world.waypoint;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

/**
 * {@code WaypointStyles}.
 */
public interface WaypointStyles {

	RegistryKey<? extends Registry<WaypointStyle>>
			REGISTRY =
			RegistryKey.ofRegistry(Identifier.ofVanilla("waypoint_style_asset"));

	RegistryKey<WaypointStyle> DEFAULT = of("default");

	RegistryKey<WaypointStyle> BOWTIE = of("bowtie");

	static RegistryKey<WaypointStyle> of(String id) {
		return RegistryKey.of(REGISTRY, Identifier.ofVanilla(id));
	}
}
