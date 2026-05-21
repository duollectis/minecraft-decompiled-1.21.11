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

@Environment(EnvType.CLIENT)
/**
 * {@code VertexBufferManager}.
 */
public abstract class VertexBufferManager {

	public static VertexBufferManager create(
			GLCapabilities capabilities,
			DebugLabelManager labeler,
			Set<String> usedCapabilities
	) {
		if (capabilities.GL_ARB_vertex_attrib_binding && GlBackend.allowGlArbVABinding) {
			usedCapabilities.add("GL_ARB_vertex_attrib_binding");
			return new VertexBufferManager.ARBVertexBufferManager(labeler);
		}
		else {
			return new VertexBufferManager.DefaultVertexBufferManager(labeler);
		}
	}

	public abstract void setupBuffer(VertexFormat format, @Nullable GlGpuBuffer into);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ARBVertexBufferManager}.
	 */
	static class ARBVertexBufferManager extends VertexBufferManager {

		private final Map<VertexFormat, VertexBufferManager.AllocatedBuffer> cache = new HashMap<>();
		private final DebugLabelManager labeler;
		private final boolean applyMesaWorkaround;

		public ARBVertexBufferManager(DebugLabelManager labeler) {
			this.labeler = labeler;
			if ("Mesa".equals(GlStateManager._getString(7936))) {
				String string = GlStateManager._getString(7938);
				this.applyMesaWorkaround =
						string.contains("25.0.0") || string.contains("25.0.1") || string.contains("25.0.2");
			}
			else {
				this.applyMesaWorkaround = false;
			}
		}

		@Override
		public void setupBuffer(VertexFormat format, @Nullable GlGpuBuffer into) {
			VertexBufferManager.AllocatedBuffer allocatedBuffer = this.cache.get(format);
			if (allocatedBuffer != null) {
				GlStateManager._glBindVertexArray(allocatedBuffer.glId);
				if (into != null && allocatedBuffer.buffer != into) {
					if (this.applyMesaWorkaround && allocatedBuffer.buffer != null
							&& allocatedBuffer.buffer.id == into.id) {
						ARBVertexAttribBinding.glBindVertexBuffer(0, 0, 0L, 0);
					}

					ARBVertexAttribBinding.glBindVertexBuffer(0, into.id, 0L, format.getVertexSize());
					allocatedBuffer.buffer = into;
				}
			}
			else {
				int i = GlStateManager._glGenVertexArrays();
				GlStateManager._glBindVertexArray(i);
				if (into != null) {
					List<VertexFormatElement> list = format.getElements();

					for (int j = 0; j < list.size(); j++) {
						VertexFormatElement vertexFormatElement = list.get(j);
						GlStateManager._enableVertexAttribArray(j);
						switch (vertexFormatElement.usage()) {
							case POSITION:
							case GENERIC:
							case UV:
								if (vertexFormatElement.type() == VertexFormatElement.Type.FLOAT) {
									ARBVertexAttribBinding.glVertexAttribFormat(
											j,
											vertexFormatElement.count(),
											GlConst.toGl(vertexFormatElement.type()),
											false,
											format.getOffset(vertexFormatElement)
									);
								}
								else {
									ARBVertexAttribBinding.glVertexAttribIFormat(
											j,
											vertexFormatElement.count(),
											GlConst.toGl(vertexFormatElement.type()),
											format.getOffset(vertexFormatElement)
									);
								}
								break;
							case NORMAL:
							case COLOR:
								ARBVertexAttribBinding.glVertexAttribFormat(
										j,
										vertexFormatElement.count(),
										GlConst.toGl(vertexFormatElement.type()),
										true,
										format.getOffset(vertexFormatElement)
								);
						}

						ARBVertexAttribBinding.glVertexAttribBinding(j, 0);
					}
				}

				if (into != null) {
					ARBVertexAttribBinding.glBindVertexBuffer(0, into.id, 0L, format.getVertexSize());
				}

				VertexBufferManager.AllocatedBuffer
						allocatedBuffer2 =
						new VertexBufferManager.AllocatedBuffer(i, format, into);
				this.labeler.labelAllocatedBuffer(allocatedBuffer2);
				this.cache.put(format, allocatedBuffer2);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code AllocatedBuffer}.
	 */
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

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DefaultVertexBufferManager}.
	 */
	static class DefaultVertexBufferManager extends VertexBufferManager {

		private final Map<VertexFormat, VertexBufferManager.AllocatedBuffer> cache = new HashMap<>();
		private final DebugLabelManager labeler;

		public DefaultVertexBufferManager(DebugLabelManager labeler) {
			this.labeler = labeler;
		}

		@Override
		public void setupBuffer(VertexFormat format, @Nullable GlGpuBuffer into) {
			VertexBufferManager.AllocatedBuffer allocatedBuffer = this.cache.get(format);
			if (allocatedBuffer == null) {
				int i = GlStateManager._glGenVertexArrays();
				GlStateManager._glBindVertexArray(i);
				if (into != null) {
					GlStateManager._glBindBuffer(34962, into.id);
					setupBuffer(format, true);
				}

				VertexBufferManager.AllocatedBuffer
						allocatedBuffer2 =
						new VertexBufferManager.AllocatedBuffer(i, format, into);
				this.labeler.labelAllocatedBuffer(allocatedBuffer2);
				this.cache.put(format, allocatedBuffer2);
			}
			else {
				GlStateManager._glBindVertexArray(allocatedBuffer.glId);
				if (into != null && allocatedBuffer.buffer != into) {
					GlStateManager._glBindBuffer(34962, into.id);
					allocatedBuffer.buffer = into;
					setupBuffer(format, false);
				}
			}
		}

		private static void setupBuffer(VertexFormat format, boolean vbaIsNew) {
			int i = format.getVertexSize();
			List<VertexFormatElement> list = format.getElements();

			for (int j = 0; j < list.size(); j++) {
				VertexFormatElement vertexFormatElement = list.get(j);
				if (vbaIsNew) {
					GlStateManager._enableVertexAttribArray(j);
				}

				switch (vertexFormatElement.usage()) {
					case POSITION:
					case GENERIC:
					case UV:
						if (vertexFormatElement.type() == VertexFormatElement.Type.FLOAT) {
							GlStateManager._vertexAttribPointer(
									j,
									vertexFormatElement.count(),
									GlConst.toGl(vertexFormatElement.type()),
									false,
									i,
									format.getOffset(vertexFormatElement)
							);
						}
						else {
							GlStateManager._vertexAttribIPointer(
									j,
									vertexFormatElement.count(),
									GlConst.toGl(vertexFormatElement.type()),
									i,
									format.getOffset(vertexFormatElement)
							);
						}
						break;
					case NORMAL:
					case COLOR:
						GlStateManager._vertexAttribPointer(
								j,
								vertexFormatElement.count(),
								GlConst.toGl(vertexFormatElement.type()),
								true,
								i,
								format.getOffset(vertexFormatElement)
						);
				}
			}
		}
	}
}
