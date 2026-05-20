package net.minecraft.command;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;

public class EntitySelectorOptions {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<String, EntitySelectorOptions.SelectorOption> OPTIONS = Maps.newHashMap();
   public static final DynamicCommandExceptionType UNKNOWN_OPTION_EXCEPTION = new DynamicCommandExceptionType(
      option -> Text.stringifiedTranslatable("argument.entity.options.unknown", option)
   );
   public static final DynamicCommandExceptionType INAPPLICABLE_OPTION_EXCEPTION = new DynamicCommandExceptionType(
      option -> Text.stringifiedTranslatable("argument.entity.options.inapplicable", option)
   );
   public static final SimpleCommandExceptionType NEGATIVE_DISTANCE_EXCEPTION = new SimpleCommandExceptionType(
      Text.translatable("argument.entity.options.distance.negative")
   );
   public static final SimpleCommandExceptionType NEGATIVE_LEVEL_EXCEPTION = new SimpleCommandExceptionType(
      Text.translatable("argument.entity.options.level.negative")
   );
   public static final SimpleCommandExceptionType TOO_SMALL_LEVEL_EXCEPTION = new SimpleCommandExceptionType(
      Text.translatable("argument.entity.options.limit.toosmall")
   );
   public static final DynamicCommandExceptionType IRREVERSIBLE_SORT_EXCEPTION = new DynamicCommandExceptionType(
      sortType -> Text.stringifiedTranslatable("argument.entity.options.sort.irreversible", sortType)
   );
   public static final DynamicCommandExceptionType INVALID_MODE_EXCEPTION = new DynamicCommandExceptionType(
      gameMode -> Text.stringifiedTranslatable("argument.entity.options.mode.invalid", gameMode)
   );
   public static final DynamicCommandExceptionType INVALID_TYPE_EXCEPTION = new DynamicCommandExceptionType(
      entity -> Text.stringifiedTranslatable("argument.entity.options.type.invalid", entity)
   );

   private static void putOption(String id, EntitySelectorOptions.SelectorHandler handler, Predicate<EntitySelectorReader> condition, Text description) {
      OPTIONS.put(id, new EntitySelectorOptions.SelectorOption(handler, condition, description));
   }

   public static void register() {
      if (OPTIONS.isEmpty()) {
         putOption("name", reader -> {
            int i = reader.getReader().getCursor();
            boolean bl = reader.readNegationCharacter();
            String string = reader.getReader().readString();
            if (reader.excludesName() && !bl) {
               reader.getReader().setCursor(i);
               throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "name");
            } else {
               if (bl) {
                  reader.setExcludesName(true);
               } else {
                  reader.setSelectsName(true);
               }

               reader.addPredicate(entity -> entity.getStringifiedName().equals(string) != bl);
            }
         }, reader -> !reader.selectsName(), Text.translatable("argument.entity.options.name.description"));
         putOption(
            "distance",
            reader -> {
               int i = reader.getReader().getCursor();
               NumberRange.DoubleRange doubleRange = NumberRange.DoubleRange.parse(reader.getReader());
               if ((!doubleRange.getMin().isPresent() || !((Double)doubleRange.getMin().get() < 0.0))
                  && (!doubleRange.getMax().isPresent() || !((Double)doubleRange.getMax().get() < 0.0))) {
                  reader.setDistance(doubleRange);
                  reader.setLocalWorldOnly();
               } else {
                  reader.getReader().setCursor(i);
                  throw NEGATIVE_DISTANCE_EXCEPTION.createWithContext(reader.getReader());
               }
            },
            reader -> reader.getDistance() == null,
            Text.translatable("argument.entity.options.distance.description")
         );
         putOption(
            "level",
            reader -> {
               int i = reader.getReader().getCursor();
               NumberRange.IntRange intRange = NumberRange.IntRange.parse(reader.getReader());
               if ((!intRange.getMin().isPresent() || (Integer)intRange.getMin().get() >= 0)
                  && (!intRange.getMax().isPresent() || (Integer)intRange.getMax().get() >= 0)) {
                  reader.setLevelRange(intRange);
                  reader.setIncludesNonPlayers(false);
               } else {
                  reader.getReader().setCursor(i);
                  throw NEGATIVE_LEVEL_EXCEPTION.createWithContext(reader.getReader());
               }
            },
            reader -> reader.getLevelRange() == null,
            Text.translatable("argument.entity.options.level.description")
         );
         putOption("x", reader -> {
            reader.setLocalWorldOnly();
            reader.setX(reader.getReader().readDouble());
         }, reader -> reader.getX() == null, Text.translatable("argument.entity.options.x.description"));
         putOption("y", reader -> {
            reader.setLocalWorldOnly();
            reader.setY(reader.getReader().readDouble());
         }, reader -> reader.getY() == null, Text.translatable("argument.entity.options.y.description"));
         putOption("z", reader -> {
            reader.setLocalWorldOnly();
            reader.setZ(reader.getReader().readDouble());
         }, reader -> reader.getZ() == null, Text.translatable("argument.entity.options.z.description"));
         putOption("dx", reader -> {
            reader.setLocalWorldOnly();
            reader.setDx(reader.getReader().readDouble());
         }, reader -> reader.getDx() == null, Text.translatable("argument.entity.options.dx.description"));
         putOption("dy", reader -> {
            reader.setLocalWorldOnly();
            reader.setDy(reader.getReader().readDouble());
         }, reader -> reader.getDy() == null, Text.translatable("argument.entity.options.dy.description"));
         putOption("dz", reader -> {
            reader.setLocalWorldOnly();
            reader.setDz(reader.getReader().readDouble());
         }, reader -> reader.getDz() == null, Text.translatable("argument.entity.options.dz.description"));
         putOption(
            "x_rotation",
            reader -> reader.setPitchRange(NumberRange.AngleRange.parse(reader.getReader())),
            reader -> reader.getPitchRange() == null,
            Text.translatable("argument.entity.options.x_rotation.description")
         );
         putOption(
            "y_rotation",
            reader -> reader.setYawRange(NumberRange.AngleRange.parse(reader.getReader())),
            reader -> reader.getYawRange() == null,
            Text.translatable("argument.entity.options.y_rotation.description")
         );
         putOption("limit", reader -> {
            int i = reader.getReader().getCursor();
            int j = reader.getReader().readInt();
            if (j < 1) {
               reader.getReader().setCursor(i);
               throw TOO_SMALL_LEVEL_EXCEPTION.createWithContext(reader.getReader());
            } else {
               reader.setLimit(j);
               reader.setHasLimit(true);
            }
         }, reader -> !reader.isSenderOnly() && !reader.hasLimit(), Text.translatable("argument.entity.options.limit.description"));
         putOption(
            "sort",
            reader -> {
               int i = reader.getReader().getCursor();
               String string = reader.getReader().readUnquotedString();
               reader.setSuggestionProvider(
                  (builder, consumer) -> CommandSource.suggestMatching(Arrays.asList("nearest", "furthest", "random", "arbitrary"), builder)
               );

               reader.setSorter(switch (string) {
                  case "nearest" -> EntitySelectorReader.NEAREST;
                  case "furthest" -> EntitySelectorReader.FURTHEST;
                  case "random" -> EntitySelectorReader.RANDOM;
                  case "arbitrary" -> EntitySelector.ARBITRARY;
                  default -> {
                     reader.getReader().setCursor(i);
                     throw IRREVERSIBLE_SORT_EXCEPTION.createWithContext(reader.getReader(), string);
                  }
               });
               reader.setHasSorter(true);
            },
            reader -> !reader.isSenderOnly() && !reader.hasSorter(),
            Text.translatable("argument.entity.options.sort.description")
         );
         putOption("gamemode", reader -> {
            reader.setSuggestionProvider((builder, consumer) -> {
               String stringx = builder.getRemaining().toLowerCase(Locale.ROOT);
               boolean blx = !reader.excludesGameMode();
               boolean bl2 = true;
               if (!stringx.isEmpty()) {
                  if (stringx.charAt(0) == '!') {
                     blx = false;
                     stringx = stringx.substring(1);
                  } else {
                     bl2 = false;
                  }
               }

               for (GameMode gameModex : GameMode.values()) {
                  if (gameModex.getId().toLowerCase(Locale.ROOT).startsWith(stringx)) {
                     if (bl2) {
                        builder.suggest("!" + gameModex.getId());
                     }

                     if (blx) {
                        builder.suggest(gameModex.getId());
                     }
                  }
               }

               return builder.buildFuture();
            });
            int i = reader.getReader().getCursor();
            boolean bl = reader.readNegationCharacter();
            if (reader.excludesGameMode() && !bl) {
               reader.getReader().setCursor(i);
               throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "gamemode");
            } else {
               String string = reader.getReader().readUnquotedString();
               GameMode gameMode = GameMode.byId(string, null);
               if (gameMode == null) {
                  reader.getReader().setCursor(i);
                  throw INVALID_MODE_EXCEPTION.createWithContext(reader.getReader(), string);
               } else {
                  reader.setIncludesNonPlayers(false);
                  reader.addPredicate(entity -> {
                     if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
                        GameMode gameMode2 = serverPlayerEntity.getGameMode();
                        return gameMode2 == gameMode ^ bl;
                     } else {
                        return false;
                     }
                  });
                  if (bl) {
                     reader.setExcludesGameMode(true);
                  } else {
                     reader.setSelectsGameMode(true);
                  }
               }
            }
         }, reader -> !reader.selectsGameMode(), Text.translatable("argument.entity.options.gamemode.description"));
         putOption("team", reader -> {
            boolean bl = reader.readNegationCharacter();
            String string = reader.getReader().readUnquotedString();
            reader.addPredicate(entity -> {
               AbstractTeam abstractTeam = entity.getScoreboardTeam();
               String string2 = abstractTeam == null ? "" : abstractTeam.getName();
               return string2.equals(string) != bl;
            });
            if (bl) {
               reader.setExcludesTeam(true);
            } else {
               reader.setSelectsTeam(true);
            }
         }, reader -> !reader.selectsTeam(), Text.translatable("argument.entity.options.team.description"));
         putOption("type", reader -> {
            reader.setSuggestionProvider((builder, consumer) -> {
               CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder, String.valueOf('!'));
               CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.streamTags().map(tag -> tag.getTag().id()), builder, "!#");
               if (!reader.excludesEntityType()) {
                  CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder);
                  CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.streamTags().map(tag -> tag.getTag().id()), builder, String.valueOf('#'));
               }

               return builder.buildFuture();
            });
            int i = reader.getReader().getCursor();
            boolean bl = reader.readNegationCharacter();
            if (reader.excludesEntityType() && !bl) {
               reader.getReader().setCursor(i);
               throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "type");
            } else {
               if (bl) {
                  reader.setExcludesEntityType();
               }

               if (reader.readTagCharacter()) {
                  TagKey<EntityType<?>> tagKey = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.fromCommandInput(reader.getReader()));
                  reader.addPredicate(entity -> entity.getType().isIn(tagKey) != bl);
               } else {
                  Identifier identifier = Identifier.fromCommandInput(reader.getReader());
                  EntityType<?> entityType = Registries.ENTITY_TYPE.getOptionalValue(identifier).orElseThrow(() -> {
                     reader.getReader().setCursor(i);
                     return INVALID_TYPE_EXCEPTION.createWithContext(reader.getReader(), identifier.toString());
                  });
                  if (Objects.equals(EntityType.PLAYER, entityType) && !bl) {
                     reader.setIncludesNonPlayers(false);
                  }

                  reader.addPredicate(entity -> Objects.equals(entityType, entity.getType()) != bl);
                  if (!bl) {
                     reader.setEntityType(entityType);
                  }
               }
            }
         }, reader -> !reader.selectsEntityType(), Text.translatable("argument.entity.options.type.description"));
         putOption("tag", reader -> {
            boolean bl = reader.readNegationCharacter();
            String string = reader.getReader().readUnquotedString();
            reader.addPredicate(entity -> "".equals(string) ? entity.getCommandTags().isEmpty() != bl : entity.getCommandTags().contains(string) != bl);
         }, reader -> true, Text.translatable("argument.entity.options.tag.description"));
         putOption("nbt", reader -> {
            boolean bl = reader.readNegationCharacter();
            NbtCompound nbtCompound = StringNbtReader.readCompoundAsArgument(reader.getReader());
            reader.addPredicate(entity -> {
               boolean var9;
               try (ErrorReporter.Logging logging = new ErrorReporter.Logging(entity.getErrorReporterContext(), LOGGER)) {
                  NbtWriteView nbtWriteView = NbtWriteView.create(logging, entity.getRegistryManager());
                  entity.writeData(nbtWriteView);
                  if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
                     ItemStack itemStack = serverPlayerEntity.getInventory().getSelectedStack();
                     if (!itemStack.isEmpty()) {
                        nbtWriteView.put("SelectedItem", ItemStack.CODEC, itemStack);
                     }
                  }

                  var9 = NbtHelper.matches(nbtCompound, nbtWriteView.getNbt(), true) != bl;
               }

               return var9;
            });
         }, reader -> true, Text.translatable("argument.entity.options.nbt.description"));
         putOption("scores", reader -> {
            StringReader stringReader = reader.getReader();
            Map<String, NumberRange.IntRange> map = Maps.newHashMap();
            stringReader.expect('{');
            stringReader.skipWhitespace();

            while (stringReader.canRead() && stringReader.peek() != '}') {
               stringReader.skipWhitespace();
               String string = stringReader.readUnquotedString();
               stringReader.skipWhitespace();
               stringReader.expect('=');
               stringReader.skipWhitespace();
               NumberRange.IntRange intRange = NumberRange.IntRange.parse(stringReader);
               map.put(string, intRange);
               stringReader.skipWhitespace();
               if (stringReader.canRead() && stringReader.peek() == ',') {
                  stringReader.skip();
               }
            }

            stringReader.expect('}');
            if (!map.isEmpty()) {
               reader.addPredicate(entity -> {
                  Scoreboard scoreboard = entity.getEntityWorld().getServer().getScoreboard();

                  for (Entry<String, NumberRange.IntRange> entry : map.entrySet()) {
                     ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(entry.getKey());
                     if (scoreboardObjective == null) {
                        return false;
                     }

                     ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(entity, scoreboardObjective);
                     if (readableScoreboardScore == null) {
                        return false;
                     }

                     if (!entry.getValue().test(readableScoreboardScore.getScore())) {
                        return false;
                     }
                  }

                  return true;
               });
            }

            reader.setSelectsScores(true);
         }, reader -> !reader.selectsScores(), Text.translatable("argument.entity.options.scores.description"));
         putOption("advancements", reader -> {
            StringReader stringReader = reader.getReader();
            Map<Identifier, Predicate<AdvancementProgress>> map = Maps.newHashMap();
            stringReader.expect('{');
            stringReader.skipWhitespace();

            while (stringReader.canRead() && stringReader.peek() != '}') {
               stringReader.skipWhitespace();
               Identifier identifier = Identifier.fromCommandInput(stringReader);
               stringReader.skipWhitespace();
               stringReader.expect('=');
               stringReader.skipWhitespace();
               if (stringReader.canRead() && stringReader.peek() == '{') {
                  Map<String, Predicate<CriterionProgress>> map2 = Maps.newHashMap();
                  stringReader.skipWhitespace();
                  stringReader.expect('{');
                  stringReader.skipWhitespace();

                  while (stringReader.canRead() && stringReader.peek() != '}') {
                     stringReader.skipWhitespace();
                     String string = stringReader.readUnquotedString();
                     stringReader.skipWhitespace();
                     stringReader.expect('=');
                     stringReader.skipWhitespace();
                     boolean bl = stringReader.readBoolean();
                     map2.put(string, criterionProgress -> criterionProgress.isObtained() == bl);
                     stringReader.skipWhitespace();
                     if (stringReader.canRead() && stringReader.peek() == ',') {
                        stringReader.skip();
                     }
                  }

                  stringReader.skipWhitespace();
                  stringReader.expect('}');
                  stringReader.skipWhitespace();
                  map.put(identifier, advancementProgress -> {
                     for (Entry<String, Predicate<CriterionProgress>> entry : map2.entrySet()) {
                        CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
                        if (criterionProgress == null || !entry.getValue().test(criterionProgress)) {
                           return false;
                        }
                     }

                     return true;
                  });
               } else {
                  boolean bl2 = stringReader.readBoolean();
                  map.put(identifier, advancementProgress -> advancementProgress.isDone() == bl2);
               }

               stringReader.skipWhitespace();
               if (stringReader.canRead() && stringReader.peek() == ',') {
                  stringReader.skip();
               }
            }

            stringReader.expect('}');
            if (!map.isEmpty()) {
               reader.addPredicate(entity -> {
                  if (!(entity instanceof ServerPlayerEntity serverPlayerEntity)) {
                     return false;
                  } else {
                     PlayerAdvancementTracker playerAdvancementTracker = serverPlayerEntity.getAdvancementTracker();
                     ServerAdvancementLoader serverAdvancementLoader = serverPlayerEntity.getEntityWorld().getServer().getAdvancementLoader();

                     for (Entry<Identifier, Predicate<AdvancementProgress>> entry : map.entrySet()) {
                        AdvancementEntry advancementEntry = serverAdvancementLoader.get(entry.getKey());
                        if (advancementEntry == null || !entry.getValue().test(playerAdvancementTracker.getProgress(advancementEntry))) {
                           return false;
                        }
                     }

                     return true;
                  }
               });
               reader.setIncludesNonPlayers(false);
            }

            reader.setSelectsAdvancements(true);
         }, reader -> !reader.selectsAdvancements(), Text.translatable("argument.entity.options.advancements.description"));
         putOption(
            "predicate",
            reader -> {
               boolean bl = reader.readNegationCharacter();
               RegistryKey<LootCondition> registryKey = RegistryKey.of(RegistryKeys.PREDICATE, Identifier.fromCommandInput(reader.getReader()));
               reader.addPredicate(
                  entity -> {
                     if (entity.getEntityWorld() instanceof ServerWorld serverWorld) {
                        Optional<LootCondition> optional = serverWorld.getServer()
                           .getReloadableRegistries()
                           .createRegistryLookup()
                           .getOptionalEntry(registryKey)
                           .map(RegistryEntry::value);
                        if (optional.isEmpty()) {
                           return false;
                        } else {
                           LootWorldContext lootWorldContext = new LootWorldContext.Builder(serverWorld)
                              .add(LootContextParameters.THIS_ENTITY, entity)
                              .add(LootContextParameters.ORIGIN, entity.getEntityPos())
                              .build(LootContextTypes.SELECTOR);
                           LootContext lootContext = new LootContext.Builder(lootWorldContext).build(Optional.empty());
                           lootContext.markActive(LootContext.predicate(optional.get()));
                           return bl ^ optional.get().test(lootContext);
                        }
                     } else {
                        return false;
                     }
                  }
               );
            },
            reader -> true,
            Text.translatable("argument.entity.options.predicate.description")
         );
      }
   }

   public static EntitySelectorOptions.SelectorHandler getHandler(EntitySelectorReader reader, String option, int restoreCursor) throws CommandSyntaxException {
      EntitySelectorOptions.SelectorOption selectorOption = OPTIONS.get(option);
      if (selectorOption != null) {
         if (selectorOption.condition.test(reader)) {
            return selectorOption.handler;
         } else {
            throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
         }
      } else {
         reader.getReader().setCursor(restoreCursor);
         throw UNKNOWN_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
      }
   }

   public static void suggestOptions(EntitySelectorReader reader, SuggestionsBuilder suggestionBuilder) {
      String string = suggestionBuilder.getRemaining().toLowerCase(Locale.ROOT);

      for (Entry<String, EntitySelectorOptions.SelectorOption> entry : OPTIONS.entrySet()) {
         if (entry.getValue().condition.test(reader) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(string)) {
            suggestionBuilder.suggest(entry.getKey() + "=", entry.getValue().description);
         }
      }
   }

   @FunctionalInterface
   public interface SelectorHandler {
      void handle(EntitySelectorReader reader) throws CommandSyntaxException;
   }

   record SelectorOption(EntitySelectorOptions.SelectorHandler handler, Predicate<EntitySelectorReader> condition, Text description) {
   }
}
