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
 * {@code EquippableComponent}.
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
	 * Of carpet.
	 *
	 * @param color color
	 *
	 * @return EquippableComponent — результат операции
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
	 * Of saddle.
	 *
	 * @return EquippableComponent — результат операции
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
	 * Of harness.
	 *
	 * @param color color
	 *
	 * @return EquippableComponent — результат операции
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
	 * Equip.
	 *
	 * @param stack stack
	 * @param player player
	 *
	 * @return ActionResult — результат операции
	 */
	public ActionResult equip(ItemStack stack, PlayerEntity player) {
		if (player.canUseSlot(this.slot) && this.allows(player.getType())) {
			ItemStack itemStack = player.getEquippedStack(this.slot);
			if ((!EnchantmentHelper.hasAnyEnchantmentsWith(
					itemStack,
					EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE
			) || player.isCreative()
			)
					&& !ItemStack.areItemsAndComponentsEqual(stack, itemStack)) {
				if (!player.getEntityWorld().isClient()) {
					player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
				}

				if (stack.getCount() <= 1) {
					ItemStack itemStack2 = itemStack.isEmpty() ? stack : itemStack.copyAndEmpty();
					ItemStack itemStack3 = player.isCreative() ? stack.copy() : stack.copyAndEmpty();
					player.equipStack(this.slot, itemStack3);
					return ActionResult.SUCCESS.withNewHandStack(itemStack2);
				}
				else {
					ItemStack itemStack2 = itemStack.copyAndEmpty();
					ItemStack itemStack3 = stack.splitUnlessCreative(1, player);
					player.equipStack(this.slot, itemStack3);
					if (!player.getInventory().insertStack(itemStack2)) {
						player.dropItem(itemStack2, false);
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
	 * Equip on interact.
	 *
	 * @param player player
	 * @param entity entity
	 * @param stack stack
	 *
	 * @return ActionResult — результат операции
	 */
	public ActionResult equipOnInteract(PlayerEntity player, LivingEntity entity, ItemStack stack) {
		if (entity.canEquip(stack, this.slot) && !entity.hasStackEquipped(this.slot) && entity.isAlive()) {
			if (!player.getEntityWorld().isClient()) {
				entity.equipStack(this.slot, stack.split(1));
				if (entity instanceof MobEntity mobEntity) {
					mobEntity.setDropGuaranteed(this.slot);
				}
			}

			return ActionResult.SUCCESS;
		}
		else {
			return ActionResult.PASS;
		}
	}

	/**
	 * Allows.
	 *
	 * @param entityType entity type
	 *
	 * @return boolean — результат операции
	 */
	public boolean allows(EntityType<?> entityType) {
		return this.allowedEntities.isEmpty() || this.allowedEntities.get().contains(entityType.getRegistryEntry());
	}

	/**
	 * {@code Builder}.
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

		/**
		 * Build.
		 *
		 * @return EquippableComponent — результат операции
		 */
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
