package net.minecraft;

/**
 * {@code SaveVersion}.
 */
public record SaveVersion(int id, String series) {

	public static final String MAIN_SERIES = "main";

	public boolean isNotMainSeries() {
		return !this.series.equals("main");
	}

	public boolean isAvailableTo(SaveVersion other) {
		return SharedConstants.OPEN_INCOMPATIBLE_WORLDS ? true : this.series().equals(other.series());
	}
}
