package net.minecraft.client.util;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.StringVisitable;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code TextCollector}.
 */
public class TextCollector {

	private final List<StringVisitable> texts = Lists.newArrayList();

	/**
	 * Add.
	 *
	 * @param text text
	 */
	public void add(StringVisitable text) {
		this.texts.add(text);
	}

	public @Nullable StringVisitable getRawCombined() {
		if (this.texts.isEmpty()) {
			return null;
		}
		else {
			return this.texts.size() == 1 ? this.texts.get(0) : StringVisitable.concat(this.texts);
		}
	}

	public StringVisitable getCombined() {
		StringVisitable stringVisitable = this.getRawCombined();
		return stringVisitable != null ? stringVisitable : StringVisitable.EMPTY;
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.texts.clear();
	}
}
