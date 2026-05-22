package net.minecraft.world.tick;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.Hash.Strategy;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Сериализуемый тик с относительной задержкой.
 * Хранится в NBT чанка и восстанавливается при загрузке.
 *
 * @param <T>      тип объекта (блок, жидкость и т.д.)
 * @param type     объект, которому нужно выполнить тик
 * @param pos      позиция блока
 * @param delay    задержка в тиках относительно момента сохранения
 * @param priority приоритет выполнения
 */
public record Tick<T>(T type, BlockPos pos, int delay, TickPriority priority) {

	/**
	 * Стратегия хэширования по паре (тип, позиция) для {@code ObjectOpenCustomHashSet}.
	 * Позволяет проверять наличие тика без учёта задержки и приоритета.
	 */
	public static final Strategy<Tick<?>> HASH_STRATEGY = new Strategy<>() {
		@Override
		public int hashCode(Tick<?> tick) {
			return 31 * tick.pos().hashCode() + tick.type().hashCode();
		}

		@Override
		public boolean equals(@Nullable Tick<?> a, @Nullable Tick<?> b) {
			if (a == b) {
				return true;
			}

			return a != null && b != null
				? a.type() == b.type() && a.pos().equals(b.pos())
				: false;
		}
	};

	/**
	 * Создаёт codec для сериализации тиков в NBT.
	 * Позиция кодируется как три отдельных поля x/y/z для совместимости с форматом Minecraft.
	 *
	 * @param typeCodec codec для типа объекта
	 */
	public static <T> Codec<Tick<T>> createCodec(Codec<T> typeCodec) {
		MapCodec<BlockPos> posCodec = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codec.INT.fieldOf("x").forGetter(Vec3i::getX),
				Codec.INT.fieldOf("y").forGetter(Vec3i::getY),
				Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
			).apply(instance, BlockPos::new)
		);

		return RecordCodecBuilder.create(
			instance -> instance.group(
				typeCodec.fieldOf("i").forGetter(Tick::type),
				posCodec.forGetter(Tick::pos),
				Codec.INT.fieldOf("t").forGetter(Tick::delay),
				TickPriority.CODEC.fieldOf("p").forGetter(Tick::priority)
			).apply(instance, Tick::new)
		);
	}

	/**
	 * Фильтрует список тиков, оставляя только те, что принадлежат заданному чанку.
	 *
	 * @param ticks    исходный список тиков
	 * @param chunkPos позиция чанка
	 */
	public static <T> List<Tick<T>> filter(List<Tick<T>> ticks, ChunkPos chunkPos) {
		long chunkKey = chunkPos.toLong();
		return ticks.stream()
			.filter(tick -> ChunkPos.toLong(tick.pos()) == chunkKey)
			.toList();
	}

	/**
	 * Конвертирует в {@link OrderedTick} с абсолютным временем срабатывания.
	 *
	 * @param currentTime  текущее игровое время в тиках
	 * @param subTickOrder порядковый номер для детерминированной сортировки
	 */
	public OrderedTick<T> createOrderedTick(long currentTime, long subTickOrder) {
		return new OrderedTick<>(type, pos, currentTime + delay, priority, subTickOrder);
	}

	/**
	 * Создаёт «ключевой» тик с нулевой задержкой и нормальным приоритетом.
	 * Используется исключительно для поиска в хэш-наборе по паре (тип, позиция).
	 *
	 * @param type тип объекта
	 * @param pos  позиция блока
	 */
	public static <T> Tick<T> create(T type, BlockPos pos) {
		return new Tick<>(type, pos, 0, TickPriority.NORMAL);
	}
}
