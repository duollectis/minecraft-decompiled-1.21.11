package net.minecraft.client.gl;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Скомпилированный конвейер рендеринга: связывает описание {@link RenderPipeline}
 * с готовой к использованию {@link ShaderProgram}.
 */
@Environment(EnvType.CLIENT)
public record CompiledShaderPipeline(RenderPipeline info, ShaderProgram program) implements CompiledRenderPipeline {

	@Override
	public boolean isValid() {
		return program != ShaderProgram.INVALID;
	}
}
