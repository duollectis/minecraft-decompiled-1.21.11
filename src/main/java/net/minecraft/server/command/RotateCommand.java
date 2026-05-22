package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;

/**
 * Команда {@code /rotate}: изменение поворота сущностей.
 */
public class RotateCommand {

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("rotate")
				                                                                 .requires(CommandManager.requirePermissionLevel(
						                                                                 CommandManager.GAMEMASTERS_CHECK))
				)
						.then(
								((RequiredArgumentBuilder) CommandManager
										.argument("target", EntityArgumentType.entity())
										.then(
												CommandManager.argument("rotation", RotationArgumentType.rotation())
												              .executes(
														              context -> rotateToPos(
																              (ServerCommandSource) context.getSource(),
																              EntityArgumentType.getEntity(
																		              context,
																		              "target"
																              ),
																              RotationArgumentType.getRotation(
																		              context,
																		              "rotation"
																              )
														              )
												              )
										)
								)
										.then(
												((LiteralArgumentBuilder) CommandManager.literal("facing")
												                                        .then(
														                                        CommandManager
																                                        .literal(
																		                                        "entity")
																                                        .then(
																		                                        ((RequiredArgumentBuilder) CommandManager
																				                                        .argument(
																						                                        "facingEntity",
																						                                        EntityArgumentType.entity()
																				                                        )
																				                                        .executes(
																						                                        context -> rotateFacingLookTarget(
																								                                        (ServerCommandSource) context.getSource(),
																								                                        EntityArgumentType.getEntity(
																										                                        context,
																										                                        "target"
																								                                        ),
																								                                        new LookTarget.LookAtEntity(
																										                                        EntityArgumentType.getEntity(
																												                                        context,
																												                                        "facingEntity"
																										                                        ),
																										                                        EntityAnchorArgumentType.EntityAnchor.FEET
																								                                        )
																						                                        )
																				                                        )
																		                                        )
																				                                        .then(
																						                                        CommandManager
																								                                        .argument(
																										                                        "facingAnchor",
																										                                        EntityAnchorArgumentType.entityAnchor()
																								                                        )
																								                                        .executes(
																										                                        context -> rotateFacingLookTarget(
																												                                        (ServerCommandSource) context.getSource(),
																												                                        EntityArgumentType.getEntity(
																														                                        context,
																														                                        "target"
																												                                        ),
																												                                        new LookTarget.LookAtEntity(
																														                                        EntityArgumentType.getEntity(
																																                                        context,
																																                                        "facingEntity"
																														                                        ),
																														                                        EntityAnchorArgumentType.getEntityAnchor(
																																                                        context,
																																                                        "facingAnchor"
																														                                        )
																												                                        )
																										                                        )
																								                                        )
																				                                        )
																                                        )
												                                        )
												)
														.then(
																CommandManager
																		.argument(
																				"facingLocation",
																				Vec3ArgumentType.vec3()
																		)
																		.executes(
																				context -> rotateFacingLookTarget(
																						(ServerCommandSource) context.getSource(),
																						EntityArgumentType.getEntity(
																								context,
																								"target"
																						),
																						new LookTarget.LookAtPosition(
																								Vec3ArgumentType.getVec3(
																										context,
																										"facingLocation"
																								))
																				)
																		)
														)
										)
						)
		);
	}

	private static int rotateToPos(ServerCommandSource source, Entity entity, PosArgument pos) {
		Vec2f vec2f = pos.getRotation(source);
		float f = pos.isYRelative() ? vec2f.y - entity.getYaw() : vec2f.y;
		float g = pos.isXRelative() ? vec2f.x - entity.getPitch() : vec2f.x;
		entity.rotate(f, pos.isYRelative(), g, pos.isXRelative());
		source.sendFeedback(() -> Text.translatable("commands.rotate.success", entity.getDisplayName()), true);
		return 1;
	}

	private static int rotateFacingLookTarget(ServerCommandSource source, Entity entity, LookTarget lookTarget) {
		lookTarget.look(source, entity);
		source.sendFeedback(() -> Text.translatable("commands.rotate.success", entity.getDisplayName()), true);
		return 1;
	}
}
