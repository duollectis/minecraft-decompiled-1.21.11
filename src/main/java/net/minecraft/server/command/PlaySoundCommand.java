package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@code PlaySoundCommand}.
 */
public class PlaySoundCommand {

	private static final SimpleCommandExceptionType
			FAILED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.playsound.failed"));

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		RequiredArgumentBuilder<ServerCommandSource, Identifier>
				requiredArgumentBuilder =
				(RequiredArgumentBuilder<ServerCommandSource, Identifier>) CommandManager.argument(
						                                                                         "sound", IdentifierArgumentType.identifier()
				                                                                         )
				                                                                         .suggests(SuggestionProviders.cast(
						                                                                         SuggestionProviders.AVAILABLE_SOUNDS))
				                                                                         .executes(
						                                                                         context -> execute(
								                                                                         (ServerCommandSource) context.getSource(),
								                                                                         toList(((ServerCommandSource) context.getSource()).getPlayer()),
								                                                                         IdentifierArgumentType.getIdentifier(
										                                                                         context,
										                                                                         "sound"
								                                                                         ),
								                                                                         SoundCategory.MASTER,
								                                                                         ((ServerCommandSource) context.getSource()).getPosition(),
								                                                                         1.0F,
								                                                                         1.0F,
								                                                                         0.0F
						                                                                         )
				                                                                         );

		for (SoundCategory soundCategory : SoundCategory.values()) {
			requiredArgumentBuilder.then(makeArgumentsForCategory(soundCategory));
		}

		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("playsound")
				                                                                 .requires(CommandManager.requirePermissionLevel(
						                                                                 CommandManager.GAMEMASTERS_CHECK))
				)
						.then(requiredArgumentBuilder)
		);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> makeArgumentsForCategory(SoundCategory category) {
		return (LiteralArgumentBuilder<ServerCommandSource>) ((LiteralArgumentBuilder) CommandManager
				.literal(category.getName())
				.executes(
						context -> execute(
								(ServerCommandSource) context.getSource(),
								toList(((ServerCommandSource) context.getSource()).getPlayer()),
								IdentifierArgumentType.getIdentifier(context, "sound"),
								category,
								((ServerCommandSource) context.getSource()).getPosition(),
								1.0F,
								1.0F,
								0.0F
						)
				)
		)
				.then(
						((RequiredArgumentBuilder) CommandManager.argument("targets", EntityArgumentType.players())
						                                         .executes(
								                                         context -> execute(
										                                         (ServerCommandSource) context.getSource(),
										                                         EntityArgumentType.getPlayers(
												                                         context,
												                                         "targets"
										                                         ),
										                                         IdentifierArgumentType.getIdentifier(
												                                         context,
												                                         "sound"
										                                         ),
										                                         category,
										                                         ((ServerCommandSource) context.getSource()).getPosition(),
										                                         1.0F,
										                                         1.0F,
										                                         0.0F
								                                         )
						                                         )
						)
								.then(
										((RequiredArgumentBuilder) CommandManager
												.argument("pos", Vec3ArgumentType.vec3())
												.executes(
														context -> execute(
																(ServerCommandSource) context.getSource(),
																EntityArgumentType.getPlayers(context, "targets"),
																IdentifierArgumentType.getIdentifier(context, "sound"),
																category,
																Vec3ArgumentType.getVec3(context, "pos"),
																1.0F,
																1.0F,
																0.0F
														)
												)
										)
												.then(
														((RequiredArgumentBuilder) CommandManager
																.argument("volume", FloatArgumentType.floatArg(0.0F))
																.executes(
																		context -> execute(
																				(ServerCommandSource) context.getSource(),
																				EntityArgumentType.getPlayers(
																						context,
																						"targets"
																				),
																				IdentifierArgumentType.getIdentifier(
																						context,
																						"sound"
																				),
																				category,
																				Vec3ArgumentType.getVec3(
																						context,
																						"pos"
																				),
																				(Float) context.getArgument(
																						"volume",
																						Float.class
																				),
																				1.0F,
																				0.0F
																		)
																)
														)
																.then(
																		((RequiredArgumentBuilder) CommandManager
																				.argument(
																						"pitch",
																						FloatArgumentType.floatArg(
																								0.0F,
																								2.0F
																						)
																				)
																				.executes(
																						context -> execute(
																								(ServerCommandSource) context.getSource(),
																								EntityArgumentType.getPlayers(
																										context,
																										"targets"
																								),
																								IdentifierArgumentType.getIdentifier(
																										context,
																										"sound"
																								),
																								category,
																								Vec3ArgumentType.getVec3(
																										context,
																										"pos"
																								),
																								(Float) context.getArgument(
																										"volume",
																										Float.class
																								),
																								(Float) context.getArgument(
																										"pitch",
																										Float.class
																								),
																								0.0F
																						)
																				)
																		)
																				.then(
																						CommandManager
																								.argument(
																										"minVolume",
																										FloatArgumentType.floatArg(
																												0.0F,
																												1.0F
																										)
																								)
																								.executes(
																										context -> execute(
																												(ServerCommandSource) context.getSource(),
																												EntityArgumentType.getPlayers(
																														context,
																														"targets"
																												),
																												IdentifierArgumentType.getIdentifier(
																														context,
																														"sound"
																												),
																												category,
																												Vec3ArgumentType.getVec3(
																														context,
																														"pos"
																												),
																												(Float) context.getArgument(
																														"volume",
																														Float.class
																												),
																												(Float) context.getArgument(
																														"pitch",
																														Float.class
																												),
																												(Float) context.getArgument(
																														"minVolume",
																														Float.class
																												)
																										)
																								)
																				)
																)
												)
								)
				);
	}

	private static Collection<ServerPlayerEntity> toList(@Nullable ServerPlayerEntity player) {
		return player != null ? List.of(player) : List.of();
	}

	private static int execute(
			ServerCommandSource source,
			Collection<ServerPlayerEntity> targets,
			Identifier sound,
			SoundCategory category,
			Vec3d pos,
			float volume,
			float pitch,
			float minVolume
	) throws CommandSyntaxException {
		RegistryEntry<SoundEvent> registryEntry = RegistryEntry.of(SoundEvent.of(sound));
		double d = MathHelper.square(registryEntry.value().getDistanceToTravel(volume));
		ServerWorld serverWorld = source.getWorld();
		long l = serverWorld.getRandom().nextLong();
		List<ServerPlayerEntity> list = new ArrayList<>();

		for (ServerPlayerEntity serverPlayerEntity : targets) {
			if (serverPlayerEntity.getEntityWorld() == serverWorld) {
				double e = pos.x - serverPlayerEntity.getX();
				double f = pos.y - serverPlayerEntity.getY();
				double g = pos.z - serverPlayerEntity.getZ();
				double h = e * e + f * f + g * g;
				Vec3d vec3d = pos;
				float i = volume;
				if (h > d) {
					if (minVolume <= 0.0F) {
						continue;
					}

					double j = Math.sqrt(h);
					vec3d =
							new Vec3d(
									serverPlayerEntity.getX() + e / j * 2.0,
									serverPlayerEntity.getY() + f / j * 2.0,
									serverPlayerEntity.getZ() + g / j * 2.0
							);
					i = minVolume;
				}

				serverPlayerEntity.networkHandler
						.sendPacket(new PlaySoundS2CPacket(
								registryEntry,
								category,
								vec3d.getX(),
								vec3d.getY(),
								vec3d.getZ(),
								i,
								pitch,
								l
						));
				list.add(serverPlayerEntity);
			}
		}

		int k = list.size();
		if (k == 0) {
			throw FAILED_EXCEPTION.create();
		}
		else {
			if (k == 1) {
				source.sendFeedback(
						() -> Text.translatable(
								"commands.playsound.success.single",
								Text.of(sound),
								list.getFirst().getDisplayName()
						), true
				);
			}
			else {
				source.sendFeedback(
						() -> Text.translatable("commands.playsound.success.multiple", Text.of(sound), k),
						true
				);
			}

			return k;
		}
	}
}
