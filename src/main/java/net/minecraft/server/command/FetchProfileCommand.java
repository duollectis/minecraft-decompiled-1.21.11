package net.minecraft.server.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtOps;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.GameProfileResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.*;
import net.minecraft.text.object.PlayerTextObjectContents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code FetchProfileCommand}.
 */
public class FetchProfileCommand {

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("fetchprofile")
						.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
				)
						.then(
								CommandManager.literal("name")
								              .then(
										              CommandManager.argument("name", StringArgumentType.greedyString())
										                            .executes(context -> executeName(
												                            (ServerCommandSource) context.getSource(),
												                            StringArgumentType.getString(
														                            context,
														                            "name"
												                            )
										                            ))
								              )
						)
				)
						.then(
								CommandManager.literal("id")
								              .then(
										              CommandManager.argument("id", UuidArgumentType.uuid())
										                            .executes(context -> executeId(
												                            (ServerCommandSource) context.getSource(),
												                            UuidArgumentType.getUuid(context, "id")
										                            ))
								              )
						)
		);
	}

	private static void sendResult(
			ServerCommandSource source,
			GameProfile profile,
			String successText,
			Text inputText
	) {
		ProfileComponent profileComponent = ProfileComponent.ofStatic(profile);
		ProfileComponent.CODEC
				.encodeStart(NbtOps.INSTANCE, profileComponent)
				.ifSuccess(
						profileComponentx -> {
							String string2 = profileComponentx.toString();
							MutableText mutableText = Text.object(new PlayerTextObjectContents(profileComponent, true));
							TextCodecs.CODEC
									.encodeStart(NbtOps.INSTANCE, mutableText)
									.ifSuccess(
											headText -> {
												String string3 = headText.toString();
												source.sendFeedback(
														() -> {
															Text text2 = Texts.join(
																	List.of(
																			Text
																					.translatable(
																							"commands.fetchprofile.copy_component")
																					.styled(style -> style.withClickEvent(
																							new ClickEvent.CopyToClipboard(
																									string2))),
																			Text
																					.translatable(
																							"commands.fetchprofile.give_item")
																					.styled(
																							style -> style.withClickEvent(
																									new ClickEvent.RunCommand(
																											"give @s minecraft:player_head[profile="
																													+ string2
																													+ "]"))
																					),
																			Text
																					.translatable(
																							"commands.fetchprofile.summon_mannequin")
																					.styled(
																							style -> style.withClickEvent(
																									new ClickEvent.RunCommand(
																											"summon minecraft:mannequin ~ ~ ~ {profile:"
																													+ string2
																													+ "}"))
																					),
																			Text
																					.translatable(
																							"commands.fetchprofile.copy_text",
																							mutableText.formatted(
																									Formatting.WHITE)
																					)
																					.styled(style -> style.withClickEvent(
																							new ClickEvent.CopyToClipboard(
																									string3)))
																	),
																	ScreenTexts.SPACE,
																	style -> Texts.bracketed(style.formatted(Formatting.GREEN))
															);
															return Text.translatable(successText, inputText, text2);
														},
														false
												);
											}
									)
									.ifError(error -> source.sendError(Text.translatable(
											"commands.fetchprofile.failed_to_serialize",
											error.message()
									)));
						}
				)
				.ifError(error -> source.sendError(Text.translatable(
						"commands.fetchprofile.failed_to_serialize",
						error.message()
				)));
	}

	private static int executeName(ServerCommandSource source, String name) {
		MinecraftServer minecraftServer = source.getServer();
		GameProfileResolver gameProfileResolver = minecraftServer.getApiServices().profileResolver();
		Util.getDownloadWorkerExecutor()
		    .execute(
				    () -> {
					    Text text = Text.literal(name);
					    Optional<GameProfile> optional = gameProfileResolver.getProfileByName(name);
					    minecraftServer.execute(
							    () -> optional.ifPresentOrElse(
									    profile -> sendResult(
											    source,
											    profile,
											    "commands.fetchprofile.name.success",
											    text
									    ),
									    () -> source.sendError(Text.translatable(
											    "commands.fetchprofile.name.failure",
											    text
									    ))
							    )
					    );
				    }
		    );
		return 1;
	}

	private static int executeId(ServerCommandSource source, UUID id) {
		MinecraftServer minecraftServer = source.getServer();
		GameProfileResolver gameProfileResolver = minecraftServer.getApiServices().profileResolver();
		Util.getDownloadWorkerExecutor()
		    .execute(
				    () -> {
					    Text text = Text.of(id);
					    Optional<GameProfile> optional = gameProfileResolver.getProfileById(id);
					    minecraftServer.execute(
							    () -> optional.ifPresentOrElse(
									    profile -> sendResult(
											    source,
											    profile,
											    "commands.fetchprofile.id.success",
											    text
									    ),
									    () -> source.sendError(Text.translatable(
											    "commands.fetchprofile.id.failure",
											    text
									    ))
							    )
					    );
				    }
		    );
		return 1;
	}
}
