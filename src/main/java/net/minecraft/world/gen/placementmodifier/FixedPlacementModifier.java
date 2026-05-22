package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

import java.util.List;
import java.util.stream.Stream;

/**
 * Модификатор размещения с фиксированным набором позиций — возвращает только те
 * из них, которые принадлежат тому же чанку, что и входная позиция.
 */
public class FixedPlacementModifier extends PlacementModifier {

	public static final MapCodec<FixedPlacementModifier> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(BlockPos.CODEC
				.listOf()
				.fieldOf("positions")
				.forGetter(modifier -> modifier.positions))
			.apply(instance, FixedPlacementModifier::new)
	);
	private final List<BlockPos> positions;

	public static FixedPlacementModifier of(BlockPos... positions) {
		return new FixedPlacementModifier(List.of(positions));
	}

	private FixedPlacementModifier(List<BlockPos> positions) {
		this.positions = positions;
	}

	@Override
	public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
		int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
		int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
		boolean hasMatch = false;

		for (BlockPos candidate : positions) {
			if (chunkSectionMatchesPos(chunkX, chunkZ, candidate)) {
				hasMatch = true;
				break;
			}
		}

		return hasMatch
			? positions.stream().filter(candidate -> chunkSectionMatchesPos(chunkX, chunkZ, candidate))
			: Stream.empty();
	}

	private static boolean chunkSectionMatchesPos(int chunkSectionX, int chunkSectionZ, BlockPos pos) {
		return chunkSectionX == ChunkSectionPos.getSectionCoord(pos.getX())
				&& chunkSectionZ == ChunkSectionPos.getSectionCoord(pos.getZ());
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.FIXED_PLACEMENT;
	}
}
