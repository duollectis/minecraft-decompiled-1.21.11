package net.minecraft.client.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

/**
 * Скомпилированный GLSL-шейдер (вершинный или фрагментный).
 * Хранит OpenGL-дескриптор шейдера и освобождает его при закрытии.
 */
@Environment(EnvType.CLIENT)
public class CompiledShader implements AutoCloseable {

	private static final int CLOSED = -1;

	public static final CompiledShader INVALID_SHADER =
		new CompiledShader(-1, Identifier.ofVanilla("invalid"), ShaderType.VERTEX);

	private final Identifier id;
	private final ShaderType shaderType;
	private int handle;

	public CompiledShader(int handle, Identifier id, ShaderType shaderType) {
		this.id = id;
		this.handle = handle;
		this.shaderType = shaderType;
	}

	@Override
	public void close() {
		if (handle == CLOSED) {
			throw new IllegalStateException("Already closed");
		}

		RenderSystem.assertOnRenderThread();
		GlStateManager.glDeleteShader(handle);
		handle = CLOSED;
	}

	public Identifier getId() {
		return id;
	}

	public int getHandle() {
		return handle;
	}

	public String getDebugLabel() {
		return shaderType.idConverter().toResourcePath(id).toString();
	}
}
