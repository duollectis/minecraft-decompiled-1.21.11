package net.minecraft.client.font;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code FontFilterType}.
 */
public enum FontFilterType implements StringIdentifiable {
	UNIFORM("uniform"),
	JAPANESE_VARIANTS("jp");

	public static final Codec<FontFilterType> CODEC = StringIdentifiable.createCodec(FontFilterType::values);
	private final String id;

	private FontFilterType(final String id) {
		this.id = id;
	}

	@Override
	public String asString() {
		return this.id;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FilterMap}.
	 */
	public static class FilterMap {

		private final Map<FontFilterType, Boolean> activeFilters;
		public static final Codec<FontFilterType.FilterMap> CODEC = Codec.unboundedMap(FontFilterType.CODEC, Codec.BOOL)
		                                                                 .xmap(
				                                                                 FontFilterType.FilterMap::new,
				                                                                 filterType -> filterType.activeFilters
		                                                                 );
		public static final FontFilterType.FilterMap NO_FILTER = new FontFilterType.FilterMap(Map.of());

		public FilterMap(Map<FontFilterType, Boolean> activeFilters) {
			this.activeFilters = activeFilters;
		}

		public boolean isAllowed(Set<FontFilterType> activeFilters) {
			for (Entry<FontFilterType, Boolean> entry : this.activeFilters.entrySet()) {
				if (activeFilters.contains(entry.getKey()) != entry.getValue()) {
					return false;
				}
			}

			return true;
		}

		public FontFilterType.FilterMap apply(FontFilterType.FilterMap activeFilters) {
			Map<FontFilterType, Boolean> map = new HashMap<>(activeFilters.activeFilters);
			map.putAll(this.activeFilters);
			return new FontFilterType.FilterMap(Map.copyOf(map));
		}
	}
}
