package net.minecraft.client.network;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mojang.blocklist.BlockListSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Predicate;

/**
 * Проверяет, разрешено ли подключение к указанному адресу согласно спискам блокировки.
 * <p>Реализации списков блокировки загружаются через {@link ServiceLoader} из
 * {@link BlockListSupplier}. Адрес считается разрешённым, если ни один из загруженных
 * списков не блокирует его имя хоста или IP-адрес.
 */
@Environment(EnvType.CLIENT)
public interface BlockListChecker {

	/**
	 * Проверяет, разрешён ли адрес по имени хоста и IP.
	 *
	 * @param address адрес для проверки
	 * @return {@code true} если адрес не заблокирован
	 */
	boolean isAllowed(Address address);

	/**
	 * Проверяет, разрешён ли адрес сервера по строковому представлению.
	 *
	 * @param address адрес сервера для проверки
	 * @return {@code true} если адрес не заблокирован
	 */
	boolean isAllowed(ServerAddress address);

	/**
	 * Создаёт экземпляр, агрегирующий все доступные списки блокировки через SPI.
	 *
	 * @return составной проверщик списков блокировки
	 */
	static BlockListChecker create() {
		ImmutableList<Predicate<String>> blockLists = Streams
				.stream(ServiceLoader.load(BlockListSupplier.class))
				.<Predicate<String>>map(BlockListSupplier::createBlockList)
				.filter(Objects::nonNull)
				.collect(ImmutableList.toImmutableList());

		return new BlockListChecker() {
			@Override
			public boolean isAllowed(Address address) {
				String hostName = address.getHostName();
				String hostAddress = address.getHostAddress();
				return blockLists.stream().noneMatch(
						predicate -> predicate.test(hostName) || predicate.test(hostAddress)
				);
			}

			@Override
			public boolean isAllowed(ServerAddress address) {
				String host = address.getAddress();
				return blockLists.stream().noneMatch(predicate -> predicate.test(host));
			}
		};
	}
}
