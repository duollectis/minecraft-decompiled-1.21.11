package net.minecraft.util.math;

/**
 * Набор стандартных функций плавности (easing functions) для анимаций.
 * Все методы принимают нормализованный параметр {@code t} в диапазоне [0, 1]
 * и возвращают значение в том же диапазоне с различными кривыми ускорения/замедления.
 */
public class Easing {

	private static final float BACK_OVERSHOOT = 1.70158F;
	private static final float BACK_OVERSHOOT_OUT = 2.70158F;
	private static final float BACK_OVERSHOOT_IN_OUT = 2.5949094F;
	private static final float BOUNCE_FACTOR = 7.5625F;
	private static final float BOUNCE_DIVISOR = 2.75F;

	public static float inBack(float t) {
		return MathHelper.square(t) * (BACK_OVERSHOOT_OUT * t - BACK_OVERSHOOT);
	}

	public static float inBounce(float t) {
		return 1.0F - outBounce(1.0F - t);
	}

	public static float inCubic(float t) {
		return MathHelper.cube(t);
	}

	public static float inElastic(float t) {
		if (t == 0.0F) {
			return 0.0F;
		}

		if (t == 1.0F) {
			return 1.0F;
		}

		return (float) (-Math.pow(2.0, 10.0 * t - 10.0) * Math.sin((t * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0)));
	}

	public static float inExpo(float t) {
		return t == 0.0F ? 0.0F : (float) Math.pow(2.0, 10.0 * t - 10.0);
	}

	public static float inQuart(float t) {
		return MathHelper.square(MathHelper.square(t));
	}

	public static float inQuint(float t) {
		return MathHelper.square(MathHelper.square(t)) * t;
	}

	public static float inSine(float t) {
		return 1.0F - MathHelper.cos(t * (float) (Math.PI / 2));
	}

	public static float inOutBounce(float t) {
		return t < 0.5F
				? (1.0F - outBounce(1.0F - 2.0F * t)) / 2.0F
				: (1.0F + outBounce(2.0F * t - 1.0F)) / 2.0F;
	}

	public static float inOutCirc(float t) {
		return t < 0.5F
				? (float) ((1.0 - Math.sqrt(1.0 - Math.pow(2.0 * t, 2.0))) / 2.0)
				: (float) ((Math.sqrt(1.0 - Math.pow(-2.0 * t + 2.0, 2.0)) + 1.0) / 2.0);
	}

	public static float inOutCubic(float t) {
		return t < 0.5F
				? 4.0F * MathHelper.cube(t)
				: (float) (1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0);
	}

	public static float inOutQuad(float t) {
		return t < 0.5F
				? 2.0F * MathHelper.square(t)
				: (float) (1.0 - Math.pow(-2.0 * t + 2.0, 2.0) / 2.0);
	}

	public static float inOutQuart(float t) {
		return t < 0.5F
				? 8.0F * MathHelper.square(MathHelper.square(t))
				: (float) (1.0 - Math.pow(-2.0 * t + 2.0, 4.0) / 2.0);
	}

	public static float inOutQuint(float t) {
		return t < 0.5F
				? 16.0F * t * t * t * t * t
				: (float) (1.0 - Math.pow(-2.0 * t + 2.0, 5.0) / 2.0);
	}

	public static float outBounce(float t) {
		if (t < 0.36363637F) {
			return BOUNCE_FACTOR * MathHelper.square(t);
		}

		if (t < 0.72727275F) {
			return BOUNCE_FACTOR * MathHelper.square(t - 0.54545456F) + 0.75F;
		}

		return t < 0.9090909090909091F
				? BOUNCE_FACTOR * MathHelper.square(t - 0.8181818F) + 0.9375F
				: BOUNCE_FACTOR * MathHelper.square(t - 0.95454544F) + 0.984375F;
	}

	public static float outElastic(float t) {
		if (t == 0.0F) {
			return 0.0F;
		}

		return t == 1.0F
				? 1.0F
				: (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0);
	}

	public static float outExpo(float t) {
		return t == 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0 * t);
	}

	public static float outQuad(float t) {
		return 1.0F - MathHelper.square(1.0F - t);
	}

	public static float outQuint(float t) {
		return 1.0F - (float) Math.pow(1.0 - t, 5.0);
	}

	public static float outSine(float t) {
		return MathHelper.sin(t * (float) (Math.PI / 2));
	}

	public static float inOutSine(float t) {
		return -(MathHelper.cos((float) Math.PI * t) - 1.0F) / 2.0F;
	}

	public static float outBack(float t) {
		return 1.0F + BACK_OVERSHOOT_OUT * MathHelper.cube(t - 1.0F) + BACK_OVERSHOOT * MathHelper.square(t - 1.0F);
	}

	public static float outQuart(float t) {
		return 1.0F - MathHelper.square(MathHelper.square(1.0F - t));
	}

	public static float outCubic(float t) {
		return 1.0F - MathHelper.cube(1.0F - t);
	}

	public static float inOutExpo(float t) {
		if (t < 0.5F) {
			return t == 0.0F ? 0.0F : (float) (Math.pow(2.0, 20.0 * t - 10.0) / 2.0);
		}

		return t == 1.0F ? 1.0F : (float) ((2.0 - Math.pow(2.0, -20.0 * t + 10.0)) / 2.0);
	}

	public static float inQuad(float t) {
		return t * t;
	}

	public static float outCirc(float t) {
		return (float) Math.sqrt(1.0F - MathHelper.square(t - 1.0F));
	}

	public static float inOutElastic(float t) {
		if (t == 0.0F) {
			return 0.0F;
		}

		if (t == 1.0F) {
			return 1.0F;
		}

		double sinArg = (20.0 * t - 11.125) * (Math.PI * 4.0 / 9.0);
		return t < 0.5F
				? (float) (-(Math.pow(2.0, 20.0 * t - 10.0) * Math.sin(sinArg)) / 2.0)
				: (float) (Math.pow(2.0, -20.0 * t + 10.0) * Math.sin(sinArg) / 2.0 + 1.0);
	}

	public static float inCirc(float t) {
		return (float) (-Math.sqrt(1.0F - t * t)) + 1.0F;
	}

	public static float inOutBack(float t) {
		if (t < 0.5F) {
			return 4.0F * t * t * (7.189819F * t - BACK_OVERSHOOT_IN_OUT) / 2.0F;
		}

		float shifted = 2.0F * t - 2.0F;
		return (shifted * shifted * (3.5949094F * shifted + BACK_OVERSHOOT_IN_OUT) + 2.0F) / 2.0F;
	}
}
