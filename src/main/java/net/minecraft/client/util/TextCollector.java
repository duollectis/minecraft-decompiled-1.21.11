package net.minecraft.client.util;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.StringVisitable;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Аккумулятор фрагментов {@link StringVisitable} для последующего объединения.
 * Оптимизирует случай единственного фрагмента, возвращая его без обёртки.
 */
@Environment(EnvType.CLIENT)
public class TextCollector {

	private final List<StringVisitable> texts = Lists.newArrayList();

	public void add(StringVisitable text) {
		texts.add(text);
	}

	public @Nullable StringVisitable getRawCombined() {
		if (texts.isEmpty()) {
			return null;
		}

		return texts.size() == 1 ? texts.get(0) : StringVisitable.concat(texts);
	}

	public StringVisitable getCombined() {
		StringVisitable combined = getRawCombined();
		return combined != null ? combined : StringVisitable.EMPTY;
	}

	public void clear() {
		texts.clear();
	}
}
