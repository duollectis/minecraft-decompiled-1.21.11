package net.minecraft.structure.pool.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Привязка псевдонима пула структур к случайно выбранной группе привязок.
 * Из взвешенного набора {@code groups} случайно выбирается одна группа
 * (список {@link StructurePoolAliasBinding}), и все привязки в ней применяются.
 * Это позволяет атомарно переключать сразу несколько псевдонимов как единый набор.
 */
public record RandomGroupStructurePoolAliasBinding(
	Pool<List<StructurePoolAliasBinding>> groups
) implements StructurePoolAliasBinding {

	static final MapCodec<RandomGroupStructurePoolAliasBinding> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Pool.createNonEmptyCodec(Codec.list(StructurePoolAliasBinding.CODEC))
				.fieldOf("groups")
				.forGetter(RandomGroupStructurePoolAliasBinding::groups)
		).apply(instance, RandomGroupStructurePoolAliasBinding::new)
	);

	@Override
	public void forEach(
		Random random,
		BiConsumer<RegistryKey<StructurePool>, RegistryKey<StructurePool>> aliasConsumer
	) {
		groups.getOrEmpty(random)
			.ifPresent(group -> group.forEach(binding -> binding.forEach(random, aliasConsumer)));
	}

	@Override
	public Stream<RegistryKey<StructurePool>> streamTargets() {
		return groups.getEntries()
			.stream()
			.flatMap(entry -> entry.value().stream())
			.flatMap(StructurePoolAliasBinding::streamTargets);
	}

	@Override
	public MapCodec<RandomGroupStructurePoolAliasBinding> getCodec() {
		return CODEC;
	}
}
