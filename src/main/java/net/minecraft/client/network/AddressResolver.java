package net.minecraft.client.network;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Функциональный интерфейс для разрешения {@link ServerAddress} в {@link Address}.
 * <p>Реализация по умолчанию {@link #DEFAULT} выполняет DNS-резолвинг через
 * {@link InetAddress#getByName}. При ошибке разрешения возвращает {@link Optional#empty()}.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface AddressResolver {

	Logger LOGGER = LogUtils.getLogger();

	AddressResolver DEFAULT = address -> {
		try {
			InetAddress inetAddress = InetAddress.getByName(address.getAddress());
			return Optional.of(Address.create(new InetSocketAddress(inetAddress, address.getPort())));
		}
		catch (UnknownHostException e) {
			LOGGER.debug("Couldn't resolve server {} address", address.getAddress(), e);
			return Optional.empty();
		}
	};

	/**
	 * Разрешает адрес сервера в сетевой адрес.
	 *
	 * @param address адрес сервера для разрешения
	 * @return разрешённый адрес или {@link Optional#empty()} при ошибке
	 */
	Optional<Address> resolve(ServerAddress address);
}
