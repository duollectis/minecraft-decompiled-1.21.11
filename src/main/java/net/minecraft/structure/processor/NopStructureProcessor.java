package net.minecraft.structure.processor;

import com.mojang.serialization.MapCodec;

/**
 * Процессор-заглушка, не выполняющий никаких преобразований.
 * Возвращает каждый блок без изменений. Используется как нейтральный элемент
 * в цепочках процессоров или как значение по умолчанию.
 * Является синглтоном — используется через {@link #INSTANCE}.
 */
public class NopStructureProcessor extends StructureProcessor {

	public static final MapCodec<NopStructureProcessor> CODEC = MapCodec.unit(() -> NopStructureProcessor.INSTANCE);
	public static final NopStructureProcessor INSTANCE = new NopStructureProcessor();

	private NopStructureProcessor() {
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.NOP;
	}
}
