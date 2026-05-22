package net.minecraft.structure.processor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Процессор структур, заменяющий jigsaw-блоки на их финальные состояния.
 * Читает поле {@code final_state} из NBT jigsaw-блока и устанавливает соответствующий блок.
 * Если финальное состояние — {@link Blocks#STRUCTURE_VOID}, блок удаляется (возвращается {@code null}).
 * <p>
 * Если флаг {@link SharedConstants#KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN} установлен,
 * jigsaw-блоки сохраняются без замены (режим отладки).
 */
public class JigsawReplacementStructureProcessor extends StructureProcessor {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<JigsawReplacementStructureProcessor> CODEC =
		MapCodec.unit(() -> JigsawReplacementStructureProcessor.INSTANCE);

	public static final JigsawReplacementStructureProcessor INSTANCE = new JigsawReplacementStructureProcessor();

	private JigsawReplacementStructureProcessor() {
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
		BlockState blockState = currentBlockInfo.state();

		if (!blockState.isOf(Blocks.JIGSAW) || SharedConstants.KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN) {
			return currentBlockInfo;
		}

		if (currentBlockInfo.nbt() == null) {
			LOGGER.warn("Jigsaw block at {} is missing nbt, will not replace", pos);
			return currentBlockInfo;
		}

		String finalStateString = currentBlockInfo.nbt().getString("final_state", "minecraft:air");

		BlockState finalState;
		try {
			BlockArgumentParser.BlockResult blockResult = BlockArgumentParser.block(
				world.createCommandRegistryWrapper(RegistryKeys.BLOCK),
				finalStateString,
				true
			);
			finalState = blockResult.blockState();
		} catch (CommandSyntaxException exception) {
			LOGGER.error(
				"Failed to parse jigsaw replacement state '{}' at {}: {}",
				finalStateString,
				pos,
				exception.getMessage()
			);
			return null;
		}

		return finalState.isOf(Blocks.STRUCTURE_VOID)
			? null
			: new StructureTemplate.StructureBlockInfo(currentBlockInfo.pos(), finalState, null);
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.JIGSAW_REPLACEMENT;
	}
}
