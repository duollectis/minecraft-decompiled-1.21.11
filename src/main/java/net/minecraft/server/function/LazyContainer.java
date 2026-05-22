package net.minecraft.server.function;

import com.mojang.serialization.Codec;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Ленивый контейнер для {@link CommandFunction}: загружает функцию по идентификатору
 * при первом обращении через {@link CommandFunctionManager}.
 */
public class LazyContainer {

	public static final Codec<LazyContainer> CODEC = Identifier.CODEC.xmap(LazyContainer::new, LazyContainer::getId);

	private final Identifier id;
	private boolean initialized;
	private Optional<CommandFunction<ServerCommandSource>> function = Optional.empty();

	public LazyContainer(Identifier id) {
		this.id = id;
	}

	public Optional<CommandFunction<ServerCommandSource>> get(CommandFunctionManager manager) {
		if (!initialized) {
			function = manager.getFunction(id);
			initialized = true;
		}

		return function;
	}

	public Identifier getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		return o instanceof LazyContainer other && id.equals(other.getId());
	}
}
