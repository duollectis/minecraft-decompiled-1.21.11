package net.minecraft.entity.spawn;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.gen.structure.Structure;

/**
 * {@code StructureSpawnCondition}.
 */
public record StructureSpawnCondition(RegistryEntryList<Structure> requiredStructures) implements SpawnCondition {

	public static final MapCodec<StructureSpawnCondition> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(RegistryCodecs
							.entryList(RegistryKeys.STRUCTURE)
							.fieldOf("structures")
							.forGetter(StructureSpawnCondition::requiredStructures))
					.apply(instance, StructureSpawnCondition::new)
	);

	public boolean test(SpawnContext spawnContext) {
		return spawnContext
				.world()
				.toServerWorld()
				.getStructureAccessor()
				.getStructureContaining(spawnContext.pos(), this.requiredStructures)
				.hasChildren();
	}

	@Override
	public MapCodec<StructureSpawnCondition> getCodec() {
		return CODEC;
	}
}
