package net.minecraft.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Невидимая сущность-триггер для отслеживания атак и взаимодействий игроков.
 * Не получает урона, не имеет физики. Размер задаётся через DataTracker.
 * Хранит UUID последнего атаковавшего и последнего взаимодействовавшего игрока.
 */
public class InteractionEntity extends Entity implements Attackable, Targeter {

	private static final TrackedData<Float> WIDTH =
			DataTracker.registerData(InteractionEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> HEIGHT =
			DataTracker.registerData(InteractionEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Boolean> RESPONSE =
			DataTracker.registerData(InteractionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private static final float DEFAULT_WIDTH = 1.0F;
	private static final float DEFAULT_HEIGHT = 1.0F;

	private @Nullable Interaction attack;
	private @Nullable Interaction interaction;

	public InteractionEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
		noClip = true;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(WIDTH, DEFAULT_WIDTH);
		builder.add(HEIGHT, DEFAULT_HEIGHT);
		builder.add(RESPONSE, false);
	}

	@Override
	protected void readCustomData(ReadView view) {
		setInteractionWidth(view.getFloat("width", DEFAULT_WIDTH));
		setInteractionHeight(view.getFloat("height", DEFAULT_HEIGHT));
		attack = view.<Interaction>read("attack", Interaction.CODEC).orElse(null);
		interaction = view.<Interaction>read("interaction", Interaction.CODEC).orElse(null);
		setResponse(view.getBoolean("response", false));
		setBoundingBox(calculateBoundingBox());
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putFloat("width", getInteractionWidth());
		view.putFloat("height", getInteractionHeight());
		view.putNullable("attack", Interaction.CODEC, attack);
		view.putNullable("interaction", Interaction.CODEC, interaction);
		view.putBoolean("response", shouldRespond());
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (HEIGHT.equals(data) || WIDTH.equals(data)) {
			calculateDimensions();
		}
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public boolean canHit() {
		return true;
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.IGNORE;
	}

	@Override
	public boolean canAvoidTraps() {
		return true;
	}

	/**
	 * Записывает атаку игрока и при необходимости триггерит критерий достижения.
	 * Возвращает {@code false} (поглощает событие), если сущность должна реагировать.
	 */
	@Override
	public boolean handleAttack(Entity attacker) {
		if (!(attacker instanceof PlayerEntity playerEntity)) {
			return false;
		}

		attack = new Interaction(playerEntity.getUuid(), getEntityWorld().getTime());
		if (playerEntity instanceof ServerPlayerEntity serverPlayerEntity) {
			Criteria.PLAYER_HURT_ENTITY.trigger(
					serverPlayerEntity,
					this,
					playerEntity.getDamageSources().generic(),
					1.0F,
					1.0F,
					false
			);
		}

		return !shouldRespond();
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (getEntityWorld().isClient()) {
			return shouldRespond() ? ActionResult.SUCCESS : ActionResult.CONSUME;
		}

		interaction = new Interaction(player.getUuid(), getEntityWorld().getTime());
		return ActionResult.CONSUME;
	}

	@Override
	public void tick() {
	}

	@Override
	public @Nullable LivingEntity getLastAttacker() {
		return attack != null ? getEntityWorld().getPlayerByUuid(attack.player()) : null;
	}

	@Override
	public @Nullable LivingEntity getTarget() {
		return interaction != null ? getEntityWorld().getPlayerByUuid(interaction.player()) : null;
	}

	public final void setInteractionWidth(float width) {
		dataTracker.set(WIDTH, width);
	}

	public final float getInteractionWidth() {
		return dataTracker.get(WIDTH);
	}

	public final void setInteractionHeight(float height) {
		dataTracker.set(HEIGHT, height);
	}

	public final float getInteractionHeight() {
		return dataTracker.get(HEIGHT);
	}

	public final void setResponse(boolean response) {
		dataTracker.set(RESPONSE, response);
	}

	public final boolean shouldRespond() {
		return dataTracker.get(RESPONSE);
	}

	private EntityDimensions getDimensions() {
		return EntityDimensions.changing(getInteractionWidth(), getInteractionHeight());
	}

	@Override
	public EntityDimensions getDimensions(EntityPose pose) {
		return getDimensions();
	}

	@Override
	protected Box calculateDefaultBoundingBox(Vec3d pos) {
		return getDimensions().getBoxAt(pos);
	}

	/**
	 * Запись о взаимодействии: UUID игрока и временная метка тика.
	 */
	record Interaction(UUID player, long timestamp) {

		public static final Codec<Interaction> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Uuids.INT_STREAM_CODEC.fieldOf("player").forGetter(Interaction::player),
						Codec.LONG.fieldOf("timestamp").forGetter(Interaction::timestamp)
				)
				.apply(instance, Interaction::new)
		);
	}
}
