package net.minecraft.registry;

import net.minecraft.enchantment.provider.TradeRebalanceEnchantmentProviders;

import java.util.concurrent.CompletableFuture;

/**
 * Регистрирует встроенные реестры для экспериментальной функции
 * «Trade Rebalance» (перебалансировка торговли с жителями).
 * <p>
 * Добавляет провайдеры зачарований, специфичные для этой фичи,
 * и валидирует их через {@link ExperimentalRegistriesValidator}.
 */
public class TradeRebalanceBuiltinRegistries {

	private static final RegistryBuilder REGISTRY_BUILDER = new RegistryBuilder()
			.addRegistry(RegistryKeys.ENCHANTMENT_PROVIDER, TradeRebalanceEnchantmentProviders::bootstrap);

	/**
	 * Валидирует реестры Trade Rebalance на основе переданного lookup.
	 *
	 * @param registriesFuture будущий lookup базовых реестров
	 * @return будущая пара (полный lookup + патчи)
	 */
	public static CompletableFuture<RegistryBuilder.FullPatchesRegistriesPair> validate(
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
	) {
		return ExperimentalRegistriesValidator.validate(registriesFuture, REGISTRY_BUILDER);
	}
}
