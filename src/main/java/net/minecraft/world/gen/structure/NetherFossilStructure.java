package net.minecraft.world.gen.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.NetherFossilGenerator;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.heightprovider.HeightProvider;

import java.util.Optional;

/**
 * Структура окаменелости Нижнего мира. Высота размещения определяется провайдером {@link HeightProvider},
 * а позиция по X/Z выбирается случайно внутри чанка. Ищет первую позицию над душевым песком или твёрдым блоком.
 */
public class NetherFossilStructure extends Structure {

	public static final MapCodec<NetherFossilStructure> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							configCodecBuilder(instance),
							HeightProvider.CODEC.fieldOf("height").forGetter(structure -> structure.height)
					)
					.apply(instance, NetherFossilStructure::new)
	);
	public final HeightProvider height;

	public NetherFossilStructure(Structure.Config config, HeightProvider height) {
		super(config);
		this.height = height;
	}

	@Override
	public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		ChunkRandom chunkRandom = context.random();
		int x = context.chunkPos().getStartX() + chunkRandom.nextInt(16);
		int z = context.chunkPos().getStartZ() + chunkRandom.nextInt(16);
		int seaLevel = context.chunkGenerator().getSeaLevel();
		HeightContext heightContext = new HeightContext(context.chunkGenerator(), context.world());
		int y = height.get(chunkRandom, heightContext);
		VerticalBlockSample columnSample = context.chunkGenerator().getColumnSample(x, z, context.world(), context.noiseConfig());
		BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);

		while (y > seaLevel) {
			BlockState above = columnSample.getState(y);
			BlockState below = columnSample.getState(--y);
			if (above.isAir()
					&& (below.isOf(Blocks.SOUL_SAND) || below.isSideSolidFullSquare(
					EmptyBlockView.INSTANCE,
					mutable.setY(y),
					Direction.UP
			)
			)) {
				break;
			}
		}

		if (y <= seaLevel) {
			return Optional.empty();
		}

		BlockPos spawnPos = new BlockPos(x, y, z);
		return Optional.of(
				new Structure.StructurePosition(
						spawnPos,
						collector -> NetherFossilGenerator.addPieces(
								context.structureTemplateManager(),
								collector,
								chunkRandom,
								spawnPos
						)
				)
		);
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.NETHER_FOSSIL;
	}
}
