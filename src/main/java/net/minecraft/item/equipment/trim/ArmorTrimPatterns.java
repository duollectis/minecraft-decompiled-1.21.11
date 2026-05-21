package net.minecraft.item.equipment.trim;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * {@code ArmorTrimPatterns}.
 */
public class ArmorTrimPatterns {

	public static final RegistryKey<ArmorTrimPattern> SENTRY = of("sentry");
	public static final RegistryKey<ArmorTrimPattern> DUNE = of("dune");
	public static final RegistryKey<ArmorTrimPattern> COAST = of("coast");
	public static final RegistryKey<ArmorTrimPattern> WILD = of("wild");
	public static final RegistryKey<ArmorTrimPattern> WARD = of("ward");
	public static final RegistryKey<ArmorTrimPattern> EYE = of("eye");
	public static final RegistryKey<ArmorTrimPattern> VEX = of("vex");
	public static final RegistryKey<ArmorTrimPattern> TIDE = of("tide");
	public static final RegistryKey<ArmorTrimPattern> SNOUT = of("snout");
	public static final RegistryKey<ArmorTrimPattern> RIB = of("rib");
	public static final RegistryKey<ArmorTrimPattern> SPIRE = of("spire");
	public static final RegistryKey<ArmorTrimPattern> WAYFINDER = of("wayfinder");
	public static final RegistryKey<ArmorTrimPattern> SHAPER = of("shaper");
	public static final RegistryKey<ArmorTrimPattern> SILENCE = of("silence");
	public static final RegistryKey<ArmorTrimPattern> RAISER = of("raiser");
	public static final RegistryKey<ArmorTrimPattern> HOST = of("host");
	public static final RegistryKey<ArmorTrimPattern> FLOW = of("flow");
	public static final RegistryKey<ArmorTrimPattern> BOLT = of("bolt");

	/**
	 * Bootstrap.
	 *
	 * @param registry registry
	 */
	public static void bootstrap(Registerable<ArmorTrimPattern> registry) {
		register(registry, SENTRY);
		register(registry, DUNE);
		register(registry, COAST);
		register(registry, WILD);
		register(registry, WARD);
		register(registry, EYE);
		register(registry, VEX);
		register(registry, TIDE);
		register(registry, SNOUT);
		register(registry, RIB);
		register(registry, SPIRE);
		register(registry, WAYFINDER);
		register(registry, SHAPER);
		register(registry, SILENCE);
		register(registry, RAISER);
		register(registry, HOST);
		register(registry, FLOW);
		register(registry, BOLT);
	}

	/**
	 * Register.
	 *
	 * @param registry registry
	 * @param key key
	 */
	public static void register(Registerable<ArmorTrimPattern> registry, RegistryKey<ArmorTrimPattern> key) {
		ArmorTrimPattern
				armorTrimPattern =
				new ArmorTrimPattern(
						getId(key),
						Text.translatable(Util.createTranslationKey("trim_pattern", key.getValue())),
						false
				);
		registry.register(key, armorTrimPattern);
	}

	private static RegistryKey<ArmorTrimPattern> of(String id) {
		return RegistryKey.of(RegistryKeys.TRIM_PATTERN, Identifier.ofVanilla(id));
	}

	public static Identifier getId(RegistryKey<ArmorTrimPattern> key) {
		return key.getValue();
	}
}
