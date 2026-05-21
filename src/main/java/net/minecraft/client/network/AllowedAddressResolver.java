package net.minecraft.client.network;

import com.google.common.annotations.VisibleForTesting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Optional;

/**
 * Разрешает адрес сервера с учётом SRV-перенаправлений и списков блокировки.
 * <p>Порядок обработки:
 * <ol>
 *   <li>Разрешить исходный адрес через {@link AddressResolver}.</li>
 *   <li>Проверить разрешённый адрес и исходный адрес через {@link BlockListChecker}.</li>
 *   <li>Если есть SRV-перенаправление — разрешить его и снова проверить через блок-лист.</li>
 * </ol>
 */
@Environment(EnvType.CLIENT)
public class AllowedAddressResolver {

	/**
	 * Стандартный экземпляр с DNS-резолвером, SRV-перенаправлением и системным блок-листом.
	 */
	public static final AllowedAddressResolver DEFAULT = new AllowedAddressResolver(
			AddressResolver.DEFAULT,
			RedirectResolver.createSrv(),
			BlockListChecker.create()
	);

	private final AddressResolver addressResolver;
	private final RedirectResolver redirectResolver;
	private final BlockListChecker blockListChecker;

	@VisibleForTesting
	AllowedAddressResolver(
			AddressResolver addressResolver,
			RedirectResolver redirectResolver,
			BlockListChecker blockListChecker
	) {
		this.addressResolver = addressResolver;
		this.redirectResolver = redirectResolver;
		this.blockListChecker = blockListChecker;
	}

	/**
	 * Разрешает адрес сервера, применяя SRV-перенаправление и проверку блок-листа.
	 *
	 * @param address адрес сервера для разрешения
	 * @return разрешённый {@link Address} или {@link Optional#empty()} если адрес заблокирован
	 */
	public Optional<Address> resolve(ServerAddress address) {
		Optional<Address> resolved = addressResolver.resolve(address);

		boolean isResolvedBlocked = resolved.isPresent() && blockListChecker.isAllowed(resolved.get()) == false;
		boolean isOriginalBlocked = blockListChecker.isAllowed(address) == false;

		if (isResolvedBlocked || isOriginalBlocked) {
			return Optional.empty();
		}

		Optional<ServerAddress> redirect = redirectResolver.lookupRedirect(address);

		if (redirect.isPresent()) {
			return addressResolver.resolve(redirect.get()).filter(blockListChecker::isAllowed);
		}

		return resolved;
	}
}
