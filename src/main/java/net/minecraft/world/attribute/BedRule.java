package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Правило поведения кровати в измерении: определяет, можно ли спать,
 * устанавливать точку возрождения, взрывается ли кровать и какое сообщение
 * показывается при невозможности использования.
 */
public record BedRule(
	Condition canSleep,
	Condition canSetSpawn,
	boolean explodes,
	Optional<Text> errorMessage
) {

	/** Правило Верхнего мира: сон ночью, спавн всегда, без взрыва. */
	public static final BedRule OVERWORLD = new BedRule(
		Condition.WHEN_DARK,
		Condition.ALWAYS,
		false,
		Optional.of(Text.translatable("block.minecraft.bed.no_sleep"))
	);

	/** Правило других измерений: сон и спавн запрещены, кровать взрывается. */
	public static final BedRule OTHER_DIMENSION = new BedRule(
		Condition.NEVER,
		Condition.NEVER,
		true,
		Optional.empty()
	);

	public static final Codec<BedRule> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Condition.CODEC.fieldOf("can_sleep").forGetter(BedRule::canSleep),
			Condition.CODEC.fieldOf("can_set_spawn").forGetter(BedRule::canSetSpawn),
			Codec.BOOL.optionalFieldOf("explodes", false).forGetter(BedRule::explodes),
			TextCodecs.CODEC.optionalFieldOf("error_message").forGetter(BedRule::errorMessage)
		).apply(instance, BedRule::new)
	);

	/** Проверяет, можно ли спать в кровати в данном мире. */
	public boolean canSleep(World world) {
		return canSleep.test(world);
	}

	/** Проверяет, можно ли установить точку возрождения в данном мире. */
	public boolean canSetSpawn(World world) {
		return canSetSpawn.test(world);
	}

	/**
	 * Возвращает причину неудачи при попытке сна.
	 * Если сообщение об ошибке отсутствует — причина без текста.
	 */
	public PlayerEntity.SleepFailureReason getFailureReason() {
		return new PlayerEntity.SleepFailureReason(errorMessage.orElse(null));
	}

	/**
	 * Условие применимости правила кровати в зависимости от состояния мира.
	 */
	public enum Condition implements StringIdentifiable {
		ALWAYS("always"),
		WHEN_DARK("when_dark"),
		NEVER("never");

		public static final Codec<Condition> CODEC = StringIdentifiable.createCodec(Condition::values);

		private final String name;

		Condition(String name) {
			this.name = name;
		}

		/**
		 * Проверяет выполнение условия для данного мира.
		 *
		 * @param world мир, в котором проверяется условие
		 * @return {@code true} если условие выполнено
		 */
		public boolean test(World world) {
			return switch (this) {
				case ALWAYS -> true;
				case WHEN_DARK -> world.isNight();
				case NEVER -> false;
			};
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
