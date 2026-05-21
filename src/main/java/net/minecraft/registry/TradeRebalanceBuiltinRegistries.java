package net.minecraft.registry;

import net.minecraft.enchantment.provider.TradeRebalanceEnchantmentProviders;

import java.util.concurrent.CompletableFuture;

/**
 * {@code TradeRebalanceBuiltinRegistries}.
 */
public class TradeRebalanceBuiltinRegistries {

	private static final RegistryBuilder REGISTRY_BUILDER = new RegistryBuilder()
			.addRegistry(RegistryKeys.ENCHANTMENT_PROVIDER, TradeRebalanceEnchantmentProviders::bootstrap);

	public static CompletableFuture<RegistryBuilder.FullPatchesRegistriesPair> validate(CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
		return ExperimentalRegistriesValidator.validate(registriesFuture, REGISTRY_BUILDER);
	}
}
