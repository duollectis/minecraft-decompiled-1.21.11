package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.argument.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;

import java.util.Collection;
import java.util.Collections;

/**
 * {@code SpawnPointCommand}.
 */
public class SpawnPointCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("spawnpoint")
						.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
				)
						.executes(
								context -> execute(
										(ServerCommandSource) context.getSource(),
										Collections.singleton(((ServerCommandSource) context.getSource()).getPlayerOrThrow()),
										BlockPos.ofFloored(((ServerCommandSource) context.getSource()).getPosition()),
										DefaultPosArgument.DEFAULT_ROTATION
								)
						)
				)
						.then(
								((RequiredArgumentBuilder) CommandManager
										.argument("targets", EntityArgumentType.players())
										.executes(
												context -> execute(
														(ServerCommandSource) context.getSource(),
														EntityArgumentType.getPlayers(context, "targets"),
														BlockPos.ofFloored(((ServerCommandSource) context.getSource()).getPosition()),
														DefaultPosArgument.DEFAULT_ROTATION
												)
										)
								)
										.then(
												((RequiredArgumentBuilder) CommandManager
														.argument("pos", BlockPosArgumentType.blockPos())
														.executes(
																context -> execute(
																		(ServerCommandSource) context.getSource(),
																		EntityArgumentType.getPlayers(
																				context,
																				"targets"
																		),
																		BlockPosArgumentType.getValidBlockPos(
																				context,
																				"pos"
																		),
																		DefaultPosArgument.DEFAULT_ROTATION
																)
														)
												)
														.then(
																CommandManager
																		.argument(
																				"rotation",
																				RotationArgumentType.rotation()
																		)
																		.executes(
																				context -> execute(
																						(ServerCommandSource) context.getSource(),
																						EntityArgumentType.getPlayers(
																								context,
																								"targets"
																						),
																						BlockPosArgumentType.getValidBlockPos(
																								context,
																								"pos"
																						),
																						RotationArgumentType.getRotation(
																								context,
																								"rotation"
																						)
																				)
																		)
														)
										)
						)
		);
	}

	private static int execute(
			ServerCommandSource source,
			Collection<ServerPlayerEntity> targets,
			BlockPos pos,
			PosArgument rotation
	) {
		RegistryKey<World> registryKey = source.getWorld().getRegistryKey();
		Vec2f vec2f = rotation.getRotation(source);
		float f = MathHelper.wrapDegrees(vec2f.y);
		float g = MathHelper.clamp(vec2f.x, -90.0F, 90.0F);

		for (ServerPlayerEntity serverPlayerEntity : targets) {
			serverPlayerEntity.setSpawnPoint(
					new ServerPlayerEntity.Respawn(
							WorldProperties.SpawnPoint.create(
									registryKey,
									pos,
									f,
									g
							), true
					), false
			);
		}

		String string = registryKey.getValue().toString();
		if (targets.size() == 1) {
			source.sendFeedback(
					() -> Text.translatable(
							"commands.spawnpoint.success.single",
							pos.getX(),
							pos.getY(),
							pos.getZ(),
							f,
							g,
							string,
							targets.iterator().next().getDisplayName()
					),
					true
			);
		}
		else {
			source.sendFeedback(
					() -> Text.translatable(
							"commands.spawnpoint.success.multiple",
							pos.getX(),
							pos.getY(),
							pos.getZ(),
							f,
							g,
							string,
							targets.size()
					), true
			);
		}

		return targets.size();
	}
}
