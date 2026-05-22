package net.minecraft.util;

import org.apache.commons.lang3.ObjectUtils;

import java.util.function.Supplier;

/**
 * Статус модификации клиента или сервера, определяемый по бренду JAR-файла и его подписи.
 * Используется для отображения предупреждений в отчётах об ошибках.
 */
public record ModStatus(Confidence confidence, String description) {

	/**
	 * Определяет статус модификации среды выполнения.
	 * Сначала проверяет бренд JAR, затем — наличие цифровой подписи.
	 *
	 * @param vanillaBrand ожидаемый бренд ванильного клиента/сервера
	 * @param brandSupplier поставщик текущего бренда
	 * @param environment название среды ("client" или "server") для сообщений
	 * @param clazz класс для проверки подписи JAR
	 * @return статус модификации с уровнем уверенности
	 */
	public static ModStatus check(
		String vanillaBrand,
		Supplier<String> brandSupplier,
		String environment,
		Class<?> clazz
	) {
		String currentBrand = brandSupplier.get();

		if (!vanillaBrand.equals(currentBrand)) {
			return new ModStatus(Confidence.DEFINITELY, environment + " brand changed to '" + currentBrand + "'");
		}

		return clazz.getSigners() == null
			? new ModStatus(Confidence.VERY_LIKELY, environment + " jar signature invalidated")
			: new ModStatus(Confidence.PROBABLY_NOT, environment + " jar signature and brand is untouched");
	}

	public boolean isModded() {
		return confidence.modded;
	}

	/**
	 * Объединяет два статуса, выбирая максимальный уровень уверенности и конкатенируя описания.
	 *
	 * @param other второй статус для объединения
	 * @return объединённый статус
	 */
	public ModStatus combine(ModStatus other) {
		return new ModStatus(
			ObjectUtils.max(confidence, other.confidence),
			description + "; " + other.description
		);
	}

	public String getMessage() {
		return confidence.description + " " + description;
	}

	/**
	 * Уровень уверенности в том, что среда была модифицирована.
	 */
	public enum Confidence {
		PROBABLY_NOT("Probably not.", false),
		VERY_LIKELY("Very likely;", true),
		DEFINITELY("Definitely;", true);

		final String description;
		final boolean modded;

		Confidence(String description, boolean modded) {
			this.description = description;
			this.modded = modded;
		}
	}
}
