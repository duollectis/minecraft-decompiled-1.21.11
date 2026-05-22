package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/**
	 * Компонент блокировки атак (щит и аналоги). Определяет задержку активации, кулдаун при сбивании,
	 * правила снижения урона, урон по предмету, а также звуки блокировки и сбивания.
	 */
public record BlocksAttacksComponent(
		float blockDelaySeconds,
		float disableCooldownScale,
		List<BlocksAttacksComponent.DamageReduction> damageReductions,
		BlocksAttacksComponent.ItemDamage itemDamage,
		Optional<TagKey<DamageType>> bypassedBy,
		Optional<RegistryEntry<SoundEvent>> blockSound,
		Optional<RegistryEntry<SoundEvent>> disableSound
) {

	private static final float TICKS_PER_SECOND = 20.0F;

	public static final Codec<BlocksAttacksComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										Codecs.NON_NEGATIVE_FLOAT
												.optionalFieldOf("block_delay_seconds", 0.0F)
												.forGetter(BlocksAttacksComponent::blockDelaySeconds),
										Codecs.NON_NEGATIVE_FLOAT
												.optionalFieldOf("disable_cooldown_scale", 1.0F)
												.forGetter(BlocksAttacksComponent::disableCooldownScale),
										BlocksAttacksComponent.DamageReduction.CODEC
												.listOf()
												.optionalFieldOf(
														"damage_reductions",
														List.of(new BlocksAttacksComponent.DamageReduction(
																90.0F,
																Optional.empty(),
																0.0F,
																1.0F
														))
												)
												.forGetter(BlocksAttacksComponent::damageReductions),
										BlocksAttacksComponent.ItemDamage.CODEC
												.optionalFieldOf("item_damage", BlocksAttacksComponent.ItemDamage.DEFAULT)
												.forGetter(BlocksAttacksComponent::itemDamage),
										TagKey
												.codec(RegistryKeys.DAMAGE_TYPE)
												.optionalFieldOf("bypassed_by")
												.forGetter(BlocksAttacksComponent::bypassedBy),
										SoundEvent.ENTRY_CODEC.optionalFieldOf("block_sound").forGetter(BlocksAttacksComponent::blockSound),
										SoundEvent.ENTRY_CODEC
												.optionalFieldOf("disabled_sound")
												.forGetter(BlocksAttacksComponent::disableSound)
								)
								.apply(instance, BlocksAttacksComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, BlocksAttacksComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT,
			BlocksAttacksComponent::blockDelaySeconds,
			PacketCodecs.FLOAT,
			BlocksAttacksComponent::disableCooldownScale,
			BlocksAttacksComponent.DamageReduction.PACKET_CODEC.collect(PacketCodecs.toList()),
			BlocksAttacksComponent::damageReductions,
			BlocksAttacksComponent.ItemDamage.PACKET_CODEC,
			BlocksAttacksComponent::itemDamage,
			TagKey.packetCodec(RegistryKeys.DAMAGE_TYPE).collect(PacketCodecs::optional),
			BlocksAttacksComponent::bypassedBy,
			SoundEvent.ENTRY_PACKET_CODEC.collect(PacketCodecs::optional),
			BlocksAttacksComponent::blockSound,
			SoundEvent.ENTRY_PACKET_CODEC.collect(PacketCodecs::optional),
			BlocksAttacksComponent::disableSound,
			BlocksAttacksComponent::new
	);

	/**
		 * Воспроизводит звук успешной блокировки атаки в позиции сущности.
		 *
		 * @param world мир, в котором воспроизводится звук
		 * @param from  сущность, заблокировавшая атаку
		 */
	public void playBlockSound(ServerWorld world, LivingEntity from) {
		blockSound.ifPresent(
				sound -> world.playSound(
						null,
						from.getX(),
						from.getY(),
						from.getZ(),
						sound,
						from.getSoundCategory(),
						1.0F,
						0.8F + world.random.nextFloat() * 0.4F
				)
		);
	}

	/**
		 * Применяет кулдаун щита после сбивания блокировки. Если кулдаун больше нуля —
		 * устанавливает его игроку, сбрасывает активный предмет и воспроизводит звук сбивания.
		 *
		 * @param world           мир для воспроизведения звука
		 * @param affectedEntity  сущность, у которой сбили блокировку
		 * @param cooldownSeconds длительность кулдауна в секундах (масштабируется на {@code disableCooldownScale})
		 * @param stack           стек предмета-щита для установки кулдауна
		 */
	public void applyShieldCooldown(
			ServerWorld world,
			LivingEntity affectedEntity,
			float cooldownSeconds,
			ItemStack stack
	) {
		int cooldownTicks = convertCooldownToTicks(cooldownSeconds);
		if (cooldownTicks > 0) {
			if (affectedEntity instanceof PlayerEntity playerEntity) {
				playerEntity.getItemCooldownManager().set(stack, cooldownTicks);
			}

			affectedEntity.clearActiveItem();
			disableSound.ifPresent(
					sound -> world.playSound(
							null,
							affectedEntity.getX(),
							affectedEntity.getY(),
							affectedEntity.getZ(),
							sound,
							affectedEntity.getSoundCategory(),
							0.8F,
							0.8F + world.random.nextFloat() * 0.4F
					)
			);
		}
	}

	/**
		 * Обрабатывает попадание по щиту: начисляет статистику использования и наносит
		 * урон прочности предмета пропорционально заблокированному урону.
		 *
		 * @param world  мир (используется для проверки клиент/сервер)
		 * @param stack  стек предмета-щита
		 * @param entity сущность, держащая щит
		 * @param hand   рука, в которой держится щит
		 * @param damage заблокированный урон
		 */
	public void onShieldHit(World world, ItemStack stack, LivingEntity entity, Hand hand, float damage) {
		if (!(entity instanceof PlayerEntity playerEntity)) {
			return;
		}

		if (!world.isClient()) {
			playerEntity.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
		}

		int durabilityDamage = itemDamage.calculate(damage);
		if (durabilityDamage > 0) {
			stack.damage(durabilityDamage, entity, hand.getEquipmentSlot());
		}
	}

	private int convertCooldownToTicks(float cooldownSeconds) {
		float scaled = cooldownSeconds * disableCooldownScale;
		return scaled > 0.0F ? Math.round(scaled * TICKS_PER_SECOND) : 0;
	}

	public int getBlockDelayTicks() {
		return Math.round(blockDelaySeconds * TICKS_PER_SECOND);
	}

	public float getDamageReductionAmount(DamageSource source, float damage, double angle) {
		float total = 0.0F;

		for (DamageReduction reduction : damageReductions) {
			total += reduction.getReductionAmount(source, damage, angle);
		}

		return MathHelper.clamp(total, 0.0F, damage);
	}

	/**
		 * Правило снижения урона: угол блокировки, тип урона, базовое и пропорциональное снижение.
		 */
	public record DamageReduction(
			float horizontalBlockingAngle,
			Optional<RegistryEntryList<DamageType>> type,
			float base,
			float factor
	) {

		public static final Codec<BlocksAttacksComponent.DamageReduction> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
											Codecs.POSITIVE_FLOAT
													.optionalFieldOf("horizontal_blocking_angle", 90.0F)
													.forGetter(BlocksAttacksComponent.DamageReduction::horizontalBlockingAngle),
											RegistryCodecs
													.entryList(RegistryKeys.DAMAGE_TYPE)
													.optionalFieldOf("type")
													.forGetter(BlocksAttacksComponent.DamageReduction::type),
											Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacksComponent.DamageReduction::base),
											Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacksComponent.DamageReduction::factor)
									)
									.apply(instance, BlocksAttacksComponent.DamageReduction::new)
		);
		public static final PacketCodec<RegistryByteBuf, BlocksAttacksComponent.DamageReduction>
				PACKET_CODEC =
				PacketCodec.tuple(
						PacketCodecs.FLOAT,
						BlocksAttacksComponent.DamageReduction::horizontalBlockingAngle,
						PacketCodecs.registryEntryList(RegistryKeys.DAMAGE_TYPE).collect(PacketCodecs::optional),
						BlocksAttacksComponent.DamageReduction::type,
						PacketCodecs.FLOAT,
						BlocksAttacksComponent.DamageReduction::base,
						PacketCodecs.FLOAT,
						BlocksAttacksComponent.DamageReduction::factor,
						BlocksAttacksComponent.DamageReduction::new
				);

		public float getReductionAmount(DamageSource source, float damage, double angle) {
			if (angle > (float) (Math.PI / 180.0) * horizontalBlockingAngle) {
				return 0.0F;
			}

			if (type.isPresent() && !type.get().contains(source.getTypeRegistryEntry())) {
				return 0.0F;
			}

			return MathHelper.clamp(base + factor * damage, 0.0F, damage);
		}
	}

	/**
		 * Параметры урона по прочности предмета-щита при блокировке.
		 * Урон рассчитывается как {@code base + factor * damage}, если {@code damage >= threshold}.
		 */
	public record ItemDamage(float threshold, float base, float factor) {

		public static final Codec<BlocksAttacksComponent.ItemDamage> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
											Codecs.NON_NEGATIVE_FLOAT
													.fieldOf("threshold")
													.forGetter(BlocksAttacksComponent.ItemDamage::threshold),
											Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacksComponent.ItemDamage::base),
											Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacksComponent.ItemDamage::factor)
									)
									.apply(instance, BlocksAttacksComponent.ItemDamage::new)
		);
		public static final PacketCodec<ByteBuf, BlocksAttacksComponent.ItemDamage> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.FLOAT,
				BlocksAttacksComponent.ItemDamage::threshold,
				PacketCodecs.FLOAT,
				BlocksAttacksComponent.ItemDamage::base,
				PacketCodecs.FLOAT,
				BlocksAttacksComponent.ItemDamage::factor,
				BlocksAttacksComponent.ItemDamage::new
		);
		public static final BlocksAttacksComponent.ItemDamage
				DEFAULT =
				new BlocksAttacksComponent.ItemDamage(1.0F, 0.0F, 1.0F);

		public int calculate(float damage) {
			return damage < threshold ? 0 : MathHelper.floor(base + factor * damage);
		}
	}
}
