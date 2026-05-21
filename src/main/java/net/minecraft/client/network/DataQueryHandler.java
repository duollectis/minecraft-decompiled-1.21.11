package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.QueryBlockNbtC2SPacket;
import net.minecraft.network.packet.c2s.play.QueryEntityNbtC2SPacket;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Управляет запросами NBT-данных блоков и сущностей с сервера.
 * <p>Отправляет запросы с уникальным идентификатором транзакции и сопоставляет
 * ответы сервера с соответствующими колбэками. Поддерживает только один
 * активный запрос одновременно.
 */
@Environment(EnvType.CLIENT)
public class DataQueryHandler {

	private static final int NO_PENDING_TRANSACTION = -1;

	private final ClientPlayNetworkHandler networkHandler;
	private int expectedTransactionId = NO_PENDING_TRANSACTION;
	private @Nullable Consumer<NbtCompound> callback;

	/**
	 * Создаёт обработчик запросов данных.
	 *
	 * @param networkHandler сетевой обработчик для отправки пакетов
	 */
	public DataQueryHandler(ClientPlayNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	/**
	 * Обрабатывает ответ сервера на запрос NBT-данных.
	 *
	 * @param transactionId идентификатор транзакции из ответа сервера
	 * @param nbt           NBT-данные или {@code null} если объект не найден
	 * @return {@code true} если ответ соответствует ожидаемой транзакции
	 */
	public boolean handleQueryResponse(int transactionId, @Nullable NbtCompound nbt) {
		if (expectedTransactionId != transactionId || callback == null) {
			return false;
		}

		callback.accept(nbt);
		callback = null;
		return true;
	}

	/**
	 * Запрашивает NBT-данные сущности по её сетевому идентификатору.
	 *
	 * @param entityNetworkId сетевой идентификатор сущности
	 * @param callback        колбэк, вызываемый с полученными данными
	 */
	public void queryEntityNbt(int entityNetworkId, Consumer<NbtCompound> callback) {
		int transactionId = nextQuery(callback);
		networkHandler.sendPacket(new QueryEntityNbtC2SPacket(transactionId, entityNetworkId));
	}

	/**
	 * Запрашивает NBT-данные блока по его позиции.
	 *
	 * @param pos      позиция блока в мире
	 * @param callback колбэк, вызываемый с полученными данными
	 */
	public void queryBlockNbt(BlockPos pos, Consumer<NbtCompound> callback) {
		int transactionId = nextQuery(callback);
		networkHandler.sendPacket(new QueryBlockNbtC2SPacket(transactionId, pos));
	}

	private int nextQuery(Consumer<NbtCompound> callback) {
		this.callback = callback;
		return ++expectedTransactionId;
	}
}
