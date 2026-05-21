package net.minecraft.server.function;

import com.mojang.serialization.Codec;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * {@code LazyContainer}.
 */
public class LazyContainer {

	public static final Codec<LazyContainer> CODEC = Identifier.CODEC.xmap(LazyContainer::new, LazyContainer::getId);
	private final Identifier id;
	private boolean initialized;
	private Optional<CommandFunction<ServerCommandSource>> function = Optional.empty();

	public LazyContainer(Identifier id) {
		this.id = id;
	}

	public Optional<CommandFunction<ServerCommandSource>> get(CommandFunctionManager commandFunctionManager) {
		if (!this.initialized) {
			this.function = commandFunctionManager.getFunction(this.id);
			this.initialized = true;
		}

		return this.function;
	}

	public Identifier getId() {
		return this.id;
	}

	@Override
	public boolean equals(Object o) {
		return o == this ? true
		                 : o instanceof LazyContainer lazyContainer && this.getId().equals(lazyContainer.getId());
	}
}
