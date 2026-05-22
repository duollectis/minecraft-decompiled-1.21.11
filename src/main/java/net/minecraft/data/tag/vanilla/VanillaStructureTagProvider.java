package net.minecraft.data.tag.vanilla;

import net.minecraft.data.DataOutput;
import net.minecraft.data.tag.SimpleTagProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;

import java.util.concurrent.CompletableFuture;

/**
 * {@code VanillaStructureTagProvider}.
 */
public class VanillaStructureTagProvider extends SimpleTagProvider<Structure> {

	public VanillaStructureTagProvider(
			DataOutput output,
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
	) {
		super(output, RegistryKeys.STRUCTURE, registriesFuture);
	}

	@Override
	protected void configure(RegistryWrapper.WrapperLookup registries) {
		builder(StructureTags.VILLAGE)
		    .add(StructureKeys.VILLAGE_PLAINS)
		    .add(StructureKeys.VILLAGE_DESERT)
		    .add(StructureKeys.VILLAGE_SAVANNA)
		    .add(StructureKeys.VILLAGE_SNOWY)
		    .add(StructureKeys.VILLAGE_TAIGA);
		builder(StructureTags.MINESHAFT).add(StructureKeys.MINESHAFT).add(StructureKeys.MINESHAFT_MESA);
		builder(StructureTags.OCEAN_RUIN).add(StructureKeys.OCEAN_RUIN_COLD).add(StructureKeys.OCEAN_RUIN_WARM);
		builder(StructureTags.SHIPWRECK).add(StructureKeys.SHIPWRECK).add(StructureKeys.SHIPWRECK_BEACHED);
		builder(StructureTags.RUINED_PORTAL)
		    .add(StructureKeys.RUINED_PORTAL_DESERT)
		    .add(StructureKeys.RUINED_PORTAL_JUNGLE)
		    .add(StructureKeys.RUINED_PORTAL_MOUNTAIN)
		    .add(StructureKeys.RUINED_PORTAL_NETHER)
		    .add(StructureKeys.RUINED_PORTAL_OCEAN)
		    .add(StructureKeys.RUINED_PORTAL)
		    .add(StructureKeys.RUINED_PORTAL_SWAMP);
		builder(StructureTags.CATS_SPAWN_IN).add(StructureKeys.SWAMP_HUT);
		builder(StructureTags.CATS_SPAWN_AS_BLACK).add(StructureKeys.SWAMP_HUT);
		builder(StructureTags.EYE_OF_ENDER_LOCATED).add(StructureKeys.STRONGHOLD);
		builder(StructureTags.DOLPHIN_LOCATED).addTag(StructureTags.OCEAN_RUIN).addTag(StructureTags.SHIPWRECK);
		builder(StructureTags.ON_WOODLAND_EXPLORER_MAPS).add(StructureKeys.MANSION);
		builder(StructureTags.ON_OCEAN_EXPLORER_MAPS).add(StructureKeys.MONUMENT);
		builder(StructureTags.ON_TREASURE_MAPS).add(StructureKeys.BURIED_TREASURE);
		builder(StructureTags.ON_TRIAL_CHAMBERS_MAPS).add(StructureKeys.TRIAL_CHAMBERS);
		builder(StructureTags.ON_SAVANNA_VILLAGE_MAPS).add(StructureKeys.VILLAGE_SAVANNA);
		builder(StructureTags.ON_DESERT_VILLAGE_MAPS).add(StructureKeys.VILLAGE_DESERT);
		builder(StructureTags.ON_PLAINS_VILLAGE_MAPS).add(StructureKeys.VILLAGE_PLAINS);
		builder(StructureTags.ON_TAIGA_VILLAGE_MAPS).add(StructureKeys.VILLAGE_TAIGA);
		builder(StructureTags.ON_SNOWY_VILLAGE_MAPS).add(StructureKeys.VILLAGE_SNOWY);
		builder(StructureTags.ON_SWAMP_EXPLORER_MAPS).add(StructureKeys.SWAMP_HUT);
		builder(StructureTags.ON_JUNGLE_EXPLORER_MAPS).add(StructureKeys.JUNGLE_PYRAMID);
	}
}
