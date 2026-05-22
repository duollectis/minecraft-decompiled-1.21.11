package net.minecraft.client.font;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Тип фильтра шрифта, управляющий условным включением провайдеров глифов.
 * <p>
 * {@code UNIFORM} — включает провайдеры только при активной опции «Единообразный шрифт».
 * {@code JAPANESE_VARIANTS} — включает японские варианты глифов при соответствующей опции.
 */
@Environment(EnvType.CLIENT)
public enum FontFilterType implements StringIdentifiable {
	UNIFORM("uniform"),
	JAPANESE_VARIANTS("jp");

	public static final Codec<FontFilterType> CODEC = StringIdentifiable.createCodec(FontFilterType::values);
	private final String id;

	FontFilterType(final String id) {
		this.id = id;
	}

	@Override
	public String asString() {
		return id;
	}

	/**
	 * Карта активных фильтров шрифта: тип фильтра → требуемое состояние (вкл/выкл).
	 * Используется для условного включения провайдеров глифов в {@link FontStorage}.
	 */
	@Environment(EnvType.CLIENT)
	public static class FilterMap {

		public static final Codec<FontFilterType.FilterMap> CODEC = Codec.unboundedMap(FontFilterType.CODEC, Codec.BOOL)
		                                                                  .xmap(
				                                                                  FontFilterType.FilterMap::new,
				                                                                  filterMap -> filterMap.activeFilters
		                                                                  );
		public static final FontFilterType.FilterMap NO_FILTER = new FontFilterType.FilterMap(Map.of());

		private final Map<FontFilterType, Boolean> activeFilters;

		public FilterMap(Map<FontFilterType, Boolean> activeFilters) {
			this.activeFilters = activeFilters;
		}

		/**
		 * Проверяет, разрешён ли провайдер при данном наборе активных фильтров.
		 * Провайдер разрешён, если для каждого фильтра в карте его состояние
		 * совпадает с требуемым.
		 *
		 * @param activeFilters набор активных в данный момент фильтров
		 * @return {@code true}, если провайдер должен быть включён
		 */
		public boolean isAllowed(Set<FontFilterType> activeFilters) {
			for (Entry<FontFilterType, Boolean> entry : this.activeFilters.entrySet()) {
				if (activeFilters.contains(entry.getKey()) != entry.getValue()) {
					return false;
				}
			}

			return true;
		}

		/**
		 * Создаёт новую карту фильтров, объединяя текущую с переданной.
		 * Значения из {@code this} перезаписывают значения из {@code base}.
		 *
		 * @param base базовая карта фильтров
		 * @return новая объединённая карта фильтров
		 */
		public FontFilterType.FilterMap apply(FontFilterType.FilterMap base) {
			Map<FontFilterType, Boolean> merged = new HashMap<>(base.activeFilters);
			merged.putAll(activeFilters);
			return new FontFilterType.FilterMap(Map.copyOf(merged));
		}
	}
}
