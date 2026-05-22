package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

/**
 * Базовый класс источника ввода игрока. Хранит текущее состояние движения
 * и флаги действий (прыжок, присед, спринт). Подклассы переопределяют
 * {@link #tick()} для обновления состояния из реального устройства ввода.
 */
@Environment(EnvType.CLIENT)
public class Input {

	private static final float FORWARD_THRESHOLD = 1.0E-5F;

	public PlayerInput playerInput = PlayerInput.DEFAULT;
	protected Vec2f movementVector = Vec2f.ZERO;

	public void tick() {
	}

	public Vec2f getMovementInput() {
		return movementVector;
	}

	public boolean hasForwardMovement() {
		return movementVector.y > FORWARD_THRESHOLD;
	}

	/** Принудительно устанавливает флаг прыжка в текущем состоянии ввода. */
	public void jump() {
		playerInput = new PlayerInput(
				playerInput.forward(),
				playerInput.backward(),
				playerInput.left(),
				playerInput.right(),
				true,
				playerInput.sneak(),
				playerInput.sprint()
		);
	}
}
