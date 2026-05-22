package net.minecraft.world.gen.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.MineshaftGenerator;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Структура шахты. Тип шахты (обычная или мезовая) определяет используемые блоки дерева.
 * Позиция по Y вычисляется динамически: для мезы — между уровнем моря и поверхностью,
 * для обычной — смещается в допустимый диапазон подземелья.
 */
public class MineshaftStructure extends Structure {

	public static final MapCodec<MineshaftStructure> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    configCodecBuilder(instance),
					                    MineshaftStructure.Type.CODEC
							                    .fieldOf("mineshaft_type")
							                    .forGetter(mineshaftStructure -> mineshaftStructure.type)
			                    )
			                    .apply(instance, MineshaftStructure::new)
	);
	private final MineshaftStructure.Type type;

	public MineshaftStructure(Structure.Config config, MineshaftStructure.Type type) {
		super(config);
		this.type = type;
	}

	@Override
	public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		context.random().nextDouble();
		ChunkPos chunkPos = context.chunkPos();
		BlockPos anchorPos = new BlockPos(chunkPos.getCenterX(), 50, chunkPos.getStartZ());
		StructurePiecesCollector collector = new StructurePiecesCollector();
		int verticalShift = addPieces(collector, context);
		return Optional.of(new Structure.StructurePosition(
				anchorPos.add(0, verticalShift, 0),
				Either.right(collector)
		));
	}

	private int addPieces(StructurePiecesCollector collector, Structure.Context context) {
		ChunkPos chunkPos = context.chunkPos();
		ChunkRandom chunkRandom = context.random();
		ChunkGenerator chunkGenerator = context.chunkGenerator();
		MineshaftGenerator.MineshaftRoom mineshaftRoom = new MineshaftGenerator.MineshaftRoom(
				0, chunkRandom, chunkPos.getOffsetX(2), chunkPos.getOffsetZ(2), type
		);
		collector.addPiece(mineshaftRoom);
		mineshaftRoom.fillOpenings(mineshaftRoom, collector, chunkRandom);
		int seaLevel = chunkGenerator.getSeaLevel();

		if (type == MineshaftStructure.Type.MESA) {
			BlockPos center = collector.getBoundingBox().getCenter();
			int surfaceY = chunkGenerator.getHeight(
					center.getX(),
					center.getZ(),
					Heightmap.Type.WORLD_SURFACE_WG,
					context.world(),
					context.noiseConfig()
			);
			int targetY = surfaceY <= seaLevel ? seaLevel : MathHelper.nextBetween(chunkRandom, seaLevel, surfaceY);
			int shift = targetY - center.getY();
			collector.shift(shift);
			return shift;
		}

		return collector.shiftInto(seaLevel, chunkGenerator.getMinimumY(), chunkRandom, 10);
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.MINESHAFT;
	}

	/** Тип шахты, определяющий используемые блоки дерева. */
	public enum Type implements StringIdentifiable {
		NORMAL("normal", Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_FENCE),
		MESA("mesa", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE);

		public static final Codec<MineshaftStructure.Type> CODEC = StringIdentifiable.createCodec(MineshaftStructure.Type::values);
		private static final IntFunction<MineshaftStructure.Type> BY_ID = ValueLists.createIndexToValueFunction(
				(MineshaftStructure.Type t) -> t.ordinal(), values(), ValueLists.OutOfBoundsHandling.ZERO
		);
		private final String name;
		private final BlockState log;
		private final BlockState planks;
		private final BlockState fence;

		Type(String name, Block log, Block planks, Block fence) {
			this.name = name;
			this.log = log.getDefaultState();
			this.planks = planks.getDefaultState();
			this.fence = fence.getDefaultState();
		}

		public static MineshaftStructure.Type byId(int id) {
			return BY_ID.apply(id);
		}

		public String getName() {
			return name;
		}

		public BlockState getLog() {
			return log;
		}

		public BlockState getPlanks() {
			return planks;
		}

		public BlockState getFence() {
			return fence;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
