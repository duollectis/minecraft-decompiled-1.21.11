package net.minecraft.advancement;

import com.google.common.collect.Lists;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Реализует алгоритм Рейнгольда–Тилфорда для расстановки узлов дерева достижений.
 * <p>
 * Каждый узел хранит вычисленную строку ({@code row}) и глубину ({@code depth}),
 * которые затем применяются к {@link AdvancementDisplay#setPos} через {@link #apply()}.
 */
public class AdvancementPositioner {

	private final PlacedAdvancement advancement;
	private final @Nullable AdvancementPositioner parent;
	private final @Nullable AdvancementPositioner previousSibling;
	private final int childrenSize;
	private final List<AdvancementPositioner> children = Lists.newArrayList();
	private AdvancementPositioner optionalLast;
	private @Nullable AdvancementPositioner substituteChild;
	private int depth;
	private float row;
	private float relativeRowInSiblings;
	private float rowShift;
	private float rowOffset;

	public AdvancementPositioner(
		PlacedAdvancement advancement,
		@Nullable AdvancementPositioner parent,
		@Nullable AdvancementPositioner previousSibling,
		int childrenSize,
		int depth
	) {
		if (advancement.getAdvancement().display().isEmpty()) {
			throw new IllegalArgumentException("Can't position an invisible advancement!");
		}

		this.advancement = advancement;
		this.parent = parent;
		this.previousSibling = previousSibling;
		this.childrenSize = childrenSize;
		optionalLast = this;
		this.depth = depth;
		row = -1.0F;

		AdvancementPositioner lastChild = null;
		for (PlacedAdvancement child : advancement.getChildren()) {
			lastChild = findChildrenRecursively(child, lastChild);
		}
	}

	private @Nullable AdvancementPositioner findChildrenRecursively(
		PlacedAdvancement advancement,
		@Nullable AdvancementPositioner lastChild
	) {
		if (advancement.getAdvancement().display().isPresent()) {
			lastChild = new AdvancementPositioner(advancement, this, lastChild, children.size() + 1, depth + 1);
			children.add(lastChild);
		} else {
			for (PlacedAdvancement child : advancement.getChildren()) {
				lastChild = findChildrenRecursively(child, lastChild);
			}
		}
		return lastChild;
	}

	private void calculateRecursively() {
		if (children.isEmpty()) {
			row = previousSibling != null ? previousSibling.row + 1.0F : 0.0F;
			return;
		}

		AdvancementPositioner last = null;
		for (AdvancementPositioner child : children) {
			child.calculateRecursively();
			last = child.onFinishCalculation(last == null ? child : last);
		}

		onFinishChildrenCalculation();

		float firstRow = children.get(0).row;
		float lastRow = children.get(children.size() - 1).row;
		float midRow = (firstRow + lastRow) / 2.0F;

		if (previousSibling != null) {
			row = previousSibling.row + 1.0F;
			relativeRowInSiblings = row - midRow;
		} else {
			row = midRow;
		}
	}

	private float findMinRowRecursively(float deltaRow, int depth, float minRow) {
		row += deltaRow;
		this.depth = depth;
		if (row < minRow) {
			minRow = row;
		}
		for (AdvancementPositioner child : children) {
			minRow = child.findMinRowRecursively(deltaRow + relativeRowInSiblings, depth + 1, minRow);
		}
		return minRow;
	}

	private void increaseRowRecursively(float deltaRow) {
		row += deltaRow;
		for (AdvancementPositioner child : children) {
			child.increaseRowRecursively(deltaRow);
		}
	}

	private void onFinishChildrenCalculation() {
		float accumulated = 0.0F;
		float totalShift = 0.0F;

		for (int index = children.size() - 1; index >= 0; index--) {
			AdvancementPositioner child = children.get(index);
			child.row += accumulated;
			child.relativeRowInSiblings += accumulated;
			totalShift += child.rowShift;
			accumulated += child.rowOffset + totalShift;
		}
	}

	private @Nullable AdvancementPositioner getFirstChild() {
		if (substituteChild != null) {
			return substituteChild;
		}
		return children.isEmpty() ? null : children.get(0);
	}

	private @Nullable AdvancementPositioner getLastChild() {
		if (substituteChild != null) {
			return substituteChild;
		}
		return children.isEmpty() ? null : children.get(children.size() - 1);
	}

	private AdvancementPositioner onFinishCalculation(AdvancementPositioner last) {
		if (previousSibling == null) {
			return last;
		}

		AdvancementPositioner innerRight = this;
		AdvancementPositioner outerRight = this;
		AdvancementPositioner innerLeft = previousSibling;
		AdvancementPositioner outerLeft = parent.children.get(0);

		float shiftRight = relativeRowInSiblings;
		float shiftOuterRight = relativeRowInSiblings;
		float shiftLeft = innerLeft.relativeRowInSiblings;
		float shiftOuterLeft = outerLeft.relativeRowInSiblings;

		while (innerLeft.getLastChild() != null && innerRight.getFirstChild() != null) {
			innerLeft = innerLeft.getLastChild();
			innerRight = innerRight.getFirstChild();
			outerLeft = outerLeft.getFirstChild();
			outerRight = outerRight.getLastChild();
			outerRight.optionalLast = this;

			float gap = innerLeft.row + shiftLeft - (innerRight.row + shiftRight) + 1.0F;
			if (gap > 0.0F) {
				innerLeft.getLast(this, last).pushDown(this, gap);
				shiftRight += gap;
				shiftOuterRight += gap;
			}

			shiftLeft += innerLeft.relativeRowInSiblings;
			shiftRight += innerRight.relativeRowInSiblings;
			shiftOuterLeft += outerLeft.relativeRowInSiblings;
			shiftOuterRight += outerRight.relativeRowInSiblings;
		}

		if (innerLeft.getLastChild() != null && outerRight.getLastChild() == null) {
			outerRight.substituteChild = innerLeft.getLastChild();
			outerRight.relativeRowInSiblings += shiftLeft - shiftOuterRight;
		} else {
			if (innerRight.getFirstChild() != null && outerLeft.getFirstChild() == null) {
				outerLeft.substituteChild = innerRight.getFirstChild();
				outerLeft.relativeRowInSiblings += shiftRight - shiftOuterLeft;
			}
			last = this;
		}

		return last;
	}

	private void pushDown(AdvancementPositioner positioner, float extraRowDistance) {
		float ratio = positioner.childrenSize - childrenSize;
		if (ratio != 0.0F) {
			positioner.rowShift -= extraRowDistance / ratio;
			rowShift += extraRowDistance / ratio;
		}
		positioner.rowOffset += extraRowDistance;
		positioner.row += extraRowDistance;
		positioner.relativeRowInSiblings += extraRowDistance;
	}

	private AdvancementPositioner getLast(AdvancementPositioner positioner, AdvancementPositioner fallback) {
		return optionalLast != null && positioner.parent.children.contains(optionalLast)
			? optionalLast
			: fallback;
	}

	private void apply() {
		advancement.getAdvancement().display().ifPresent(display -> display.setPos(depth, row));
		for (AdvancementPositioner child : children) {
			child.apply();
		}
	}

	/**
	 * Вычисляет и применяет позиции всех достижений в дереве с заданным корнем.
	 *
	 * @throws IllegalArgumentException если корневое достижение не имеет дисплея
	 */
	public static void arrangeForTree(PlacedAdvancement root) {
		if (root.getAdvancement().display().isEmpty()) {
			throw new IllegalArgumentException("Can't position children of an invisible root!");
		}

		AdvancementPositioner positioner = new AdvancementPositioner(root, null, null, 1, 0);
		positioner.calculateRecursively();

		float minRow = positioner.findMinRowRecursively(0.0F, 0, positioner.row);
		if (minRow < 0.0F) {
			positioner.increaseRowRecursively(-minRow);
		}

		positioner.apply();
	}
}
