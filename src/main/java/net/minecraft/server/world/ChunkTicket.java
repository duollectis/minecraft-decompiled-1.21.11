package net.minecraft.server.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;

/**
 * Класс Chunk Ticket.
 */
public class ChunkTicket {

	public static final MapCodec<ChunkTicket> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Registries.TICKET_TYPE.getCodec().fieldOf("type").forGetter(ChunkTicket::getType),
					                    Codecs.NON_NEGATIVE_INT.fieldOf("level").forGetter(ChunkTicket::getLevel),
					                    Codec.LONG.optionalFieldOf("ticks_left", 0L).forGetter(ticket -> ticket.ticksLeft)
			                    )
			                    .apply(instance, ChunkTicket::new)
	);
	private final ChunkTicketType type;
	private final int level;
	private long ticksLeft;

	public ChunkTicket(ChunkTicketType type, int level) {
		this(type, level, type.expiryTicks());
	}

	private ChunkTicket(ChunkTicketType type, int level, long ticksLeft) {
		this.type = type;
		this.level = level;
		this.ticksLeft = ticksLeft;
	}

	@Override
	public String toString() {
		return this.type.canExpire()
		       ? "Ticket["
		         + Util.registryValueToString(Registries.TICKET_TYPE, this.type)
		         + " "
		         + this.level
		         + "] with "
		         + this.ticksLeft
		         + " ticks left ( out of"
		         + this.type.expiryTicks()
		         + ")"
		       : "Ticket[" + Util.registryValueToString(Registries.TICKET_TYPE, this.type) + " " + this.level
		         + "] with no timeout";
	}

	public ChunkTicketType getType() {
		return this.type;
	}

	public int getLevel() {
		return this.level;
	}

	/**
	 * Refresh expiry.
	 */
	public void refreshExpiry() {
		this.ticksLeft = this.type.expiryTicks();
	}

	public void tick() {
		if (this.type.canExpire()) {
			this.ticksLeft--;
		}
	}

	public boolean isExpired() {
		return this.type.canExpire() && this.ticksLeft < 0L;
	}
}
