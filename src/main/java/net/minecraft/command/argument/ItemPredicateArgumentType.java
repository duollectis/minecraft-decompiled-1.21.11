package net.minecraft.command.argument;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.component.ComponentPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;

public class ItemPredicateArgumentType extends ParserBackedArgumentType<ItemPredicateArgumentType.ItemStackPredicateArgument> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo:'bar'}");
   static final DynamicCommandExceptionType INVALID_ITEM_ID_EXCEPTION = new DynamicCommandExceptionType(
      id -> Text.stringifiedTranslatable("argument.item.id.invalid", id)
   );
   static final DynamicCommandExceptionType UNKNOWN_ITEM_TAG_EXCEPTION = new DynamicCommandExceptionType(
      tag -> Text.stringifiedTranslatable("arguments.item.tag.unknown", tag)
   );
   static final DynamicCommandExceptionType UNKNOWN_ITEM_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(
      component -> Text.stringifiedTranslatable("arguments.item.component.unknown", component)
   );
   static final Dynamic2CommandExceptionType MALFORMED_ITEM_COMPONENT_EXCEPTION = new Dynamic2CommandExceptionType(
      (componentId, component) -> Text.stringifiedTranslatable("arguments.item.component.malformed", componentId, component)
   );
   static final DynamicCommandExceptionType UNKNOWN_ITEM_PREDICATE_EXCEPTION = new DynamicCommandExceptionType(
      predicate -> Text.stringifiedTranslatable("arguments.item.predicate.unknown", predicate)
   );
   static final Dynamic2CommandExceptionType MALFORMED_ITEM_PREDICATE_EXCEPTION = new Dynamic2CommandExceptionType(
      (predicateId, predicate) -> Text.stringifiedTranslatable("arguments.item.predicate.malformed", predicateId, predicate)
   );
   private static final Identifier COUNT_ID = Identifier.ofVanilla("count");
   static final Map<Identifier, ItemPredicateArgumentType.ComponentCheck> SPECIAL_COMPONENT_CHECKS = Stream.of(
         new ItemPredicateArgumentType.ComponentCheck(COUNT_ID, stack -> true, NumberRange.IntRange.CODEC.map(range -> stack -> range.test(stack.getCount())))
      )
      .collect(Collectors.toUnmodifiableMap(ItemPredicateArgumentType.ComponentCheck::id, check -> (ItemPredicateArgumentType.ComponentCheck)check));
   static final Map<Identifier, ItemPredicateArgumentType.SubPredicateCheck> SPECIAL_SUB_PREDICATE_CHECKS = Stream.of(
         new ItemPredicateArgumentType.SubPredicateCheck(COUNT_ID, NumberRange.IntRange.CODEC.map(range -> stack -> range.test(stack.getCount())))
      )
      .collect(Collectors.toUnmodifiableMap(ItemPredicateArgumentType.SubPredicateCheck::id, check -> (ItemPredicateArgumentType.SubPredicateCheck)check));

   private static ItemPredicateArgumentType.SubPredicateCheck hasComponentCheck(RegistryEntry.Reference<ComponentType<?>> type) {
      Predicate<ItemStack> predicate = holder -> holder.contains(type.value());
      return new ItemPredicateArgumentType.SubPredicateCheck(type.registryKey().getValue(), Unit.CODEC.map(v -> predicate));
   }

   public ItemPredicateArgumentType(CommandRegistryAccess commandRegistryAccess) {
      super(ItemPredicateParsing.createParser(new ItemPredicateArgumentType.Context(commandRegistryAccess)).map(predicates -> Util.allOf(predicates)::test));
   }

   public static ItemPredicateArgumentType itemPredicate(CommandRegistryAccess commandRegistryAccess) {
      return new ItemPredicateArgumentType(commandRegistryAccess);
   }

   public static ItemPredicateArgumentType.ItemStackPredicateArgument getItemStackPredicate(CommandContext<ServerCommandSource> context, String name) {
      return (ItemPredicateArgumentType.ItemStackPredicateArgument)context.getArgument(name, ItemPredicateArgumentType.ItemStackPredicateArgument.class);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   record ComponentCheck(Identifier id, Predicate<ItemStack> presenceChecker, Decoder<? extends Predicate<ItemStack>> valueChecker) {

      public static <T> ItemPredicateArgumentType.ComponentCheck read(ImmutableStringReader reader, Identifier id, ComponentType<T> type) throws CommandSyntaxException {
         Codec<T> codec = type.getCodec();
         if (codec == null) {
            throw ItemPredicateArgumentType.UNKNOWN_ITEM_COMPONENT_EXCEPTION.createWithContext(reader, id);
         } else {
            return new ItemPredicateArgumentType.ComponentCheck(id, stack -> stack.contains(type), codec.map(expected -> stack -> {
               T object2 = stack.get(type);
               return Objects.equals(expected, object2);
            }));
         }
      }

      public Predicate<ItemStack> createPredicate(ImmutableStringReader reader, Dynamic<?> value) throws CommandSyntaxException {
         DataResult<? extends Predicate<ItemStack>> dataResult = this.valueChecker.parse(value);
         return (Predicate<ItemStack>)dataResult.getOrThrow(
            error -> ItemPredicateArgumentType.MALFORMED_ITEM_COMPONENT_EXCEPTION.createWithContext(reader, this.id.toString(), error)
         );
      }
   }

   static class Context
      implements ItemPredicateParsing.Callbacks<Predicate<ItemStack>, ItemPredicateArgumentType.ComponentCheck, ItemPredicateArgumentType.SubPredicateCheck> {
      private final RegistryWrapper.WrapperLookup registries;
      private final RegistryWrapper.Impl<Item> itemRegistryWrapper;
      private final RegistryWrapper.Impl<ComponentType<?>> dataComponentTypeRegistryWrapper;
      private final RegistryWrapper.Impl<ComponentPredicate.Type<?>> itemSubPredicateTypeRegistryWrapper;

      Context(RegistryWrapper.WrapperLookup registries) {
         this.registries = registries;
         this.itemRegistryWrapper = registries.getOrThrow(RegistryKeys.ITEM);
         this.dataComponentTypeRegistryWrapper = registries.getOrThrow(RegistryKeys.DATA_COMPONENT_TYPE);
         this.itemSubPredicateTypeRegistryWrapper = registries.getOrThrow(RegistryKeys.DATA_COMPONENT_PREDICATE_TYPE);
      }

      public Predicate<ItemStack> itemMatchPredicate(ImmutableStringReader immutableStringReader, Identifier identifier) throws CommandSyntaxException {
         RegistryEntry.Reference<Item> reference = this.itemRegistryWrapper
            .getOptional(RegistryKey.of(RegistryKeys.ITEM, identifier))
            .orElseThrow(() -> ItemPredicateArgumentType.INVALID_ITEM_ID_EXCEPTION.createWithContext(immutableStringReader, identifier));
         return stack -> stack.itemMatches(reference);
      }

      public Predicate<ItemStack> tagMatchPredicate(ImmutableStringReader immutableStringReader, Identifier identifier) throws CommandSyntaxException {
         RegistryEntryList<Item> registryEntryList = this.itemRegistryWrapper
            .getOptional(TagKey.of(RegistryKeys.ITEM, identifier))
            .orElseThrow(() -> ItemPredicateArgumentType.UNKNOWN_ITEM_TAG_EXCEPTION.createWithContext(immutableStringReader, identifier));
         return stack -> stack.isIn(registryEntryList);
      }

      public ItemPredicateArgumentType.ComponentCheck componentCheck(ImmutableStringReader immutableStringReader, Identifier identifier) throws CommandSyntaxException {
         ItemPredicateArgumentType.ComponentCheck componentCheck = ItemPredicateArgumentType.SPECIAL_COMPONENT_CHECKS.get(identifier);
         if (componentCheck != null) {
            return componentCheck;
         } else {
            ComponentType<?> componentType = this.dataComponentTypeRegistryWrapper
               .getOptional(RegistryKey.of(RegistryKeys.DATA_COMPONENT_TYPE, identifier))
               .map(RegistryEntry::value)
               .orElseThrow(() -> ItemPredicateArgumentType.UNKNOWN_ITEM_COMPONENT_EXCEPTION.createWithContext(immutableStringReader, identifier));
            return ItemPredicateArgumentType.ComponentCheck.read(immutableStringReader, identifier, componentType);
         }
      }

      public Predicate<ItemStack> componentMatchPredicate(
         ImmutableStringReader immutableStringReader, ItemPredicateArgumentType.ComponentCheck componentCheck, Dynamic<?> dynamic
      ) throws CommandSyntaxException {
         return componentCheck.createPredicate(immutableStringReader, RegistryOps.withRegistry(dynamic, this.registries));
      }

      public Predicate<ItemStack> componentPresencePredicate(
         ImmutableStringReader immutableStringReader, ItemPredicateArgumentType.ComponentCheck componentCheck
      ) {
         return componentCheck.presenceChecker;
      }

      public ItemPredicateArgumentType.SubPredicateCheck subPredicateCheck(ImmutableStringReader immutableStringReader, Identifier identifier) throws CommandSyntaxException {
         ItemPredicateArgumentType.SubPredicateCheck subPredicateCheck = ItemPredicateArgumentType.SPECIAL_SUB_PREDICATE_CHECKS.get(identifier);
         return subPredicateCheck != null
            ? subPredicateCheck
            : this.itemSubPredicateTypeRegistryWrapper
               .getOptional(RegistryKey.of(RegistryKeys.DATA_COMPONENT_PREDICATE_TYPE, identifier))
               .map(ItemPredicateArgumentType.SubPredicateCheck::new)
               .or(
                  () -> this.dataComponentTypeRegistryWrapper
                     .getOptional(RegistryKey.of(RegistryKeys.DATA_COMPONENT_TYPE, identifier))
                     .map(ItemPredicateArgumentType::hasComponentCheck)
               )
               .orElseThrow(() -> ItemPredicateArgumentType.UNKNOWN_ITEM_PREDICATE_EXCEPTION.createWithContext(immutableStringReader, identifier));
      }

      public Predicate<ItemStack> subPredicatePredicate(
         ImmutableStringReader immutableStringReader, ItemPredicateArgumentType.SubPredicateCheck subPredicateCheck, Dynamic<?> dynamic
      ) throws CommandSyntaxException {
         return subPredicateCheck.createPredicate(immutableStringReader, RegistryOps.withRegistry(dynamic, this.registries));
      }

      @Override
      public Stream<Identifier> streamItemIds() {
         return this.itemRegistryWrapper.streamKeys().map(RegistryKey::getValue);
      }

      @Override
      public Stream<Identifier> streamTags() {
         return this.itemRegistryWrapper.streamTagKeys().map(TagKey::id);
      }

      @Override
      public Stream<Identifier> streamComponentIds() {
         return Stream.concat(
            ItemPredicateArgumentType.SPECIAL_COMPONENT_CHECKS.keySet().stream(),
            this.dataComponentTypeRegistryWrapper
               .streamEntries()
               .filter(entry -> !entry.value().shouldSkipSerialization())
               .map(entry -> entry.registryKey().getValue())
         );
      }

      @Override
      public Stream<Identifier> streamSubPredicateIds() {
         return Stream.concat(
            ItemPredicateArgumentType.SPECIAL_SUB_PREDICATE_CHECKS.keySet().stream(),
            this.itemSubPredicateTypeRegistryWrapper.streamKeys().map(RegistryKey::getValue)
         );
      }

      public Predicate<ItemStack> negate(Predicate<ItemStack> predicate) {
         return predicate.negate();
      }

      public Predicate<ItemStack> anyOf(List<Predicate<ItemStack>> list) {
         return Util.anyOf(list);
      }
   }

   public interface ItemStackPredicateArgument extends Predicate<ItemStack> {
   }

   record SubPredicateCheck(Identifier id, Decoder<? extends Predicate<ItemStack>> type) {
      public SubPredicateCheck(RegistryEntry.Reference<ComponentPredicate.Type<?>> type) {
         this(type.registryKey().getValue(), type.value().getPredicateCodec().map(predicate -> predicate::test));
      }

      public Predicate<ItemStack> createPredicate(ImmutableStringReader reader, Dynamic<?> value) throws CommandSyntaxException {
         DataResult<? extends Predicate<ItemStack>> dataResult = this.type.parse(value);
         return (Predicate<ItemStack>)dataResult.getOrThrow(
            error -> ItemPredicateArgumentType.MALFORMED_ITEM_PREDICATE_EXCEPTION.createWithContext(reader, this.id.toString(), error)
         );
      }
   }
}
