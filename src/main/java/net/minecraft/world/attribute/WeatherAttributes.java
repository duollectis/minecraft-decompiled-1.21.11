package net.minecraft.world.attribute;

import com.google.common.collect.Sets;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;
import net.minecraft.world.attribute.timeline.Timelines;

import java.util.Set;

/**
 * Предопределённые карты атрибутов для погодных эффектов (дождь и гроза).
 * Применяются поверх базовых атрибутов измерения через временну́ю функцию,
 * интерполируя значения по градиенту погоды.
 */
public class WeatherAttributes {

	/** Эффекты дождя: затемнение неба, тумана, облаков, снижение уровня света. */
	public static final EnvironmentAttributeMap RAIN_EFFECTS = EnvironmentAttributeMap.builder()
		.with(
			EnvironmentAttributes.SKY_COLOR_VISUAL,
			ColorModifier.BLEND_TO_GRAY,
			new ColorModifier.BlendToGrayArg(0.6F, 0.75F)
		)
		.with(
			EnvironmentAttributes.FOG_COLOR_VISUAL,
			ColorModifier.MULTIPLY_RGB,
			ColorHelper.fromFloats(1.0F, 0.5F, 0.5F, 0.6F)
		)
		.with(
			EnvironmentAttributes.CLOUD_COLOR_VISUAL,
			ColorModifier.BLEND_TO_GRAY,
			new ColorModifier.BlendToGrayArg(0.24F, 0.5F)
		)
		.with(
			EnvironmentAttributes.SKY_LIGHT_LEVEL_GAMEPLAY,
			FloatModifier.ALPHA_BLEND,
			new BlendArgument(4.0F, 0.3125F)
		)
		.with(
			EnvironmentAttributes.SKY_LIGHT_COLOR_VISUAL,
			ColorModifier.ALPHA_BLEND,
			ColorHelper.withAlpha(0.3125F, Timelines.NIGHT_SKY_LIGHT_COLOR)
		)
		.with(
			EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL,
			FloatModifier.ALPHA_BLEND,
			new BlendArgument(0.24F, 0.3125F)
		)
		.with(EnvironmentAttributes.STAR_BRIGHTNESS_VISUAL, 0.0F)
		.with(
			EnvironmentAttributes.SUNRISE_SUNSET_COLOR_VISUAL,
			ColorModifier.MULTIPLY_ARGB,
			ColorHelper.fromFloats(1.0F, 0.5F, 0.5F, 0.6F)
		)
		.with(EnvironmentAttributes.BEES_STAY_IN_HIVE_GAMEPLAY, true)
		.build();

	/** Эффекты грозы: более сильное затемнение, чем при дожде. */
	public static final EnvironmentAttributeMap THUNDER_EFFECTS = EnvironmentAttributeMap.builder()
		.with(
			EnvironmentAttributes.SKY_COLOR_VISUAL,
			ColorModifier.BLEND_TO_GRAY,
			new ColorModifier.BlendToGrayArg(0.24F, 0.94F)
		)
		.with(
			EnvironmentAttributes.FOG_COLOR_VISUAL,
			ColorModifier.MULTIPLY_RGB,
			ColorHelper.fromFloats(1.0F, 0.25F, 0.25F, 0.3F)
		)
		.with(
			EnvironmentAttributes.CLOUD_COLOR_VISUAL,
			ColorModifier.BLEND_TO_GRAY,
			new ColorModifier.BlendToGrayArg(0.095F, 0.94F)
		)
		.with(
			EnvironmentAttributes.SKY_LIGHT_LEVEL_GAMEPLAY,
			FloatModifier.ALPHA_BLEND,
			new BlendArgument(4.0F, 0.52734375F)
		)
		.with(
			EnvironmentAttributes.SKY_LIGHT_COLOR_VISUAL,
			ColorModifier.ALPHA_BLEND,
			ColorHelper.withAlpha(0.52734375F, Timelines.NIGHT_SKY_LIGHT_COLOR)
		)
		.with(
			EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL,
			FloatModifier.ALPHA_BLEND,
			new BlendArgument(0.24F, 0.52734375F)
		)
		.with(EnvironmentAttributes.STAR_BRIGHTNESS_VISUAL, 0.0F)
		.with(
			EnvironmentAttributes.SUNRISE_SUNSET_COLOR_VISUAL,
			ColorModifier.MULTIPLY_ARGB,
			ColorHelper.fromFloats(1.0F, 0.25F, 0.25F, 0.3F)
		)
		.with(EnvironmentAttributes.BEES_STAY_IN_HIVE_GAMEPLAY, true)
		.build();

	private static final Set<EnvironmentAttribute<?>> ATTRIBUTES =
		Sets.union(RAIN_EFFECTS.keySet(), THUNDER_EFFECTS.keySet());

	/**
	 * Добавляет временны́е модификаторы погоды для всех затронутых атрибутов в билдер.
	 * Каждый атрибут получает функцию, интерполирующую между базовым значением
	 * и эффектами дождя/грозы по их градиентам.
	 *
	 * @param builder билдер атрибутов мира
	 * @param weather источник градиентов погоды
	 */
	public static void addWeatherAttributes(
		WorldEnvironmentAttributeAccess.Builder builder,
		WeatherAccess weather
	) {
		for (EnvironmentAttribute<?> attribute : ATTRIBUTES) {
			addWeatherAttribute(builder, weather, attribute);
		}
	}

	private static <Value> void addWeatherAttribute(
		WorldEnvironmentAttributeAccess.Builder builder,
		WeatherAccess weather,
		EnvironmentAttribute<Value> attribute
	) {
		EnvironmentAttributeMap.Entry<Value, ?> rainEntry = RAIN_EFFECTS.getEntry(attribute);
		EnvironmentAttributeMap.Entry<Value, ?> thunderEntry = THUNDER_EFFECTS.getEntry(attribute);

		builder.timeBased(attribute, (value, time) -> {
			float thunderGradient = weather.getThunderGradient();
			float rainOnlyGradient = weather.getRainGradient() - thunderGradient;

			if (rainEntry != null && rainOnlyGradient > 0.0F) {
				Value rainValue = rainEntry.apply(value);
				value = attribute.getType().stateChangeLerp().apply(rainOnlyGradient, value, rainValue);
			}

			if (thunderEntry != null && thunderGradient > 0.0F) {
				Value thunderValue = thunderEntry.apply(value);
				value = attribute.getType().stateChangeLerp().apply(thunderGradient, value, thunderValue);
			}

			return value;
		});
	}

	/**
	 * Источник данных о текущей погоде: градиенты дождя и грозы в диапазоне [0.0; 1.0].
	 */
	public interface WeatherAccess {

		/**
		 * Создаёт реализацию на основе реального мира.
		 *
		 * @param world мир, из которого берутся градиенты погоды
		 * @return реализация {@link WeatherAccess}
		 */
		static WeatherAccess ofWorld(World world) {
			return new WeatherAccess() {
				@Override
				public float getRainGradient() {
					return world.getRainGradient(1.0F);
				}

				@Override
				public float getThunderGradient() {
					return world.getThunderGradient(1.0F);
				}
			};
		}

		float getRainGradient();

		float getThunderGradient();
	}
}
