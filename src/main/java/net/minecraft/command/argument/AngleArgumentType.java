package net.minecraft.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Collection;

/**
 * Тип аргумента команды для разбора угла в градусах.
 * Поддерживает абсолютные значения ({@code 45}) и относительные ({@code ~}, {@code ~-5}).
 * Относительный угол прибавляется к текущему повороту источника команды по оси Y.
 */
public class AngleArgumentType implements ArgumentType<AngleArgumentType.Angle> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0", "~", "~-5");

	public static final SimpleCommandExceptionType INCOMPLETE_ANGLE_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.angle.incomplete"));

	public static final SimpleCommandExceptionType INVALID_ANGLE_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.angle.invalid"));

	public static AngleArgumentType angle() {
		return new AngleArgumentType();
	}

	public static float getAngle(CommandContext<ServerCommandSource> context, String name) {
		return ((AngleArgumentType.Angle) context.getArgument(name, AngleArgumentType.Angle.class))
				.getAngle((ServerCommandSource) context.getSource());
	}

	@Override
	public AngleArgumentType.Angle parse(StringReader reader) throws CommandSyntaxException {
		if (!reader.canRead()) {
			throw INCOMPLETE_ANGLE_EXCEPTION.createWithContext(reader);
		}

		boolean relative = CoordinateArgument.isRelative(reader);
		float value = reader.canRead() && reader.peek() != ' ' ? reader.readFloat() : 0.0F;

		if (Float.isNaN(value) || Float.isInfinite(value)) {
			throw INVALID_ANGLE_EXCEPTION.createWithContext(reader);
		}

		return new AngleArgumentType.Angle(value, relative);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	/**
	 * Разобранный угол с флагом относительности.
	 * При вычислении итогового значения относительный угол прибавляется к повороту
	 * источника команды по оси Y и нормализуется в диапазон [-180, 180].
	 */
	public static final class Angle {

		private final float angle;
		private final boolean relative;

		Angle(float angle, boolean relative) {
			this.angle = angle;
			this.relative = relative;
		}

		public float getAngle(ServerCommandSource source) {
			return MathHelper.wrapDegrees(relative ? angle + source.getRotation().y : angle);
		}

	}

}
