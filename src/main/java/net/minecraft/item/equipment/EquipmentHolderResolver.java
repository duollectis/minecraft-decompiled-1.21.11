package net.minecraft.item.equipment;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.entry.RegistryEntryList;

@FunctionalInterface
public interface EquipmentHolderResolver {
   RegistryEntryList<EntityType<?>> get(RegistryEntryLookup<EntityType<?>> registry);
}
