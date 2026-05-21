package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.decoration.DisplayEntity;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code DisplayEntityRenderState}.
 */
public abstract class DisplayEntityRenderState extends EntityRenderState {

	public DisplayEntity.@Nullable RenderState displayRenderState;
	public float lerpProgress;
	public float yaw;
	public float pitch;
	public float cameraYaw;
	public float cameraPitch;

	/**
	 * Проверяет возможность render.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public abstract boolean canRender();
}
