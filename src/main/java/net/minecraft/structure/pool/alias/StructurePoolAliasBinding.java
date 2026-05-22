package net.minecraft.structure.pool.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Привязка псевдонима пула структур к одному или нескольким целевым пулам.
 * Используется в jigsaw-генерации для подмены пулов в зависимости от биома или других условий,
 * что позволяет создавать вариативные структуры без дублирования шаблонов.
 */
public interface StructurePoolAliasBinding {

	Codec<StructurePoolAliasBinding> CODEC = Registries.POOL_ALIAS_BINDING
		.getCodec()
		.dispatch(StructurePoolAliasBinding::getCodec, Function.identity());

	/**
	 * Перебирает все пары (псевдоним → цель) для данной привязки.
	 * Случайный выбор цели (если применимо) выполняется с помощью переданного {@code random}.
	 */
	void forEach(Random random, BiConsumer<RegistryKey<StructurePool>, RegistryKey<StructurePool>> aliasConsumer);

	Stream<RegistryKey<StructurePool>> streamTargets();

	static DirectStructurePoolAliasBinding direct(String alias, String target) {
		return direct(StructurePools.ofVanilla(alias), StructurePools.ofVanilla(target));
	}

	static DirectStructurePoolAliasBinding direct(
		RegistryKey<StructurePool> alias,
		RegistryKey<StructurePool> target
	) {
		return new DirectStructurePoolAliasBinding(alias, target);
	}

	static RandomStructurePoolAliasBinding random(String alias, Pool<String> targets) {
		Pool.Builder<RegistryKey<StructurePool>> builder = Pool.builder();
		targets.getEntries().forEach(entry -> builder.add(StructurePools.ofVanilla(entry.value()), entry.weight()));
		return random(StructurePools.ofVanilla(alias), builder.build());
	}

	static RandomStructurePoolAliasBinding random(
		RegistryKey<StructurePool> alias,
		Pool<RegistryKey<StructurePool>> targets
	) {
		return new RandomStructurePoolAliasBinding(alias, targets);
	}

	static RandomGroupStructurePoolAliasBinding randomGroup(Pool<List<StructurePoolAliasBinding>> groups) {
		return new RandomGroupStructurePoolAliasBinding(groups);
	}

	MapCodec<? extends StructurePoolAliasBinding> getCodec();
}
