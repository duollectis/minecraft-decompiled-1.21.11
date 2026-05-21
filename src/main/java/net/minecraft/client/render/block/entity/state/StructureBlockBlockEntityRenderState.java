package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.StructureBoxRendering;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code StructureBlockBlockEntityRenderState}.
 */
public class StructureBlockBlockEntityRenderState extends BlockEntityRenderState {

	public boolean visible;
	public StructureBoxRendering.RenderMode renderMode;
	public StructureBoxRendering.StructureBox structureBox;
	public StructureBlockBlockEntityRenderState.@Nullable InvisibleRenderType @Nullable [] invisibleBlocks;
	public boolean @Nullable [] structureVoidBlocks;

	@Environment(EnvType.CLIENT)
	/**
	 * {@code InvisibleRenderType}.
	 */
	public static enum InvisibleRenderType {
		AIR,
		BARRIER,
		LIGHT,
		STRUCTURE_VOID;
	}
}
