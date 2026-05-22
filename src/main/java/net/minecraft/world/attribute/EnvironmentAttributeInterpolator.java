package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.WeightedInterpolation;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

/**
 * Интерполятор атрибутов окружения между тиками на стороне клиента.
 * Хранит предыдущее и текущее значения каждого атрибута и плавно
 * переходит между ними в зависимости от прогресса частичного тика.
 * <p>
 * Используется для сглаживания визуальных изменений (цвет неба, туман и т.д.)
 * без рывков при смене биома или времени суток.
 */
public class EnvironmentAttributeInterpolator {

	private final Map<EnvironmentAttribute<?>, Entry<?>> entries = new Reference2ObjectOpenHashMap<>();
	private final Function<EnvironmentAttribute<?>, Entry<?>> entryFactory = Entry::new;

	@Nullable World world;
	@Nullable Vec3d pos;
	final WeightedAttributeList pool = new WeightedAttributeList();

	/** Сбрасывает состояние интерполятора: очищает мир, позицию и все записи. */
	public void clear() {
		world = null;
		pos = null;
		pool.clear();
		entries.clear();
	}

	/**
	 * Обновляет интерполятор для нового тика: пересчитывает взвешенный пул биомов
	 * и инвалидирует устаревшие записи атрибутов.
	 *
	 * @param world текущий мир
	 * @param pos позиция игрока
	 */
	public void update(World world, Vec3d pos) {
		this.world = world;
		this.pos = pos;
		entries.values().removeIf(Entry::update);
		pool.clear();
		WeightedInterpolation.interpolate(
			pos.multiply(0.25),
			world.getBiomeAccess()::getBiomeForNoiseGen,
			(weight, biome) -> pool.add(weight, biome.value().getEnvironmentAttributes())
		);
	}

	/**
	 * Возвращает интерполированное значение атрибута для заданного прогресса тика.
	 * Если запись для атрибута ещё не создана — создаёт её.
	 *
	 * @param attribute запрашиваемый атрибут
	 * @param tickProgress прогресс частичного тика [0.0; 1.0]
	 * @return интерполированное значение
	 */
	@SuppressWarnings("unchecked")
	public <Value> Value get(EnvironmentAttribute<Value> attribute, float tickProgress) {
		Entry<Value> entry = (Entry<Value>) entries.computeIfAbsent(attribute, entryFactory);
		return entry.get(attribute, tickProgress);
	}

	/**
	 * Запись интерполятора для одного атрибута.
	 * Хранит предыдущее значение {@code last} и текущее {@code current}.
	 * При вызове {@link #update()} переводит {@code current} в {@code last}.
	 */
	class Entry<Value> {

		private Value last;
		private @Nullable Value current;

		Entry(EnvironmentAttribute<Value> attribute) {
			Value initial = compute(attribute);
			last = initial;
			current = initial;
		}

		private Value compute(EnvironmentAttribute<Value> attribute) {
			return world != null && pos != null
				? world.getEnvironmentAttributes().getAttributeValue(attribute, pos, pool)
				: attribute.getDefaultValue();
		}

		/**
		 * Переводит текущее значение в предыдущее и сбрасывает кеш.
		 * Возвращает {@code true} если запись устарела и должна быть удалена.
		 */
		public boolean update() {
			if (current == null) {
				return true;
			}

			last = current;
			current = null;
			return false;
		}

		/**
		 * Возвращает интерполированное значение между предыдущим и текущим.
		 * Вычисляет текущее значение лениво при первом обращении.
		 *
		 * @param attribute атрибут для вычисления текущего значения
		 * @param tickProgress прогресс частичного тика [0.0; 1.0]
		 * @return интерполированное значение
		 */
		public Value get(EnvironmentAttribute<Value> attribute, float tickProgress) {
			if (current == null) {
				current = compute(attribute);
			}

			return attribute.getType().partialTickLerp().apply(tickProgress, last, current);
		}
	}
}
