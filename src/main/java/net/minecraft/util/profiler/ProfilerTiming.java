package net.minecraft.util.profiler;

/**
 * Результат замера одной секции профайлера: имя, процент от родителя,
 * процент от общего времени тика и количество посещений.
 */
public final class ProfilerTiming implements Comparable<ProfilerTiming> {

	// 0xFF4B4B4B — тёмно-серый базовый цвет для визуализации секций
	private static final int BASE_COLOR = -12303292;
	private static final int COLOR_MASK = 0xAAAAAA;

	public final double parentSectionUsagePercentage;
	public final double totalUsagePercentage;
	public final long visitCount;
	public final String name;

	public ProfilerTiming(String name, double parentUsagePercentage, double totalUsagePercentage, long visitCount) {
		this.name = name;
		this.parentSectionUsagePercentage = parentUsagePercentage;
		this.totalUsagePercentage = totalUsagePercentage;
		this.visitCount = visitCount;
	}

	@Override
	public int compareTo(ProfilerTiming other) {
		if (other.parentSectionUsagePercentage < parentSectionUsagePercentage) {
			return -1;
		}

		return other.parentSectionUsagePercentage > parentSectionUsagePercentage
			? 1
			: other.name.compareTo(name);
	}

	public int getColor() {
		return (name.hashCode() & COLOR_MASK) + BASE_COLOR;
	}
}
