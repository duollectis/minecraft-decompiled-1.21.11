package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Interpolator;

import java.util.Map;

/**
 * Модификатор атрибута окружения: применяет операцию к текущему значению атрибута
 * с заданным аргументом. Каждый тип атрибута имеет свой набор допустимых модификаторов.
 *
 * @param <Subject> тип значения атрибута
 * @param <Argument> тип аргумента операции
 */
public interface EnvironmentAttributeModifier<Subject, Argument> {

	/** Стандартные булевые модификаторы, индексированные по типу. */
	Map<Type, EnvironmentAttributeModifier<Boolean, ?>> BOOLEAN_MODIFIERS = Map.of(
		Type.AND, BooleanModifier.AND,
		Type.NAND, BooleanModifier.NAND,
		Type.OR, BooleanModifier.OR,
		Type.NOR, BooleanModifier.NOR,
		Type.XOR, BooleanModifier.XOR,
		Type.XNOR, BooleanModifier.XNOR
	);

	/** Стандартные числовые модификаторы для float-атрибутов. */
	Map<Type, EnvironmentAttributeModifier<Float, ?>> FLOAT_MODIFIERS = Map.of(
		Type.ALPHA_BLEND, FloatModifier.ALPHA_BLEND,
		Type.ADD, FloatModifier.ADD,
		Type.SUBTRACT, FloatModifier.SUBTRACT,
		Type.MULTIPLY, FloatModifier.MULTIPLY,
		Type.MINIMUM, FloatModifier.MINIMUM,
		Type.MAXIMUM, FloatModifier.MAXIMUM
	);

	/** Стандартные цветовые модификаторы для RGB-атрибутов (без альфа-канала). */
	Map<Type, EnvironmentAttributeModifier<Integer, ?>> RGB = Map.of(
		Type.ALPHA_BLEND, ColorModifier.ALPHA_BLEND,
		Type.ADD, ColorModifier.ADD,
		Type.SUBTRACT, ColorModifier.SUBTRACT,
		Type.MULTIPLY, ColorModifier.MULTIPLY_RGB,
		Type.BLEND_TO_GRAY, ColorModifier.BLEND_TO_GRAY
	);

	/** Стандартные цветовые модификаторы для ARGB-атрибутов (с альфа-каналом). */
	Map<Type, EnvironmentAttributeModifier<Integer, ?>> ARGB = Map.of(
		Type.ALPHA_BLEND, ColorModifier.ALPHA_BLEND,
		Type.ADD, ColorModifier.ADD,
		Type.SUBTRACT, ColorModifier.SUBTRACT,
		Type.MULTIPLY, ColorModifier.MULTIPLY_ARGB,
		Type.BLEND_TO_GRAY, ColorModifier.BLEND_TO_GRAY
	);

	/**
	 * Возвращает синглтон-модификатор перезаписи: просто заменяет текущее значение аргументом.
	 *
	 * @param <Value> тип значения
	 * @return модификатор-перезапись
	 */
	@SuppressWarnings("unchecked")
	static <Value> EnvironmentAttributeModifier<Value, Value> override() {
		return (EnvironmentAttributeModifier<Value, Value>) OverrideModifier.INSTANCE;
	}

	/**
	 * Применяет модификацию к текущему значению атрибута.
	 *
	 * @param value текущее значение
	 * @param argument аргумент операции
	 * @return модифицированное значение
	 */
	Subject apply(Subject value, Argument argument);

	/**
	 * Возвращает codec для аргумента данного модификатора в контексте конкретного атрибута.
	 *
	 * @param attribute атрибут, для которого используется модификатор
	 * @return codec аргумента
	 */
	Codec<Argument> argumentCodec(EnvironmentAttribute<Subject> attribute);

	/**
	 * Возвращает интерполятор для аргументов в ключевых кадрах таймлайна.
	 *
	 * @param attribute атрибут, для которого используется модификатор
	 * @return интерполятор аргументов
	 */
	Interpolator<Argument> argumentKeyframeLerp(EnvironmentAttribute<Subject> attribute);

	/**
	 * Модификатор-перезапись: игнорирует текущее значение и возвращает аргумент.
	 * Используется как значение по умолчанию при сериализации без явного модификатора.
	 *
	 * @param <Value> тип значения
	 */
	record OverrideModifier<Value>() implements EnvironmentAttributeModifier<Value, Value> {

		static final OverrideModifier<?> INSTANCE = new OverrideModifier<>();

		@Override
		public Value apply(Value current, Value argument) {
			return argument;
		}

		@Override
		public Codec<Value> argumentCodec(EnvironmentAttribute<Value> attribute) {
			return attribute.getCodec();
		}

		@Override
		public Interpolator<Value> argumentKeyframeLerp(EnvironmentAttribute<Value> attribute) {
			return attribute.getType().keyframeLerp();
		}
	}

	/**
	 * Перечисление всех поддерживаемых типов модификаторов.
	 * Используется как ключ в библиотеках модификаторов и при сериализации.
	 */
	enum Type implements StringIdentifiable {
		OVERRIDE("override"),
		ALPHA_BLEND("alpha_blend"),
		ADD("add"),
		SUBTRACT("subtract"),
		MULTIPLY("multiply"),
		BLEND_TO_GRAY("blend_to_gray"),
		MINIMUM("minimum"),
		MAXIMUM("maximum"),
		AND("and"),
		NAND("nand"),
		OR("or"),
		NOR("nor"),
		XOR("xor"),
		XNOR("xnor");

		public static final Codec<Type> CODEC = StringIdentifiable.createCodec(Type::values);

		private final String name;

		Type(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
