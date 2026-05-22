package net.minecraft.client.gl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBVertexAttribBinding;
import org.lwjgl.opengl.GLCapabilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Управляет VAO (Vertex Array Objects) и привязкой вершинных буферов.
 * Выбирает между ARB-расширением (прямая привязка буферов) и стандартным
 * подходом через {@code glVertexAttribPointer} в зависимости от возможностей GPU.
 */
@Environment(EnvType.CLIENT)
public abstract class VertexBufferManager {

	private static final int GL_ARRAY_BUFFER = 34962;
	private static final int GL_VENDOR = 7936;
	private static final int GL_VERSION = 7938;

	public static VertexBufferManager create(
		GLCapabilities capabilities,
		DebugLabelManager labeler,
		Set<String> usedCapabilities
	) {
		if (capabilities.GL_ARB_vertex_attrib_binding && GlBackend.allowGlArbVABinding) {
			usedCapabilities.add("GL_ARB_vertex_attrib_binding");
			return new ARBVertexBufferManager(labeler);
		} else {
			return new DefaultVertexBufferManager(labeler);
		}
	}

	/**
	 * Настраивает VAO для указанного формата вершин и привязывает к нему буфер.
	 *
	 * @param format формат вершин, описывающий атрибуты
	 * @param into   GPU-буфер с вершинными данными, или {@code null} для создания пустого VAO
	 */
	public abstract void setupBuffer(VertexFormat format, @Nullable GlGpuBuffer into);

	@Environment(EnvType.CLIENT)
	static class ARBVertexBufferManager extends VertexBufferManager {

		private final Map<VertexFormat, AllocatedBuffer> cache = new HashMap<>();
		private final DebugLabelManager labeler;
		private final boolean applyMesaWorkaround;

		ARBVertexBufferManager(DebugLabelManager labeler) {
			this.labeler = labeler;

			String vendor = GlStateManager._getString(GL_VENDOR);
			if ("Mesa".equals(vendor)) {
				String version = GlStateManager._getString(GL_VERSION);
				applyMesaWorkaround = version.contains("25.0.0")
					|| version.contains("25.0.1")
					|| version.contains("25.0.2");
			} else {
				applyMesaWorkaround = false;
			}
		}

		@Override
		public void setupBuffer(VertexFormat format, @Nullable GlGpuBuffer into) {
			AllocatedBuffer allocated = cache.get(format);

			if (allocated != null) {
				GlStateManager._glBindVertexArray(allocated.glId);

				if (into != null && allocated.buffer != into) {
					if (applyMesaWorkaround && allocated.buffer != null && allocated.buffer.id == into.id) {
						ARBVertexAttribBinding.glBindVertexBuffer(0, 0, 0L, 0);
					}

					ARBVertexAttribBinding.glBindVertexBuffer(0, into.id, 0L, format.getVertexSize());
					allocated.buffer = into;
				}

				return;
			}

			int vaoId = GlStateManager._glGenVertexArrays();
			GlStateManager._glBindVertexArray(vaoId);

			if (into != null) {
				List<VertexFormatElement> elements = format.getElements();

				for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
					VertexFormatElement element = elements.get(elementIndex);
					GlStateManager._enableVertexAttribArray(elementIndex);

					switch (element.usage()) {
						case POSITION, GENERIC, UV -> {
							if (element.type() == VertexFormatElement.Type.FLOAT) {
								ARBVertexAttribBinding.glVertexAttribFormat(
									elementIndex,
									element.count(),
									GlConst.toGl(element.type()),
									false,
									format.getOffset(element)
								);
							} else {
								ARBVertexAttribBinding.glVertexAttribIFormat(
									elementIndex,
									element.count(),
									GlConst.toGl(element.type()),
									format.getOffset(element)
								);
							}
						}
						case NORMAL, COLOR -> ARBVertexAttribBinding.glVertexAttribFormat(
							elementIndex,
							element.count(),
							GlConst.toGl(element.type()),
							true,
							format.getOffset(element)
						);
					}

					ARBVertexAttribBinding.glVertexAttribBinding(elementIndex, 0);
				}

				ARBVertexAttribBinding.glBindVertexBuffer(0, into.id, 0L, format.getVertexSize());
			}

			AllocatedBuffer newAllocated = new AllocatedBuffer(vaoId, format, into);
			labeler.labelAllocatedBuffer(newAllocated);
			cache.put(format, newAllocated);
		}
	}

	@Environment(EnvType.CLIENT)
	static class DefaultVertexBufferManager extends VertexBufferManager {

		private final Map<VertexFormat, AllocatedBuffer> cache = new HashMap<>();
		private final DebugLabelManager labeler;

		DefaultVertexBufferManager(DebugLabelManager labeler) {
			this.labeler = labeler;
		}

		@Override
		public void setupBuffer(VertexFormat format, @Nullable GlGpuBuffer into) {
			AllocatedBuffer allocated = cache.get(format);

			if (allocated == null) {
				int vaoId = GlStateManager._glGenVertexArrays();
				GlStateManager._glBindVertexArray(vaoId);

				if (into != null) {
					GlStateManager._glBindBuffer(GL_ARRAY_BUFFER, into.id);
					setupAttributes(format, true);
				}

				AllocatedBuffer newAllocated = new AllocatedBuffer(vaoId, format, into);
				labeler.labelAllocatedBuffer(newAllocated);
				cache.put(format, newAllocated);

				return;
			}

			GlStateManager._glBindVertexArray(allocated.glId);

			if (into != null && allocated.buffer != into) {
				GlStateManager._glBindBuffer(GL_ARRAY_BUFFER, into.id);
				allocated.buffer = into;
				setupAttributes(format, false);
			}
		}

		private static void setupAttributes(VertexFormat format, boolean vaoIsNew) {
			int vertexSize = format.getVertexSize();
			List<VertexFormatElement> elements = format.getElements();

			for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
				VertexFormatElement element = elements.get(elementIndex);

				if (vaoIsNew) {
					GlStateManager._enableVertexAttribArray(elementIndex);
				}

				switch (element.usage()) {
					case POSITION, GENERIC, UV -> {
						if (element.type() == VertexFormatElement.Type.FLOAT) {
							GlStateManager._vertexAttribPointer(
								elementIndex,
								element.count(),
								GlConst.toGl(element.type()),
								false,
								vertexSize,
								format.getOffset(element)
							);
						} else {
							GlStateManager._vertexAttribIPointer(
								elementIndex,
								element.count(),
								GlConst.toGl(element.type()),
								vertexSize,
								format.getOffset(element)
							);
						}
					}
					case NORMAL, COLOR -> GlStateManager._vertexAttribPointer(
						elementIndex,
						element.count(),
						GlConst.toGl(element.type()),
						true,
						vertexSize,
						format.getOffset(element)
					);
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static class AllocatedBuffer {

		final int glId;
		final VertexFormat vertexFormat;
		@Nullable GlGpuBuffer buffer;

		AllocatedBuffer(int glId, VertexFormat vertexFormat, @Nullable GlGpuBuffer buffer) {
			this.glId = glId;
			this.vertexFormat = vertexFormat;
			this.buffer = buffer;
		}
	}
}
