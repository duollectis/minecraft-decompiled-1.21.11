package net.minecraft.nbt.visitor;

import net.minecraft.nbt.*;

/**
 * Интерфейс посетителя для обхода дерева NBT-элементов.
 * <p>
 * Реализует паттерн Visitor (GoF): каждый конкретный тип {@link net.minecraft.nbt.NbtElement}
 * вызывает соответствующий метод {@code visit*}, позволяя внешней логике обрабатывать
 * каждый тип без instanceof-проверок. Используется для сериализации в строку,
 * форматированного вывода и других операций обхода.
 */
public interface NbtElementVisitor {

	void visitString(NbtString element);

	void visitByte(NbtByte element);

	void visitShort(NbtShort element);

	void visitInt(NbtInt element);

	void visitLong(NbtLong element);

	void visitFloat(NbtFloat element);

	void visitDouble(NbtDouble element);

	void visitByteArray(NbtByteArray element);

	void visitIntArray(NbtIntArray element);

	void visitLongArray(NbtLongArray element);

	void visitList(NbtList element);

	void visitCompound(NbtCompound compound);

	void visitEnd(NbtEnd element);
}
