package net.minecraft.text.object;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.text.StyleSpriteSource;

/**
 * Содержимое встроенного объекта, отображающего голову игрока.
 *
 * <p>Использует {@link ProfileComponent} для получения UUID и текстур скина.
 * Флаг {@code hat} определяет, использовать ли второй слой скина (шляпу).
 * По умолчанию {@code hat = true}, что соответствует стандартному отображению головы.</p>
 */
public record PlayerTextObjectContents(ProfileComponent player, boolean hat) implements TextObjectContents {

	public static final MapCodec<PlayerTextObjectContents> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					ProfileComponent.CODEC.fieldOf("player").forGetter(PlayerTextObjectContents::player),
					Codec.BOOL.optionalFieldOf("hat", true).forGetter(PlayerTextObjectContents::hat)
			)
			.apply(instance, PlayerTextObjectContents::new)
	);

	@Override
	public StyleSpriteSource spriteSource() {
		return new StyleSpriteSource.Player(player, hat);
	}

	@Override
	public String asText() {
		return player.getName()
				.map(name -> "[" + name + " head]")
				.orElse("[unknown player head]");
	}

	@Override
	public MapCodec<PlayerTextObjectContents> getCodec() {
		return CODEC;
	}
}
