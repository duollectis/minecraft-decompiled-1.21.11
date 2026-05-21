package net.minecraft.data.advancement;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * {@code AdvancementTabGenerator}.
 */
public interface AdvancementTabGenerator {

	void accept(RegistryWrapper.WrapperLookup registries, Consumer<AdvancementEntry> exporter);

	static AdvancementEntry reference(String id) {
		return Advancement.Builder.create().build(Identifier.of(id));
	}
}
