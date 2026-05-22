package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.util.math.Divider;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Виджет-контейнер, выравнивающий дочерние элементы по сетке строк и столбцов.
 * Поддерживает объединение ячеек (span), настраиваемые отступы между строками и столбцами,
 * а также удобный {@link Adder} для последовательного добавления элементов по строкам.
 */
@Environment(EnvType.CLIENT)
public class GridWidget extends WrapperWidget {

	private final List<Widget> children = new ArrayList<>();
	private final List<GridWidget.Element> grids = new ArrayList<>();
	private final Positioner mainPositioner = Positioner.create();
	private int rowSpacing = 0;
	private int columnSpacing = 0;

	public GridWidget() {
		this(0, 0);
	}

	public GridWidget(int x, int y) {
		super(x, y, 0, 0);
	}

	@Override
	public void refreshPositions() {
		super.refreshPositions();
		int maxRow = 0;
		int maxColumn = 0;

		for (GridWidget.Element element : grids) {
			maxRow = Math.max(element.getRowEnd(), maxRow);
			maxColumn = Math.max(element.getColumnEnd(), maxColumn);
		}

		int[] columnWidths = new int[maxColumn + 1];
		int[] rowHeights = new int[maxRow + 1];

		for (GridWidget.Element element : grids) {
			int cellHeight = element.getHeight() - (element.occupiedRows - 1) * rowSpacing;
			Divider rowDivider = new Divider(cellHeight, element.occupiedRows);
			for (int row = element.row; row <= element.getRowEnd(); row++) {
				rowHeights[row] = Math.max(rowHeights[row], rowDivider.nextInt());
			}

			int cellWidth = element.getWidth() - (element.occupiedColumns - 1) * columnSpacing;
			Divider colDivider = new Divider(cellWidth, element.occupiedColumns);
			for (int col = element.column; col <= element.getColumnEnd(); col++) {
				columnWidths[col] = Math.max(columnWidths[col], colDivider.nextInt());
			}
		}

		int[] columnOffsets = new int[maxColumn + 1];
		int[] rowOffsets = new int[maxRow + 1];
		columnOffsets[0] = 0;
		for (int col = 1; col <= maxColumn; col++) {
			columnOffsets[col] = columnOffsets[col - 1] + columnWidths[col - 1] + columnSpacing;
		}

		rowOffsets[0] = 0;
		for (int row = 1; row <= maxRow; row++) {
			rowOffsets[row] = rowOffsets[row - 1] + rowHeights[row - 1] + rowSpacing;
		}

		for (GridWidget.Element element : grids) {
			int totalWidth = 0;
			for (int col = element.column; col <= element.getColumnEnd(); col++) {
				totalWidth += columnWidths[col];
			}

			totalWidth += columnSpacing * (element.occupiedColumns - 1);
			element.setX(getX() + columnOffsets[element.column], totalWidth);

			int totalHeight = 0;
			for (int row = element.row; row <= element.getRowEnd(); row++) {
				totalHeight += rowHeights[row];
			}

			totalHeight += rowSpacing * (element.occupiedRows - 1);
			element.setY(getY() + rowOffsets[element.row], totalHeight);
		}

		this.width = columnOffsets[maxColumn] + columnWidths[maxColumn];
		this.height = rowOffsets[maxRow] + rowHeights[maxRow];
	}

	public <T extends Widget> T add(T widget, int row, int column) {
		return add(widget, row, column, copyPositioner());
	}

	public <T extends Widget> T add(T widget, int row, int column, Positioner positioner) {
		return add(widget, row, column, 1, 1, positioner);
	}

	public <T extends Widget> T add(T widget, int row, int column, Consumer<Positioner> callback) {
		return add(widget, row, column, 1, 1, Util.make(copyPositioner(), callback));
	}

	public <T extends Widget> T add(T widget, int row, int column, int occupiedRows, int occupiedColumns) {
		return add(widget, row, column, occupiedRows, occupiedColumns, copyPositioner());
	}

	/**
	 * Добавляет виджет в ячейку сетки с заданным span-ом по строкам и столбцам.
	 *
	 * @param widget виджет для добавления
	 * @param row начальная строка (0-based)
	 * @param column начальный столбец (0-based)
	 * @param occupiedRows количество занимаемых строк (минимум 1)
	 * @param occupiedColumns количество занимаемых столбцов (минимум 1)
	 * @param positioner позиционер для выравнивания внутри ячейки
	 * @return переданный виджет (для fluent-цепочек)
	 */
	public <T extends Widget> T add(
			T widget,
			int row,
			int column,
			int occupiedRows,
			int occupiedColumns,
			Positioner positioner
	) {
		if (occupiedRows < 1) {
			throw new IllegalArgumentException("Occupied rows must be at least 1");
		}

		if (occupiedColumns < 1) {
			throw new IllegalArgumentException("Occupied columns must be at least 1");
		}

		grids.add(new GridWidget.Element(widget, row, column, occupiedRows, occupiedColumns, positioner));
		children.add(widget);
		return widget;
	}

	public <T extends Widget> T add(
			T widget,
			int row,
			int column,
			int occupiedRows,
			int occupiedColumns,
			Consumer<Positioner> callback
	) {
		return add(widget, row, column, occupiedRows, occupiedColumns, Util.make(copyPositioner(), callback));
	}

	public GridWidget setColumnSpacing(int columnSpacing) {
		this.columnSpacing = columnSpacing;
		return this;
	}

	public GridWidget setRowSpacing(int rowSpacing) {
		this.rowSpacing = rowSpacing;
		return this;
	}

	public GridWidget setSpacing(int spacing) {
		return setColumnSpacing(spacing).setRowSpacing(spacing);
	}

	@Override
	public void forEachElement(Consumer<Widget> consumer) {
		children.forEach(consumer);
	}

	public Positioner copyPositioner() {
		return mainPositioner.copy();
	}

	public Positioner getMainPositioner() {
		return mainPositioner;
	}

	public GridWidget.Adder createAdder(int columns) {
		return new GridWidget.Adder(columns);
	}

	/**
	 * Вспомогательный класс для последовательного добавления виджетов в сетку по строкам.
	 * Автоматически переносит на следующую строку при превышении числа столбцов.
	 */
	@Environment(EnvType.CLIENT)
	public final class Adder {

		private final int columns;
		private int totalOccupiedColumns;

		Adder(int columns) {
			this.columns = columns;
		}

		public <T extends Widget> T add(T widget) {
			return add(widget, 1);
		}

		public <T extends Widget> T add(T widget, int occupiedColumns) {
			return add(widget, occupiedColumns, getMainPositioner());
		}

		public <T extends Widget> T add(T widget, Positioner positioner) {
			return add(widget, 1, positioner);
		}

		public <T extends Widget> T add(T widget, int occupiedColumns, Positioner positioner) {
			int row = totalOccupiedColumns / columns;
			int col = totalOccupiedColumns % columns;
			if (col + occupiedColumns > columns) {
				row++;
				col = 0;
				totalOccupiedColumns = MathHelper.roundUpToMultiple(totalOccupiedColumns, columns);
			}

			totalOccupiedColumns += occupiedColumns;
			return GridWidget.this.add(widget, row, col, 1, occupiedColumns, positioner);
		}

		public GridWidget getGridWidget() {
			return GridWidget.this;
		}

		public Positioner copyPositioner() {
			return GridWidget.this.copyPositioner();
		}

		public Positioner getMainPositioner() {
			return GridWidget.this.getMainPositioner();
		}
	}

	@Environment(EnvType.CLIENT)
	static class Element extends WrapperWidget.WrappedElement {

		final int row;
		final int column;
		final int occupiedRows;
		final int occupiedColumns;

		Element(Widget widget, int row, int column, int occupiedRows, int occupiedColumns, Positioner positioner) {
			super(widget, positioner.toImpl());
			this.row = row;
			this.column = column;
			this.occupiedRows = occupiedRows;
			this.occupiedColumns = occupiedColumns;
		}

		public int getRowEnd() {
			return row + occupiedRows - 1;
		}

		public int getColumnEnd() {
			return column + occupiedColumns - 1;
		}
	}
}
