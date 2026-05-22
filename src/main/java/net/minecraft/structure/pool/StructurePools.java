package net.minecraft.structure.pool;

import com.google.common.collect.ImmutableList;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.*;
import net.minecraft.util.Identifier;

/**
 * Утилитарный класс для регистрации и получения ключей {@link StructurePool}.
 * Содержит ключ пустого пула {@link #EMPTY}, используемого как заглушка,
 * и точку входа {@link #bootstrap} для регистрации всех ванильных пулов.
 */
public class StructurePools {

	public static final RegistryKey<StructurePool> EMPTY = ofVanilla("empty");

	public static RegistryKey<StructurePool> of(Identifier id) {
		return RegistryKey.of(RegistryKeys.TEMPLATE_POOL, id);
	}

	public static RegistryKey<StructurePool> ofVanilla(String id) {
		return of(Identifier.ofVanilla(id));
	}

	public static RegistryKey<StructurePool> of(String id) {
		return of(Identifier.of(id));
	}

	public static void register(Registerable<StructurePool> registerable, String id, StructurePool pool) {
		registerable.register(ofVanilla(id), pool);
	}

	/**
	 * Регистрирует все ванильные пулы структур, включая пустой пул и пулы
	 * для каждого типа генерируемых структур (деревни, бастионы и т.д.).
	 */
	public static void bootstrap(Registerable<StructurePool> registerable) {
		RegistryEntryLookup<StructurePool> poolLookup = registerable.getRegistryLookup(RegistryKeys.TEMPLATE_POOL);
		RegistryEntry<StructurePool> emptyEntry = poolLookup.getOrThrow(EMPTY);
		registerable.register(EMPTY, new StructurePool(emptyEntry, ImmutableList.of(), StructurePool.Projection.RIGID));

		BastionRemnantGenerator.bootstrap(registerable);
		PillagerOutpostGenerator.bootstrap(registerable);
		VillageGenerator.bootstrap(registerable);
		AncientCityGenerator.bootstrap(registerable);
		TrailRuinsGenerator.bootstrap(registerable);
		TrialChamberData.bootstrap(registerable);
	}
}
