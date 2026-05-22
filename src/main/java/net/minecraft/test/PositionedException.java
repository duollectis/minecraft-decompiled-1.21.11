package net.minecraft.test;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Исключение теста, привязанное к конкретной позиции в мире.
 * Содержит как абсолютные координаты блока, так и относительные (внутри структуры теста).
 */
public class PositionedException extends GameTestException {

	private final BlockPos pos;
	private final BlockPos relativePos;

	public PositionedException(Text message, BlockPos pos, BlockPos relativePos, int tick) {
		super(message, tick);
		this.pos = pos;
		this.relativePos = relativePos;
	}

	@Override
	public Text getText() {
		return Text.translatable(
				"test.error.position",
				message,
				pos.getX(),
				pos.getY(),
				pos.getZ(),
				relativePos.getX(),
				relativePos.getY(),
				relativePos.getZ(),
				tick
		);
	}

	public Text getDebugMessage() {
		return message;
	}

	public BlockPos getRelativePos() {
		return relativePos;
	}

	public BlockPos getPos() {
		return pos;
	}
}
