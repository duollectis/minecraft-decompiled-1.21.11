package net.minecraft.data.advancement.vanilla;

import net.minecraft.data.DataOutput;
import net.minecraft.data.advancement.AdvancementProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code VanillaAdvancementProviders}.
 */
public class VanillaAdvancementProviders {

	public static AdvancementProvider createVanillaProvider(
			DataOutput output,
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
	) {
		return new AdvancementProvider(
				output,
				registriesFuture,
				List.of(
						new VanillaEndTabAdvancementGenerator(),
						new VanillaHusbandryTabAdvancementGenerator(),
						new VanillaAdventureTabAdvancementGenerator(),
						new VanillaNetherTabAdvancementGenerator(),
						new VanillaStoryTabAdvancementGenerator()
				)
		);
	}
}
