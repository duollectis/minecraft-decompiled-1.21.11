package net.minecraft.util.profiler;

/**
 * Категория сэмплируемой метрики, определяющая группировку данных в CSV-дампе.
 */
public enum SampleType {
	PATH_FINDING("pathfinding"),
	EVENT_LOOPS("event-loops"),
	CONSECUTIVE_EXECUTORS("consecutive-executors"),
	TICK_LOOP("ticking"),
	JVM("jvm"),
	CHUNK_RENDERING("chunk rendering"),
	CHUNK_RENDERING_DISPATCHING("chunk rendering dispatching"),
	CPU("cpu"),
	GPU("gpu");

	private final String name;

	SampleType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
