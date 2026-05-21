package net.minecraft.network.packet.s2c.play;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Класс entity status effect s2 c packet.
 */
public class EntityStatusEffectS2CPacket implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<RegistryByteBuf, EntityStatusEffectS2CPacket> CODEC = Packet.createCodec(
			EntityStatusEffectS2CPacket::write, EntityStatusEffectS2CPacket::new
	);
	private static final int AMBIENT_MASK = 1;
	private static final int SHOW_PARTICLES_MASK = 2;
	private static final int SHOW_ICON_MASK = 4;
	private static final int KEEP_FADING_MASK = 8;
	private final int entityId;
	private final RegistryEntry<StatusEffect> effectId;
	private final int amplifier;
	private final int duration;
	private final byte flags;

	public EntityStatusEffectS2CPacket(int entityId, StatusEffectInstance effect, boolean keepFading) {
		this.entityId = entityId;
		this.effectId = effect.getEffectType();
		this.amplifier = effect.getAmplifier();
		this.duration = effect.getDuration();
		byte b = 0;
		if (effect.isAmbient()) {
			b = (byte) (b | 1);
		}

		if (effect.shouldShowParticles()) {
			b = (byte) (b | 2);
		}

		if (effect.shouldShowIcon()) {
			b = (byte) (b | 4);
		}

		if (keepFading) {
			b = (byte) (b | 8);
		}

		this.flags = b;
	}

	private EntityStatusEffectS2CPacket(RegistryByteBuf buf) {
		this.entityId = buf.readVarInt();
		this.effectId = StatusEffect.ENTRY_PACKET_CODEC.decode(buf);
		this.amplifier = buf.readVarInt();
		this.duration = buf.readVarInt();
		this.flags = buf.readByte();
	}

	private void write(RegistryByteBuf buf) {
		buf.writeVarInt(this.entityId);
		StatusEffect.ENTRY_PACKET_CODEC.encode(buf, this.effectId);
		buf.writeVarInt(this.amplifier);
		buf.writeVarInt(this.duration);
		buf.writeByte(this.flags);
	}

	@Override
	public PacketType<EntityStatusEffectS2CPacket> getPacketType() {
		return PlayPackets.UPDATE_MOB_EFFECT;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onEntityStatusEffect(this);
	}

	public int getEntityId() {
		return this.entityId;
	}

	public RegistryEntry<StatusEffect> getEffectId() {
		return this.effectId;
	}

	public int getAmplifier() {
		return this.amplifier;
	}

	public int getDuration() {
		return this.duration;
	}

	/**
	 * Определяет, следует ли show particles.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldShowParticles() {
		return (this.flags & 2) != 0;
	}

	public boolean isAmbient() {
		return (this.flags & 1) != 0;
	}

	/**
	 * Определяет, следует ли show icon.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldShowIcon() {
		return (this.flags & 4) != 0;
	}

	/**
	 * Keep fading.
	 *
	 * @return boolean — результат операции
	 */
	public boolean keepFading() {
		return (this.flags & 8) != 0;
	}
}
