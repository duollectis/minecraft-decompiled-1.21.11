package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Consumer;

/**
 * Фабричные методы для создания составных {@link VertexConsumer}, перенаправляющих
 * вершинные данные одновременно нескольким получателям.
 * Используется для одновременной записи в основной буфер и буфер контуров.
 */
@Environment(EnvType.CLIENT)
public class VertexConsumers {

	/** Бросает исключение — вызов без аргументов не имеет смысла. */
	public static VertexConsumer union() {
		throw new IllegalArgumentException();
	}

	/** Возвращает единственный потребитель без обёртки. */
	public static VertexConsumer union(VertexConsumer first) {
		return first;
	}

	/** Создаёт оптимизированный двойной потребитель для двух получателей. */
	public static VertexConsumer union(VertexConsumer first, VertexConsumer second) {
		return new VertexConsumers.Dual(first, second);
	}

	/** Создаёт потребитель, транслирующий данные произвольному числу получателей. */
	public static VertexConsumer union(VertexConsumer... delegates) {
		return new VertexConsumers.Union(delegates);
	}

	/** Оптимизированная реализация для ровно двух получателей без накладных расходов массива. */
	@Environment(EnvType.CLIENT)
	static class Dual implements VertexConsumer {

		private final VertexConsumer first;
		private final VertexConsumer second;

		public Dual(VertexConsumer first, VertexConsumer second) {
			if (first == second) {
				throw new IllegalArgumentException("Duplicate delegates");
			}

			this.first = first;
			this.second = second;
		}

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			first.vertex(x, y, z);
			second.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			first.color(red, green, blue, alpha);
			second.color(red, green, blue, alpha);
			return this;
		}

		@Override
		public VertexConsumer color(int argb) {
			first.color(argb);
			second.color(argb);
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			first.texture(u, v);
			second.texture(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			first.overlay(u, v);
			second.overlay(u, v);
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			first.light(u, v);
			second.light(u, v);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			first.normal(x, y, z);
			second.normal(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer lineWidth(float width) {
			first.lineWidth(width);
			second.lineWidth(width);
			return this;
		}

		@Override
		public void vertex(
				float x,
				float y,
				float z,
				int color,
				float u,
				float v,
				int overlay,
				int light,
				float normalX,
				float normalY,
				float normalZ
		) {
			first.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
			second.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
		}
	}

	/** Реализация для произвольного числа получателей через массив делегатов. */
	@Environment(EnvType.CLIENT)
	record Union(VertexConsumer[] delegates) implements VertexConsumer {

		Union(VertexConsumer[] delegates) {
			for (int i = 0; i < delegates.length; i++) {
				for (int j = i + 1; j < delegates.length; j++) {
					if (delegates[i] == delegates[j]) {
						throw new IllegalArgumentException("Duplicate delegates");
					}
				}
			}

			this.delegates = delegates;
		}

		private void delegate(Consumer<VertexConsumer> action) {
			for (VertexConsumer consumer : delegates) {
				action.accept(consumer);
			}
		}

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			delegate(consumer -> consumer.vertex(x, y, z));
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			delegate(consumer -> consumer.color(red, green, blue, alpha));
			return this;
		}

		@Override
		public VertexConsumer color(int argb) {
			delegate(consumer -> consumer.color(argb));
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			delegate(consumer -> consumer.texture(u, v));
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			delegate(consumer -> consumer.overlay(u, v));
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			delegate(consumer -> consumer.light(u, v));
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			delegate(consumer -> consumer.normal(x, y, z));
			return this;
		}

		@Override
		public VertexConsumer lineWidth(float width) {
			delegate(consumer -> consumer.lineWidth(width));
			return this;
		}

		@Override
		public void vertex(
				float x,
				float y,
				float z,
				int color,
				float u,
				float v,
				int overlay,
				int light,
				float normalX,
				float normalY,
				float normalZ
		) {
			delegate(consumer -> consumer.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ));
		}
	}
}
