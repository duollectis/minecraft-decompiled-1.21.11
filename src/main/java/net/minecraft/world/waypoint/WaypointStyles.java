package net.minecraft.world.waypoint;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

/**
 * Реестр и встроенные ключи стилей вейпоинтов.
 * <p>
 * Стили регистрируются в реестре {@code waypoint_style_asset} и определяют
 * визуальное представление вейпоинта на клиенте (форму, анимацию и т.д.).
 */
public interface WaypointStyles {

	RegistryKey<? extends Registry<WaypointStyle>> REGISTRY =
		RegistryKey.ofRegistry(Identifier.ofVanilla("waypoint_style_asset"));

	/** Стандартный стиль вейпоинта, используемый по умолчанию. */
	RegistryKey<WaypointStyle> DEFAULT = of("default");

	/** Стиль в форме галстука-бабочки. */
	RegistryKey<WaypointStyle> BOWTIE = of("bowtie");

	static RegistryKey<WaypointStyle> of(String id) {
		return RegistryKey.of(REGISTRY, Identifier.ofVanilla(id));
	}
}
