package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * {@code TeleportCommand}.
 */
public class TeleportCommand {

	private static final SimpleCommandExceptionType INVALID_POSITION_EXCEPTION = new SimpleCommandExceptionType(
			Text.translatable("commands.teleport.invalidPosition")
	);

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("teleport")
						.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
				)
						.then(
								CommandManager.argument("location", Vec3ArgumentType.vec3())
								              .executes(
										              context -> execute(
												              (ServerCommandSource) context.getSource(),
												              Collections.singleton(((ServerCommandSource) context.getSource()).getEntityOrThrow()),
												              ((ServerCommandSource) context.getSource()).getWorld(),
												              Vec3ArgumentType.getPosArgument(context, "location"),
												              null,
												              null
										              )
								              )
						)
				)
						.then(
								CommandManager.argument("destination", EntityArgumentType.entity())
								              .executes(
										              context -> execute(
												              (ServerCommandSource) context.getSource(),
												              Collections.singleton(((ServerCommandSource) context.getSource()).getEntityOrThrow()),
												              EntityArgumentType.getEntity(context, "destination")
										              )
								              )
						)
				)
						.then(
								((RequiredArgumentBuilder) CommandManager
										.argument("targets", EntityArgumentType.entities())
										.then(
												((RequiredArgumentBuilder) ((RequiredArgumentBuilder) CommandManager
														.argument("location", Vec3ArgumentType.vec3())
														.executes(
																context -> execute(
																		(ServerCommandSource) context.getSource(),
																		EntityArgumentType.getEntities(
																				context,
																				"targets"
																		),
																		((ServerCommandSource) context.getSource()).getWorld(),
																		Vec3ArgumentType.getPosArgument(
																				context,
																				"location"
																		),
																		null,
																		null
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
																						EntityArgumentType.getEntities(
																								context,
																								"targets"
																						),
																						((ServerCommandSource) context.getSource()).getWorld(),
																						Vec3ArgumentType.getPosArgument(
																								context,
																								"location"
																						),
																						RotationArgumentType.getRotation(
																								context,
																								"rotation"
																						),
																						null
																				)
																		)
														)
												)
														.then(
																((LiteralArgumentBuilder) CommandManager
																		.literal("facing")
																		.then(
																				CommandManager.literal("entity")
																				              .then(
																						              ((RequiredArgumentBuilder) CommandManager
																								              .argument(
																										              "facingEntity",
																										              EntityArgumentType.entity()
																								              )
																								              .executes(
																										              context -> execute(
																												              (ServerCommandSource) context.getSource(),
																												              EntityArgumentType.getEntities(
																														              context,
																														              "targets"
																												              ),
																												              ((ServerCommandSource) context.getSource()).getWorld(),
																												              Vec3ArgumentType.getPosArgument(
																														              context,
																														              "location"
																												              ),
																												              null,
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
																														              context -> execute(
																																              (ServerCommandSource) context.getSource(),
																																              EntityArgumentType.getEntities(
																																		              context,
																																		              "targets"
																																              ),
																																              ((ServerCommandSource) context.getSource()).getWorld(),
																																              Vec3ArgumentType.getPosArgument(
																																		              context,
																																		              "location"
																																              ),
																																              null,
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
																								context -> execute(
																										(ServerCommandSource) context.getSource(),
																										EntityArgumentType.getEntities(
																												context,
																												"targets"
																										),
																										((ServerCommandSource) context.getSource()).getWorld(),
																										Vec3ArgumentType.getPosArgument(
																												context,
																												"location"
																										),
																										null,
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
								)
										.then(
												CommandManager.argument("destination", EntityArgumentType.entity())
												              .executes(
														              context -> execute(
																              (ServerCommandSource) context.getSource(),
																              EntityArgumentType.getEntities(
																		              context,
																		              "targets"
																              ),
																              EntityArgumentType.getEntity(
																		              context,
																		              "destination"
																              )
														              )
												              )
										)
						)
		);
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("tp")
				                                                                 .requires(CommandManager.requirePermissionLevel(
						                                                                 CommandManager.GAMEMASTERS_CHECK))
				)
						.redirect(literalCommandNode)
		);
	}

	private static int execute(ServerCommandSource source, Collection<? extends Entity> targets, Entity destination)
	throws CommandSyntaxException {
		for (Entity entity : targets) {
			teleport(
					source,
					entity,
					(ServerWorld) destination.getEntityWorld(),
					destination.getX(),
					destination.getY(),
					destination.getZ(),
					EnumSet.noneOf(PositionFlag.class),
					destination.getYaw(),
					destination.getPitch(),
					null
			);
		}

		if (targets.size() == 1) {
			source.sendFeedback(
					() -> Text.translatable(
							"commands.teleport.success.entity.single",
							targets.iterator().next().getDisplayName(),
							destination.getDisplayName()
					), true
			);
		}
		else {
			source.sendFeedback(
					() -> Text.translatable(
							"commands.teleport.success.entity.multiple",
							targets.size(),
							destination.getDisplayName()
					), true
			);
		}

		return targets.size();
	}

	private static int execute(
			ServerCommandSource source,
			Collection<? extends Entity> targets,
			ServerWorld world,
			PosArgument location,
			@Nullable PosArgument rotation,
			@Nullable LookTarget facingLocation
	) throws CommandSyntaxException {
		Vec3d vec3d = location.getPos(source);
		Vec2f vec2f = rotation == null ? null : rotation.getRotation(source);

		for (Entity entity : targets) {
			Set<PositionFlag>
					set =
					getFlags(location, rotation, entity.getEntityWorld().getRegistryKey() == world.getRegistryKey());
			if (vec2f == null) {
				teleport(
						source,
						entity,
						world,
						vec3d.x,
						vec3d.y,
						vec3d.z,
						set,
						entity.getYaw(),
						entity.getPitch(),
						facingLocation
				);
			}
			else {
				teleport(source, entity, world, vec3d.x, vec3d.y, vec3d.z, set, vec2f.y, vec2f.x, facingLocation);
			}
		}

		if (targets.size() == 1) {
			source.sendFeedback(
					() -> Text.translatable(
							"commands.teleport.success.location.single",
							targets.iterator().next().getDisplayName(),
							formatFloat(vec3d.x),
							formatFloat(vec3d.y),
							formatFloat(vec3d.z)
					),
					true
			);
		}
		else {
			source.sendFeedback(
					() -> Text.translatable(
							"commands.teleport.success.location.multiple",
							targets.size(),
							formatFloat(vec3d.x),
							formatFloat(vec3d.y),
							formatFloat(vec3d.z)
					),
					true
			);
		}

		return targets.size();
	}

	private static Set<PositionFlag> getFlags(PosArgument pos, @Nullable PosArgument rotation, boolean sameDimension) {
		Set<PositionFlag> set = PositionFlag.ofDeltaPos(pos.isXRelative(), pos.isYRelative(), pos.isZRelative());
		Set<PositionFlag>
				set2 =
				sameDimension ? PositionFlag.ofPos(pos.isXRelative(), pos.isYRelative(), pos.isZRelative()) : Set.of();
		Set<PositionFlag>
				set3 =
				rotation == null ? PositionFlag.ROT
				                 : PositionFlag.ofRot(rotation.isYRelative(), rotation.isXRelative());
		return PositionFlag.combine(set, set2, set3);
	}

	private static String formatFloat(double d) {
		return String.format(Locale.ROOT, "%f", d);
	}

	private static void teleport(
			ServerCommandSource source,
			Entity target,
			ServerWorld world,
			double x,
			double y,
			double z,
			Set<PositionFlag> movementFlags,
			float yaw,
			float pitch,
			@Nullable LookTarget facingLocation
	) throws CommandSyntaxException {
		BlockPos blockPos = BlockPos.ofFloored(x, y, z);
		if (!World.isValid(blockPos)) {
			throw INVALID_POSITION_EXCEPTION.create();
		}
		else {
			double d = movementFlags.contains(PositionFlag.X) ? x - target.getX() : x;
			double e = movementFlags.contains(PositionFlag.Y) ? y - target.getY() : y;
			double f = movementFlags.contains(PositionFlag.Z) ? z - target.getZ() : z;
			float g = movementFlags.contains(PositionFlag.Y_ROT) ? yaw - target.getYaw() : yaw;
			float h = movementFlags.contains(PositionFlag.X_ROT) ? pitch - target.getPitch() : pitch;
			float i = MathHelper.wrapDegrees(g);
			float j = MathHelper.wrapDegrees(h);
			if (target.teleport(world, d, e, f, movementFlags, i, j, true)) {
				if (facingLocation != null) {
					facingLocation.look(source, target);
				}

				if (!(target instanceof LivingEntity livingEntity && livingEntity.isGliding())) {
					target.setVelocity(target.getVelocity().multiply(1.0, 0.0, 1.0));
					target.setOnGround(true);
				}

				if (target instanceof PathAwareEntity pathAwareEntity) {
					pathAwareEntity.getNavigation().stop();
				}
			}
		}
	}
}
