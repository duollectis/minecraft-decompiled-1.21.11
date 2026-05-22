package net.minecraft.util.profiler.log;

/**
 * Интерфейс кольцевого буфера многомерных отладочных сэмплов.
 * Позволяет читать накопленные значения по индексу и измерению.
 */
public interface MultiValueDebugSampleLog {

	int getDimension();

	int getLength();

	long get(int index);

	long get(int index, int dimension);

	void clear();
}
