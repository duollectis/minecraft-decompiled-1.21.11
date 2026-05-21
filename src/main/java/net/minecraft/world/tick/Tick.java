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
 * {@code Tick}.
 */
public record Tick<T>(T type, BlockPos pos, int delay, TickPriority priority) {

	public static final Strategy<Tick<?>> HASH_STRATEGY = new Strategy<Tick<?>>() {
		/**
		 * Проверяет наличие h code.
		 *
		 * @param tick tick
		 *
		 * @return int — {@code true} если условие выполнено
		 */
		public int hashCode(Tick<?> tick) {
			return 31 * tick.pos().hashCode() + tick.type().hashCode();
		}

		/**
		 * Equals.
		 *
		 * @param tick tick
		 * @param tick2 tick2
		 *
		 * @return boolean — результат операции
		 */
		public boolean equals(@Nullable Tick<?> tick, @Nullable Tick<?> tick2) {
			if (tick == tick2) {
				return true;
			}
			else {
				return tick != null && tick2 != null ? tick.type() == tick2.type() && tick.pos().equals(tick2.pos())
				                                     : false;
			}
		}
	};

	/**
	 * Создаёт codec.
	 *
	 * @param typeCodec type codec
	 *
	 * @return Codec> — результат операции
	 */
	public static <T> Codec<Tick<T>> createCodec(Codec<T> typeCodec) {
		MapCodec<BlockPos> mapCodec = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Codec.INT.fieldOf("x").forGetter(Vec3i::getX),
						                    Codec.INT.fieldOf("y").forGetter(Vec3i::getY),
						                    Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
				                    )
				                    .apply(instance, BlockPos::new)
		);
		return RecordCodecBuilder.create(
				instance -> instance.group(
						                    typeCodec.fieldOf("i").forGetter(Tick::type),
						                    mapCodec.forGetter(Tick::pos),
						                    Codec.INT.fieldOf("t").forGetter(Tick::delay),
						                    TickPriority.CODEC.fieldOf("p").forGetter(Tick::priority)
				                    )
				                    .apply(instance, Tick::new)
		);
	}

	/**
	 * Filter.
	 *
	 * @param ticks ticks
	 * @param chunkPos chunk pos
	 *
	 * @return List> — результат операции
	 */
	public static <T> List<Tick<T>> filter(List<Tick<T>> ticks, ChunkPos chunkPos) {
		long l = chunkPos.toLong();
		return ticks.stream().filter(tick -> ChunkPos.toLong(tick.pos()) == l).toList();
	}

	/**
	 * Создаёт ordered tick.
	 *
	 * @param time time
	 * @param subTickOrder sub tick order
	 *
	 * @return OrderedTick — результат операции
	 */
	public OrderedTick<T> createOrderedTick(long time, long subTickOrder) {
		return new OrderedTick<>(this.type, this.pos, time + this.delay, this.priority, subTickOrder);
	}

	/**
	 * Create.
	 *
	 * @param type type
	 * @param pos pos
	 *
	 * @return Tick — результат операции
	 */
	public static <T> Tick<T> create(T type, BlockPos pos) {
		return new Tick<>(type, pos, 0, TickPriority.NORMAL);
	}
}
