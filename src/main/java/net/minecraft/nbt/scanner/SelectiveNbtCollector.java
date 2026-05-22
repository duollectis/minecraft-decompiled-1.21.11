package net.minecraft.nbt.scanner;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Расширение {@link NbtCollector}, которое читает только запрошенные поля.
 * <p>
 * Принимает набор {@link NbtScanQuery} и при сканировании пропускает все поля,
 * не входящие в запросы. Останавливается досрочно, когда все запросы выполнены
 * ({@link #getQueriesLeft()} == 0).
 * <p>
 * Используется для эффективного чтения отдельных полей из больших NBT-структур
 * без полной десериализации.
 */
public class SelectiveNbtCollector extends NbtCollector {

	private int queriesLeft;
	private final Set<NbtType<?>> allPossibleTypes;
	private final Deque<NbtTreeNode> selectionStack = new ArrayDeque<>();

	public SelectiveNbtCollector(NbtScanQuery... queries) {
		queriesLeft = queries.length;
		Builder<NbtType<?>> builder = ImmutableSet.builder();
		NbtTreeNode rootNode = NbtTreeNode.createRoot();

		for (NbtScanQuery query : queries) {
			rootNode.add(query);
			builder.add(query.type());
		}

		selectionStack.push(rootNode);
		builder.add(NbtCompound.TYPE);
		allPossibleTypes = builder.build();
	}

	@Override
	public NbtScanner.Result start(NbtType<?> rootType) {
		return rootType != NbtCompound.TYPE ? NbtScanner.Result.HALT : super.start(rootType);
	}

	@Override
	public NbtScanner.NestedResult visitSubNbtType(NbtType<?> type) {
		NbtTreeNode currentNode = selectionStack.element();

		if (getDepth() > currentNode.depth()) {
			return super.visitSubNbtType(type);
		}

		if (queriesLeft <= 0) {
			return NbtScanner.NestedResult.BREAK;
		}

		return allPossibleTypes.contains(type) ? super.visitSubNbtType(type) : NbtScanner.NestedResult.SKIP;
	}

	@Override
	public NbtScanner.NestedResult startSubNbt(NbtType<?> type, String key) {
		NbtTreeNode currentNode = selectionStack.element();

		if (getDepth() > currentNode.depth()) {
			return super.startSubNbt(type, key);
		}

		if (currentNode.selectedFields().remove(key, type)) {
			queriesLeft--;
			return super.startSubNbt(type, key);
		}

		if (type == NbtCompound.TYPE) {
			NbtTreeNode childNode = currentNode.fieldsToRecurse().get(key);
			if (childNode != null) {
				selectionStack.push(childNode);
				return super.startSubNbt(type, key);
			}
		}

		return NbtScanner.NestedResult.SKIP;
	}

	@Override
	public NbtScanner.Result endNested() {
		if (getDepth() == selectionStack.element().depth()) {
			selectionStack.pop();
		}

		return super.endNested();
	}

	/** Возвращает количество ещё не выполненных запросов. */
	public int getQueriesLeft() {
		return queriesLeft;
	}
}
