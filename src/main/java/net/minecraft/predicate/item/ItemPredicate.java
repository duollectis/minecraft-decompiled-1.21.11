package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.component.ComponentsPredicate;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;

public record ItemPredicate(Optional<RegistryEntryList<Item>> items, NumberRange.IntRange count, ComponentsPredicate components) implements Predicate<ItemStack> {
   public static final Codec<ItemPredicate> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            RegistryCodecs.entryList(RegistryKeys.ITEM).optionalFieldOf("items").forGetter(ItemPredicate::items),
            NumberRange.IntRange.CODEC.optionalFieldOf("count", NumberRange.IntRange.ANY).forGetter(ItemPredicate::count),
            ComponentsPredicate.CODEC.forGetter(ItemPredicate::components)
         )
         .apply(instance, ItemPredicate::new)
   );

   public boolean test(ItemStack stack) {
      if (this.items.isPresent() && !stack.isIn(this.items.get())) {
         return false;
      } else {
         return !this.count.test(stack.getCount()) ? false : this.components.test((ComponentsAccess)stack);
      }
   }

   public static class Builder {
      private Optional<RegistryEntryList<Item>> item = Optional.empty();
      private NumberRange.IntRange count = NumberRange.IntRange.ANY;
      private ComponentsPredicate components = ComponentsPredicate.EMPTY;

      public static ItemPredicate.Builder create() {
         return new ItemPredicate.Builder();
      }

      public ItemPredicate.Builder items(RegistryEntryLookup<Item> itemRegistry, ItemConvertible... items) {
         this.item = Optional.of(RegistryEntryList.of(item -> item.asItem().getRegistryEntry(), items));
         return this;
      }

      public ItemPredicate.Builder tag(RegistryEntryLookup<Item> itemRegistry, TagKey<Item> tag) {
         this.item = Optional.of(itemRegistry.getOrThrow(tag));
         return this;
      }

      public ItemPredicate.Builder count(NumberRange.IntRange count) {
         this.count = count;
         return this;
      }

      public ItemPredicate.Builder components(ComponentsPredicate components) {
         this.components = components;
         return this;
      }

      public ItemPredicate build() {
         return new ItemPredicate(this.item, this.count, this.components);
      }
   }
}
