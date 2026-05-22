package net.minecraft.enchantment;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.EnchantableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.effect.EnchantmentEffectTarget;
import net.minecraft.enchantment.provider.EnchantmentProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Утилитарный класс для работы с зачарованиями на предметах и сущностях.
 * Предоставляет методы для чтения, применения и генерации зачарований.
 */
public class EnchantmentHelper {

	public static int getLevel(RegistryEntry<Enchantment> enchantment, ItemStack stack) {
		ItemEnchantmentsComponent component =
				stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		return component.getLevel(enchantment);
	}

	/**
	 * Применяет изменения к компоненту зачарований предмета через переданный строитель.
	 * Если предмет не содержит компонент зачарований, возвращает {@link ItemEnchantmentsComponent#DEFAULT}.
	 *
	 * @param stack   предмет для изменения
	 * @param applier функция, изменяющая строитель компонента
	 * @return новый компонент зачарований после применения изменений
	 */
	public static ItemEnchantmentsComponent apply(
			ItemStack stack,
			java.util.function.Consumer<ItemEnchantmentsComponent.Builder> applier
	) {
		ComponentType<ItemEnchantmentsComponent> componentType = getEnchantmentsComponentType(stack);
		ItemEnchantmentsComponent existing = stack.get(componentType);

		if (existing == null) {
			return ItemEnchantmentsComponent.DEFAULT;
		}

		ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(existing);
		applier.accept(builder);
		ItemEnchantmentsComponent result = builder.build();
		stack.set(componentType, result);
		return result;
	}

	public static boolean canHaveEnchantments(ItemStack stack) {
		return stack.contains(getEnchantmentsComponentType(stack));
	}

	public static void set(ItemStack stack, ItemEnchantmentsComponent enchantments) {
		stack.set(getEnchantmentsComponentType(stack), enchantments);
	}

	public static ItemEnchantmentsComponent getEnchantments(ItemStack stack) {
		return stack.getOrDefault(getEnchantmentsComponentType(stack), ItemEnchantmentsComponent.DEFAULT);
	}

	/**
	 * Возвращает тип компонента зачарований для предмета.
	 * Зачарованные книги используют {@link DataComponentTypes#STORED_ENCHANTMENTS},
	 * все остальные предметы — {@link DataComponentTypes#ENCHANTMENTS}.
	 */
	public static ComponentType<ItemEnchantmentsComponent> getEnchantmentsComponentType(ItemStack stack) {
		return stack.isOf(Items.ENCHANTED_BOOK)
				? DataComponentTypes.STORED_ENCHANTMENTS
				: DataComponentTypes.ENCHANTMENTS;
	}

	public static boolean hasEnchantments(ItemStack stack) {
		return !stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT).isEmpty()
				|| !stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
				.isEmpty();
	}

	public static int getItemDamage(ServerWorld world, ItemStack stack, int baseItemDamage) {
		MutableFloat damage = new MutableFloat(baseItemDamage);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value().modifyItemDamage(world, level, stack, damage)
		);
		return damage.intValue();
	}

	public static int getAmmoUse(
			ServerWorld world,
			ItemStack rangedWeaponStack,
			ItemStack projectileStack,
			int baseAmmoUse
	) {
		MutableFloat ammoUse = new MutableFloat(baseAmmoUse);
		forEachEnchantment(
				rangedWeaponStack,
				(enchantment, level) -> enchantment.value().modifyAmmoUse(world, level, projectileStack, ammoUse)
		);
		return ammoUse.intValue();
	}

	public static int getBlockExperience(ServerWorld world, ItemStack stack, int baseBlockExperience) {
		MutableFloat experience = new MutableFloat(baseBlockExperience);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value().modifyBlockExperience(world, level, stack, experience)
		);
		return experience.intValue();
	}

	public static int getMobExperience(
			ServerWorld world,
			@Nullable Entity attacker,
			Entity mob,
			int baseMobExperience
	) {
		if (attacker instanceof LivingEntity livingEntity) {
			MutableFloat experience = new MutableFloat(baseMobExperience);
			forEachEnchantment(
					livingEntity,
					(enchantment, level, context) -> enchantment.value()
							.modifyMobExperience(world, level, context.stack(), mob, experience)
			);
			return experience.intValue();
		}

		return baseMobExperience;
	}

	public static ItemStack getEnchantedBookWith(EnchantmentLevelEntry entry) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.addEnchantment(entry.enchantment(), entry.level());
		return book;
	}

	/**
	 * Итерирует по всем зачарованиям предмета (компонент {@link DataComponentTypes#ENCHANTMENTS}).
	 *
	 * @param stack    предмет для итерации
	 * @param consumer потребитель, получающий запись реестра зачарования и его уровень
	 */
	public static void forEachEnchantment(ItemStack stack, EnchantmentHelper.Consumer consumer) {
		ItemEnchantmentsComponent component =
				stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

		for (Entry<RegistryEntry<Enchantment>> entry : component.getEnchantmentEntries()) {
			consumer.accept((RegistryEntry<Enchantment>) entry.getKey(), entry.getIntValue());
		}
	}

	/**
	 * Итерирует по зачарованиям предмета в конкретном слоте, фильтруя по совместимости слота.
	 * Создаёт контекст эффекта для каждого подходящего зачарования.
	 */
	public static void forEachEnchantment(
			ItemStack stack,
			EquipmentSlot slot,
			LivingEntity entity,
			EnchantmentHelper.ContextAwareConsumer contextAwareConsumer
	) {
		if (stack.isEmpty()) {
			return;
		}

		ItemEnchantmentsComponent component = stack.get(DataComponentTypes.ENCHANTMENTS);

		if (component == null || component.isEmpty()) {
			return;
		}

		EnchantmentEffectContext context = new EnchantmentEffectContext(stack, slot, entity);

		for (Entry<RegistryEntry<Enchantment>> entry : component.getEnchantmentEntries()) {
			RegistryEntry<Enchantment> enchantment = (RegistryEntry<Enchantment>) entry.getKey();

			if (enchantment.value().slotMatches(slot)) {
				contextAwareConsumer.accept(enchantment, entry.getIntValue(), context);
			}
		}
	}

	/**
	 * Итерирует по всем зачарованиям во всех слотах экипировки сущности.
	 */
	public static void forEachEnchantment(
			LivingEntity entity,
			EnchantmentHelper.ContextAwareConsumer contextAwareConsumer
	) {
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			forEachEnchantment(entity.getEquippedStack(slot), slot, entity, contextAwareConsumer);
		}
	}

	/**
	 * Проверяет, делает ли какое-либо зачарование сущность неуязвимой к данному источнику урона.
	 */
	public static boolean isInvulnerableTo(ServerWorld world, LivingEntity user, DamageSource damageSource) {
		MutableBoolean invulnerable = new MutableBoolean();
		forEachEnchantment(
				user,
				(enchantment, level, context) -> invulnerable.setValue(
						invulnerable.isTrue()
								|| enchantment.value().hasDamageImmunityTo(world, level, user, damageSource)
				)
		);
		return invulnerable.isTrue();
	}

	public static float getProtectionAmount(ServerWorld world, LivingEntity user, DamageSource damageSource) {
		MutableFloat protection = new MutableFloat(0.0F);
		forEachEnchantment(
				user,
				(enchantment, level, context) -> enchantment.value()
						.modifyDamageProtection(world, level, context.stack(), user, damageSource, protection)
		);
		return protection.floatValue();
	}

	public static float getDamage(
			ServerWorld world,
			ItemStack stack,
			Entity target,
			DamageSource damageSource,
			float baseDamage
	) {
		MutableFloat damage = new MutableFloat(baseDamage);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyDamage(world, level, stack, target, damageSource, damage)
		);
		return damage.floatValue();
	}

	public static float getSmashDamagePerFallenBlock(
			ServerWorld world,
			ItemStack stack,
			Entity target,
			DamageSource damageSource,
			float baseSmashDamagePerFallenBlock
	) {
		MutableFloat damage = new MutableFloat(baseSmashDamagePerFallenBlock);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifySmashDamagePerFallenBlock(world, level, stack, target, damageSource, damage)
		);
		return damage.floatValue();
	}

	public static float getArmorEffectiveness(
			ServerWorld world,
			ItemStack stack,
			Entity user,
			DamageSource damageSource,
			float baseArmorEffectiveness
	) {
		MutableFloat effectiveness = new MutableFloat(baseArmorEffectiveness);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyArmorEffectiveness(world, level, stack, user, damageSource, effectiveness)
		);
		return effectiveness.floatValue();
	}

	public static float modifyKnockback(
			ServerWorld world,
			ItemStack stack,
			Entity target,
			DamageSource damageSource,
			float baseKnockback
	) {
		MutableFloat knockback = new MutableFloat(baseKnockback);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyKnockback(world, level, stack, target, damageSource, knockback)
		);
		return knockback.floatValue();
	}

	/**
	 * Вызывает POST_ATTACK эффекты зачарований после нанесения урона цели.
	 * Если атакующий — живая сущность, используется её оружие.
	 */
	public static void onTargetDamaged(ServerWorld world, Entity target, DamageSource damageSource) {
		if (damageSource.getAttacker() instanceof LivingEntity livingEntity) {
			onTargetDamaged(world, target, damageSource, livingEntity.getWeaponStack());
		} else {
			onTargetDamaged(world, target, damageSource, null);
		}
	}

	/**
	 * Вызывает POST_PIERCING_ATTACK эффекты зачарований при пробивающей атаке.
	 */
	public static void onAttack(ServerWorld world, Entity attacker) {
		if (attacker instanceof LivingEntity livingEntity) {
			forEachEnchantment(
					attacker.getWeaponStack(),
					EquipmentSlot.MAINHAND,
					livingEntity,
					(enchantment, level, context) -> enchantment.value()
							.onPiercingAttack(world, level, context, attacker)
			);
		}
	}

	public static void onTargetDamaged(
			ServerWorld world,
			Entity target,
			DamageSource damageSource,
			@Nullable ItemStack weapon
	) {
		onTargetDamaged(world, target, damageSource, weapon, null);
	}

	/**
	 * Полная версия обработки POST_ATTACK эффектов.
	 * Применяет эффекты как к жертве (VICTIM), так и к атакующему (ATTACKER).
	 *
	 * @param weapon        оружие атакующего (может быть null)
	 * @param breakCallback колбэк поломки оружия (используется если атакующий не живая сущность)
	 */
	public static void onTargetDamaged(
			ServerWorld world,
			Entity target,
			DamageSource damageSource,
			@Nullable ItemStack weapon,
			java.util.function.@Nullable Consumer<Item> breakCallback
	) {
		if (target instanceof LivingEntity livingTarget) {
			forEachEnchantment(
					livingTarget,
					(enchantment, level, context) -> enchantment.value().onTargetDamaged(
							world,
							level,
							context,
							EnchantmentEffectTarget.VICTIM,
							target,
							damageSource
					)
			);
		}

		if (weapon == null) {
			return;
		}

		if (damageSource.getAttacker() instanceof LivingEntity livingAttacker) {
			forEachEnchantment(
					weapon,
					EquipmentSlot.MAINHAND,
					livingAttacker,
					(enchantment, level, context) -> enchantment.value().onTargetDamaged(
							world,
							level,
							context,
							EnchantmentEffectTarget.ATTACKER,
							target,
							damageSource
					)
			);
		} else if (breakCallback != null) {
			EnchantmentEffectContext context = new EnchantmentEffectContext(weapon, null, null, breakCallback);
			forEachEnchantment(
					weapon,
					(enchantment, level) -> enchantment.value().onTargetDamaged(
							world,
							level,
							context,
							EnchantmentEffectTarget.ATTACKER,
							target,
							damageSource
					)
			);
		}
	}

	/**
	 * Применяет локационные эффекты всех зачарований для всех слотов сущности.
	 */
	public static void applyLocationBasedEffects(ServerWorld world, LivingEntity user) {
		forEachEnchantment(
				user,
				(enchantment, level, context) -> enchantment.value()
						.applyLocationBasedEffects(world, level, context, user)
		);
	}

	public static void applyLocationBasedEffects(
			ServerWorld world,
			ItemStack stack,
			LivingEntity user,
			EquipmentSlot slot
	) {
		forEachEnchantment(
				stack,
				slot,
				user,
				(enchantment, level, context) -> enchantment.value()
						.applyLocationBasedEffects(world, level, context, user)
		);
	}

	public static void removeLocationBasedEffects(LivingEntity user) {
		forEachEnchantment(
				user,
				(enchantment, level, context) -> enchantment.value().removeLocationBasedEffects(level, context, user)
		);
	}

	public static void removeLocationBasedEffects(ItemStack stack, LivingEntity user, EquipmentSlot slot) {
		forEachEnchantment(
				stack,
				slot,
				user,
				(enchantment, level, context) -> enchantment.value().removeLocationBasedEffects(level, context, user)
		);
	}

	public static void onTick(ServerWorld world, LivingEntity user) {
		forEachEnchantment(
				user,
				(enchantment, level, context) -> enchantment.value().onTick(world, level, context, user)
		);
	}

	/**
	 * Возвращает максимальный уровень зачарования среди всех предметов экипировки сущности,
	 * в которых это зачарование активно.
	 */
	public static int getEquipmentLevel(RegistryEntry<Enchantment> enchantment, LivingEntity entity) {
		int maxLevel = 0;

		for (ItemStack stack : enchantment.value().getEquipment(entity).values()) {
			int stackLevel = getLevel(enchantment, stack);

			if (stackLevel > maxLevel) {
				maxLevel = stackLevel;
			}
		}

		return maxLevel;
	}

	public static int getProjectileCount(ServerWorld world, ItemStack stack, Entity user, int baseProjectileCount) {
		MutableFloat count = new MutableFloat(baseProjectileCount);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyProjectileCount(world, level, stack, user, count)
		);
		return Math.max(0, count.intValue());
	}

	public static float getProjectileSpread(
			ServerWorld world,
			ItemStack stack,
			Entity user,
			float baseProjectileSpread
	) {
		MutableFloat spread = new MutableFloat(baseProjectileSpread);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyProjectileSpread(world, level, stack, user, spread)
		);
		return Math.max(0.0F, spread.floatValue());
	}

	public static int getProjectilePiercing(ServerWorld world, ItemStack weaponStack, ItemStack projectileStack) {
		MutableFloat piercing = new MutableFloat(0.0F);
		forEachEnchantment(
				weaponStack,
				(enchantment, level) -> enchantment.value()
						.modifyProjectilePiercing(world, level, projectileStack, piercing)
		);
		return Math.max(0, piercing.intValue());
	}

	public static void onProjectileSpawned(
			ServerWorld world,
			ItemStack weaponStack,
			ProjectileEntity projectile,
			java.util.function.Consumer<Item> onBreak
	) {
		LivingEntity owner = projectile.getOwner() instanceof LivingEntity living ? living : null;
		EnchantmentEffectContext context = new EnchantmentEffectContext(weaponStack, null, owner, onBreak);
		forEachEnchantment(
				weaponStack,
				(enchantment, level) -> enchantment.value()
						.onProjectileSpawned(world, level, context, projectile)
		);
	}

	public static void onHitBlock(
			ServerWorld world,
			ItemStack stack,
			@Nullable LivingEntity user,
			Entity enchantedEntity,
			@Nullable EquipmentSlot slot,
			Vec3d pos,
			BlockState state,
			java.util.function.Consumer<Item> onBreak
	) {
		EnchantmentEffectContext context = new EnchantmentEffectContext(stack, slot, user, onBreak);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.onHitBlock(world, level, context, enchantedEntity, pos, state)
		);
	}

	public static int getRepairWithExperience(ServerWorld world, ItemStack stack, int baseRepairWithExperience) {
		MutableFloat repair = new MutableFloat(baseRepairWithExperience);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyRepairWithExperience(world, level, stack, repair)
		);
		return Math.max(0, repair.intValue());
	}

	/**
	 * Вычисляет шанс выпадения экипировки с учётом зачарований атакующего и жертвы.
	 * Эффекты EQUIPMENT_DROPS применяются для ролей VICTIM (на жертве) и ATTACKER (на атакующем).
	 */
	public static float getEquipmentDropChance(
			ServerWorld world,
			LivingEntity attacker,
			DamageSource damageSource,
			float baseEquipmentDropChance
	) {
		MutableFloat dropChance = new MutableFloat(baseEquipmentDropChance);
		Random random = attacker.getRandom();

		forEachEnchantment(attacker, (enchantment, level, context) -> {
			LootContext lootContext = Enchantment.createEnchantedDamageLootContext(world, level, attacker, damageSource);
			enchantment.value().getEffect(EnchantmentEffectComponentTypes.EQUIPMENT_DROPS).forEach(effect -> {
				if (effect.enchanted() == EnchantmentEffectTarget.VICTIM
						&& effect.affected() == EnchantmentEffectTarget.VICTIM
						&& effect.test(lootContext)) {
					dropChance.setValue(effect.effect().apply(level, random, dropChance.floatValue()));
				}
			});
		});

		if (damageSource.getAttacker() instanceof LivingEntity livingAttacker) {
			forEachEnchantment(livingAttacker, (enchantment, level, context) -> {
				LootContext lootContext =
						Enchantment.createEnchantedDamageLootContext(world, level, attacker, damageSource);
				enchantment.value().getEffect(EnchantmentEffectComponentTypes.EQUIPMENT_DROPS).forEach(effect -> {
					if (effect.enchanted() == EnchantmentEffectTarget.ATTACKER
							&& effect.affected() == EnchantmentEffectTarget.VICTIM
							&& effect.test(lootContext)) {
						dropChance.setValue(effect.effect().apply(level, random, dropChance.floatValue()));
					}
				});
			});
		}

		return dropChance.floatValue();
	}

	public static void applyAttributeModifiers(
			ItemStack stack,
			AttributeModifierSlot slot,
			BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer
	) {
		forEachEnchantment(stack, (enchantment, level) -> enchantment.value()
				.getEffect(EnchantmentEffectComponentTypes.ATTRIBUTES)
				.forEach(effect -> {
					if (enchantment.value().definition().slots().contains(slot)) {
						attributeModifierConsumer.accept(
								effect.attribute(),
								effect.createAttributeModifier(level, slot)
						);
					}
				})
		);
	}

	public static void applyAttributeModifiers(
			ItemStack stack,
			EquipmentSlot slot,
			BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer
	) {
		forEachEnchantment(stack, (enchantment, level) -> enchantment.value()
				.getEffect(EnchantmentEffectComponentTypes.ATTRIBUTES)
				.forEach(effect -> {
					if (enchantment.value().slotMatches(slot)) {
						attributeModifierConsumer.accept(
								effect.attribute(),
								effect.createAttributeModifier(level, slot)
						);
					}
				})
		);
	}

	public static int getFishingLuckBonus(ServerWorld world, ItemStack stack, Entity user) {
		MutableFloat bonus = new MutableFloat(0.0F);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyFishingLuckBonus(world, level, stack, user, bonus)
		);
		return Math.max(0, bonus.intValue());
	}

	public static float getFishingTimeReduction(ServerWorld world, ItemStack stack, Entity user) {
		MutableFloat reduction = new MutableFloat(0.0F);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyFishingTimeReduction(world, level, stack, user, reduction)
		);
		return Math.max(0.0F, reduction.floatValue());
	}

	public static int getTridentReturnAcceleration(ServerWorld world, ItemStack stack, Entity user) {
		MutableFloat acceleration = new MutableFloat(0.0F);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyTridentReturnAcceleration(world, level, stack, user, acceleration)
		);
		return Math.max(0, acceleration.intValue());
	}

	public static float getCrossbowChargeTime(ItemStack stack, LivingEntity user, float baseCrossbowChargeTime) {
		MutableFloat chargeTime = new MutableFloat(baseCrossbowChargeTime);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyCrossbowChargeTime(user.getRandom(), level, chargeTime)
		);
		return Math.max(0.0F, chargeTime.floatValue());
	}

	public static float getTridentSpinAttackStrength(ItemStack stack, LivingEntity user) {
		MutableFloat strength = new MutableFloat(0.0F);
		forEachEnchantment(
				stack,
				(enchantment, level) -> enchantment.value()
						.modifyTridentSpinAttackStrength(user.getRandom(), level, strength)
		);
		return strength.floatValue();
	}

	public static boolean hasAnyEnchantmentsIn(ItemStack stack, TagKey<Enchantment> tag) {
		ItemEnchantmentsComponent component =
				stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

		for (Entry<RegistryEntry<Enchantment>> entry : component.getEnchantmentEntries()) {
			RegistryEntry<Enchantment> enchantment = (RegistryEntry<Enchantment>) entry.getKey();

			if (enchantment.isIn(tag)) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasAnyEnchantmentsWith(ItemStack stack, ComponentType<?> componentType) {
		MutableBoolean found = new MutableBoolean(false);
		forEachEnchantment(stack, (enchantment, level) -> {
			if (enchantment.value().effects().contains(componentType)) {
				found.setTrue();
			}
		});
		return found.booleanValue();
	}

	/**
	 * Возвращает эффект наивысшего уровня зачарования из списочного компонента.
	 * Если уровень превышает размер списка, возвращается последний элемент.
	 */
	public static <T> Optional<T> getEffect(ItemStack stack, ComponentType<List<T>> componentType) {
		Pair<List<T>, Integer> pair = getHighestLevelEffect(stack, componentType);

		if (pair == null) {
			return Optional.empty();
		}

		List<T> effects = pair.getFirst();
		int level = pair.getSecond();
		return Optional.of(effects.get(Math.min(level, effects.size()) - 1));
	}

	/**
	 * Возвращает пару (эффект, уровень) для зачарования с наибольшим уровнем,
	 * содержащего указанный компонент.
	 *
	 * @param stack         предмет для поиска
	 * @param componentType тип компонента эффекта
	 * @return пара (значение компонента, уровень зачарования) или {@code null}, если не найдено
	 */
	public static <T> Pair<T, Integer> getHighestLevelEffect(ItemStack stack, ComponentType<T> componentType) {
		MutableObject<Pair<T, Integer>> result = new MutableObject<>();
		forEachEnchantment(stack, (enchantment, level) -> {
			if (result.getValue() == null || result.getValue().getSecond() < level) {
				T value = enchantment.value().effects().get(componentType);

				if (value != null) {
					result.setValue(Pair.of(value, level));
				}
			}
		});
		return result.getValue();
	}

	/**
	 * Выбирает случайный слот экипировки сущности, предмет в котором удовлетворяет предикату
	 * и содержит зачарование с указанным компонентом.
	 *
	 * @param componentType  тип компонента зачарования
	 * @param entity         сущность для поиска
	 * @param stackPredicate дополнительный фильтр по предмету
	 * @return случайный контекст эффекта или {@link Optional#empty()}
	 */
	public static Optional<EnchantmentEffectContext> chooseEquipmentWith(
			ComponentType<?> componentType,
			LivingEntity entity,
			Predicate<ItemStack> stackPredicate
	) {
		List<EnchantmentEffectContext> candidates = new ArrayList<>();

		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			ItemStack stack = entity.getEquippedStack(slot);

			if (!stackPredicate.test(stack)) {
				continue;
			}

			ItemEnchantmentsComponent component =
					stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

			for (Entry<RegistryEntry<Enchantment>> entry : component.getEnchantmentEntries()) {
				RegistryEntry<Enchantment> enchantment = (RegistryEntry<Enchantment>) entry.getKey();

				if (enchantment.value().effects().contains(componentType)
						&& enchantment.value().slotMatches(slot)) {
					candidates.add(new EnchantmentEffectContext(stack, slot, entity));
				}
			}
		}

		return Util.getRandomOrEmpty(candidates, entity.getRandom());
	}

	/**
	 * Вычисляет требуемый уровень опыта для зачарования в столе зачарований.
	 * Алгоритм учитывает количество книжных полок и случайность.
	 *
	 * @param slotIndex     индекс слота стола (0 — дешёвый, 1 — средний, 2 — дорогой)
	 * @param bookshelfCount количество книжных полок (ограничивается до 15)
	 */
	public static int calculateRequiredExperienceLevel(
			Random random,
			int slotIndex,
			int bookshelfCount,
			ItemStack stack
	) {
		EnchantableComponent enchantableComponent = stack.get(DataComponentTypes.ENCHANTABLE);

		if (enchantableComponent == null) {
			return 0;
		}

		int clampedBookshelfCount = Math.min(bookshelfCount, 15);
		int base = random.nextInt(8) + 1 + (clampedBookshelfCount >> 1) + random.nextInt(clampedBookshelfCount + 1);

		if (slotIndex == 0) {
			return Math.max(base / 3, 1);
		}

		return slotIndex == 1 ? base * 2 / 3 + 1 : Math.max(base, clampedBookshelfCount * 2);
	}

	/**
	 * Зачаровывает предмет, используя указанный уровень и список возможных зачарований.
	 * Если передан {@link Optional#empty()}, используются все зачарования из реестра.
	 */
	public static ItemStack enchant(
			Random random,
			ItemStack stack,
			int level,
			DynamicRegistryManager dynamicRegistryManager,
			Optional<? extends RegistryEntryList<Enchantment>> enchantments
	) {
		return enchant(
				random,
				stack,
				level,
				enchantments.map(RegistryEntryList::stream)
						.orElseGet(() -> dynamicRegistryManager
								.getOrThrow(RegistryKeys.ENCHANTMENT)
								.streamEntries()
								.map(reference -> (RegistryEntry<Enchantment>) reference)
						)
		);
	}

	public static ItemStack enchant(
			Random random,
			ItemStack stack,
			int level,
			Stream<RegistryEntry<Enchantment>> possibleEnchantments
	) {
		List<EnchantmentLevelEntry> enchantmentList = generateEnchantments(random, stack, level, possibleEnchantments);

		if (stack.isOf(Items.BOOK)) {
			stack = new ItemStack(Items.ENCHANTED_BOOK);
		}

		for (EnchantmentLevelEntry entry : enchantmentList) {
			stack.addEnchantment(entry.enchantment(), entry.level());
		}

		return stack;
	}

	/**
	 * Генерирует список зачарований для предмета на основе уровня силы зачарования.
	 * Алгоритм: случайно выбирает первое зачарование, затем итеративно добавляет
	 * совместимые зачарования, пока уровень делится на 2 и выпадает шанс (level из 50).
	 *
	 * @param level                уровень силы зачарования (модифицируется внутри)
	 * @param possibleEnchantments поток возможных зачарований для выбора
	 */
	public static List<EnchantmentLevelEntry> generateEnchantments(
			Random random,
			ItemStack stack,
			int level,
			Stream<RegistryEntry<Enchantment>> possibleEnchantments
	) {
		List<EnchantmentLevelEntry> result = Lists.newArrayList();
		EnchantableComponent enchantableComponent = stack.get(DataComponentTypes.ENCHANTABLE);

		if (enchantableComponent == null) {
			return result;
		}

		level += 1
				+ random.nextInt(enchantableComponent.value() / 4 + 1)
				+ random.nextInt(enchantableComponent.value() / 4 + 1);

		float bonus = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
		level = MathHelper.clamp(Math.round(level + level * bonus), 1, Integer.MAX_VALUE);

		List<EnchantmentLevelEntry> candidates = getPossibleEntries(level, stack, possibleEnchantments);

		if (candidates.isEmpty()) {
			return result;
		}

		Weighting.getRandom(random, candidates, EnchantmentLevelEntry::getWeight).ifPresent(result::add);

		while (random.nextInt(50) <= level) {
			if (!result.isEmpty()) {
				removeConflicts(candidates, result.getLast());
			}

			if (candidates.isEmpty()) {
				break;
			}

			Weighting.getRandom(random, candidates, EnchantmentLevelEntry::getWeight).ifPresent(result::add);
			level /= 2;
		}

		return result;
	}

	/**
	 * Удаляет из списка кандидатов все зачарования, конфликтующие с выбранным.
	 */
	public static void removeConflicts(
			List<EnchantmentLevelEntry> possibleEntries,
			EnchantmentLevelEntry pickedEntry
	) {
		possibleEntries.removeIf(entry -> !Enchantment.canBeCombined(pickedEntry.enchantment(), entry.enchantment()));
	}

	/**
	 * Проверяет, совместимо ли зачарование-кандидат со всеми уже выбранными зачарованиями.
	 */
	public static boolean isCompatible(
			Collection<RegistryEntry<Enchantment>> existing,
			RegistryEntry<Enchantment> candidate
	) {
		for (RegistryEntry<Enchantment> existing1 : existing) {
			if (!Enchantment.canBeCombined(existing1, candidate)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Возвращает список возможных зачарований с уровнями для данного предмета и уровня силы.
	 * Для каждого зачарования выбирается наибольший уровень, при котором стоимость попадает в диапазон.
	 *
	 * @param level                уровень силы зачарования
	 * @param stack                зачаровываемый предмет
	 * @param possibleEnchantments поток зачарований для фильтрации
	 */
	public static List<EnchantmentLevelEntry> getPossibleEntries(
			int level,
			ItemStack stack,
			Stream<RegistryEntry<Enchantment>> possibleEnchantments
	) {
		List<EnchantmentLevelEntry> result = Lists.newArrayList();
		boolean isBook = stack.isOf(Items.BOOK);

		possibleEnchantments
				.filter(enchantment -> enchantment.value().isPrimaryItem(stack) || isBook)
				.forEach(enchantment -> {
					Enchantment value = enchantment.value();

					for (int enchLevel = value.getMaxLevel(); enchLevel >= value.getMinLevel(); enchLevel--) {
						if (level >= value.getMinPower(enchLevel) && level <= value.getMaxPower(enchLevel)) {
							result.add(new EnchantmentLevelEntry((RegistryEntry<Enchantment>) enchantment, enchLevel));
							break;
						}
					}
				});

		return result;
	}

	/**
	 * Применяет провайдер зачарований к предмету, если провайдер зарегистрирован.
	 *
	 * @param providerKey ключ провайдера в реестре {@link RegistryKeys#ENCHANTMENT_PROVIDER}
	 */
	public static void applyEnchantmentProvider(
			ItemStack stack,
			DynamicRegistryManager registryManager,
			RegistryKey<EnchantmentProvider> providerKey,
			LocalDifficulty localDifficulty,
			Random random
	) {
		EnchantmentProvider provider =
				registryManager.getOrThrow(RegistryKeys.ENCHANTMENT_PROVIDER).get(providerKey);

		if (provider == null) {
			return;
		}

		apply(
				stack,
				componentBuilder -> provider.provideEnchantments(stack, componentBuilder, random, localDifficulty)
		);
	}

	/** Потребитель зачарования и его уровня без контекста слота. */
	@FunctionalInterface
	public interface Consumer {

		void accept(RegistryEntry<Enchantment> enchantment, int level);
	}

	/** Потребитель зачарования, уровня и контекста эффекта (слот + предмет + владелец). */
	@FunctionalInterface
	public interface ContextAwareConsumer {

		void accept(RegistryEntry<Enchantment> enchantment, int level, EnchantmentEffectContext context);
	}
}
