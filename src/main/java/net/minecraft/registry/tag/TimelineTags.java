package net.minecraft.registry.tag;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.attribute.timeline.Timeline;

/**
 * {@code TimelineTags}.
 */
public interface TimelineTags {

	TagKey<Timeline> UNIVERSAL = of("universal");

	TagKey<Timeline> IN_OVERWORLD = of("in_overworld");

	TagKey<Timeline> IN_NETHER = of("in_nether");

	TagKey<Timeline> IN_END = of("in_end");

	private static TagKey<Timeline> of(String name) {
		return TagKey.of(RegistryKeys.TIMELINE, Identifier.ofVanilla(name));
	}
}
