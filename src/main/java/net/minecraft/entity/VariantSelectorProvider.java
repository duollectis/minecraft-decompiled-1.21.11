package net.minecraft.entity;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

public interface VariantSelectorProvider<Context, Condition extends VariantSelectorProvider.SelectorCondition<Context>> {
   List<VariantSelectorProvider.Selector<Context, Condition>> getSelectors();

   static <C, T> Stream<T> select(Stream<T> entries, Function<T, VariantSelectorProvider<C, ?>> providerGetter, C context) {
      List<VariantSelectorProvider.UnwrappedSelector<C, T>> list = new ArrayList<>();
      entries.forEach(
         entry -> {
            VariantSelectorProvider<C, ?> variantSelectorProvider = providerGetter.apply((T)entry);

            for (VariantSelectorProvider.Selector<C, ?> selector : variantSelectorProvider.getSelectors()) {
               list.add(
                  new VariantSelectorProvider.UnwrappedSelector<>(
                     (T)entry,
                     selector.priority(),
                     (VariantSelectorProvider.SelectorCondition<C>)DataFixUtils.orElseGet(
                        selector.condition(), VariantSelectorProvider.SelectorCondition::alwaysTrue
                     )
                  )
               );
            }
         }
      );
      list.sort(VariantSelectorProvider.UnwrappedSelector.PRIORITY_COMPARATOR);
      Iterator<VariantSelectorProvider.UnwrappedSelector<C, T>> iterator = list.iterator();
      int i = Integer.MIN_VALUE;

      while (iterator.hasNext()) {
         VariantSelectorProvider.UnwrappedSelector<C, T> unwrappedSelector = iterator.next();
         if (unwrappedSelector.priority < i) {
            iterator.remove();
         } else if (unwrappedSelector.condition.test(context)) {
            i = unwrappedSelector.priority;
         } else {
            iterator.remove();
         }
      }

      return list.stream().map(VariantSelectorProvider.UnwrappedSelector::entry);
   }

   static <C, T> Optional<T> select(Stream<T> entries, Function<T, VariantSelectorProvider<C, ?>> providerGetter, Random random, C context) {
      List<T> list = select(entries, providerGetter, context).toList();
      return Util.getRandomOrEmpty(list, random);
   }

   static <Context, Condition extends VariantSelectorProvider.SelectorCondition<Context>> List<VariantSelectorProvider.Selector<Context, Condition>> createSingle(
      Condition condition, int priority
   ) {
      return List.of(new VariantSelectorProvider.Selector<>(condition, priority));
   }

   static <Context, Condition extends VariantSelectorProvider.SelectorCondition<Context>> List<VariantSelectorProvider.Selector<Context, Condition>> createFallback(
      int priority
   ) {
      return List.of(new VariantSelectorProvider.Selector<>(Optional.empty(), priority));
   }

   public record Selector<Context, Condition extends VariantSelectorProvider.SelectorCondition<Context>>(Optional<Condition> condition, int priority) {
      public Selector(Condition condition, int priority) {
         this(Optional.of(condition), priority);
      }

      public Selector(int priority) {
         this(Optional.empty(), priority);
      }

      public static <Context, Condition extends VariantSelectorProvider.SelectorCondition<Context>> Codec<VariantSelectorProvider.Selector<Context, Condition>> createCodec(
         Codec<Condition> conditionCodec
      ) {
         return RecordCodecBuilder.create(
            instance -> instance.group(
                  conditionCodec.optionalFieldOf("condition").forGetter(VariantSelectorProvider.Selector::condition),
                  Codec.INT.fieldOf("priority").forGetter(VariantSelectorProvider.Selector::priority)
               )
               .apply(instance, VariantSelectorProvider.Selector::new)
         );
      }
   }

   @FunctionalInterface
   public interface SelectorCondition<C> extends Predicate<C> {
      static <C> VariantSelectorProvider.SelectorCondition<C> alwaysTrue() {
         return context -> true;
      }
   }

   public record UnwrappedSelector<C, T>(T entry, int priority, VariantSelectorProvider.SelectorCondition<C> condition) {
      public static final Comparator<VariantSelectorProvider.UnwrappedSelector<?, ?>> PRIORITY_COMPARATOR = Comparator
         .<VariantSelectorProvider.UnwrappedSelector<?, ?>, Integer>comparing(selector -> selector.priority())
         .reversed();
   }
}
