package net.minecraft.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.server.command.ServerCommandSource;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Источник NBT-данных, читающий данные из сущностей, найденных по селектору.
 *
 * <p>Селектор парсится из строки при создании объекта. Если строка невалидна,
 * поле {@code selector} будет {@code null} и {@link #get} вернёт пустой поток.
 * Сравнение экземпляров выполняется только по строке {@code rawSelector},
 * так как {@code selector} является производным полем.</p>
 */
public record EntityNbtDataSource(String rawSelector, @Nullable EntitySelector selector) implements NbtDataSource {

	public static final MapCodec<EntityNbtDataSource> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codec.STRING.fieldOf("entity").forGetter(EntityNbtDataSource::rawSelector))
					.apply(instance, EntityNbtDataSource::new)
	);

	public EntityNbtDataSource(String rawSelector) {
		this(rawSelector, parseSelector(rawSelector));
	}

	private static @Nullable EntitySelector parseSelector(String rawSelector) {
		try {
			EntitySelectorReader reader = new EntitySelectorReader(new StringReader(rawSelector), true);
			return reader.read();
		}
		catch (CommandSyntaxException e) {
			return null;
		}
	}

	@Override
	public Stream<NbtCompound> get(ServerCommandSource source) throws CommandSyntaxException {
		if (selector == null) {
			return Stream.empty();
		}

		List<? extends Entity> entities = selector.getEntities(source);
		return entities.stream().map(NbtPredicate::entityToNbt);
	}

	@Override
	public MapCodec<EntityNbtDataSource> getCodec() {
		return CODEC;
	}

	@Override
	public String toString() {
		return "entity=" + rawSelector;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof EntityNbtDataSource other && rawSelector.equals(other.rawSelector);
	}

	@Override
	public int hashCode() {
		return rawSelector.hashCode();
	}
}
