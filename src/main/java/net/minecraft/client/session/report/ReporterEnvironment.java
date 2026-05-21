package net.minecraft.client.session.report;

import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ClientInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.RealmInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ThirdPartyServerInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.realms.dto.RealmsServer;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code ReporterEnvironment}.
 */
public record ReporterEnvironment(String clientVersion, ReporterEnvironment.@Nullable Server server) {

	/**
	 * Of integrated server.
	 *
	 * @return ReporterEnvironment — результат операции
	 */
	public static ReporterEnvironment ofIntegratedServer() {
		return ofServer(null);
	}

	/**
	 * Of third party server.
	 *
	 * @param ip ip
	 *
	 * @return ReporterEnvironment — результат операции
	 */
	public static ReporterEnvironment ofThirdPartyServer(String ip) {
		return ofServer(new ReporterEnvironment.Server.ThirdParty(ip));
	}

	/**
	 * Of realm.
	 *
	 * @param server server
	 *
	 * @return ReporterEnvironment — результат операции
	 */
	public static ReporterEnvironment ofRealm(RealmsServer server) {
		return ofServer(new ReporterEnvironment.Server.Realm(server));
	}

	/**
	 * Of server.
	 *
	 * @param server server
	 *
	 * @return ReporterEnvironment — результат операции
	 */
	public static ReporterEnvironment ofServer(ReporterEnvironment.@Nullable Server server) {
		return new ReporterEnvironment(getVersion(), server);
	}

	/**
	 * To client info.
	 *
	 * @return ClientInfo — результат операции
	 */
	public ClientInfo toClientInfo() {
		return new ClientInfo(this.clientVersion, Locale.getDefault().toLanguageTag());
	}

	/**
	 * To third party server info.
	 *
	 * @return @Nullable ThirdPartyServerInfo — результат операции
	 */
	public @Nullable ThirdPartyServerInfo toThirdPartyServerInfo() {
		return this.server instanceof ReporterEnvironment.Server.ThirdParty thirdParty ? new ThirdPartyServerInfo(
				thirdParty.ip) : null;
	}

	/**
	 * To realm info.
	 *
	 * @return @Nullable RealmInfo — результат операции
	 */
	public @Nullable RealmInfo toRealmInfo() {
		return this.server instanceof ReporterEnvironment.Server.Realm realm
		       ? new RealmInfo(String.valueOf(realm.realmId()), realm.slotId()) : null;
	}

	private static String getVersion() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(SharedConstants.getGameVersion().id());
		if (MinecraftClient.getModStatus().isModded()) {
			stringBuilder.append(" (modded)");
		}

		return stringBuilder.toString();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Server}.
	 */
	public interface Server {

		@Environment(EnvType.CLIENT)
		/**
		 * {@code Realm}.
		 */
		public record Realm(long realmId, int slotId) implements ReporterEnvironment.Server {

			public Realm(RealmsServer server) {
				this(server.id, server.activeSlot);
			}
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code ThirdParty}.
		 */
		public record ThirdParty(String ip) implements ReporterEnvironment.Server {
		}
	}
}
