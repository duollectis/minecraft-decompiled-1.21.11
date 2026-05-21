package net.minecraft.item.equipment;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.entry.RegistryEntryList;

@FunctionalInterface
/**
 * {@code EquipmentHolderResolver}.
 */
public interface EquipmentHolderResolver {

	RegistryEntryList<EntityType<?>> get(RegistryEntryLookup<EntityType<?>> registry);
}
