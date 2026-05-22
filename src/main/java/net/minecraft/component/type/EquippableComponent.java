package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
	 * Компонент экипируемого предмета. Описывает, в какой слот экипировки надевается
	 * предмет, какой звук воспроизводится, какие сущности могут его носить,
	 * а также поведение при диспенсере, обмене и стрижке.
	 */
public record EquippableComponent(
		EquipmentSlot slot,
		RegistryEntry<SoundEvent> equipSound,
		Optional<RegistryKey<EquipmentAsset>> assetId,
		Optional<Identifier> cameraOverlay,
		Optional<RegistryEntryList<EntityType<?>>> allowedEntities,
		boolean dispensable,
		boolean swappable,
		boolean damageOnHurt,
		boolean equipOnInteract,
		boolean canBeSheared,
		RegistryEntry<SoundEvent> shearingSound
) {

	public static final Codec<EquippableComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										EquipmentSlot.CODEC.fieldOf("slot").forGetter(EquippableComponent::slot),
										SoundEvent.ENTRY_CODEC
												.optionalFieldOf("equip_sound", SoundEvents.ITEM_ARMOR_EQUIP_GENERIC)
												.forGetter(EquippableComponent::equipSound),
										RegistryKey
												.createCodec(EquipmentAssetKeys.REGISTRY_KEY)
												.optionalFieldOf("asset_id")
												.forGetter(EquippableComponent::assetId),
										Identifier.CODEC.optionalFieldOf("camera_overlay").forGetter(EquippableComponent::cameraOverlay),
										RegistryCodecs
												.entryList(RegistryKeys.ENTITY_TYPE)
												.optionalFieldOf("allowed_entities")
												.forGetter(EquippableComponent::allowedEntities),
										Codec.BOOL.optionalFieldOf("dispensable", true).forGetter(EquippableComponent::dispensable),
										Codec.BOOL.optionalFieldOf("swappable", true).forGetter(EquippableComponent::swappable),
										Codec.BOOL.optionalFieldOf("damage_on_hurt", true).forGetter(EquippableComponent::damageOnHurt),
										Codec.BOOL
												.optionalFieldOf("equip_on_interact", false)
												.forGetter(EquippableComponent::equipOnInteract),
										Codec.BOOL.optionalFieldOf("can_be_sheared", false).forGetter(EquippableComponent::canBeSheared),
										SoundEvent.ENTRY_CODEC
												.optionalFieldOf(
														"shearing_sound",
														Registries.SOUND_EVENT.getEntry(SoundEvents.ITEM_SHEARS_SNIP)
												)
												.forGetter(EquippableComponent::shearingSound)
								)
								.apply(instance, EquippableComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, EquippableComponent> PACKET_CODEC = PacketCodec.tuple(
			EquipmentSlot.PACKET_CODEC,
			EquippableComponent::slot,
			SoundEvent.ENTRY_PACKET_CODEC,
			EquippableComponent::equipSound,
			RegistryKey.createPacketCodec(EquipmentAssetKeys.REGISTRY_KEY).collect(PacketCodecs::optional),
			EquippableComponent::assetId,
			Identifier.PACKET_CODEC.collect(PacketCodecs::optional),
			EquippableComponent::cameraOverlay,
			PacketCodecs.registryEntryList(RegistryKeys.ENTITY_TYPE).collect(PacketCodecs::optional),
			EquippableComponent::allowedEntities,
			PacketCodecs.BOOLEAN,
			EquippableComponent::dispensable,
			PacketCodecs.BOOLEAN,
			EquippableComponent::swappable,
			PacketCodecs.BOOLEAN,
			EquippableComponent::damageOnHurt,
			PacketCodecs.BOOLEAN,
			EquippableComponent::equipOnInteract,
			PacketCodecs.BOOLEAN,
			EquippableComponent::canBeSheared,
			SoundEvent.ENTRY_PACKET_CODEC,
			EquippableComponent::shearingSound,
			EquippableComponent::new
	);

	/**
		 * Создаёт компонент для ковра ламы заданного цвета. Надевается в слот {@code BODY},
		 * может быть снят ножницами.
		 *
		 * @param color цвет ковра
		 * @return готовый компонент экипировки для ковра ламы
		 */
	public static EquippableComponent ofCarpet(DyeColor color) {
		return builder(EquipmentSlot.BODY)
				.equipSound(SoundEvents.ENTITY_LLAMA_SWAG)
				.model(EquipmentAssetKeys.CARPET_FROM_COLOR.get(color))
				.allowedEntities(EntityType.LLAMA, EntityType.TRADER_LLAMA)
				.canBeSheared(true)
				.shearingSound(SoundEvents.ITEM_LLAMA_CARPET_UNEQUIP)
				.build();
	}

	/**
		 * Создаёт компонент для седла. Надевается в слот {@code SADDLE} на любую
		 * сущность из тега {@code CAN_EQUIP_SADDLE}, может быть снято ножницами.
		 *
		 * @return готовый компонент экипировки для седла
		 */
	public static EquippableComponent ofSaddle() {
		RegistryEntryLookup<EntityType<?>> registryEntryLookup = Registries.createEntryLookup(Registries.ENTITY_TYPE);
		return builder(EquipmentSlot.SADDLE)
				.equipSound(SoundEvents.ENTITY_HORSE_SADDLE)
				.model(EquipmentAssetKeys.SADDLE)
				.allowedEntities(registryEntryLookup.getOrThrow(EntityTypeTags.CAN_EQUIP_SADDLE))
				.equipOnInteract(true)
				.canBeSheared(true)
				.shearingSound(SoundEvents.ITEM_SADDLE_UNEQUIP)
				.build();
	}

	/**
		 * Создаёт компонент для упряжи счастливого гаста заданного цвета. Надевается
		 * в слот {@code BODY} на сущности из тега {@code CAN_EQUIP_HARNESS}.
		 *
		 * @param color цвет упряжи
		 * @return готовый компонент экипировки для упряжи
		 */
	public static EquippableComponent ofHarness(DyeColor color) {
		RegistryEntryLookup<EntityType<?>> registryEntryLookup = Registries.createEntryLookup(Registries.ENTITY_TYPE);
		return builder(EquipmentSlot.BODY)
				.equipSound(SoundEvents.ENTITY_HAPPY_GHAST_EQUIP)
				.model(EquipmentAssetKeys.HARNESS_FROM_COLOR.get(color))
				.allowedEntities(registryEntryLookup.getOrThrow(EntityTypeTags.CAN_EQUIP_HARNESS))
				.equipOnInteract(true)
				.canBeSheared(true)
				.shearingSound(Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_HAPPY_GHAST_UNEQUIP))
				.build();
	}

	public static EquippableComponent.Builder builder(EquipmentSlot slot) {
		return new EquippableComponent.Builder(slot);
	}

	/**
		 * Надевает предмет на игрока в соответствующий слот. Учитывает зачарование
		 * {@code PREVENT_ARMOR_CHANGE}, режим творчества и количество предметов в стаке.
		 * При замене возвращает ранее надетый предмет в руку игрока.
		 *
		 * @param stack  надеваемый предмет
		 * @param player игрок, надевающий предмет
		 * @return результат действия
		 */
	public ActionResult equip(ItemStack stack, PlayerEntity player) {
		if (player.canUseSlot(slot) && allows(player.getType())) {
			ItemStack currentlyEquipped = player.getEquippedStack(slot);
			if ((!EnchantmentHelper.hasAnyEnchantmentsWith(
					currentlyEquipped,
					EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE
			) || player.isCreative()
			)
					&& !ItemStack.areItemsAndComponentsEqual(stack, currentlyEquipped)) {
				if (!player.getEntityWorld().isClient()) {
					player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
				}

				if (stack.getCount() <= 1) {
					ItemStack returnToHand = currentlyEquipped.isEmpty() ? stack : currentlyEquipped.copyAndEmpty();
					ItemStack toEquip = player.isCreative() ? stack.copy() : stack.copyAndEmpty();
					player.equipStack(slot, toEquip);
					return ActionResult.SUCCESS.withNewHandStack(returnToHand);
				}
				else {
					ItemStack displaced = currentlyEquipped.copyAndEmpty();
					ItemStack toEquip = stack.splitUnlessCreative(1, player);
					player.equipStack(slot, toEquip);
					if (!player.getInventory().insertStack(displaced)) {
						player.dropItem(displaced, false);
					}

					return ActionResult.SUCCESS.withNewHandStack(stack);
				}
			}
			else {
				return ActionResult.FAIL;
			}
		}
		else {
			return ActionResult.PASS;
		}
	}

	/**
		 * Надевает предмет на сущность при взаимодействии игрока. Работает только
		 * если слот свободен, сущность жива и может носить данный предмет.
		 *
		 * @param player игрок, взаимодействующий с сущностью
		 * @param entity целевая сущность
		 * @param stack  надеваемый предмет
		 * @return результат действия
		 */
	public ActionResult equipOnInteract(PlayerEntity player, LivingEntity entity, ItemStack stack) {
		if (entity.canEquip(stack, slot) && !entity.hasStackEquipped(slot) && entity.isAlive()) {
			if (!player.getEntityWorld().isClient()) {
				entity.equipStack(slot, stack.split(1));
				if (entity instanceof MobEntity mobEntity) {
					mobEntity.setDropGuaranteed(slot);
				}
			}

			return ActionResult.SUCCESS;
		}
		else {
			return ActionResult.PASS;
		}
	}

	/**
		 * Проверяет, разрешено ли данному типу сущности носить этот предмет.
		 * Если список {@code allowedEntities} пуст — разрешено всем.
		 *
		 * @param entityType тип сущности для проверки
		 * @return {@code true} если сущность может носить предмет
		 */
	public boolean allows(EntityType<?> entityType) {
		return allowedEntities.isEmpty() || allowedEntities.get().contains(entityType.getRegistryEntry());
	}

	/**
		 * Строитель для создания {@link EquippableComponent} с настраиваемыми параметрами.
		 */
	public static class Builder {

		private final EquipmentSlot slot;
		private RegistryEntry<SoundEvent> equipSound = SoundEvents.ITEM_ARMOR_EQUIP_GENERIC;
		private Optional<RegistryKey<EquipmentAsset>> model = Optional.empty();
		private Optional<Identifier> cameraOverlay = Optional.empty();
		private Optional<RegistryEntryList<EntityType<?>>> allowedEntities = Optional.empty();
		private boolean dispensable = true;
		private boolean swappable = true;
		private boolean damageOnHurt = true;
		private boolean equipOnInteract;
		private boolean canBeSheared;
		private RegistryEntry<SoundEvent> shearingSound = Registries.SOUND_EVENT.getEntry(SoundEvents.ITEM_SHEARS_SNIP);

		Builder(EquipmentSlot slot) {
			this.slot = slot;
		}

		public EquippableComponent.Builder equipSound(RegistryEntry<SoundEvent> equipSound) {
			this.equipSound = equipSound;
			return this;
		}

		public EquippableComponent.Builder model(RegistryKey<EquipmentAsset> model) {
			this.model = Optional.of(model);
			return this;
		}

		public EquippableComponent.Builder cameraOverlay(Identifier cameraOverlay) {
			this.cameraOverlay = Optional.of(cameraOverlay);
			return this;
		}

		public EquippableComponent.Builder allowedEntities(EntityType<?>... allowedEntities) {
			return this.allowedEntities(RegistryEntryList.of(EntityType::getRegistryEntry, allowedEntities));
		}

		public EquippableComponent.Builder allowedEntities(RegistryEntryList<EntityType<?>> allowedEntities) {
			this.allowedEntities = Optional.of(allowedEntities);
			return this;
		}

		public EquippableComponent.Builder dispensable(boolean dispensable) {
			this.dispensable = dispensable;
			return this;
		}

		public EquippableComponent.Builder swappable(boolean swappable) {
			this.swappable = swappable;
			return this;
		}

		public EquippableComponent.Builder damageOnHurt(boolean damageOnHurt) {
			this.damageOnHurt = damageOnHurt;
			return this;
		}

		public EquippableComponent.Builder equipOnInteract(boolean equipOnInteract) {
			this.equipOnInteract = equipOnInteract;
			return this;
		}

		public EquippableComponent.Builder canBeSheared(boolean canBeSheared) {
			this.canBeSheared = canBeSheared;
			return this;
		}

		public EquippableComponent.Builder shearingSound(RegistryEntry<SoundEvent> shearingSound) {
			this.shearingSound = shearingSound;
			return this;
		}

		public EquippableComponent build() {
			return new EquippableComponent(
					this.slot,
					this.equipSound,
					this.model,
					this.cameraOverlay,
					this.allowedEntities,
					this.dispensable,
					this.swappable,
					this.damageOnHurt,
					this.equipOnInteract,
					this.canBeSheared,
					this.shearingSound
			);
		}
	}
}
