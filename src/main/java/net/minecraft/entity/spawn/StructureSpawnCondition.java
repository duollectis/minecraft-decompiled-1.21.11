package net.minecraft.entity.spawn;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.gen.structure.Structure;

/**
 * Условие спауна, проверяющее нахождение позиции внутри одной из заданных структур.
 * Использует {@code StructureAccessor} серверного мира для поиска структуры,
 * содержащей позицию спауна. Условие выполнено, если найденная структура имеет дочерние элементы.
 */
public record StructureSpawnCondition(RegistryEntryList<Structure> requiredStructures) implements SpawnCondition {

	public static final MapCodec<StructureSpawnCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(
				RegistryCodecs.entryList(RegistryKeys.STRUCTURE)
					.fieldOf("structures")
					.forGetter(StructureSpawnCondition::requiredStructures)
			)
			.apply(instance, StructureSpawnCondition::new)
	);

	/**
	 * Проверяет, находится ли позиция спауна внутри одной из требуемых структур.
	 * Структура считается подходящей, если у неё есть дочерние элементы (т.е. она сгенерирована).
	 */
	@Override
	public boolean test(SpawnContext context) {
		return context.world()
			.toServerWorld()
			.getStructureAccessor()
			.getStructureContaining(context.pos(), requiredStructures)
			.hasChildren();
	}

	@Override
	public MapCodec<StructureSpawnCondition> getCodec() {
		return CODEC;
	}
}
