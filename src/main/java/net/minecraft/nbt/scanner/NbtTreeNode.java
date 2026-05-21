package net.minecraft.nbt.scanner;

import net.minecraft.nbt.NbtType;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code NbtTreeNode}.
 */
public record NbtTreeNode(int depth, Map<String, NbtType<?>> selectedFields, Map<String, NbtTreeNode> fieldsToRecurse) {

	private NbtTreeNode(int depth) {
		this(depth, new HashMap<>(), new HashMap<>());
	}

	/**
	 * Создаёт root.
	 *
	 * @return NbtTreeNode — результат операции
	 */
	public static NbtTreeNode createRoot() {
		return new NbtTreeNode(1);
	}

	/**
	 * Add.
	 *
	 * @param query query
	 */
	public void add(NbtScanQuery query) {
		if (this.depth <= query.path().size()) {
			this.fieldsToRecurse
					.computeIfAbsent(query.path().get(this.depth - 1), path -> new NbtTreeNode(this.depth + 1))
					.add(query);
		}
		else {
			this.selectedFields.put(query.key(), query.type());
		}
	}

	public boolean isTypeEqual(NbtType<?> type, String key) {
		return type.equals(this.selectedFields().get(key));
	}
}
