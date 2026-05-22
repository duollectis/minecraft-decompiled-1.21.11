package net.minecraft.entity.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.IntFunction;

/**
 * Базовый класс для иллагеров, использующих заклинания.
 */
public abstract class SpellcastingIllagerEntity extends IllagerEntity {

	private static final int INITIAL_SPELL_COOLDOWN = 20;
	private static final TrackedData<Byte>
			SPELL =
			DataTracker.registerData(SpellcastingIllagerEntity.class, TrackedDataHandlerRegistry.BYTE);
	protected int spellTicks;
	private SpellcastingIllagerEntity.Spell spell = SpellcastingIllagerEntity.Spell.NONE;

	protected SpellcastingIllagerEntity(EntityType<? extends SpellcastingIllagerEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(SPELL, (byte) 0);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		spellTicks = view.getInt("SpellTicks", 0);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("SpellTicks", spellTicks);
	}

	@Override
	public IllagerEntity.State getState() {
		if (isSpellcasting()) {
			return IllagerEntity.State.SPELLCASTING;
		}

		return isCelebrating() ? IllagerEntity.State.CELEBRATING : IllagerEntity.State.CROSSED;
	}

	public boolean isSpellcasting() {
		return getEntityWorld().isClient() ? dataTracker.get(SPELL) > 0 : spellTicks > 0;
	}

	public void setSpell(SpellcastingIllagerEntity.Spell spell) {
		this.spell = spell;
		dataTracker.set(SPELL, (byte) spell.id);
	}

	protected SpellcastingIllagerEntity.Spell getSpell() {
		return getEntityWorld().isClient()
				? SpellcastingIllagerEntity.Spell.byId(dataTracker.get(SPELL))
				: spell;
	}

	@Override
	protected void mobTick(ServerWorld world) {
		super.mobTick(world);
		if (spellTicks > 0) {
			spellTicks--;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!getEntityWorld().isClient() || !isSpellcasting()) {
			return;
		}

		SpellcastingIllagerEntity.Spell currentSpell = getSpell();
		float colorR = (float) currentSpell.particleVelocity[0];
		float colorG = (float) currentSpell.particleVelocity[1];
		float colorB = (float) currentSpell.particleVelocity[2];
		float swingAngle = bodyYaw * (float) (Math.PI / 180.0) + MathHelper.cos(age * 0.6662F) * 0.25F;
		float cosAngle = MathHelper.cos(swingAngle);
		float sinAngle = MathHelper.sin(swingAngle);
		double handOffset = 0.6 * getScale();
		double handHeight = 1.8 * getScale();
		TintedParticleEffect particleEffect = TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, colorR, colorG, colorB);

		getEntityWorld().addParticleClient(
				particleEffect,
				getX() + cosAngle * handOffset,
				getY() + handHeight,
				getZ() + sinAngle * handOffset,
				0.0,
				0.0,
				0.0
		);
		getEntityWorld().addParticleClient(
				particleEffect,
				getX() - cosAngle * handOffset,
				getY() + handHeight,
				getZ() - sinAngle * handOffset,
				0.0,
				0.0,
				0.0
		);
	}

	protected int getSpellTicks() {
		return spellTicks;
	}

	protected abstract SoundEvent getCastSpellSound();

	protected abstract class CastSpellGoal extends Goal {

		protected int spellCooldown;
		protected int startTime;

		@Override
		public boolean canStart() {
			LivingEntity target = SpellcastingIllagerEntity.this.getTarget();
			if (target == null || !target.isAlive()) {
				return false;
			}

			if (SpellcastingIllagerEntity.this.isSpellcasting()) {
				return false;
			}

			return SpellcastingIllagerEntity.this.age >= startTime;
		}

		@Override
		public boolean shouldContinue() {
			LivingEntity target = SpellcastingIllagerEntity.this.getTarget();
			return target != null && target.isAlive() && spellCooldown > 0;
		}

		@Override
		public void start() {
			spellCooldown = getTickCount(getInitialCooldown());
			SpellcastingIllagerEntity.this.spellTicks = getSpellTicks();
			startTime = SpellcastingIllagerEntity.this.age + startTimeDelay();
			SoundEvent prepareSound = getSoundPrepare();

			if (prepareSound != null) {
				SpellcastingIllagerEntity.this.playSound(prepareSound, 1.0F, 1.0F);
			}

			SpellcastingIllagerEntity.this.setSpell(getSpell());
		}

		@Override
		public void tick() {
			spellCooldown--;
			if (spellCooldown == 0) {
				castSpell();
				SpellcastingIllagerEntity.this.playSound(
						SpellcastingIllagerEntity.this.getCastSpellSound(),
						1.0F,
						1.0F
				);
			}
		}

		protected abstract void castSpell();

		protected int getInitialCooldown() {
			return INITIAL_SPELL_COOLDOWN;
		}

		protected abstract int getSpellTicks();

		protected abstract int startTimeDelay();

		protected abstract @Nullable SoundEvent getSoundPrepare();

		protected abstract SpellcastingIllagerEntity.Spell getSpell();
	}

	protected class LookAtTargetGoal extends Goal {

		public LookAtTargetGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			return SpellcastingIllagerEntity.this.getSpellTicks() > 0;
		}

		@Override
		public void start() {
			super.start();
			SpellcastingIllagerEntity.this.navigation.stop();
		}

		@Override
		public void stop() {
			super.stop();
			SpellcastingIllagerEntity.this.setSpell(SpellcastingIllagerEntity.Spell.NONE);
		}

		@Override
		public void tick() {
			LivingEntity target = SpellcastingIllagerEntity.this.getTarget();
			if (target == null) {
				return;
			}

			SpellcastingIllagerEntity.this
					.getLookControl()
					.lookAt(
							target,
							SpellcastingIllagerEntity.this.getMaxHeadRotation(),
							SpellcastingIllagerEntity.this.getMaxLookPitchChange()
					);
		}
	}

	protected enum Spell {
		NONE(0, 0.0, 0.0, 0.0),
		SUMMON_VEX(1, 0.7, 0.7, 0.8),
		FANGS(2, 0.4, 0.3, 0.35),
		WOLOLO(3, 0.7, 0.5, 0.2),
		DISAPPEAR(4, 0.3, 0.3, 0.8),
		BLINDNESS(5, 0.1, 0.1, 0.2);

		private static final IntFunction<SpellcastingIllagerEntity.Spell> BY_ID = ValueLists.createIndexToValueFunction(
				(SpellcastingIllagerEntity.Spell spell) -> spell.id, values(), ValueLists.OutOfBoundsHandling.ZERO
		);
		final int id;
		final double[] particleVelocity;

		private Spell(
				final int id,
				final double particleVelocityX,
				final double particleVelocityY,
				final double particleVelocityZ
		) {
			this.id = id;
			this.particleVelocity = new double[]{particleVelocityX, particleVelocityY, particleVelocityZ};
		}

		public static SpellcastingIllagerEntity.Spell byId(int id) {
			return BY_ID.apply(id);
		}
	}
}
