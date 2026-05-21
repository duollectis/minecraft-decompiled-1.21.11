package net.minecraft.client.network;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Управляет ожидающими обновлениями блоков на стороне клиента.
 * <p>Когда игрок взаимодействует с блоком, клиент оптимистично применяет изменение
 * и сохраняет его здесь с порядковым номером. Когда сервер подтверждает транзакцию,
 * обновления с номером ≤ подтверждённому применяются к миру через
 * {@link #processPendingUpdates}.
 * <p>Реализует {@link AutoCloseable} для использования в try-with-resources:
 * закрытие сбрасывает флаг ожидающей последовательности.
 */
@Environment(EnvType.CLIENT)
public class PendingUpdateManager implements AutoCloseable {

	private final Long2ObjectOpenHashMap<PendingUpdate> blockPosToPendingUpdate = new Long2ObjectOpenHashMap<>();
	private int sequence;
	private boolean pendingSequence;

	/**
	 * Добавляет ожидающее обновление блока с текущим порядковым номером.
	 * Если обновление для этой позиции уже существует — обновляет его номер.
	 *
	 * @param pos    позиция блока
	 * @param state  новое состояние блока
	 * @param player игрок, инициировавший изменение
	 */
	public void addPendingUpdate(BlockPos pos, BlockState state, ClientPlayerEntity player) {
		blockPosToPendingUpdate.compute(
				pos.asLong(),
				(posLong, existing) -> existing != null
				                       ? existing.withSequence(sequence)
				                       : new PendingUpdate(sequence, state, player.getEntityPos())
		);
	}

	/**
	 * Проверяет наличие ожидающего обновления для позиции и обновляет его состояние.
	 *
	 * @param pos   позиция блока
	 * @param state новое состояние блока от сервера
	 * @return {@code true} если ожидающее обновление найдено
	 */
	public boolean hasPendingUpdate(BlockPos pos, BlockState state) {
		PendingUpdate update = blockPosToPendingUpdate.get(pos.asLong());

		if (update == null) {
			return false;
		}

		update.setBlockState(state);
		return true;
	}

	/**
	 * Применяет все ожидающие обновления с порядковым номером ≤ {@code maxProcessableSequence}.
	 *
	 * @param maxProcessableSequence максимальный подтверждённый порядковый номер
	 * @param world                  клиентский мир для применения обновлений
	 */
	public void processPendingUpdates(int maxProcessableSequence, ClientWorld world) {
		ObjectIterator<Long2ObjectMap.Entry<PendingUpdate>> iterator =
				blockPosToPendingUpdate.long2ObjectEntrySet().iterator();

		while (iterator.hasNext()) {
			Long2ObjectMap.Entry<PendingUpdate> entry = iterator.next();
			PendingUpdate update = entry.getValue();

			if (update.sequence <= maxProcessableSequence) {
				BlockPos pos = BlockPos.fromLong(entry.getLongKey());
				iterator.remove();
				world.processPendingUpdate(pos, update.blockState, update.playerPos);
			}
		}
	}

	/**
	 * Увеличивает порядковый номер и помечает наличие ожидающей последовательности.
	 * Предназначен для использования в try-with-resources.
	 *
	 * @return {@code this} для цепочки вызовов
	 */
	public PendingUpdateManager incrementSequence() {
		sequence++;
		pendingSequence = true;
		return this;
	}

	/**
	 * Сбрасывает флаг ожидающей последовательности.
	 */
	@Override
	public void close() {
		pendingSequence = false;
	}

	/**
	 * Возвращает текущий порядковый номер транзакции.
	 *
	 * @return порядковый номер
	 */
	public int getSequence() {
		return sequence;
	}

	/**
	 * Проверяет, есть ли активная ожидающая последовательность.
	 *
	 * @return {@code true} если последовательность ожидает подтверждения
	 */
	public boolean hasPendingSequence() {
		return pendingSequence;
	}

	/**
	 * Запись об ожидающем обновлении блока.
	 */
	@Environment(EnvType.CLIENT)
	static class PendingUpdate {

		final Vec3d playerPos;
		int sequence;
		BlockState blockState;

		PendingUpdate(int sequence, BlockState blockState, Vec3d playerPos) {
			this.sequence = sequence;
			this.blockState = blockState;
			this.playerPos = playerPos;
		}

		PendingUpdate withSequence(int sequence) {
			this.sequence = sequence;
			return this;
		}

		void setBlockState(BlockState state) {
			blockState = state;
		}
	}
}
