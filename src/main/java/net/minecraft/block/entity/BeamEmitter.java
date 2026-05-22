package net.minecraft.block.entity;

import java.util.List;

/**
 * Интерфейс для блок-сущностей, испускающих вертикальный луч (например, маяк).
 */
public interface BeamEmitter {

	List<BeamEmitter.BeamSegment> getBeamSegments();

	/**
	 * Один непрерывный сегмент луча маяка с единым цветом.
	 * Высота сегмента увеличивается при прохождении через прозрачные блоки того же цвета.
	 */
	class BeamSegment {

		private final int color;
		private int height;

		public BeamSegment(int color) {
			this.color = color;
			height = 1;
		}

		public void increaseHeight() {
			height++;
		}

		public int getColor() {
			return color;
		}

		public int getHeight() {
			return height;
		}
	}
}
