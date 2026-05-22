package net.minecraft.util.profiler.log;

/**
 * Журнал отладочных сэмплов: принимает одно или несколько значений за тик
 * и передаёт их подписчикам или сохраняет в кольцевом буфере.
 */
public interface DebugSampleLog {

	void set(long[] values);

	void push(long value);

	void push(long value, int column);
}
