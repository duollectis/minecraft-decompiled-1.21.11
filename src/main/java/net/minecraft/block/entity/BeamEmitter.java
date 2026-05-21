package net.minecraft.block.entity;

import java.util.List;

/**
 * {@code BeamEmitter}.
 */
public interface BeamEmitter {

	List<BeamEmitter.BeamSegment> getBeamSegments();

	/**
	 * {@code BeamSegment}.
	 */
	public static class BeamSegment {

		private final int color;
		private int height;

		public BeamSegment(int color) {
			this.color = color;
			this.height = 1;
		}

		/**
		 * Increase height.
		 */
		public void increaseHeight() {
			this.height++;
		}

		public int getColor() {
			return this.color;
		}

		public int getHeight() {
			return this.height;
		}
	}
}
