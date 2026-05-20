package net.minecraft.server.command;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public class EffectCommand {
   private static final SimpleCommandExceptionType GIVE_FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.effect.give.failed"));
   private static final SimpleCommandExceptionType CLEAR_EVERYTHING_FAILED_EXCEPTION = new SimpleCommandExceptionType(
      Text.translatable("commands.effect.clear.everything.failed")
   );
   private static final SimpleCommandExceptionType CLEAR_SPECIFIC_FAILED_EXCEPTION = new SimpleCommandExceptionType(
      Text.translatable("commands.effect.clear.specific.failed")
   );

   public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("effect")
                  .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)))
               .then(
                  ((LiteralArgumentBuilder)CommandManager.literal("clear")
                        .executes(
                           context -> executeClear(
                              (ServerCommandSource)context.getSource(), ImmutableList.of(((ServerCommandSource)context.getSource()).getEntityOrThrow())
                           )
                        ))
                     .then(
                        ((RequiredArgumentBuilder)CommandManager.argument("targets", EntityArgumentType.entities())
                              .executes(context -> executeClear((ServerCommandSource)context.getSource(), EntityArgumentType.getEntities(context, "targets"))))
                           .then(
                              CommandManager.argument("effect", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.STATUS_EFFECT))
                                 .executes(
                                    context -> executeClear(
                                       (ServerCommandSource)context.getSource(),
                                       EntityArgumentType.getEntities(context, "targets"),
                                       RegistryEntryReferenceArgumentType.getStatusEffect(context, "effect")
                                    )
                                 )
                           )
                     )
               ))
            .then(
               CommandManager.literal("give")
                  .then(
                     CommandManager.argument("targets", EntityArgumentType.entities())
                        .then(
                           ((RequiredArgumentBuilder)((RequiredArgumentBuilder)CommandManager.argument(
                                       "effect", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.STATUS_EFFECT)
                                    )
                                    .executes(
                                       context -> executeGive(
                                          (ServerCommandSource)context.getSource(),
                                          EntityArgumentType.getEntities(context, "targets"),
                                          RegistryEntryReferenceArgumentType.getStatusEffect(context, "effect"),
                                          null,
                                          0,
                                          true
                                       )
                                    ))
                                 .then(
                                    ((RequiredArgumentBuilder)CommandManager.argument("seconds", IntegerArgumentType.integer(1, 1000000))
                                          .executes(
                                             context -> executeGive(
                                                (ServerCommandSource)context.getSource(),
                                                EntityArgumentType.getEntities(context, "targets"),
                                                RegistryEntryReferenceArgumentType.getStatusEffect(context, "effect"),
                                                IntegerArgumentType.getInteger(context, "seconds"),
                                                0,
                                                true
                                             )
                                          ))
                                       .then(
                                          ((RequiredArgumentBuilder)CommandManager.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                                .executes(
                                                   context -> executeGive(
                                                      (ServerCommandSource)context.getSource(),
                                                      EntityArgumentType.getEntities(context, "targets"),
                                                      RegistryEntryReferenceArgumentType.getStatusEffect(context, "effect"),
                                                      IntegerArgumentType.getInteger(context, "seconds"),
                                                      IntegerArgumentType.getInteger(context, "amplifier"),
                                                      true
                                                   )
                                                ))
                                             .then(
                                                CommandManager.argument("hideParticles", BoolArgumentType.bool())
                                                   .executes(
                                                      context -> executeGive(
                                                         (ServerCommandSource)context.getSource(),
                                                         EntityArgumentType.getEntities(context, "targets"),
                                                         RegistryEntryReferenceArgumentType.getStatusEffect(context, "effect"),
                                                         IntegerArgumentType.getInteger(context, "seconds"),
                                                         IntegerArgumentType.getInteger(context, "amplifier"),
                                                         !BoolArgumentType.getBool(context, "hideParticles")
                                                      )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)CommandManager.literal("infinite")
                                       .executes(
                                          context -> executeGive(
                                             (ServerCommandSource)context.getSource(),
                                             EntityArgumentType.getEntities(context, "targets"),
                                             RegistryEntryReferenceArgumentType.getStatusEffect(context, "effect"),
                                             -1,
                                             0,
                                             true
                                          )
                                       ))
                                    .then(
                                       ((RequiredArgumentBuilder)CommandManager.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                             .executes(
                                                context -> executeGive(
                                                   (ServerCommandSource)context.getSource(),
                                                   EntityArgumentType.getEntities(context, "targets"),
                                                   RegistryEntryReferenceArgumentType.getStatusEffect(context, "effect"),
                                                   -1,
                                                   IntegerArgumentType.getInteger(context, "amplifier"),
                                                   true
                                                )
                                             ))
                                          .then(
                                             CommandManager.argument("hideParticles", BoolArgumentType.bool())
                                                .executes(
                                                   context -> executeGive(
                                                      (ServerCommandSource)context.getSource(),
                                                      EntityArgumentType.getEntities(context, "targets"),
                                                      RegistryEntryReferenceArgumentType.getStatusEffect(context, "effect"),
                                                      -1,
                                                      IntegerArgumentType.getInteger(context, "amplifier"),
                                                      !BoolArgumentType.getBool(context, "hideParticles")
                                                   )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int executeGive(
      ServerCommandSource source,
      Collection<? extends Entity> targets,
      RegistryEntry<StatusEffect> statusEffect,
      @Nullable Integer seconds,
      int amplifier,
      boolean showParticles
   ) throws CommandSyntaxException {
      StatusEffect statusEffect2 = statusEffect.value();
      int i = 0;
      int j;
      if (seconds != null) {
         if (statusEffect2.isInstant()) {
            j = seconds;
         } else if (seconds == -1) {
            j = -1;
         } else {
            j = seconds * 20;
         }
      } else if (statusEffect2.isInstant()) {
         j = 1;
      } else {
         j = 600;
      }

      for (Entity entity : targets) {
         if (entity instanceof LivingEntity) {
            StatusEffectInstance statusEffectInstance = new StatusEffectInstance(statusEffect, j, amplifier, false, showParticles);
            if (((LivingEntity)entity).addStatusEffect(statusEffectInstance, source.getEntity())) {
               i++;
            }
         }
      }

      if (i == 0) {
         throw GIVE_FAILED_EXCEPTION.create();
      } else {
         if (targets.size() == 1) {
            source.sendFeedback(
               () -> Text.translatable("commands.effect.give.success.single", statusEffect2.getName(), targets.iterator().next().getDisplayName(), j / 20),
               true
            );
         } else {
            source.sendFeedback(() -> Text.translatable("commands.effect.give.success.multiple", statusEffect2.getName(), targets.size(), j / 20), true);
         }

         return i;
      }
   }

   private static int executeClear(ServerCommandSource source, Collection<? extends Entity> targets) throws CommandSyntaxException {
      int i = 0;

      for (Entity entity : targets) {
         if (entity instanceof LivingEntity && ((LivingEntity)entity).clearStatusEffects()) {
            i++;
         }
      }

      if (i == 0) {
         throw CLEAR_EVERYTHING_FAILED_EXCEPTION.create();
      } else {
         if (targets.size() == 1) {
            source.sendFeedback(() -> Text.translatable("commands.effect.clear.everything.success.single", targets.iterator().next().getDisplayName()), true);
         } else {
            source.sendFeedback(() -> Text.translatable("commands.effect.clear.everything.success.multiple", targets.size()), true);
         }

         return i;
      }
   }

   private static int executeClear(ServerCommandSource source, Collection<? extends Entity> targets, RegistryEntry<StatusEffect> statusEffect) throws CommandSyntaxException {
      StatusEffect statusEffect2 = statusEffect.value();
      int i = 0;

      for (Entity entity : targets) {
         if (entity instanceof LivingEntity && ((LivingEntity)entity).removeStatusEffect(statusEffect)) {
            i++;
         }
      }

      if (i == 0) {
         throw CLEAR_SPECIFIC_FAILED_EXCEPTION.create();
      } else {
         if (targets.size() == 1) {
            source.sendFeedback(
               () -> Text.translatable("commands.effect.clear.specific.success.single", statusEffect2.getName(), targets.iterator().next().getDisplayName()),
               true
            );
         } else {
            source.sendFeedback(() -> Text.translatable("commands.effect.clear.specific.success.multiple", statusEffect2.getName(), targets.size()), true);
         }

         return i;
      }
   }
}
