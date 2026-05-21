package net.minecraft.client.network;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Optional;

/**
 * Разрешает SRV-перенаправления DNS для адресов серверов Minecraft.
 * <p>При подключении к серверу на стандартном порту {@code 25565} выполняет
 * DNS-запрос записи {@code _minecraft._tcp.<host>} и возвращает перенаправленный адрес,
 * если запись существует.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface RedirectResolver {

	Logger LOGGER = LogUtils.getLogger();

	/**
	 * Заглушка, всегда возвращающая пустой результат (используется при ошибке инициализации DNS).
	 */
	RedirectResolver INVALID = address -> Optional.empty();

	/**
	 * Ищет SRV-перенаправление для указанного адреса.
	 *
	 * @param address адрес сервера для поиска перенаправления
	 * @return перенаправленный адрес или {@link Optional#empty()} если перенаправление не найдено
	 */
	Optional<ServerAddress> lookupRedirect(ServerAddress address);

	/**
	 * Создаёт резолвер SRV-записей через JNDI DNS.
	 * При ошибке инициализации возвращает {@link #INVALID}.
	 *
	 * @return рабочий SRV-резолвер или {@link #INVALID}
	 */
	static RedirectResolver createSrv() {
		DirContext dnsContext;

		try {
			Class.forName("com.sun.jndi.dns.DnsContextFactory");
			Hashtable<String, String> env = new Hashtable<>();
			env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
			env.put("java.naming.provider.url", "dns:");
			env.put("com.sun.jndi.dns.timeout.retries", "1");
			dnsContext = new InitialDirContext(env);
		}
		catch (Throwable e) {
			LOGGER.error("Failed to initialize SRV redirect resolved, some servers might not work", e);
			return INVALID;
		}

		return address -> {
			if (address.getPort() != ServerAddress.DEFAULT_PORT) {
				return Optional.empty();
			}

			try {
				Attributes attributes = dnsContext.getAttributes(
						"_minecraft._tcp." + address.getAddress(),
						new String[]{"SRV"}
				);
				Attribute srvRecord = attributes.get("srv");

				if (srvRecord != null) {
					String[] parts = srvRecord.get().toString().split(" ", 4);
					return Optional.of(new ServerAddress(parts[3], ServerAddress.portOrDefault(parts[2])));
				}
			}
			catch (Throwable ignored) {
			}

			return Optional.empty();
		};
	}
}
