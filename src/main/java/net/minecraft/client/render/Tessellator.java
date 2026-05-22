package net.minecraft.client.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.BufferAllocator;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code Tessellator}.
 */
public class Tessellator {

	private static final int MAX_BUFFER_SIZE = 786432;
	private final BufferAllocator allocator;
	private static @Nullable Tessellator INSTANCE;

	/**
	 * Инициализирует ialize.
	 */
	public static void initialize() {
		if (INSTANCE != null) {
			throw new IllegalStateException("Tesselator has already been initialized");
		}
		else {
			INSTANCE = new Tessellator();
		}
	}

	public static Tessellator getInstance() {
		if (INSTANCE == null) {
			throw new IllegalStateException("Tesselator has not been initialized");
		}
		else {
			return INSTANCE;
		}
	}

	public Tessellator(int bufferCapacity) {
		this.allocator = new BufferAllocator(bufferCapacity);
	}

	public Tessellator() {
		this(MAX_BUFFER_SIZE);
	}

	/**
	 * Begin.
	 *
	 * @param drawMode draw mode
	 * @param format format
	 *
	 * @return BufferBuilder — результат операции
	 */
	public BufferBuilder begin(VertexFormat.DrawMode drawMode, VertexFormat format) {
		return new BufferBuilder(this.allocator, drawMode, format);
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.allocator.clear();
	}
}
