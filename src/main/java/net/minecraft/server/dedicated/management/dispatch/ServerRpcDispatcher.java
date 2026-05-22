package net.minecraft.server.dedicated.management.dispatch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.dedicated.management.RpcKickReason;
import net.minecraft.server.dedicated.management.RpcPlayer;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

/**
 * Класс Server Rpc Dispatcher.
 */
public class ServerRpcDispatcher {

	public static ServerRpcDispatcher.RpcStatus status(ManagementHandlerDispatcher dispatcher) {
		return !dispatcher.getServerHandler().isLoading()
		       ? ServerRpcDispatcher.RpcStatus.EMPTY
		       : new ServerRpcDispatcher.RpcStatus(
				       true,
				       PlayersRpcDispatcher.get(dispatcher),
				       ServerMetadata.Version.create()
		       );
	}

	/**
	 * Save.
	 *
	 * @param dispatcher dispatcher
	 * @param flush flush
	 * @param remote remote
	 *
	 * @return boolean — результат операции
	 */
	public static boolean save(ManagementHandlerDispatcher dispatcher, boolean flush, ManagementConnectionId remote) {
		return dispatcher.getServerHandler().save(true, flush, true, remote);
	}

	/**
	 * Stop.
	 *
	 * @param dispatcher dispatcher
	 * @param remote remote
	 *
	 * @return boolean — результат операции
	 */
	public static boolean stop(ManagementHandlerDispatcher dispatcher, ManagementConnectionId remote) {
		dispatcher.submit(() -> dispatcher.getServerHandler().stop(false, remote));
		return true;
	}

	public static boolean systemMessage(
			ManagementHandlerDispatcher dispatcher,
			ServerRpcDispatcher.RpcSystemMessage message,
			ManagementConnectionId remote
	) {
		Text text = message.message().toText().orElse(null);
		if (text == null) {
			return false;
		}
		else {
			if (message.receivingPlayers().isPresent()) {
				if (message.receivingPlayers().get().isEmpty()) {
					return false;
				}

				for (RpcPlayer rpcPlayer : message.receivingPlayers().get()) {
					ServerPlayerEntity serverPlayerEntity;
					if (rpcPlayer.id().isPresent()) {
						serverPlayerEntity = dispatcher.getPlayerListHandler().getPlayer(rpcPlayer.id().get());
					}
					else {
						if (!rpcPlayer.name().isPresent()) {
							continue;
						}

						serverPlayerEntity = dispatcher.getPlayerListHandler().getPlayer(rpcPlayer.name().get());
					}

					if (serverPlayerEntity != null) {
						serverPlayerEntity.sendMessageToClient(text, message.overlay());
					}
				}
			}
			else {
				dispatcher.getServerHandler().broadcastMessage(text, message.overlay(), remote);
			}

			return true;
		}
	}

	public record RpcStatus(boolean started, List<RpcPlayer> players, ServerMetadata.Version version) {

		public static final Codec<ServerRpcDispatcher.RpcStatus> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec.BOOL.fieldOf("started").forGetter(ServerRpcDispatcher.RpcStatus::started),
						                    RpcPlayer.CODEC
								                    .codec()
								                    .listOf()
								                    .lenientOptionalFieldOf("players", List.of())
								                    .forGetter(ServerRpcDispatcher.RpcStatus::players),
						                    ServerMetadata.Version.CODEC
								                    .fieldOf("version")
								                    .forGetter(ServerRpcDispatcher.RpcStatus::version)
				                    )
				                    .apply(instance, ServerRpcDispatcher.RpcStatus::new)
		);
		public static final ServerRpcDispatcher.RpcStatus
				EMPTY =
				new ServerRpcDispatcher.RpcStatus(false, List.of(), ServerMetadata.Version.create());
	}

	public record RpcSystemMessage(RpcKickReason message, boolean overlay, Optional<List<RpcPlayer>> receivingPlayers) {

		public static final Codec<ServerRpcDispatcher.RpcSystemMessage> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    RpcKickReason.CODEC.fieldOf("message").forGetter(ServerRpcDispatcher.RpcSystemMessage::message),
						                    Codec.BOOL.fieldOf("overlay").forGetter(ServerRpcDispatcher.RpcSystemMessage::overlay),
						                    RpcPlayer.CODEC
								                    .codec()
								                    .listOf()
								                    .lenientOptionalFieldOf("receivingPlayers")
								                    .forGetter(ServerRpcDispatcher.RpcSystemMessage::receivingPlayers)
				                    )
				                    .apply(instance, ServerRpcDispatcher.RpcSystemMessage::new)
		);
	}
}
