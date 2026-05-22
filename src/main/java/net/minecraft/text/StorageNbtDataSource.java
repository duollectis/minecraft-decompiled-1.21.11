package net.minecraft.text;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.stream.Stream;

/**
 * Источник NBT-данных, читающий данные из командного хранилища данных (data storage) по идентификатору.
 *
 * <p>Хранилище данных — это глобальное серверное хранилище NBT-тегов,
 * доступное через команду {@code /data storage}. Всегда возвращает ровно один
 * {@link NbtCompound} (возможно пустой, если хранилище не содержит данных по этому ключу).</p>
 */
public record StorageNbtDataSource(Identifier id) implements NbtDataSource {

	public static final MapCodec<StorageNbtDataSource> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Identifier.CODEC.fieldOf("storage").forGetter(StorageNbtDataSource::id))
					.apply(instance, StorageNbtDataSource::new)
	);

	@Override
	public Stream<NbtCompound> get(ServerCommandSource source) {
		NbtCompound nbt = source.getServer().getDataCommandStorage().get(id);
		return Stream.of(nbt);
	}

	@Override
	public MapCodec<StorageNbtDataSource> getCodec() {
		return CODEC;
	}

	@Override
	public String toString() {
		return "storage=" + id;
	}
}
