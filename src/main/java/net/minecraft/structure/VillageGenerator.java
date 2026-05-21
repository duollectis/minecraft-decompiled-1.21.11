package net.minecraft.structure;

import net.minecraft.registry.Registerable;
import net.minecraft.structure.pool.StructurePool;

/**
 * {@code VillageGenerator}.
 */
public class VillageGenerator {

	public static void bootstrap(Registerable<StructurePool> poolRegisterable) {
		PlainsVillageData.bootstrap(poolRegisterable);
		SnowyVillageData.bootstrap(poolRegisterable);
		SavannaVillageData.bootstrap(poolRegisterable);
		DesertVillageData.bootstrap(poolRegisterable);
		TaigaVillageData.bootstrap(poolRegisterable);
	}
}
