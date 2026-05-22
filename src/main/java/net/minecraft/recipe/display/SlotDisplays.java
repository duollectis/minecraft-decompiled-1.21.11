package net.minecraft.recipe.display;

import net.minecraft.registry.Registry;

/**
 * Регистрирует все стандартные реализации {@link SlotDisplay} в реестре.
 */
public class SlotDisplays {

	public static SlotDisplay.Serializer<?> registerAndGetDefault(Registry<SlotDisplay.Serializer<?>> registry) {
		Registry.register(registry, "empty", SlotDisplay.EmptySlotDisplay.SERIALIZER);
		Registry.register(registry, "any_fuel", SlotDisplay.AnyFuelSlotDisplay.SERIALIZER);
		Registry.register(registry, "item", SlotDisplay.ItemSlotDisplay.SERIALIZER);
		Registry.register(registry, "item_stack", SlotDisplay.StackSlotDisplay.SERIALIZER);
		Registry.register(registry, "tag", SlotDisplay.TagSlotDisplay.SERIALIZER);
		Registry.register(registry, "smithing_trim", SlotDisplay.SmithingTrimSlotDisplay.SERIALIZER);
		Registry.register(registry, "with_remainder", SlotDisplay.WithRemainderSlotDisplay.SERIALIZER);
		return Registry.register(registry, "composite", SlotDisplay.CompositeSlotDisplay.SERIALIZER);
	}
}
