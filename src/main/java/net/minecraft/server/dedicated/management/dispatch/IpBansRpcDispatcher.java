package net.minecraft.server.dedicated.management.dispatch;

import com.google.common.net.InetAddresses;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.BannedIpEntry;
import net.minecraft.server.dedicated.management.RpcPlayer;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code IpBansRpcDispatcher}.
 */
public class IpBansRpcDispatcher {

	private static final String DEFAULT_SOURCE = "Management server";

	public static List<IpBansRpcDispatcher.IpBanData> get(ManagementHandlerDispatcher dispatcher) {
		return dispatcher.getBanHandler()
		                 .getIpBanList()
		                 .stream()
		                 .map(IpBansRpcDispatcher.IpBanInfo::fromBannedIpEntry)
		                 .map(IpBansRpcDispatcher.IpBanData::fromIpBanInfo)
		                 .toList();
	}

	public static List<IpBansRpcDispatcher.IpBanData> add(
			ManagementHandlerDispatcher dispatcher,
			List<IpBansRpcDispatcher.IncomingRpcIpBanData> entries,
			ManagementConnectionId remote
	) {
		entries.stream()
		       .map(ipAddress -> banIpFromRpcEntry(dispatcher, ipAddress, remote))
		       .flatMap(Collection::stream)
		       .forEach(ipBannedPlayer -> ipBannedPlayer.networkHandler.disconnect(Text.translatable(
				       "multiplayer.disconnect.ip_banned")));
		return get(dispatcher);
	}

	private static List<ServerPlayerEntity> banIpFromRpcEntry(
			ManagementHandlerDispatcher dispatcher,
			IpBansRpcDispatcher.IncomingRpcIpBanData entry,
			ManagementConnectionId remote
	) {
		IpBansRpcDispatcher.IpBanInfo ipBanInfo = entry.toIpBanInfoOrNull();
		if (ipBanInfo != null) {
			return banIp(dispatcher, ipBanInfo, remote);
		}
		else {
			if (entry.player().isPresent()) {
				Optional<ServerPlayerEntity>
						optional =
						dispatcher
								.getPlayerListHandler()
								.getPlayer(entry.player().get().id(), entry.player().get().name());
				if (optional.isPresent()) {
					return banIp(dispatcher, entry.toIpBanInfoFromPlayer(optional.get()), remote);
				}
			}

			return List.of();
		}
	}

	private static List<ServerPlayerEntity> banIp(
			ManagementHandlerDispatcher dispatcher,
			IpBansRpcDispatcher.IpBanInfo ipBanInfo,
			ManagementConnectionId remote
	) {
		dispatcher.getBanHandler().addIpAddress(ipBanInfo.toBannedIpEntry(), remote);
		return dispatcher.getPlayerListHandler().getPlayersByIpAddress(ipBanInfo.ipAddress());
	}

	public static List<IpBansRpcDispatcher.IpBanData> clearIpBans(
			ManagementHandlerDispatcher dispatcher,
			ManagementConnectionId remote
	) {
		dispatcher.getBanHandler().clearIpBanList(remote);
		return get(dispatcher);
	}

	public static List<IpBansRpcDispatcher.IpBanData> remove(
			ManagementHandlerDispatcher dispatcher,
			List<String> ipAddresses,
			ManagementConnectionId remote
	) {
		ipAddresses.forEach(address -> dispatcher.getBanHandler().removeIpAddress(address, remote));
		return get(dispatcher);
	}

	public static List<IpBansRpcDispatcher.IpBanData> set(
			ManagementHandlerDispatcher dispatcher,
			List<IpBansRpcDispatcher.IpBanData> entries,
			ManagementConnectionId remote
	) {
		Set<IpBansRpcDispatcher.IpBanInfo> set = entries.stream()
		                                                .filter(ipBanInfo -> InetAddresses.isInetAddress(ipBanInfo.ipAddress()))
		                                                .map(IpBansRpcDispatcher.IpBanData::toIpBanInfo)
		                                                .collect(Collectors.toSet());
		Set<IpBansRpcDispatcher.IpBanInfo> set2 = dispatcher.getBanHandler()
		                                                    .getIpBanList()
		                                                    .stream()
		                                                    .map(IpBansRpcDispatcher.IpBanInfo::fromBannedIpEntry)
		                                                    .collect(Collectors.toSet());
		set2.stream()
		    .filter(ipBanInfo -> !set.contains(ipBanInfo))
		    .forEach(ipBanInfoToRemove -> dispatcher
				    .getBanHandler()
				    .removeIpAddress(ipBanInfoToRemove.ipAddress(), remote));
		set.stream()
		   .filter(ipBanInfo -> !set2.contains(ipBanInfo))
		   .forEach(ipBanInfoToAdd -> dispatcher
				   .getBanHandler()
				   .addIpAddress(ipBanInfoToAdd.toBannedIpEntry(), remote));
		set.stream()
		   .filter(ipBanInfo -> !set2.contains(ipBanInfo))
		   .flatMap(newAddedIpBanInfo -> dispatcher
				   .getPlayerListHandler()
				   .getPlayersByIpAddress(newAddedIpBanInfo.ipAddress())
				   .stream())
		   .forEach(ipBanInfo -> ipBanInfo.networkHandler.disconnect(Text.translatable(
				   "multiplayer.disconnect.ip_banned")));
		return get(dispatcher);
	}

	/**
	 * {@code IncomingRpcIpBanData}.
	 */
	public record IncomingRpcIpBanData(
			Optional<RpcPlayer> player,
			Optional<String> ipAddress,
			Optional<String> reason,
			Optional<String> source,
			Optional<Instant> expires
	) {

		public static final MapCodec<IpBansRpcDispatcher.IncomingRpcIpBanData> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    RpcPlayer.CODEC
								                    .codec()
								                    .optionalFieldOf("player")
								                    .forGetter(IpBansRpcDispatcher.IncomingRpcIpBanData::player),
						                    Codec.STRING
								                    .optionalFieldOf("ip")
								                    .forGetter(IpBansRpcDispatcher.IncomingRpcIpBanData::ipAddress),
						                    Codec.STRING
								                    .optionalFieldOf("reason")
								                    .forGetter(IpBansRpcDispatcher.IncomingRpcIpBanData::reason),
						                    Codec.STRING
								                    .optionalFieldOf("source")
								                    .forGetter(IpBansRpcDispatcher.IncomingRpcIpBanData::source),
						                    Codecs.INSTANT
								                    .optionalFieldOf("expires")
								                    .forGetter(IpBansRpcDispatcher.IncomingRpcIpBanData::expires)
				                    )
				                    .apply(instance, IpBansRpcDispatcher.IncomingRpcIpBanData::new)
		);

		IpBansRpcDispatcher.IpBanInfo toIpBanInfoFromPlayer(ServerPlayerEntity player) {
			return new IpBansRpcDispatcher.IpBanInfo(
					player.getIp(),
					this.reason().orElse(null),
					this.source().orElse("Management server"),
					this.expires()
			);
		}

		IpBansRpcDispatcher.@Nullable IpBanInfo toIpBanInfoOrNull() {
			return !this.ipAddress().isEmpty() && InetAddresses.isInetAddress(this.ipAddress().get())
			       ? new IpBansRpcDispatcher.IpBanInfo(
					this.ipAddress().get(),
					this.reason().orElse(null),
					this.source().orElse("Management server"),
					this.expires()
			)
			       : null;
		}
	}

	/**
	 * {@code IpBanData}.
	 */
	public record IpBanData(
			String ipAddress,
			Optional<String> reason,
			Optional<String> source,
			Optional<Instant> expires
	) {

		public static final MapCodec<IpBansRpcDispatcher.IpBanData> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Codec.STRING.fieldOf("ip").forGetter(IpBansRpcDispatcher.IpBanData::ipAddress),
						                    Codec.STRING.optionalFieldOf("reason").forGetter(IpBansRpcDispatcher.IpBanData::reason),
						                    Codec.STRING.optionalFieldOf("source").forGetter(IpBansRpcDispatcher.IpBanData::source),
						                    Codecs.INSTANT.optionalFieldOf("expires").forGetter(IpBansRpcDispatcher.IpBanData::expires)
				                    )
				                    .apply(instance, IpBansRpcDispatcher.IpBanData::new)
		);

		private static IpBansRpcDispatcher.IpBanData fromIpBanInfo(IpBansRpcDispatcher.IpBanInfo ipBanInfo) {
			return new IpBansRpcDispatcher.IpBanData(
					ipBanInfo.ipAddress(),
					Optional.ofNullable(ipBanInfo.reason()),
					Optional.of(ipBanInfo.source()),
					ipBanInfo.expires()
			);
		}

		public static IpBansRpcDispatcher.IpBanData fromBannedIpEntry(BannedIpEntry bannedIpEntry) {
			return fromIpBanInfo(IpBansRpcDispatcher.IpBanInfo.fromBannedIpEntry(bannedIpEntry));
		}

		private IpBansRpcDispatcher.IpBanInfo toIpBanInfo() {
			return new IpBansRpcDispatcher.IpBanInfo(
					this.ipAddress(),
					this.reason().orElse(null),
					this.source().orElse("Management server"),
					this.expires()
			);
		}
	}

	/**
	 * {@code IpBanInfo}.
	 */
	record IpBanInfo(String ipAddress, @Nullable String reason, String source, Optional<Instant> expires) {

		static IpBansRpcDispatcher.IpBanInfo fromBannedIpEntry(BannedIpEntry bannedIpEntry) {
			return new IpBansRpcDispatcher.IpBanInfo(
					Objects.requireNonNull(bannedIpEntry.getKey()),
					bannedIpEntry.getReason(),
					bannedIpEntry.getSource(),
					Optional.ofNullable(bannedIpEntry.getExpiryDate()).map(Date::toInstant)
			);
		}

		BannedIpEntry toBannedIpEntry() {
			return new BannedIpEntry(
					this.ipAddress(),
					null,
					this.source(),
					this.expires().map(Date::from).orElse(null),
					this.reason()
			);
		}
	}
}
