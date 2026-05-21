package net.minecraft.entity.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.VariantSelectorProvider;
import net.minecraft.registry.Registries;

/**
 * {@code SpawnCondition}.
 */
public interface SpawnCondition extends VariantSelectorProvider.SelectorCondition<SpawnContext> {

	Codec<SpawnCondition>
			CODEC =
			Registries.SPAWN_CONDITION_TYPE.getCodec().dispatch(SpawnCondition::getCodec, codec -> codec);

	MapCodec<? extends SpawnCondition> getCodec();
}
