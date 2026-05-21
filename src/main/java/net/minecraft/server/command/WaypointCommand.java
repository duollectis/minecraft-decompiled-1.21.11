package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.waypoint.ServerWaypoint;
import net.minecraft.world.waypoint.Waypoint;
import net.minecraft.world.waypoint.WaypointStyle;
import net.minecraft.world.waypoint.WaypointStyles;

import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@code WaypointCommand}.
 */
public class WaypointCommand {

	public static void register(
			CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess registryAccess
	) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("waypoint")
						.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
				)
						.then(CommandManager
								.literal("list")
								.executes(context -> executeList((ServerCommandSource) context.getSource())))
				)
						.then(
								CommandManager.literal("modify")
								              .then(
										              ((RequiredArgumentBuilder) CommandManager
												              .argument("waypoint", EntityArgumentType.entity())
												              .then(
														              ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
																              .literal("color")
																              .then(
																		              CommandManager
																				              .argument(
																						              "color",
																						              ColorArgumentType.color()
																				              )
																				              .executes(
																						              context -> executeColor(
																								              (ServerCommandSource) context.getSource(),
																								              WaypointArgument.getWaypoint(
																										              context,
																										              "waypoint"
																								              ),
																								              ColorArgumentType.getColor(
																										              context,
																										              "color"
																								              )
																						              )
																				              )
																              )
														              )
																              .then(
																		              CommandManager.literal("hex")
																		                            .then(
																				                            CommandManager
																						                            .argument(
																								                            "color",
																								                            HexColorArgumentType.hexColor()
																						                            )
																						                            .executes(
																								                            context -> executeColor(
																										                            (ServerCommandSource) context.getSource(),
																										                            WaypointArgument.getWaypoint(
																												                            context,
																												                            "waypoint"
																										                            ),
																										                            HexColorArgumentType.getArgbColor(
																												                            context,
																												                            "color"
																										                            )
																								                            )
																						                            )
																		                            )
																              )
														              )
																              .then(
																		              CommandManager.literal("reset")
																		                            .executes(
																				                            context -> executeReset(
																						                            (ServerCommandSource) context.getSource(),
																						                            WaypointArgument.getWaypoint(
																								                            context,
																								                            "waypoint"
																						                            )
																				                            )
																		                            )
																              )
												              )
										              )
												              .then(
														              ((LiteralArgumentBuilder) CommandManager
																              .literal("style")
																              .then(
																		              CommandManager.literal("reset")
																		                            .executes(
																				                            context -> executeStyle(
																						                            (ServerCommandSource) context.getSource(),
																						                            WaypointArgument.getWaypoint(
																								                            context,
																								                            "waypoint"
																						                            ),
																						                            WaypointStyles.DEFAULT
																				                            )
																		                            )
																              )
														              )
																              .then(
																		              CommandManager.literal("set")
																		                            .then(
																				                            CommandManager
																						                            .argument(
																								                            "style",
																								                            IdentifierArgumentType.identifier()
																						                            )
																						                            .executes(
																								                            context -> executeStyle(
																										                            (ServerCommandSource) context.getSource(),
																										                            WaypointArgument.getWaypoint(
																												                            context,
																												                            "waypoint"
																										                            ),
																										                            RegistryKey.of(
																												                            WaypointStyles.REGISTRY,
																												                            IdentifierArgumentType.getIdentifier(
																														                            context,
																														                            "style"
																												                            )
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

	private static int executeStyle(
			ServerCommandSource source,
			ServerWaypoint waypoint,
			RegistryKey<WaypointStyle> style
	) {
		updateWaypointConfig(source, waypoint, config -> config.style = style);
		source.sendFeedback(() -> Text.translatable("commands.waypoint.modify.style"), false);
		return 0;
	}

	private static int executeColor(ServerCommandSource source, ServerWaypoint waypoint, Formatting color) {
		updateWaypointConfig(source, waypoint, config -> config.color = Optional.of(color.getColorValue()));
		source.sendFeedback(
				() -> Text.translatable(
						"commands.waypoint.modify.color",
						Text.literal(color.getName()).formatted(color)
				), false
		);
		return 0;
	}

	private static int executeColor(ServerCommandSource source, ServerWaypoint waypoint, Integer color) {
		updateWaypointConfig(source, waypoint, config -> config.color = Optional.of(color));
		source.sendFeedback(
				() -> Text.translatable(
						"commands.waypoint.modify.color",
						Text
								.literal(HexFormat.of().withUpperCase().toHexDigits(ColorHelper.withAlpha(0, color), 6))
								.withColor(color)
				),
				false
		);
		return 0;
	}

	private static int executeReset(ServerCommandSource source, ServerWaypoint waypoint) {
		updateWaypointConfig(source, waypoint, config -> config.color = Optional.empty());
		source.sendFeedback(() -> Text.translatable("commands.waypoint.modify.color.reset"), false);
		return 0;
	}

	private static int executeList(ServerCommandSource source) {
		ServerWorld serverWorld = source.getWorld();
		Set<ServerWaypoint> set = serverWorld.getWaypointHandler().getWaypoints();
		String string = serverWorld.getRegistryKey().getValue().toString();
		if (set.isEmpty()) {
			source.sendFeedback(() -> Text.translatable("commands.waypoint.list.empty", string), false);
			return 0;
		}
		else {
			Text text = Texts.join(
					set.stream()
					   .map(
							   waypoint -> {
								   if (waypoint instanceof LivingEntity livingEntity) {
									   BlockPos blockPos = livingEntity.getBlockPos();
									   return livingEntity.getStyledDisplayName()
									                      .copy()
									                      .styled(
											                      style -> style.withClickEvent(
													                                    new ClickEvent.SuggestCommand(
															                                    "/execute in " + string
																	                                    + " run tp @s "
																	                                    + blockPos.getX() + " "
																	                                    + blockPos.getY() + " "
																	                                    + blockPos.getZ()
													                                    )
											                                    )
											                                    .withHoverEvent(new HoverEvent.ShowText(
													                                    Text.translatable(
															                                    "chat.coordinates.tooltip")))
											                                    .withColor(waypoint.getWaypointConfig().color.orElse(
													                                    -1))
									                      );
								   }
								   else {
									   return Text.literal(waypoint.toString());
								   }
							   }
					   )
					   .toList(),
					Function.identity()
			);
			source.sendFeedback(
					() -> Text.translatable("commands.waypoint.list.success", set.size(), string, text),
					false
			);
			return set.size();
		}
	}

	private static void updateWaypointConfig(
			ServerCommandSource source,
			ServerWaypoint waypoint,
			Consumer<Waypoint.Config> configConsumer
	) {
		ServerWorld serverWorld = source.getWorld();
		serverWorld.getWaypointHandler().onUntrack(waypoint);
		configConsumer.accept(waypoint.getWaypointConfig());
		serverWorld.getWaypointHandler().onTrack(waypoint);
	}
}
