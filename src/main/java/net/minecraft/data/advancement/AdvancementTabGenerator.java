package net.minecraft.data.advancement;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * Генератор достижений для одной вкладки (таба) экрана достижений.
 * Реализации заполняют переданный {@code exporter} достижениями своей вкладки.
 */
public interface AdvancementTabGenerator {

	void accept(RegistryWrapper.WrapperLookup registries, Consumer<AdvancementEntry> exporter);

	static AdvancementEntry reference(String id) {
		return Advancement.Builder.create().build(Identifier.of(id));
	}
}
