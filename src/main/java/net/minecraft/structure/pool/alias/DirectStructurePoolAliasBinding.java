package net.minecraft.structure.pool.alias;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.random.Random;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Прямая привязка псевдонима пула: всегда отображает {@code alias} на {@code target},
 * независимо от случайности. Наиболее простой и предсказуемый тип привязки.
 */
public record DirectStructurePoolAliasBinding(
	RegistryKey<StructurePool> alias,
	RegistryKey<StructurePool> target
) implements StructurePoolAliasBinding {

	static final MapCodec<DirectStructurePoolAliasBinding> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			RegistryKey.createCodec(RegistryKeys.TEMPLATE_POOL).fieldOf("alias")
				.forGetter(DirectStructurePoolAliasBinding::alias),
			RegistryKey.createCodec(RegistryKeys.TEMPLATE_POOL).fieldOf("target")
				.forGetter(DirectStructurePoolAliasBinding::target)
		).apply(instance, DirectStructurePoolAliasBinding::new)
	);

	@Override
	public void forEach(
		Random random,
		BiConsumer<RegistryKey<StructurePool>, RegistryKey<StructurePool>> aliasConsumer
	) {
		aliasConsumer.accept(alias, target);
	}

	@Override
	public Stream<RegistryKey<StructurePool>> streamTargets() {
		return Stream.of(target);
	}

	@Override
	public MapCodec<DirectStructurePoolAliasBinding> getCodec() {
		return CODEC;
	}
}
