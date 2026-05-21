package net.minecraft.util;

import org.apache.commons.lang3.ObjectUtils;

import java.util.function.Supplier;

/**
 * {@code ModStatus}.
 */
public record ModStatus(ModStatus.Confidence confidence, String description) {

	public static ModStatus check(
			String vanillaBrand,
			Supplier<String> brandSupplier,
			String environment,
			Class<?> clazz
	) {
		String string = brandSupplier.get();
		if (!vanillaBrand.equals(string)) {
			return new ModStatus(ModStatus.Confidence.DEFINITELY, environment + " brand changed to '" + string + "'");
		}
		else {
			return clazz.getSigners() == null
			       ? new ModStatus(ModStatus.Confidence.VERY_LIKELY, environment + " jar signature invalidated")
			       : new ModStatus(
					       ModStatus.Confidence.PROBABLY_NOT,
					       environment + " jar signature and brand is untouched"
			       );
		}
	}

	public boolean isModded() {
		return this.confidence.modded;
	}

	public ModStatus combine(ModStatus brand) {
		return new ModStatus(
				(ModStatus.Confidence) ObjectUtils.max(new ModStatus.Confidence[]{this.confidence, brand.confidence}),
				this.description + "; " + brand.description
		);
	}

	public String getMessage() {
		return this.confidence.description + " " + this.description;
	}

	/**
	 * {@code Confidence}.
	 */
	public static enum Confidence {
		PROBABLY_NOT("Probably not.", false),
		VERY_LIKELY("Very likely;", true),
		DEFINITELY("Definitely;", true);

		final String description;
		final boolean modded;

		private Confidence(final String description, final boolean modded) {
			this.description = description;
			this.modded = modded;
		}
	}
}
