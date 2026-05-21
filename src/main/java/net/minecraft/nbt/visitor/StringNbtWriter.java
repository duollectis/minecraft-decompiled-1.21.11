package net.minecraft.nbt.visitor;

import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * {@code StringNbtWriter}.
 */
public class StringNbtWriter implements NbtElementVisitor {

	private static final Pattern QUOTATION_UNNECESSARY_PATTERN = Pattern.compile("[A-Za-z._]+[A-Za-z0-9._+-]*");
	private final StringBuilder result = new StringBuilder();

	public String getString() {
		return this.result.toString();
	}

	@Override
	public void visitString(NbtString element) {
		this.result.append(NbtString.escape(element.value()));
	}

	@Override
	public void visitByte(NbtByte element) {
		this.result.append(element.value()).append('b');
	}

	@Override
	public void visitShort(NbtShort element) {
		this.result.append(element.value()).append('s');
	}

	@Override
	public void visitInt(NbtInt element) {
		this.result.append(element.value());
	}

	@Override
	public void visitLong(NbtLong element) {
		this.result.append(element.value()).append('L');
	}

	@Override
	public void visitFloat(NbtFloat element) {
		this.result.append(element.value()).append('f');
	}

	@Override
	public void visitDouble(NbtDouble element) {
		this.result.append(element.value()).append('d');
	}

	@Override
	public void visitByteArray(NbtByteArray element) {
		this.result.append("[B;");
		byte[] bs = element.getByteArray();

		for (int i = 0; i < bs.length; i++) {
			if (i != 0) {
				this.result.append(',');
			}

			this.result.append(bs[i]).append('B');
		}

		this.result.append(']');
	}

	@Override
	public void visitIntArray(NbtIntArray element) {
		this.result.append("[I;");
		int[] is = element.getIntArray();

		for (int i = 0; i < is.length; i++) {
			if (i != 0) {
				this.result.append(',');
			}

			this.result.append(is[i]);
		}

		this.result.append(']');
	}

	@Override
	public void visitLongArray(NbtLongArray element) {
		this.result.append("[L;");
		long[] ls = element.getLongArray();

		for (int i = 0; i < ls.length; i++) {
			if (i != 0) {
				this.result.append(',');
			}

			this.result.append(ls[i]).append('L');
		}

		this.result.append(']');
	}

	@Override
	public void visitList(NbtList element) {
		this.result.append('[');

		for (int i = 0; i < element.size(); i++) {
			if (i != 0) {
				this.result.append(',');
			}

			element.get(i).accept(this);
		}

		this.result.append(']');
	}

	@Override
	public void visitCompound(NbtCompound compound) {
		this.result.append('{');
		List<Entry<String, NbtElement>> list = new ArrayList<>(compound.entrySet());
		list.sort(Entry.comparingByKey());

		for (int i = 0; i < list.size(); i++) {
			Entry<String, NbtElement> entry = list.get(i);
			if (i != 0) {
				this.result.append(',');
			}

			this.append(entry.getKey());
			this.result.append(':');
			entry.getValue().accept(this);
		}

		this.result.append('}');
	}

	private void append(String string) {
		if (!string.equalsIgnoreCase("true") && !string.equalsIgnoreCase("false") && QUOTATION_UNNECESSARY_PATTERN
				.matcher(string)
				.matches()) {
			this.result.append(string);
		}
		else {
			NbtString.appendEscaped(string, this.result);
		}
	}

	@Override
	public void visitEnd(NbtEnd element) {
		this.result.append("END");
	}
}
