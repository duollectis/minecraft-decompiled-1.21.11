package net.minecraft.client.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.model.json.BlockModelDefinition;

/**
 * Контракт для создателей определений моделей состояний блоков.
 * Реализации предоставляют блок и соответствующее определение модели
 * для генерации JSON-файлов blockstates.
 */
@Environment(EnvType.CLIENT)
public interface BlockModelDefinitionCreator {

	Block getBlock();

	BlockModelDefinition createBlockModelDefinition();
}
