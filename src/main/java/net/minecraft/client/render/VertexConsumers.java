package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code VertexConsumers}.
 */
public class VertexConsumers {

	/**
	 * Union.
	 *
	 * @return VertexConsumer — результат операции
	 */
	public static VertexConsumer union() {
		throw new IllegalArgumentException();
	}

	/**
	 * Union.
	 *
	 * @param first first
	 *
	 * @return VertexConsumer — результат операции
	 */
	public static VertexConsumer union(VertexConsumer first) {
		return first;
	}

	/**
	 * Union.
	 *
	 * @param first first
	 * @param second second
	 *
	 * @return VertexConsumer — результат операции
	 */
	public static VertexConsumer union(VertexConsumer first, VertexConsumer second) {
		return new VertexConsumers.Dual(first, second);
	}

	/**
	 * Union.
	 *
	 * @param delegates delegates
	 *
	 * @return VertexConsumer — результат операции
	 */
	public static VertexConsumer union(VertexConsumer... delegates) {
		return new VertexConsumers.Union(delegates);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Dual}.
	 */
	static class Dual implements VertexConsumer {

		private final VertexConsumer first;
		private final VertexConsumer second;

		public Dual(VertexConsumer first, VertexConsumer second) {
			if (first == second) {
				throw new IllegalArgumentException("Duplicate delegates");
			}
			else {
				this.first = first;
				this.second = second;
			}
		}

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			this.first.vertex(x, y, z);
			this.second.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			this.first.color(red, green, blue, alpha);
			this.second.color(red, green, blue, alpha);
			return this;
		}

		@Override
		public VertexConsumer color(int argb) {
			this.first.color(argb);
			this.second.color(argb);
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			this.first.texture(u, v);
			this.second.texture(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			this.first.overlay(u, v);
			this.second.overlay(u, v);
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			this.first.light(u, v);
			this.second.light(u, v);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			this.first.normal(x, y, z);
			this.second.normal(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer lineWidth(float width) {
			this.first.lineWidth(width);
			this.second.lineWidth(width);
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
			this.first.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
			this.second.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Union}.
	 */
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
			for (VertexConsumer vertexConsumer : this.delegates) {
				action.accept(vertexConsumer);
			}
		}

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			this.delegate(vertexConsumer -> vertexConsumer.vertex(x, y, z));
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			this.delegate(vertexConsumer -> vertexConsumer.color(red, green, blue, alpha));
			return this;
		}

		@Override
		public VertexConsumer color(int argb) {
			this.delegate(vertexConsumer -> vertexConsumer.color(argb));
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			this.delegate(vertexConsumer -> vertexConsumer.texture(u, v));
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			this.delegate(vertexConsumer -> vertexConsumer.overlay(u, v));
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			this.delegate(vertexConsumer -> vertexConsumer.light(u, v));
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			this.delegate(vertexConsumer -> vertexConsumer.normal(x, y, z));
			return this;
		}

		@Override
		public VertexConsumer lineWidth(float width) {
			this.delegate(vertexConsumer -> vertexConsumer.lineWidth(width));
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
			this.delegate(vertexConsumer -> vertexConsumer.vertex(
					x,
					y,
					z,
					color,
					u,
					v,
					overlay,
					light,
					normalX,
					normalY,
					normalZ
			));
		}
	}
}
