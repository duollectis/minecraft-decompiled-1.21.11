package net.minecraft.resource;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.UnaryOperator;

/**
 * Источник ресурс-пака: определяет способ декорирования отображаемого имени
 * и возможность включения пака пользователем позже.
 */
public interface ResourcePackSource {

	UnaryOperator<Text> NONE_SOURCE_TEXT_SUPPLIER = UnaryOperator.identity();

	/** Источник без декорирования имени. */
	ResourcePackSource NONE = create(NONE_SOURCE_TEXT_SUPPLIER, true);

	/** Встроенный пак игры. */
	ResourcePackSource BUILTIN = create(getSourceTextSupplier("pack.source.builtin"), true);

	/** Пак экспериментальных фич. */
	ResourcePackSource FEATURE = create(getSourceTextSupplier("pack.source.feature"), false);

	/** Пак из директории мира. */
	ResourcePackSource WORLD = create(getSourceTextSupplier("pack.source.world"), true);

	/** Пак, полученный с сервера. */
	ResourcePackSource SERVER = create(getSourceTextSupplier("pack.source.server"), true);

	/**
	 * Декорирует отображаемое имя пака с указанием источника.
	 *
	 * @param packDisplayName отображаемое имя пака
	 * @return декорированный текст
	 */
	Text decorate(Text packDisplayName);

	/**
	 * Может ли пак из этого источника быть включён пользователем позже.
	 *
	 * @return {@code true}, если пак можно включить вручную
	 */
	boolean canBeEnabledLater();

	static ResourcePackSource create(UnaryOperator<Text> sourceTextSupplier, boolean canBeEnabledLater) {
		return new ResourcePackSource() {
			@Override
			public Text decorate(Text packDisplayName) {
				return sourceTextSupplier.apply(packDisplayName);
			}

			@Override
			public boolean canBeEnabledLater() {
				return canBeEnabledLater;
			}
		};
	}

	private static UnaryOperator<Text> getSourceTextSupplier(String translationKey) {
		Text text = Text.translatable(translationKey);
		return name -> Text.translatable("pack.nameAndSource", name, text).formatted(Formatting.GRAY);
	}
}
