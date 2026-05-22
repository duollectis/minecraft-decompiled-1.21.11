package net.minecraft.structure.pool.alias;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.math.random.Random;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Привязка псевдонима пула структур к случайно выбранному целевому пулу.
 * При каждом вызове {@link #forEach} из взвешенного набора {@code targets}
 * случайно выбирается один целевой пул и передаётся в {@code aliasConsumer}.
 */
public record RandomStructurePoolAliasBinding(
	RegistryKey<StructurePool> alias,
	Pool<RegistryKey<StructurePool>> targets
) implements StructurePoolAliasBinding {

	static final MapCodec<RandomStructurePoolAliasBinding> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			RegistryKey.createCodec(RegistryKeys.TEMPLATE_POOL)
				.fieldOf("alias")
				.forGetter(RandomStructurePoolAliasBinding::alias),
			Pool.createNonEmptyCodec(RegistryKey.createCodec(RegistryKeys.TEMPLATE_POOL))
				.fieldOf("targets")
				.forGetter(RandomStructurePoolAliasBinding::targets)
		).apply(instance, RandomStructurePoolAliasBinding::new)
	);

	@Override
	public void forEach(
		Random random,
		BiConsumer<RegistryKey<StructurePool>, RegistryKey<StructurePool>> aliasConsumer
	) {
		targets.getOrEmpty(random)
			.ifPresent(target -> aliasConsumer.accept(alias, target));
	}

	@Override
	public Stream<RegistryKey<StructurePool>> streamTargets() {
		return targets.getEntries().stream().map(Weighted::value);
	}

	@Override
	public MapCodec<RandomStructurePoolAliasBinding> getCodec() {
		return CODEC;
	}
}
