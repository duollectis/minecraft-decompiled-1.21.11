package net.minecraft.server.world;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Класс Chunk Ticket Type.
 */
public record ChunkTicketType(long expiryTicks, @ChunkTicketType.Flags int flags) {

	public static final long NO_EXPIRATION = 0L;
	public static final int SERIALIZE = 1;
	public static final int FOR_LOADING = 2;
	public static final int FOR_SIMULATION = 4;
	public static final int RESETS_IDLE_TIMEOUT = 8;
	public static final int CAN_EXPIRE_BEFORE_LOAD = 16;
	public static final ChunkTicketType PLAYER_SPAWN = register("player_spawn", 20L, 2);
	public static final ChunkTicketType SPAWN_SEARCH = register("spawn_search", 1L, 2);
	public static final ChunkTicketType DRAGON = register("dragon", 0L, 6);
	public static final ChunkTicketType PLAYER_LOADING = register("player_loading", 0L, 2);
	public static final ChunkTicketType PLAYER_SIMULATION = register("player_simulation", 0L, 12);
	public static final ChunkTicketType FORCED = register("forced", 0L, 15);
	public static final ChunkTicketType PORTAL = register("portal", 300L, 15);
	public static final ChunkTicketType ENDER_PEARL = register("ender_pearl", 40L, 14);
	public static final ChunkTicketType UNKNOWN = register("unknown", 1L, 18);

	private static ChunkTicketType register(String id, long expiryTicks, @ChunkTicketType.Flags int flags) {
		return Registry.register(Registries.TICKET_TYPE, id, new ChunkTicketType(expiryTicks, flags));
	}

	/**
	 * Определяет, следует ли serialize.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldSerialize() {
		return (this.flags & 1) != 0;
	}

	public boolean isForLoading() {
		return (this.flags & 2) != 0;
	}

	public boolean isForSimulation() {
		return (this.flags & 4) != 0;
	}

	/**
	 * Сбрасывает s idle timeout.
	 *
	 * @return boolean — результат операции
	 */
	public boolean resetsIdleTimeout() {
		return (this.flags & 8) != 0;
	}

	/**
	 * Проверяет возможность expire before load.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canExpireBeforeLoad() {
		return (this.flags & CAN_EXPIRE_BEFORE_LOAD) != 0;
	}

	/**
	 * Проверяет возможность expire.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canExpire() {
		return this.expiryTicks != 0L;
	}

	@Retention(RetentionPolicy.CLASS)
	@Target(
			{
					ElementType.FIELD,
					ElementType.PARAMETER,
					ElementType.LOCAL_VARIABLE,
					ElementType.METHOD,
					ElementType.TYPE_USE
			}
	)
	public @interface Flags {
	}
}
