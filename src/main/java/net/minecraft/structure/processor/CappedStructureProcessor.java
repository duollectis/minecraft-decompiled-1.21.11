package net.minecraft.structure.processor;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Процессор структур, ограничивающий максимальное число применений делегата.
 * Из всех блоков шаблона случайно выбирается не более {@code limit} штук,
 * и только к ним применяется {@code delegate}. Это позволяет контролировать
 * плотность замен (например, ограничить число подозрительного гравия с лутом).
 */
public class CappedStructureProcessor extends StructureProcessor {

	public static final MapCodec<CappedStructureProcessor> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			StructureProcessorType.CODEC.fieldOf("delegate").forGetter(processor -> processor.delegate),
			IntProvider.POSITIVE_CODEC.fieldOf("limit").forGetter(processor -> processor.limit)
		).apply(instance, CappedStructureProcessor::new)
	);

	private final StructureProcessor delegate;
	private final IntProvider limit;

	public CappedStructureProcessor(StructureProcessor delegate, IntProvider limit) {
		this.delegate = delegate;
		this.limit = limit;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.CAPPED;
	}

	/**
	 * Применяет делегирующий процессор к случайно выбранному подмножеству блоков,
	 * не превышающему {@code limit}. Список модифицируется на месте.
	 */
	@Override
	public final List<StructureTemplate.StructureBlockInfo> reprocess(
		ServerWorldAccess world,
		BlockPos pos,
		BlockPos pivot,
		List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
		List<StructureTemplate.StructureBlockInfo> currentBlockInfos,
		StructurePlacementData data
	) {
		if (limit.getMax() == 0 || currentBlockInfos.isEmpty()) {
			return currentBlockInfos;
		}

		if (originalBlockInfos.size() != currentBlockInfos.size()) {
			Util.logErrorOrPause(
				"Original block info list not in sync with processed list, skipping processing. Original size: "
					+ originalBlockInfos.size()
					+ ", Processed size: "
					+ currentBlockInfos.size()
			);
			return currentBlockInfos;
		}

		Random random = Random.create(world.toServerWorld().getSeed()).nextSplitter().split(pos);
		int maxReplacements = Math.min(limit.get(random), currentBlockInfos.size());

		if (maxReplacements < 1) {
			return currentBlockInfos;
		}

		IntArrayList shuffledIndices = Util.shuffle(IntStream.range(0, currentBlockInfos.size()), random);
		IntIterator indexIterator = shuffledIndices.intIterator();
		int replacementCount = 0;

		while (indexIterator.hasNext() && replacementCount < maxReplacements) {
			int index = indexIterator.nextInt();
			StructureTemplate.StructureBlockInfo original = originalBlockInfos.get(index);
			StructureTemplate.StructureBlockInfo current = currentBlockInfos.get(index);
			StructureTemplate.StructureBlockInfo processed = delegate.process(world, pos, pivot, original, current, data);

			if (processed != null && !current.equals(processed)) {
				replacementCount++;
				currentBlockInfos.set(index, processed);
			}
		}

		return currentBlockInfos;
	}
}
