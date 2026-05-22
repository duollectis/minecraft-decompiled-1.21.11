package net.minecraft.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.PlayerInput;

import java.util.Optional;

/**
 * Предикат для проверки состояния клавиш управления игрока.
 * Каждое поле — опциональное: {@code Optional.empty()} означает «не проверять».
 */
public record InputPredicate(
		Optional<Boolean> forward,
		Optional<Boolean> backward,
		Optional<Boolean> left,
		Optional<Boolean> right,
		Optional<Boolean> jump,
		Optional<Boolean> sneak,
		Optional<Boolean> sprint
) {

	public static final Codec<InputPredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.BOOL.optionalFieldOf("forward").forGetter(InputPredicate::forward),
					Codec.BOOL.optionalFieldOf("backward").forGetter(InputPredicate::backward),
					Codec.BOOL.optionalFieldOf("left").forGetter(InputPredicate::left),
					Codec.BOOL.optionalFieldOf("right").forGetter(InputPredicate::right),
					Codec.BOOL.optionalFieldOf("jump").forGetter(InputPredicate::jump),
					Codec.BOOL.optionalFieldOf("sneak").forGetter(InputPredicate::sneak),
					Codec.BOOL.optionalFieldOf("sprint").forGetter(InputPredicate::sprint)
			)
			.apply(instance, InputPredicate::new)
	);

	public boolean matches(PlayerInput playerInput) {
		return keyMatches(forward, playerInput.forward())
				&& keyMatches(backward, playerInput.backward())
				&& keyMatches(left, playerInput.left())
				&& keyMatches(right, playerInput.right())
				&& keyMatches(jump, playerInput.jump())
				&& keyMatches(sneak, playerInput.sneak())
				&& keyMatches(sprint, playerInput.sprint());
	}

	private boolean keyMatches(Optional<Boolean> expected, boolean actual) {
		return expected.map(pressed -> pressed == actual).orElse(true);
	}
}
