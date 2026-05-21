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
 * {@code BedRule}.
 */
public record BedRule(
		BedRule.Condition canSleep,
		BedRule.Condition canSetSpawn,
		boolean explodes,
		Optional<Text> errorMessage
) {

	public static final BedRule OVERWORLD = new BedRule(
			BedRule.Condition.WHEN_DARK,
			BedRule.Condition.ALWAYS,
			false,
			Optional.of(Text.translatable("block.minecraft.bed.no_sleep"))
	);
	public static final BedRule
			OTHER_DIMENSION =
			new BedRule(BedRule.Condition.NEVER, BedRule.Condition.NEVER, true, Optional.empty());
	public static final Codec<BedRule> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    BedRule.Condition.CODEC.fieldOf("can_sleep").forGetter(BedRule::canSleep),
					                    BedRule.Condition.CODEC.fieldOf("can_set_spawn").forGetter(BedRule::canSetSpawn),
					                    Codec.BOOL.optionalFieldOf("explodes", false).forGetter(BedRule::explodes),
					                    TextCodecs.CODEC.optionalFieldOf("error_message").forGetter(BedRule::errorMessage)
			                    )
			                    .apply(instance, BedRule::new)
	);

	public boolean canSleep(World world) {
		return this.canSleep.test(world);
	}

	public boolean canSetSpawn(World world) {
		return this.canSetSpawn.test(world);
	}

	public PlayerEntity.SleepFailureReason getFailureReason() {
		return new PlayerEntity.SleepFailureReason(this.errorMessage.orElse(null));
	}

	/**
	 * {@code Condition}.
	 */
	public static enum Condition implements StringIdentifiable {
		ALWAYS("always"),
		WHEN_DARK("when_dark"),
		NEVER("never");

		public static final Codec<BedRule.Condition> CODEC = StringIdentifiable.createCodec(BedRule.Condition::values);
		private final String name;

		private Condition(final String name) {
			this.name = name;
		}

		public boolean test(World world) {
			return switch (this) {
				case ALWAYS -> true;
				case WHEN_DARK -> world.isNight();
				case NEVER -> false;
			};
		}

		@Override
		public String asString() {
			return this.name;
		}
	}
}
