package net.minecraft.structure.processor;

import java.util.List;

/**
 * Именованный список процессоров структур, хранящийся в реестре {@code processor_list}.
 * Используется как ссылка на набор процессоров в элементах пула структур
 * (например, в {@link net.minecraft.structure.pool.SinglePoolElement}).
 */
public class StructureProcessorList {

	private final List<StructureProcessor> list;

	public StructureProcessorList(List<StructureProcessor> list) {
		this.list = list;
	}

	public List<StructureProcessor> getList() {
		return list;
	}

	@Override
	public String toString() {
		return "ProcessorList[" + list + "]";
	}
}
