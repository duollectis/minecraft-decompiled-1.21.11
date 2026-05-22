package net.minecraft.world.gen.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.world.dimension.DimensionType;

/**
 * Описывает один слой плоского мира: блок и его толщину в блоках.
 * Используется в {@link FlatChunkGeneratorConfig} для построения стека слоёв снизу вверх.
 */
public class FlatChunkGeneratorLayer {

	public static final Codec<FlatChunkGeneratorLayer> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.intRange(0, DimensionType.MAX_HEIGHT)
					     .fieldOf("height")
					     .forGetter(FlatChunkGeneratorLayer::getThickness),
					Registries.BLOCK
					          .getCodec()
					          .fieldOf("block")
					          .orElse(Blocks.AIR)
					          .forGetter(layer -> layer.getBlockState().getBlock())
			).apply(instance, FlatChunkGeneratorLayer::new)
	);

	private final Block block;
	private final int thickness;

	public FlatChunkGeneratorLayer(int thickness, Block block) {
		this.thickness = thickness;
		this.block = block;
	}

	public int getThickness() {
		return thickness;
	}

	public BlockState getBlockState() {
		return block.getDefaultState();
	}

	public FlatChunkGeneratorLayer withMaxThickness(int maxThickness) {
		return thickness > maxThickness
				? new FlatChunkGeneratorLayer(maxThickness, block)
				: this;
	}

	@Override
	public String toString() {
		return (thickness != 1 ? thickness + "*" : "") + Registries.BLOCK.getId(block);
	}
}
