package net.minecraft.structure.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

/**
 * Процессор структур, смещающий каждый блок по вертикали к поверхности рельефа.
 * Итоговая Y-координата блока вычисляется как высота поверхности по карте высот {@code heightmap}
 * плюс {@code offset} плюс исходная Y-координата блока в шаблоне.
 * <p>
 * При работе в {@link ServerWorld} WG-варианты карт высот заменяются на их runtime-аналоги,
 * так как WG-карты недоступны после генерации чанков.
 */
public class GravityStructureProcessor extends StructureProcessor {

	public static final MapCodec<GravityStructureProcessor> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Heightmap.Type.CODEC
				.fieldOf("heightmap")
				.orElse(Heightmap.Type.WORLD_SURFACE_WG)
				.forGetter(processor -> processor.heightmap),
			Codec.INT.fieldOf("offset").orElse(0).forGetter(processor -> processor.offset)
		).apply(instance, GravityStructureProcessor::new)
	);

	private final Heightmap.Type heightmap;
	private final int offset;

	public GravityStructureProcessor(Heightmap.Type heightmap, int offset) {
		this.heightmap = heightmap;
		this.offset = offset;
	}

	@Override
	public StructureTemplate.@Nullable StructureBlockInfo process(
		WorldView world,
		BlockPos pos,
		BlockPos pivot,
		StructureTemplate.StructureBlockInfo originalBlockInfo,
		StructureTemplate.StructureBlockInfo currentBlockInfo,
		StructurePlacementData data
	) {
		Heightmap.Type resolvedHeightmap = resolveHeightmapType(world);
		BlockPos blockPos = currentBlockInfo.pos();
		int surfaceY = world.getTopY(resolvedHeightmap, blockPos.getX(), blockPos.getZ()) + offset;
		int templateRelativeY = originalBlockInfo.pos().getY();

		return new StructureTemplate.StructureBlockInfo(
			new BlockPos(blockPos.getX(), surfaceY + templateRelativeY, blockPos.getZ()),
			currentBlockInfo.state(),
			currentBlockInfo.nbt()
		);
	}

	/**
	 * Заменяет WG-варианты карт высот на runtime-аналоги при работе в уже сгенерированном мире.
	 */
	private Heightmap.Type resolveHeightmapType(WorldView world) {
		if (world instanceof ServerWorld) {
			if (heightmap == Heightmap.Type.WORLD_SURFACE_WG) {
				return Heightmap.Type.WORLD_SURFACE;
			}

			if (heightmap == Heightmap.Type.OCEAN_FLOOR_WG) {
				return Heightmap.Type.OCEAN_FLOOR;
			}
		}

		return heightmap;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.GRAVITY;
	}
}
