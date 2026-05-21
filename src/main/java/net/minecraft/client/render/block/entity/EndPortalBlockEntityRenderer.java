package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.client.render.block.entity.state.EndPortalBlockEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code EndPortalBlockEntityRenderer}.
 */
public class EndPortalBlockEntityRenderer extends AbstractEndPortalBlockEntityRenderer<EndPortalBlockEntity, EndPortalBlockEntityRenderState> {

	/**
	 * Создаёт render state.
	 *
	 * @return EndPortalBlockEntityRenderState — результат операции
	 */
	public EndPortalBlockEntityRenderState createRenderState() {
		return new EndPortalBlockEntityRenderState();
	}
}
