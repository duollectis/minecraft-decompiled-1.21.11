package net.minecraft.resource;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.UnaryOperator;

/**
 * {@code ResourcePackSource}.
 */
public interface ResourcePackSource {

	UnaryOperator<Text> NONE_SOURCE_TEXT_SUPPLIER = UnaryOperator.identity();

	ResourcePackSource NONE = create(NONE_SOURCE_TEXT_SUPPLIER, true);

	ResourcePackSource BUILTIN = create(getSourceTextSupplier("pack.source.builtin"), true);

	ResourcePackSource FEATURE = create(getSourceTextSupplier("pack.source.feature"), false);

	ResourcePackSource WORLD = create(getSourceTextSupplier("pack.source.world"), true);

	ResourcePackSource SERVER = create(getSourceTextSupplier("pack.source.server"), true);

	Text decorate(Text packDisplayName);

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
