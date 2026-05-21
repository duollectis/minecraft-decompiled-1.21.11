package net.minecraft.server.dedicated.management.dispatch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.dedicated.management.RpcPlayer;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * {@code OperatorsRpcDispatcher}.
 */
public class OperatorsRpcDispatcher {

	public static List<OperatorsRpcDispatcher.RpcEntry> get(ManagementHandlerDispatcher dispatcher) {
		return dispatcher.getOperatorHandler()
		                 .getOperators()
		                 .stream()
		                 .filter(operator -> operator.getKey() != null)
		                 .map(OperatorsRpcDispatcher.RpcEntry::of)
		                 .toList();
	}

	public static List<OperatorsRpcDispatcher.RpcEntry> clear(
			ManagementHandlerDispatcher dispatcher,
			ManagementConnectionId remote
	) {
		dispatcher.getOperatorHandler().clearOperators(remote);
		return get(dispatcher);
	}

	public static List<OperatorsRpcDispatcher.RpcEntry> remove(
			ManagementHandlerDispatcher dispatcher,
			List<RpcPlayer> players,
			ManagementConnectionId remote
	) {
		List<CompletableFuture<Optional<PlayerConfigEntry>>> list = players.stream()
		                                                                   .map(player -> dispatcher
				                                                                   .getPlayerListHandler()
				                                                                   .getPlayerAsync(
						                                                                   player.id(),
						                                                                   player.name()
				                                                                   ))
		                                                                   .toList();

		for (Optional<PlayerConfigEntry> optional : Util.combineSafe(list).join()) {
			optional.ifPresent(player -> dispatcher.getOperatorHandler().removeFromOperators(player, remote));
		}

		return get(dispatcher);
	}

	public static List<OperatorsRpcDispatcher.RpcEntry> add(
			ManagementHandlerDispatcher dispatcher,
			List<OperatorsRpcDispatcher.RpcEntry> operators,
			ManagementConnectionId remote
	) {
		List<CompletableFuture<Optional<OperatorsRpcDispatcher.ConfigEntry>>> list = operators.stream()
		                                                                                      .map(
				                                                                                      operator -> dispatcher
						                                                                                      .getPlayerListHandler()
						                                                                                      .getPlayerAsync(
								                                                                                      operator
										                                                                                      .player()
										                                                                                      .id(),
								                                                                                      operator
										                                                                                      .player()
										                                                                                      .name()
						                                                                                      )
						                                                                                      .thenApply(
								                                                                                      optionalPlayerEntry -> optionalPlayerEntry.map(
										                                                                                      playerEntry -> new OperatorsRpcDispatcher.ConfigEntry(
												                                                                                      playerEntry,
												                                                                                      operator.permissionLevel(),
												                                                                                      operator.bypassesPlayerLimit()
										                                                                                      )
								                                                                                      )
						                                                                                      )
		                                                                                      )
		                                                                                      .toList();

		for (Optional<OperatorsRpcDispatcher.ConfigEntry> optional : Util.combineSafe(list).join()) {
			optional.ifPresent(
					operator -> dispatcher
							.getOperatorHandler()
							.addToOperators(
									operator.user(),
									operator.permissionLevel(),
									operator.bypassesPlayerLimit(),
									remote
							)
			);
		}

		return get(dispatcher);
	}

	public static List<OperatorsRpcDispatcher.RpcEntry> set(
			ManagementHandlerDispatcher dispatcher,
			List<OperatorsRpcDispatcher.RpcEntry> operators,
			ManagementConnectionId remote
	) {
		List<CompletableFuture<Optional<OperatorsRpcDispatcher.ConfigEntry>>> list = operators.stream()
		                                                                                      .map(
				                                                                                      operator -> dispatcher
						                                                                                      .getPlayerListHandler()
						                                                                                      .getPlayerAsync(
								                                                                                      operator
										                                                                                      .player()
										                                                                                      .id(),
								                                                                                      operator
										                                                                                      .player()
										                                                                                      .name()
						                                                                                      )
						                                                                                      .thenApply(
								                                                                                      optionalPlayerEntry -> optionalPlayerEntry.map(
										                                                                                      playerEntry -> new OperatorsRpcDispatcher.ConfigEntry(
												                                                                                      playerEntry,
												                                                                                      operator.permissionLevel(),
												                                                                                      operator.bypassesPlayerLimit()
										                                                                                      )
								                                                                                      )
						                                                                                      )
		                                                                                      )
		                                                                                      .toList();
		Set<OperatorsRpcDispatcher.ConfigEntry>
				set =
				Util.combineSafe(list).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
		Set<OperatorsRpcDispatcher.ConfigEntry> set2 = dispatcher.getOperatorHandler()
		                                                         .getOperators()
		                                                         .stream()
		                                                         .filter(entry -> entry.getKey() != null)
		                                                         .map(
				                                                         operator -> new OperatorsRpcDispatcher.ConfigEntry(
						                                                         operator.getKey(),
						                                                         Optional.of(operator
								                                                         .getLevel()
								                                                         .getLevel()),
						                                                         Optional.of(operator.canBypassPlayerLimit())
				                                                         )
		                                                         )
		                                                         .collect(Collectors.toSet());
		set2.stream()
		    .filter(operator -> !set.contains(operator))
		    .forEach(operator -> dispatcher.getOperatorHandler().removeFromOperators(operator.user(), remote));
		set.stream()
		   .filter(entry -> !set2.contains(entry))
		   .forEach(
				   operator -> dispatcher
						   .getOperatorHandler()
						   .addToOperators(
								   operator.user(),
								   operator.permissionLevel(),
								   operator.bypassesPlayerLimit(),
								   remote
						   )
		   );
		return get(dispatcher);
	}

	/**
	 * {@code ConfigEntry}.
	 */
	record ConfigEntry(
			PlayerConfigEntry user,
			Optional<PermissionLevel> permissionLevel,
			Optional<Boolean> bypassesPlayerLimit
	) {
	}

	/**
	 * {@code RpcEntry}.
	 */
	public record RpcEntry(
			RpcPlayer player,
			Optional<PermissionLevel> permissionLevel,
			Optional<Boolean> bypassesPlayerLimit
	) {

		public static final MapCodec<OperatorsRpcDispatcher.RpcEntry> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    RpcPlayer.CODEC.codec().fieldOf("player").forGetter(OperatorsRpcDispatcher.RpcEntry::player),
						                    PermissionLevel.NUMERIC_CODEC
								                    .optionalFieldOf("permissionLevel")
								                    .forGetter(OperatorsRpcDispatcher.RpcEntry::permissionLevel),
						                    Codec.BOOL
								                    .optionalFieldOf("bypassesPlayerLimit")
								                    .forGetter(OperatorsRpcDispatcher.RpcEntry::bypassesPlayerLimit)
				                    )
				                    .apply(instance, OperatorsRpcDispatcher.RpcEntry::new)
		);

		public static OperatorsRpcDispatcher.RpcEntry of(OperatorEntry operator) {
			return new OperatorsRpcDispatcher.RpcEntry(
					RpcPlayer.of(Objects.requireNonNull(operator.getKey())),
					Optional.of(operator.getLevel().getLevel()),
					Optional.of(operator.canBypassPlayerLimit())
			);
		}
	}
}
