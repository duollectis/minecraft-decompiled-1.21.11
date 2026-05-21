package net.minecraft.entity.passive;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/**
 * {@code WolfSoundVariants}.
 */
public class WolfSoundVariants {

	public static final RegistryKey<WolfSoundVariant> CLASSIC = of(WolfSoundVariants.Type.CLASSIC);
	public static final RegistryKey<WolfSoundVariant> PUGLIN = of(WolfSoundVariants.Type.PUGLIN);
	public static final RegistryKey<WolfSoundVariant> SAD = of(WolfSoundVariants.Type.SAD);
	public static final RegistryKey<WolfSoundVariant> ANGRY = of(WolfSoundVariants.Type.ANGRY);
	public static final RegistryKey<WolfSoundVariant> GRUMPY = of(WolfSoundVariants.Type.GRUMPY);
	public static final RegistryKey<WolfSoundVariant> BIG = of(WolfSoundVariants.Type.BIG);
	public static final RegistryKey<WolfSoundVariant> CUTE = of(WolfSoundVariants.Type.CUTE);

	private static RegistryKey<WolfSoundVariant> of(WolfSoundVariants.Type type) {
		return RegistryKey.of(RegistryKeys.WOLF_SOUND_VARIANT, Identifier.ofVanilla(type.getId()));
	}

	public static void bootstrap(Registerable<WolfSoundVariant> registry) {
		register(registry, CLASSIC, WolfSoundVariants.Type.CLASSIC);
		register(registry, PUGLIN, WolfSoundVariants.Type.PUGLIN);
		register(registry, SAD, WolfSoundVariants.Type.SAD);
		register(registry, ANGRY, WolfSoundVariants.Type.ANGRY);
		register(registry, GRUMPY, WolfSoundVariants.Type.GRUMPY);
		register(registry, BIG, WolfSoundVariants.Type.BIG);
		register(registry, CUTE, WolfSoundVariants.Type.CUTE);
	}

	private static void register(
			Registerable<WolfSoundVariant> registry,
			RegistryKey<WolfSoundVariant> key,
			WolfSoundVariants.Type type
	) {
		registry.register(key, SoundEvents.WOLF_SOUNDS.get(type));
	}

	public static RegistryEntry<WolfSoundVariant> select(DynamicRegistryManager registries, Random random) {
		return registries.getOrThrow(RegistryKeys.WOLF_SOUND_VARIANT).getRandom(random).orElseThrow();
	}

	/**
	 * {@code Type}.
	 */
	public static enum Type {
		CLASSIC("classic", ""),
		PUGLIN("puglin", "_puglin"),
		SAD("sad", "_sad"),
		ANGRY("angry", "_angry"),
		GRUMPY("grumpy", "_grumpy"),
		BIG("big", "_big"),
		CUTE("cute", "_cute");

		private final String id;
		private final String suffix;

		private Type(final String id, final String suffix) {
			this.id = id;
			this.suffix = suffix;
		}

		public String getId() {
			return this.id;
		}

		public String getSoundEventSuffix() {
			return this.suffix;
		}
	}
}
