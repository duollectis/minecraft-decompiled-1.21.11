package net.minecraft.loot.function;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootEntityValueSource;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextParameter;

public class CopyComponentsLootFunction extends ConditionalLootFunction {
   private static final Codec<LootEntityValueSource<ComponentsAccess>> SOURCE_CODEC = LootEntityValueSource.createCodec(
      builder -> builder.forEntities(CopyComponentsLootFunction.ComponentAccessSource::new)
         .forBlockEntities(CopyComponentsLootFunction.BlockEntityComponentsSource::new)
         .forItemStacks(CopyComponentsLootFunction.ComponentAccessSource::new)
   );
   public static final MapCodec<CopyComponentsLootFunction> CODEC = RecordCodecBuilder.mapCodec(
      instance -> addConditionsField(instance)
         .and(
            instance.group(
               SOURCE_CODEC.fieldOf("source").forGetter(function -> function.source),
               ComponentType.CODEC.listOf().optionalFieldOf("include").forGetter(function -> function.include),
               ComponentType.CODEC.listOf().optionalFieldOf("exclude").forGetter(function -> function.exclude)
            )
         )
         .apply(instance, CopyComponentsLootFunction::new)
   );
   private final LootEntityValueSource<ComponentsAccess> source;
   private final Optional<List<ComponentType<?>>> include;
   private final Optional<List<ComponentType<?>>> exclude;
   private final Predicate<ComponentType<?>> filter;

   CopyComponentsLootFunction(
      List<LootCondition> conditions,
      LootEntityValueSource<ComponentsAccess> source,
      Optional<List<ComponentType<?>>> include,
      Optional<List<ComponentType<?>>> exclude
   ) {
      super(conditions);
      this.source = source;
      this.include = include.map(List::copyOf);
      this.exclude = exclude.map(List::copyOf);
      List<Predicate<ComponentType<?>>> list = new ArrayList<>(2);
      exclude.ifPresent(excludedTypes -> list.add(type -> !excludedTypes.contains(type)));
      include.ifPresent(includedTypes -> list.add(includedTypes::contains));
      this.filter = Util.allOf(list);
   }

   @Override
   public LootFunctionType<CopyComponentsLootFunction> getType() {
      return LootFunctionTypes.COPY_COMPONENTS;
   }

   @Override
   public Set<ContextParameter<?>> getAllowedParameters() {
      return Set.of(this.source.contextParam());
   }

   @Override
   public ItemStack process(ItemStack stack, LootContext context) {
      ComponentsAccess componentsAccess = this.source.get(context);
      if (componentsAccess != null) {
         if (componentsAccess instanceof ComponentMap componentMap) {
            stack.applyComponentsFrom(componentMap.filtered(this.filter));
         } else {
            Collection<ComponentType<?>> collection = this.exclude.orElse(List.of());
            this.include.map(Collection::stream).orElse(Registries.DATA_COMPONENT_TYPE.streamEntries().map(RegistryEntry::value)).forEach(type -> {
               if (!collection.contains(type)) {
                  Component<?> component = componentsAccess.getTyped(type);
                  if (component != null) {
                     stack.set(component);
                  }
               }
            });
         }
      }

      return stack;
   }

   public static CopyComponentsLootFunction.Builder entity(ContextParameter<? extends Entity> parameter) {
      return new CopyComponentsLootFunction.Builder(new CopyComponentsLootFunction.ComponentAccessSource<>(parameter));
   }

   public static CopyComponentsLootFunction.Builder blockEntity(ContextParameter<? extends BlockEntity> parameter) {
      return new CopyComponentsLootFunction.Builder(new CopyComponentsLootFunction.BlockEntityComponentsSource(parameter));
   }

   record BlockEntityComponentsSource(ContextParameter<? extends BlockEntity> contextParam)
      implements LootEntityValueSource.ContextComponentBased<BlockEntity, ComponentsAccess> {
      public ComponentsAccess get(BlockEntity blockEntity) {
         return blockEntity.createComponentMap();
      }
   }

   public static class Builder extends ConditionalLootFunction.Builder<CopyComponentsLootFunction.Builder> {
      private final LootEntityValueSource<ComponentsAccess> source;
      private Optional<com.google.common.collect.ImmutableList.Builder<ComponentType<?>>> include = Optional.empty();
      private Optional<com.google.common.collect.ImmutableList.Builder<ComponentType<?>>> exclude = Optional.empty();

      Builder(LootEntityValueSource<ComponentsAccess> source) {
         this.source = source;
      }

      public CopyComponentsLootFunction.Builder include(ComponentType<?> type) {
         if (this.include.isEmpty()) {
            this.include = Optional.of(ImmutableList.builder());
         }

         this.include.get().add(type);
         return this;
      }

      public CopyComponentsLootFunction.Builder exclude(ComponentType<?> type) {
         if (this.exclude.isEmpty()) {
            this.exclude = Optional.of(ImmutableList.builder());
         }

         this.exclude.get().add(type);
         return this;
      }

      protected CopyComponentsLootFunction.Builder getThisBuilder() {
         return this;
      }

      @Override
      public LootFunction build() {
         return new CopyComponentsLootFunction(
            this.getConditions(),
            this.source,
            this.include.map(com.google.common.collect.ImmutableList.Builder::build),
            this.exclude.map(com.google.common.collect.ImmutableList.Builder::build)
         );
      }
   }

   record ComponentAccessSource<T extends ComponentsAccess>(ContextParameter<? extends T> contextParam)
      implements LootEntityValueSource.ContextComponentBased<T, ComponentsAccess> {
      public ComponentsAccess get(T componentsAccess) {
         return componentsAccess;
      }
   }
}
