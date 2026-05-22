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

/**
 * Описание окружения, в котором была подана жалоба: версия клиента и тип сервера.
 * Используется для формирования метаданных запроса к API Mojang.
 */
@Environment(EnvType.CLIENT)
public record ReporterEnvironment(String clientVersion, ReporterEnvironment.@Nullable Server server) {

	public static ReporterEnvironment ofIntegratedServer() {
		return ofServer(null);
	}

	public static ReporterEnvironment ofThirdPartyServer(String ip) {
		return ofServer(new ReporterEnvironment.Server.ThirdParty(ip));
	}

	public static ReporterEnvironment ofRealm(RealmsServer server) {
		return ofServer(new ReporterEnvironment.Server.Realm(server));
	}

	public static ReporterEnvironment ofServer(ReporterEnvironment.@Nullable Server server) {
		return new ReporterEnvironment(getVersion(), server);
	}

	public ClientInfo toClientInfo() {
		return new ClientInfo(clientVersion, Locale.getDefault().toLanguageTag());
	}

	public @Nullable ThirdPartyServerInfo toThirdPartyServerInfo() {
		return server instanceof ReporterEnvironment.Server.ThirdParty thirdParty
				? new ThirdPartyServerInfo(thirdParty.ip)
				: null;
	}

	public @Nullable RealmInfo toRealmInfo() {
		return server instanceof ReporterEnvironment.Server.Realm realm
				? new RealmInfo(String.valueOf(realm.realmId()), realm.slotId())
				: null;
	}

	private static String getVersion() {
		StringBuilder version = new StringBuilder();
		version.append(SharedConstants.getGameVersion().id());
		if (MinecraftClient.getModStatus().isModded()) {
			version.append(" (modded)");
		}

		return version.toString();
	}

	/** Маркерный интерфейс типа сервера, на котором произошло нарушение. */
	@Environment(EnvType.CLIENT)
	public interface Server {

		@Environment(EnvType.CLIENT)
		record Realm(long realmId, int slotId) implements ReporterEnvironment.Server {

			public Realm(RealmsServer server) {
				this(server.id, server.activeSlot);
			}
		}

		@Environment(EnvType.CLIENT)
		record ThirdParty(String ip) implements ReporterEnvironment.Server {
		}
	}
}
