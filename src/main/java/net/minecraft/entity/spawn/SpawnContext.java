package net.minecraft.entity.spawn;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.attribute.EnvironmentAttributeAccess;
import net.minecraft.world.biome.Biome;

/**
 * Контекст спауна сущности: содержит позицию, доступ к миру, атрибуты окружения и биом.
 * Передаётся в {@link SpawnCondition#test} для проверки условий спауна.
 */
public record SpawnContext(
	BlockPos pos,
	ServerWorldAccess world,
	EnvironmentAttributeAccess environmentAttributes,
	RegistryEntry<Biome> biome
) {

	/**
	 * Создаёт контекст спауна для заданной позиции в мире.
	 * Биом и атрибуты окружения извлекаются непосредственно из мира по позиции.
	 *
	 * @param world серверный доступ к миру
	 * @param pos позиция спауна
	 * @return готовый контекст спауна
	 */
	public static SpawnContext of(ServerWorldAccess world, BlockPos pos) {
		RegistryEntry<Biome> biome = world.getBiome(pos);

		return new SpawnContext(pos, world, world.getEnvironmentAttributes(), biome);
	}
}
