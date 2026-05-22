package net.minecraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.fabricmc.fabric.api.block.v1.FabricBlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;

/**
 * Конкретная реализация состояния блока. Хранит ссылку на {@link Block} и набор
 * значений свойств ({@link net.minecraft.state.property.Property}), определяющих
 * внешний вид и поведение блока в мире.
 */
public class BlockState extends AbstractBlock.AbstractBlockState implements FabricBlockState {

	public static final Codec<BlockState> CODEC =
			createCodec(Registries.BLOCK.getCodec(), Block::getDefaultState).stable();

	public BlockState(
			Block block,
			Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap,
			MapCodec<BlockState> mapCodec
	) {
		super(block, propertyMap, mapCodec);
	}

	@Override
	protected BlockState asBlockState() {
		return this;
	}
}
