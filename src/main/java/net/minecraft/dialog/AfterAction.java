package net.minecraft.dialog;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Действие, выполняемое после взаимодействия пользователя с диалогом.
 * <p>
 * Определяет, что происходит с диалогом и игрой после нажатия кнопки:
 * закрытие, ожидание ответа сервера или ничего.
 */
public enum AfterAction implements StringIdentifiable {
	CLOSE(0, "close"),
	NONE(1, "none"),
	WAIT_FOR_RESPONSE(2, "wait_for_response");

	public static final IntFunction<AfterAction> INDEX_MAPPER = ValueLists.<AfterAction>createIndexToValueFunction(
		afterAction -> afterAction.index,
		values(),
		ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final StringIdentifiable.EnumCodec<AfterAction> CODEC = StringIdentifiable.createCodec(AfterAction::values);
	public static final PacketCodec<ByteBuf, AfterAction> PACKET_CODEC = PacketCodecs.indexed(
		INDEX_MAPPER,
		afterAction -> afterAction.index
	);

	private final int index;
	private final String id;

	AfterAction(int index, String id) {
		this.index = index;
		this.id = id;
	}

	@Override
	public String asString() {
		return id;
	}

	/**
	 * Проверяет, снимает ли данное действие паузу игры после выполнения.
	 * Диалоги, ставящие игру на паузу, обязаны использовать действие, которое её снимает.
	 *
	 * @return {@code true}, если действие снимает паузу (CLOSE или WAIT_FOR_RESPONSE)
	 */
	public boolean canUnpause() {
		return this == CLOSE || this == WAIT_FOR_RESPONSE;
	}
}
