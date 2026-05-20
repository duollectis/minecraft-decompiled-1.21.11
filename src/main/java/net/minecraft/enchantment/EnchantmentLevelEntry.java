package net.minecraft.enchantment;

import net.minecraft.registry.entry.RegistryEntry;

public record EnchantmentLevelEntry(RegistryEntry<Enchantment> enchantment, int level) {
   public int getWeight() {
      return this.enchantment().value().getWeight();
   }
}
