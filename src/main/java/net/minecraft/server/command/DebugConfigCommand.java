package net.minecraft.server.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.common.ShowDialogS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@code DebugConfigCommand}.
 */
public class DebugConfigCommand {

	public static void register(
			CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess registryAccess
	) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("debugconfig")
						.requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
				)
						.then(
								CommandManager.literal("config")
								              .then(
										              CommandManager.argument("target", EntityArgumentType.player())
										                            .executes(context -> executeConfig(
												                            (ServerCommandSource) context.getSource(),
												                            EntityArgumentType.getPlayer(
														                            context,
														                            "target"
												                            )
										                            ))
								              )
						)
				)
						.then(
								CommandManager.literal("unconfig")
								              .then(
										              CommandManager.argument("target", UuidArgumentType.uuid())
										                            .suggests(
												                            (context, suggestionsBuilder) -> CommandSource.suggestMatching(
														                            collectConfiguringPlayers(((ServerCommandSource) context.getSource()).getServer()),
														                            suggestionsBuilder
												                            )
										                            )
										                            .executes(context -> executeUnconfig(
												                            (ServerCommandSource) context.getSource(),
												                            UuidArgumentType.getUuid(context, "target")
										                            ))
								              )
						)
				)
						.then(
								CommandManager.literal("dialog")
								              .then(
										              CommandManager.argument("target", UuidArgumentType.uuid())
										                            .suggests(
												                            (context, suggestionsBuilder) -> CommandSource.suggestMatching(
														                            collectConfiguringPlayers(((ServerCommandSource) context.getSource()).getServer()),
														                            suggestionsBuilder
												                            )
										                            )
										                            .then(
												                            CommandManager
														                            .argument(
																                            "dialog",
																                            RegistryEntryArgumentType.dialog(
																		                            registryAccess)
														                            )
														                            .executes(
																                            context -> executeDialog(
																		                            (ServerCommandSource) context.getSource(),
																		                            UuidArgumentType.getUuid(
																				                            context,
																				                            "target"
																		                            ),
																		                            RegistryEntryArgumentType.getDialog(
																				                            context,
																				                            "dialog"
																		                            )
																                            )
														                            )
										                            )
								              )
						)
		);
	}

	private static Iterable<String> collectConfiguringPlayers(MinecraftServer server) {
		Set<String> set = new HashSet<>();

		for (ClientConnection clientConnection : server.getNetworkIo().getConnections()) {
			if (clientConnection.getPacketListener() instanceof ServerConfigurationNetworkHandler serverConfigurationNetworkHandler) {
				set.add(serverConfigurationNetworkHandler.getDebugProfile().id().toString());
			}
		}

		return set;
	}

	private static int executeConfig(ServerCommandSource source, ServerPlayerEntity player) {
		GameProfile gameProfile = player.getGameProfile();
		player.networkHandler.reconfigure();
		source.sendFeedback(
				() -> Text.literal(
						"Switched player " + gameProfile.name() + "(" + gameProfile.id() + ") to config mode"), false
		);
		return 1;
	}

	private static @Nullable ServerConfigurationNetworkHandler findConfigurationNetworkHandler(
			MinecraftServer server,
			UUID uuid
	) {
		for (ClientConnection clientConnection : server.getNetworkIo().getConnections()) {
			if (clientConnection.getPacketListener() instanceof ServerConfigurationNetworkHandler serverConfigurationNetworkHandler
					&& serverConfigurationNetworkHandler.getDebugProfile().id().equals(uuid)) {
				return serverConfigurationNetworkHandler;
			}
		}

		return null;
	}

	private static int executeUnconfig(ServerCommandSource source, UUID uuid) {
		ServerConfigurationNetworkHandler
				serverConfigurationNetworkHandler =
				findConfigurationNetworkHandler(source.getServer(), uuid);
		if (serverConfigurationNetworkHandler != null) {
			serverConfigurationNetworkHandler.endConfiguration();
			return 1;
		}
		else {
			source.sendError(Text.literal("Can't find player to unconfig"));
			return 0;
		}
	}

	private static int executeDialog(ServerCommandSource source, UUID uuid, RegistryEntry<Dialog> dialog) {
		ServerConfigurationNetworkHandler
				serverConfigurationNetworkHandler =
				findConfigurationNetworkHandler(source.getServer(), uuid);
		if (serverConfigurationNetworkHandler != null) {
			serverConfigurationNetworkHandler.sendPacket(new ShowDialogS2CPacket(dialog));
			return 1;
		}
		else {
			source.sendError(Text.literal("Can't find player to talk to"));
			return 0;
		}
	}
}
