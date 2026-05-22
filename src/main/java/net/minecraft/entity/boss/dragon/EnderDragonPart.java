package net.minecraft.entity.boss.dragon;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.jspecify.annotations.Nullable;

/**
 * Отдельная хитбокс-часть тела Эндер-дракона (голова, шея, крылья и т.д.).
 * Не сохраняется и не спавнится самостоятельно — весь урон перенаправляется владельцу {@link EnderDragonEntity}.
 */
public class EnderDragonPart extends Entity {

	public final EnderDragonEntity owner;
	public final String name;
	private final EntityDimensions partDimensions;

	public EnderDragonPart(EnderDragonEntity owner, String name, float width, float height) {
		super(owner.getType(), owner.getEntityWorld());
		partDimensions = EntityDimensions.changing(width, height);
		calculateDimensions();
		this.owner = owner;
		this.name = name;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	protected void readCustomData(ReadView view) {
	}

	@Override
	protected void writeCustomData(WriteView view) {
	}

	@Override
	public boolean canHit() {
		return true;
	}

	@Override
	public @Nullable ItemStack getPickBlockStack() {
		return owner.getPickBlockStack();
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isAlwaysInvulnerableTo(source)) {
			return false;
		}

		return owner.damagePart(world, this, source, amount);
	}

	@Override
	public boolean isPartOf(Entity entity) {
		return this == entity || owner == entity;
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityDimensions getDimensions(EntityPose pose) {
		return partDimensions;
	}

	@Override
	public boolean shouldSave() {
		return false;
	}
}
