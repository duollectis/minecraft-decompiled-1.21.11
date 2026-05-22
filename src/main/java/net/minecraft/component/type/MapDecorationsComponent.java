package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;

import java.util.Map;

/**
	 * Компонент декораций карты. Хранит именованные маркеры (иконки) на карте:
	 * игроки, структуры, пользовательские метки и т.д.
	 */
public record MapDecorationsComponent(Map<String, MapDecorationsComponent.Decoration> decorations) {

	public static final MapDecorationsComponent DEFAULT = new MapDecorationsComponent(Map.of());
	public static final Codec<MapDecorationsComponent>
			CODEC =
			Codec.unboundedMap(Codec.STRING, MapDecorationsComponent.Decoration.CODEC)
					.xmap(MapDecorationsComponent::new, MapDecorationsComponent::decorations);

	/**
		 * Возвращает новый компонент с добавленной или заменённой декорацией по идентификатору.
		 *
		 * @param id         уникальный идентификатор декорации
		 * @param decoration данные декорации (тип, координаты, угол поворота)
		 * @return новый {@code MapDecorationsComponent} с обновлённой картой декораций
		 */
	public MapDecorationsComponent with(String id, MapDecorationsComponent.Decoration decoration) {
		return new MapDecorationsComponent(Util.mapWith(decorations, id, decoration));
	}

	/**
		 * Декорация на карте: тип иконки, мировые координаты и угол поворота.
		 */
	public record Decoration(RegistryEntry<MapDecorationType> type, double x, double z, float rotation) {

		public static final Codec<MapDecorationsComponent.Decoration> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
											MapDecorationType.CODEC.fieldOf("type").forGetter(MapDecorationsComponent.Decoration::type),
											Codec.DOUBLE.fieldOf("x").forGetter(MapDecorationsComponent.Decoration::x),
											Codec.DOUBLE.fieldOf("z").forGetter(MapDecorationsComponent.Decoration::z),
											Codec.FLOAT.fieldOf("rotation").forGetter(MapDecorationsComponent.Decoration::rotation)
									)
									.apply(instance, MapDecorationsComponent.Decoration::new)
		);
	}
}
