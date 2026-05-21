package net.minecraft.server.dedicated.management.dispatch;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.dedicated.management.RpcKickReason;
import net.minecraft.server.dedicated.management.RpcPlayer;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code PlayersRpcDispatcher}.
 */
public class PlayersRpcDispatcher {

	private static final Text DEFAULT_KICK_REASON = Text.translatable("multiplayer.disconnect.kicked");

	public static List<RpcPlayer> get(ManagementHandlerDispatcher dispatcher) {
		return dispatcher.getPlayerListHandler().getPlayerList().stream().map(RpcPlayer::of).toList();
	}

	public static List<RpcPlayer> kick(
			ManagementHandlerDispatcher dispatcher,
			List<PlayersRpcDispatcher.RpcEntry> list,
			ManagementConnectionId remote
	) {
		List<RpcPlayer> list2 = new ArrayList<>();

		for (PlayersRpcDispatcher.RpcEntry rpcEntry : list) {
			ServerPlayerEntity serverPlayerEntity = getPlayer(dispatcher, rpcEntry.player());
			if (serverPlayerEntity != null) {
				dispatcher.getPlayerListHandler().removePlayer(serverPlayerEntity, remote);
				serverPlayerEntity.networkHandler.disconnect(rpcEntry.message
						.flatMap(RpcKickReason::toText)
						.orElse(DEFAULT_KICK_REASON));
				list2.add(rpcEntry.player());
			}
		}

		return list2;
	}

	private static @Nullable ServerPlayerEntity getPlayer(ManagementHandlerDispatcher dispatcher, RpcPlayer player) {
		if (player.id().isPresent()) {
			return dispatcher.getPlayerListHandler().getPlayer(player.id().get());
		}
		else {
			return player.name().isPresent() ? dispatcher.getPlayerListHandler().getPlayer(player.name().get()) : null;
		}
	}

	/**
	 * {@code RpcEntry}.
	 */
	public record RpcEntry(RpcPlayer player, Optional<RpcKickReason> message) {

		public static final MapCodec<PlayersRpcDispatcher.RpcEntry> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    RpcPlayer.CODEC.codec().fieldOf("player").forGetter(PlayersRpcDispatcher.RpcEntry::player),
						                    RpcKickReason.CODEC.optionalFieldOf("message").forGetter(PlayersRpcDispatcher.RpcEntry::message)
				                    )
				                    .apply(instance, PlayersRpcDispatcher.RpcEntry::new)
		);
	}
}
