package net.minecraft.entity.ai.pathing;

import java.util.Arrays;

/**
 * Минимальная двоичная куча для узлов пути алгоритма A*.
 * Обеспечивает O(log n) вставку и извлечение узла с минимальным весом.
 */
public class PathMinHeap {

	private static final int INITIAL_CAPACITY = 128;

	private PathNode[] pathNodes = new PathNode[INITIAL_CAPACITY];
	private int count;

	/**
	 * Добавляет узел в кучу. Выбрасывает исключение, если узел уже находится в куче.
	 */
	public PathNode push(PathNode node) {
		if (node.heapIndex >= 0) {
			throw new IllegalStateException("OW KNOWS!");
		}

		if (count == pathNodes.length) {
			PathNode[] expanded = new PathNode[count << 1];
			System.arraycopy(pathNodes, 0, expanded, 0, count);
			pathNodes = expanded;
		}

		pathNodes[count] = node;
		node.heapIndex = count;
		shiftUp(count++);
		return node;
	}

	public void clear() {
		count = 0;
	}

	public PathNode getStart() {
		return pathNodes[0];
	}

	/** Извлекает и возвращает узел с минимальным весом. */
	public PathNode pop() {
		PathNode root = pathNodes[0];
		pathNodes[0] = pathNodes[--count];
		pathNodes[count] = null;

		if (count > 0) {
			shiftDown(0);
		}

		root.heapIndex = -1;
		return root;
	}

	/** Удаляет произвольный узел из кучи, восстанавливая свойство кучи. */
	public void popNode(PathNode node) {
		pathNodes[node.heapIndex] = pathNodes[--count];
		pathNodes[count] = null;

		if (count > node.heapIndex) {
			if (pathNodes[node.heapIndex].heapWeight < node.heapWeight) {
				shiftUp(node.heapIndex);
			} else {
				shiftDown(node.heapIndex);
			}
		}

		node.heapIndex = -1;
	}

	public void setNodeWeight(PathNode node, float weight) {
		float oldWeight = node.heapWeight;
		node.heapWeight = weight;

		if (weight < oldWeight) {
			shiftUp(node.heapIndex);
		} else {
			shiftDown(node.heapIndex);
		}
	}

	public int getCount() {
		return count;
	}

	private void shiftUp(int index) {
		PathNode node = pathNodes[index];
		float weight = node.heapWeight;

		while (index > 0) {
			int parentIndex = (index - 1) >> 1;
			PathNode parent = pathNodes[parentIndex];

			if (weight >= parent.heapWeight) {
				break;
			}

			pathNodes[index] = parent;
			parent.heapIndex = index;
			index = parentIndex;
		}

		pathNodes[index] = node;
		node.heapIndex = index;
	}

	private void shiftDown(int index) {
		PathNode node = pathNodes[index];
		float weight = node.heapWeight;

		while (true) {
			int leftChild = 1 + (index << 1);
			int rightChild = leftChild + 1;

			if (leftChild >= count) {
				break;
			}

			PathNode left = pathNodes[leftChild];
			float leftWeight = left.heapWeight;

			PathNode right;
			float rightWeight;

			if (rightChild >= count) {
				right = null;
				rightWeight = Float.POSITIVE_INFINITY;
			} else {
				right = pathNodes[rightChild];
				rightWeight = right.heapWeight;
			}

			if (leftWeight < rightWeight) {
				if (leftWeight >= weight) {
					break;
				}

				pathNodes[index] = left;
				left.heapIndex = index;
				index = leftChild;
			} else {
				if (rightWeight >= weight) {
					break;
				}

				pathNodes[index] = right;
				right.heapIndex = index;
				index = rightChild;
			}
		}

		pathNodes[index] = node;
		node.heapIndex = index;
	}

	public boolean isEmpty() {
		return count == 0;
	}

	public PathNode[] getNodes() {
		return Arrays.copyOf(pathNodes, count);
	}
}
