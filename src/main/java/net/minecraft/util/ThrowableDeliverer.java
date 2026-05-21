package net.minecraft.util;

import org.jspecify.annotations.Nullable;

/**
 * {@code ThrowableDeliverer}.
 */
public class ThrowableDeliverer<T extends Throwable> {

	private @Nullable T throwable;

	/**
	 * Add.
	 *
	 * @param throwable throwable
	 */
	public void add(T throwable) {
		if (this.throwable == null) {
			this.throwable = throwable;
		}
		else {
			this.throwable.addSuppressed(throwable);
		}
	}

	/**
	 * Deliver.
	 */
	public void deliver() throws T {
		if (this.throwable != null) {
			throw this.throwable;
		}
	}
}
