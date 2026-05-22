package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.ChestType;

/**
 * Состояние рендеринга сундука: хранит тип сундука (одиночный/двойной),
 * прогресс анимации крышки, угол поворота и визуальный вариант текстуры.
 */
@Environment(EnvType.CLIENT)
public class ChestBlockEntityRenderState extends BlockEntityRenderState {

	public ChestType chestType = ChestType.SINGLE;
	public float lidAnimationProgress;
	public float yaw;
	public ChestBlockEntityRenderState.Variant variant = ChestBlockEntityRenderState.Variant.REGULAR;

	/** Визуальный вариант сундука, определяющий набор текстур для рендеринга. */
	@Environment(EnvType.CLIENT)
	public enum Variant {
		ENDER_CHEST,
		CHRISTMAS,
		TRAPPED,
		COPPER_UNAFFECTED,
		COPPER_EXPOSED,
		COPPER_WEATHERED,
		COPPER_OXIDIZED,
		REGULAR
	}
}
