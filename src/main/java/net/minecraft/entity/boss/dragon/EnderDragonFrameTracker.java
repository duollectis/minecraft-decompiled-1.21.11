package net.minecraft.entity.boss.dragon;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;

/**
 * Кольцевой буфер кадров позиции Эндер-дракона для интерполяции анимации тела.
 * Хранит последние {@link #FRAME_BUFFER_SIZE} записей (Y-координата + угол поворота),
 * позволяя плавно интерполировать положение частей тела между тиками.
 */
public class EnderDragonFrameTracker {

	public static final int FRAME_BUFFER_SIZE = 64;
	private static final int FRAME_BUFFER_MASK = FRAME_BUFFER_SIZE - 1;

	private final Frame[] frames = new Frame[FRAME_BUFFER_SIZE];
	private int currentIndex = -1;

	public EnderDragonFrameTracker() {
		Arrays.fill(frames, new Frame(0.0, 0.0F));
	}

	public void copyFrom(EnderDragonFrameTracker other) {
		System.arraycopy(other.frames, 0, frames, 0, FRAME_BUFFER_SIZE);
		currentIndex = other.currentIndex;
	}

	/**
	 * Записывает новый кадр в буфер. При первом вызове заполняет весь буфер этим кадром.
	 */
	public void tick(double y, float yaw) {
		Frame frame = new Frame(y, yaw);
		if (currentIndex < 0) {
			Arrays.fill(frames, frame);
		}

		if (++currentIndex == FRAME_BUFFER_SIZE) {
			currentIndex = 0;
		}

		frames[currentIndex] = frame;
	}

	public Frame getFrame(int age) {
		return frames[currentIndex - age & FRAME_BUFFER_MASK];
	}

	/**
	 * Возвращает линейно интерполированный кадр между {@code age} и {@code age+1}.
	 *
	 * @param age          количество тиков назад от текущего
	 * @param tickProgress прогресс интерполяции в диапазоне [0, 1]
	 */
	public Frame getLerpedFrame(int age, float tickProgress) {
		Frame current = getFrame(age);
		Frame previous = getFrame(age + 1);
		return new Frame(
				MathHelper.lerp((double) tickProgress, previous.y, current.y),
				MathHelper.lerpAngleDegrees(tickProgress, previous.yRot, current.yRot)
		);
	}

	/**
	 * Снимок состояния дракона в один тик: вертикальная позиция и угол поворота.
	 */
	public record Frame(double y, float yRot) {
	}
}
