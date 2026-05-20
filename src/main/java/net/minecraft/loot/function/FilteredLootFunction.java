package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.ErrorReporter;

public class FilteredLootFunction extends ConditionalLootFunction {
   public static final MapCodec<FilteredLootFunction> CODEC = RecordCodecBuilder.mapCodec(
      instance -> addConditionsField(instance)
         .and(
            instance.group(
               ItemPredicate.CODEC.fieldOf("item_filter").forGetter(lootFunction -> lootFunction.itemFilter),
               LootFunctionTypes.CODEC.optionalFieldOf("on_pass").forGetter(lootFunction -> lootFunction.onPass),
               LootFunctionTypes.CODEC.optionalFieldOf("on_fail").forGetter(lootFunction -> lootFunction.onFail)
            )
         )
         .apply(instance, FilteredLootFunction::new)
   );
   private final ItemPredicate itemFilter;
   private final Optional<LootFunction> onPass;
   private final Optional<LootFunction> onFail;

   FilteredLootFunction(List<LootCondition> conditions, ItemPredicate itemFilter, Optional<LootFunction> onPass, Optional<LootFunction> onFail) {
      super(conditions);
      this.itemFilter = itemFilter;
      this.onPass = onPass;
      this.onFail = onFail;
   }

   @Override
   public LootFunctionType<FilteredLootFunction> getType() {
      return LootFunctionTypes.FILTERED;
   }

   @Override
   public ItemStack process(ItemStack stack, LootContext context) {
      Optional<LootFunction> optional = this.itemFilter.test(stack) ? this.onPass : this.onFail;
      return optional.isPresent() ? optional.get().apply(stack, context) : stack;
   }

   @Override
   public void validate(LootTableReporter reporter) {
      super.validate(reporter);
      this.onPass.ifPresent(lootFunction -> lootFunction.validate(reporter.makeChild(new ErrorReporter.MapElementContext("on_pass"))));
      this.onFail.ifPresent(lootFunction -> lootFunction.validate(reporter.makeChild(new ErrorReporter.MapElementContext("on_fail"))));
   }

   public static FilteredLootFunction.Builder builder(ItemPredicate itemFilter) {
      return new FilteredLootFunction.Builder(itemFilter);
   }

   public static class Builder extends ConditionalLootFunction.Builder<FilteredLootFunction.Builder> {
      private final ItemPredicate itemFilter;
      private Optional<LootFunction> onPass = Optional.empty();
      private Optional<LootFunction> onFail = Optional.empty();

      Builder(ItemPredicate itemFilter) {
         this.itemFilter = itemFilter;
      }

      protected FilteredLootFunction.Builder getThisBuilder() {
         return this;
      }

      public FilteredLootFunction.Builder onPass(Optional<LootFunction> onPass) {
         this.onPass = onPass;
         return this;
      }

      public FilteredLootFunction.Builder onFail(Optional<LootFunction> onFail) {
         this.onFail = onFail;
         return this;
      }

      @Override
      public LootFunction build() {
         return new FilteredLootFunction(this.getConditions(), this.itemFilter, this.onPass, this.onFail);
      }
   }
}
