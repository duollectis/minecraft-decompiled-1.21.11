package net.minecraft.client.gl;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code CompiledShaderPipeline}.
 */
public record CompiledShaderPipeline(RenderPipeline info, ShaderProgram program) implements CompiledRenderPipeline {

	@Override
	public boolean isValid() {
		return this.program != ShaderProgram.INVALID;
	}
}
