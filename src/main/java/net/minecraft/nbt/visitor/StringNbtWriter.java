package net.minecraft.nbt.visitor;

import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Сериализует дерево NBT-элементов в компактную SNBT-строку (Stringified NBT).
 * <p>
 * Формат вывода соответствует спецификации SNBT: числа с суффиксами типов (b, s, L, f, d),
 * массивы с префиксами ([B;, [I;, [L;), строки в кавычках при необходимости.
 * Используется для отладки, сохранения в текстовом виде и передачи по сети.
 */
public class StringNbtWriter implements NbtElementVisitor {

	private static final Pattern QUOTATION_UNNECESSARY_PATTERN = Pattern.compile("[A-Za-z._]+[A-Za-z0-9._+-]*");
	private final StringBuilder result = new StringBuilder();

	public String getString() {
		return result.toString();
	}

	@Override
	public void visitString(NbtString element) {
		result.append(NbtString.escape(element.value()));
	}

	@Override
	public void visitByte(NbtByte element) {
		result.append(element.value()).append('b');
	}

	@Override
	public void visitShort(NbtShort element) {
		result.append(element.value()).append('s');
	}

	@Override
	public void visitInt(NbtInt element) {
		result.append(element.value());
	}

	@Override
	public void visitLong(NbtLong element) {
		result.append(element.value()).append('L');
	}

	@Override
	public void visitFloat(NbtFloat element) {
		result.append(element.value()).append('f');
	}

	@Override
	public void visitDouble(NbtDouble element) {
		result.append(element.value()).append('d');
	}

	@Override
	public void visitByteArray(NbtByteArray element) {
		result.append("[B;");
		byte[] bytes = element.getByteArray();

		for (int index = 0; index < bytes.length; index++) {
			if (index != 0) {
				result.append(',');
			}

			result.append(bytes[index]).append('B');
		}

		result.append(']');
	}

	@Override
	public void visitIntArray(NbtIntArray element) {
		result.append("[I;");
		int[] ints = element.getIntArray();

		for (int index = 0; index < ints.length; index++) {
			if (index != 0) {
				result.append(',');
			}

			result.append(ints[index]);
		}

		result.append(']');
	}

	@Override
	public void visitLongArray(NbtLongArray element) {
		result.append("[L;");
		long[] longs = element.getLongArray();

		for (int index = 0; index < longs.length; index++) {
			if (index != 0) {
				result.append(',');
			}

			result.append(longs[index]).append('L');
		}

		result.append(']');
	}

	@Override
	public void visitList(NbtList element) {
		result.append('[');

		for (int index = 0; index < element.size(); index++) {
			if (index != 0) {
				result.append(',');
			}

			element.get(index).accept(this);
		}

		result.append(']');
	}

	@Override
	public void visitCompound(NbtCompound compound) {
		result.append('{');
		List<Entry<String, NbtElement>> entries = new ArrayList<>(compound.entrySet());
		entries.sort(Entry.comparingByKey());

		for (int index = 0; index < entries.size(); index++) {
			Entry<String, NbtElement> entry = entries.get(index);
			if (index != 0) {
				result.append(',');
			}

			appendKey(entry.getKey());
			result.append(':');
			entry.getValue().accept(this);
		}

		result.append('}');
	}

	/**
	 * Добавляет ключ в результат, экранируя его при необходимости.
	 * Ключи, совпадающие с булевыми литералами или содержащие спецсимволы, берутся в кавычки.
	 */
	private void appendKey(String key) {
		if (!key.equalsIgnoreCase("true")
			&& !key.equalsIgnoreCase("false")
			&& QUOTATION_UNNECESSARY_PATTERN.matcher(key).matches()
		) {
			result.append(key);
		}
		else {
			NbtString.appendEscaped(key, result);
		}
	}

	@Override
	public void visitEnd(NbtEnd element) {
		result.append("END");
	}
}
