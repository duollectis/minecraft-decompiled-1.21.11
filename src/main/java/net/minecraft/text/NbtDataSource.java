package net.minecraft.text;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;

import java.util.stream.Stream;

/**
 * Источник NBT-данных для текстового компонента {@link NbtTextContent}.
 *
 * <p>Определяет три реализации: блок ({@link BlockNbtDataSource}),
 * сущность ({@link EntityNbtDataSource}) и хранилище данных ({@link StorageNbtDataSource}).
 * Каждая реализация знает, как получить поток {@link NbtCompound} из своего источника
 * в контексте конкретного источника команды.</p>
 */
public interface NbtDataSource {

	/**
	 * Возвращает поток NBT-соединений из данного источника.
	 *
	 * @param source источник команды, предоставляющий доступ к миру, серверу и т.д.
	 * @return поток {@link NbtCompound}, может быть пустым если источник недоступен
	 * @throws CommandSyntaxException если разрешение источника (например, селектора сущности) завершилось ошибкой
	 */
	Stream<NbtCompound> get(ServerCommandSource source) throws CommandSyntaxException;

	/** @return codec для сериализации конкретной реализации источника */
	MapCodec<? extends NbtDataSource> getCodec();
}
