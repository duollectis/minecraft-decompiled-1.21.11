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
 * {@code RandomStructurePoolAliasBinding}.
 */
public record RandomStructurePoolAliasBinding(
		RegistryKey<StructurePool> alias,
		Pool<RegistryKey<StructurePool>> targets
) implements StructurePoolAliasBinding {

	static MapCodec<RandomStructurePoolAliasBinding> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    RegistryKey
							                    .createCodec(RegistryKeys.TEMPLATE_POOL)
							                    .fieldOf("alias")
							                    .forGetter(RandomStructurePoolAliasBinding::alias),
					                    Pool.createNonEmptyCodec(RegistryKey.createCodec(RegistryKeys.TEMPLATE_POOL))
					                        .fieldOf("targets")
					                        .forGetter(RandomStructurePoolAliasBinding::targets)
			                    )
			                    .apply(instance, RandomStructurePoolAliasBinding::new)
	);

	@Override
	public void forEach(
			Random random,
			BiConsumer<RegistryKey<StructurePool>, RegistryKey<StructurePool>> aliasConsumer
	) {
		this.targets
				.getOrEmpty(random)
				.ifPresent(target -> aliasConsumer.accept(this.alias, (RegistryKey<StructurePool>) target));
	}

	@Override
	public Stream<RegistryKey<StructurePool>> streamTargets() {
		return this.targets.getEntries().stream().map(Weighted::value);
	}

	@Override
	public MapCodec<RandomStructurePoolAliasBinding> getCodec() {
		return CODEC;
	}
}
