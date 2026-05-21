package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code CompassState}.
 */
public class CompassState extends NeedleAngleState {

	public static final MapCodec<CompassState> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleAngleState::hasWobble),
					                    CompassState.Target.CODEC.fieldOf("target").forGetter(CompassState::getTarget)
			                    )
			                    .apply(instance, CompassState::new)
	);
	private final NeedleAngleState.Angler aimedAngler;
	private final NeedleAngleState.Angler aimlessAngler;
	private final CompassState.Target target;
	private final Random random = Random.create();

	public CompassState(boolean wobble, CompassState.Target target) {
		super(wobble);
		this.aimedAngler = this.createAngler(0.8F);
		this.aimlessAngler = this.createAngler(0.8F);
		this.target = target;
	}

	@Override
	protected float getAngle(ItemStack stack, ClientWorld world, int seed, HeldItemContext context) {
		GlobalPos globalPos = this.target.getPosition(world, stack, context);
		long l = world.getTime();
		return !canPointTo(context, globalPos) ? this.getAimlessAngle(seed, l)
		                                       : this.getAngleTo(context, l, globalPos.pos());
	}

	private float getAimlessAngle(int seed, long time) {
		if (this.aimlessAngler.shouldUpdate(time)) {
			this.aimlessAngler.update(time, this.random.nextFloat());
		}

		float f = this.aimlessAngler.getAngle() + scatter(seed) / 2.1474836E9F;
		return MathHelper.floorMod(f, 1.0F);
	}

	private float getAngleTo(HeldItemContext from, long time, BlockPos to) {
		float f = (float) getAngleTo(from, to);
		float g = getBodyYaw(from);
		float h;
		if (from.getEntity() instanceof PlayerEntity playerEntity && playerEntity.isMainPlayer() && playerEntity
				.getEntityWorld()
				.getTickManager()
				.shouldTick()) {
			if (this.aimedAngler.shouldUpdate(time)) {
				this.aimedAngler.update(time, 0.5F - (g - 0.25F));
			}

			h = f + this.aimedAngler.getAngle();
		}
		else {
			h = 0.5F - (g - 0.25F - f);
		}

		return MathHelper.floorMod(h, 1.0F);
	}

	private static boolean canPointTo(HeldItemContext from, @Nullable GlobalPos to) {
		return to != null && to.dimension() == from.getEntityWorld().getRegistryKey() && !(
				to.pos().getSquaredDistance(from.getEntityPos()) < 1.0E-5F
		);
	}

	private static double getAngleTo(HeldItemContext from, BlockPos to) {
		Vec3d vec3d = Vec3d.ofCenter(to);
		Vec3d vec3d2 = from.getEntityPos();
		return Math.atan2(vec3d.getZ() - vec3d2.getZ(), vec3d.getX() - vec3d2.getX()) / (float) (Math.PI * 2);
	}

	private static float getBodyYaw(HeldItemContext context) {
		return MathHelper.floorMod(context.getBodyYaw() / 360.0F, 1.0F);
	}

	private static int scatter(int seed) {
		return seed * 1327217883;
	}

	protected CompassState.Target getTarget() {
		return this.target;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Target}.
	 */
	public static enum Target implements StringIdentifiable {
		NONE("none") {
			@Override
			public @Nullable GlobalPos getPosition(
					ClientWorld world,
					ItemStack stack,
					@Nullable HeldItemContext context
			) {
				return null;
			}
		},
		LODESTONE("lodestone") {
			@Override
			public @Nullable GlobalPos getPosition(
					ClientWorld world,
					ItemStack stack,
					@Nullable HeldItemContext context
			) {
				LodestoneTrackerComponent lodestoneTrackerComponent = stack.get(DataComponentTypes.LODESTONE_TRACKER);
				return lodestoneTrackerComponent != null ? lodestoneTrackerComponent.target().orElse(null) : null;
			}
		},
		SPAWN("spawn") {
			@Override
			public GlobalPos getPosition(ClientWorld world, ItemStack stack, @Nullable HeldItemContext context) {
				return world.getSpawnPoint().globalPos();
			}
		},
		RECOVERY("recovery") {
			@Override
			public @Nullable GlobalPos getPosition(
					ClientWorld world,
					ItemStack stack,
					@Nullable HeldItemContext context
			) {
				return (context == null ? null : context.getEntity()) instanceof PlayerEntity playerEntity
				       ? playerEntity.getLastDeathPos().orElse(null) : null;
			}
		};

		public static final Codec<CompassState.Target>
				CODEC =
				StringIdentifiable.createCodec(CompassState.Target::values);
		private final String name;

		Target(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return this.name;
		}

		abstract @Nullable GlobalPos getPosition(ClientWorld world, ItemStack stack, @Nullable HeldItemContext context);
	}
}
