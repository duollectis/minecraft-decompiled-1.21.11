package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.network.packet.s2c.common.ClearDialogS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DialogCommand {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("dialog")
                  .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)))
               .then(
                  CommandManager.literal("show")
                     .then(
                        CommandManager.argument("targets", EntityArgumentType.players())
                           .then(
                              CommandManager.argument("dialog", RegistryEntryArgumentType.dialog(registryAccess))
                                 .executes(
                                    context -> executeShow(
                                       (ServerCommandSource)context.getSource(),
                                       EntityArgumentType.getPlayers(context, "targets"),
                                       RegistryEntryArgumentType.getDialog(context, "dialog")
                                    )
                                 )
                           )
                     )
               ))
            .then(
               CommandManager.literal("clear")
                  .then(
                     CommandManager.argument("targets", EntityArgumentType.players())
                        .executes(context -> executeClear((ServerCommandSource)context.getSource(), EntityArgumentType.getPlayers(context, "targets")))
                  )
            )
      );
   }

   private static int executeShow(ServerCommandSource source, Collection<ServerPlayerEntity> players, RegistryEntry<Dialog> dialog) {
      for (ServerPlayerEntity serverPlayerEntity : players) {
         serverPlayerEntity.openDialog(dialog);
      }

      if (players.size() == 1) {
         source.sendFeedback(() -> Text.translatable("commands.dialog.show.single", players.iterator().next().getDisplayName()), true);
      } else {
         source.sendFeedback(() -> Text.translatable("commands.dialog.show.multiple", players.size()), true);
      }

      return players.size();
   }

   private static int executeClear(ServerCommandSource source, Collection<ServerPlayerEntity> players) {
      for (ServerPlayerEntity serverPlayerEntity : players) {
         serverPlayerEntity.networkHandler.sendPacket(ClearDialogS2CPacket.INSTANCE);
      }

      if (players.size() == 1) {
         source.sendFeedback(() -> Text.translatable("commands.dialog.clear.single", players.iterator().next().getDisplayName()), true);
      } else {
         source.sendFeedback(() -> Text.translatable("commands.dialog.clear.multiple", players.size()), true);
      }

      return players.size();
   }
}
