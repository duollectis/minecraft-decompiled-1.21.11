package net.minecraft.structure.pool.alias;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructurePools;

import java.util.List;

/**
 * Утилитарный класс для регистрации типов привязок псевдонимов пулов структур
 * и создания вспомогательных пулов-заглушек для всех целевых пулов из списка псевдонимов.
 */
public class StructurePoolAliasBindings {

	/**
	 * Регистрирует все встроенные типы привязок псевдонимов в реестре и возвращает
	 * тип по умолчанию ({@code direct}).
	 */
	public static MapCodec<? extends StructurePoolAliasBinding> registerAndGetDefault(
		Registry<MapCodec<? extends StructurePoolAliasBinding>> registry
	) {
		Registry.register(registry, "random", RandomStructurePoolAliasBinding.CODEC);
		Registry.register(registry, "random_group", RandomGroupStructurePoolAliasBinding.CODEC);
		return Registry.register(registry, "direct", DirectStructurePoolAliasBinding.CODEC);
	}

	/**
	 * Регистрирует вспомогательные пулы-заглушки для каждого целевого пула из списка псевдонимов.
	 * Каждый такой пул содержит единственный элемент — одиночный шаблон с тем же путём,
	 * что и ключ целевого пула, и использует {@code base} как запасной пул.
	 */
	public static void registerPools(
		Registerable<StructurePool> pools,
		RegistryEntry<StructurePool> base,
		List<StructurePoolAliasBinding> aliases
	) {
		aliases.stream()
			.flatMap(StructurePoolAliasBinding::streamTargets)
			.map(target -> target.getValue().getPath())
			.forEach(path -> StructurePools.register(
				pools,
				path,
				new StructurePool(
					base,
					List.of(Pair.of(StructurePoolElement.ofSingle(path), 1)),
					StructurePool.Projection.RIGID
				)
			));
	}
}
