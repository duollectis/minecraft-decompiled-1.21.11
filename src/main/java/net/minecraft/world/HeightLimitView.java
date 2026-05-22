package net.minecraft.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Интерфейс, описывающий вертикальные границы мира или чанка.
 * Предоставляет методы для работы с нижней/верхней границами,
 * секциями чанков и проверки принадлежности Y-координаты допустимому диапазону.
 */
public interface HeightLimitView {

	int getHeight();

	int getBottomY();

	default int getTopYInclusive() {
		return getBottomY() + getHeight() - 1;
	}

	default int countVerticalSections() {
		return getTopSectionCoord() - getBottomSectionCoord() + 1;
	}

	default int getBottomSectionCoord() {
		return ChunkSectionPos.getSectionCoord(getBottomY());
	}

	default int getTopSectionCoord() {
		return ChunkSectionPos.getSectionCoord(getTopYInclusive());
	}

	default boolean isInHeightLimit(int y) {
		return y >= getBottomY() && y <= getTopYInclusive();
	}

	default boolean isOutOfHeightLimit(BlockPos pos) {
		return isOutOfHeightLimit(pos.getY());
	}

	default boolean isOutOfHeightLimit(int y) {
		return y < getBottomY() || y > getTopYInclusive();
	}

	default int getSectionIndex(int y) {
		return sectionCoordToIndex(ChunkSectionPos.getSectionCoord(y));
	}

	default int sectionCoordToIndex(int coord) {
		return coord - getBottomSectionCoord();
	}

	default int sectionIndexToCoord(int index) {
		return index + getBottomSectionCoord();
	}

	static HeightLimitView create(int bottomY, int height) {
		return new HeightLimitView() {
			@Override
			public int getHeight() {
				return height;
			}

			@Override
			public int getBottomY() {
				return bottomY;
			}
		};
	}
}
