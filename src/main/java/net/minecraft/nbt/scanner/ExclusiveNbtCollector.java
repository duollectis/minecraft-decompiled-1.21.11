package net.minecraft.nbt.scanner;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtType;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Коллектор NBT-данных, исключающий заданные поля из результата.
 * <p>
 * Работает инверсно по отношению к {@link SelectiveNbtCollector}: собирает все поля,
 * кроме тех, что явно указаны в списке исключений через {@link NbtScanQuery}.
 * Использует дерево {@link NbtTreeNode} для отслеживания текущего пути обхода
 * и принятия решения о пропуске конкретных полей.
 */
public class ExclusiveNbtCollector extends NbtCollector {

	private final Deque<NbtTreeNode> treeStack = new ArrayDeque<>();

	public ExclusiveNbtCollector(NbtScanQuery... excludedQueries) {
		NbtTreeNode rootNode = NbtTreeNode.createRoot();

		for (NbtScanQuery query : excludedQueries) {
			rootNode.add(query);
		}

		treeStack.push(rootNode);
	}

	@Override
	public NbtScanner.NestedResult startSubNbt(NbtType<?> type, String key) {
		NbtTreeNode currentNode = treeStack.element();

		if (currentNode.isTypeEqual(type, key)) {
			return NbtScanner.NestedResult.SKIP;
		}

		if (type == NbtCompound.TYPE) {
			NbtTreeNode childNode = currentNode.fieldsToRecurse().get(key);
			if (childNode != null) {
				treeStack.push(childNode);
			}
		}

		return super.startSubNbt(type, key);
	}

	@Override
	public NbtScanner.Result endNested() {
		if (getDepth() == treeStack.element().depth()) {
			treeStack.pop();
		}

		return super.endNested();
	}
}
