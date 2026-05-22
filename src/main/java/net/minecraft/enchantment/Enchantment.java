package net.minecraft.enchantment;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.effect.AttributeEnchantmentEffect;
import net.minecraft.enchantment.effect.DamageImmunityEnchantmentEffect;
import net.minecraft.enchantment.effect.EnchantmentEffectEntry;
import net.minecraft.enchantment.effect.EnchantmentEffectTarget;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.enchantment.effect.EnchantmentLocationBasedEffect;
import net.minecraft.enchantment.effect.EnchantmentValueEffect;
import net.minecraft.enchantment.effect.TargetedEnchantmentEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Описывает зачарование — его метаданные, стоимость, слоты применения и набор компонентных эффектов.
 * <p>
 * Зачарование является иммутабельным record-ом и хранится в реестре {@link RegistryKeys#ENCHANTMENT}.
 * Все эффекты описываются через {@link ComponentMap}, что позволяет гибко расширять поведение
 * без изменения базового класса.
 */
public record Enchantment(
		Text description,
		Enchantment.Definition definition,
		RegistryEntryList<Enchantment> exclusiveSet,
		ComponentMap effects
) {

	public static final int MAX_LEVEL = 255;

	public static final Codec<Enchantment> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					TextCodecs.CODEC.fieldOf("description").forGetter(Enchantment::description),
					Enchantment.Definition.CODEC.forGetter(Enchantment::definition),
					RegistryCodecs.entryList(RegistryKeys.ENCHANTMENT)
							.optionalFieldOf("exclusive_set", RegistryEntryList.of())
							.forGetter(Enchantment::exclusiveSet),
					EnchantmentEffectComponentTypes.COMPONENT_MAP_CODEC
							.optionalFieldOf("effects", ComponentMap.EMPTY)
							.forGetter(Enchantment::effects)
			).apply(instance, Enchantment::new)
	);

	public static final Codec<RegistryEntry<Enchantment>> ENTRY_CODEC =
			RegistryFixedCodec.of(RegistryKeys.ENCHANTMENT);

	public static final PacketCodec<RegistryByteBuf, RegistryEntry<Enchantment>> ENTRY_PACKET_CODEC =
			PacketCodecs.registryEntry(RegistryKeys.ENCHANTMENT);

	public static Enchantment.Cost constantCost(int base) {
		return new Enchantment.Cost(base, 0);
	}

	public static Enchantment.Cost leveledCost(int base, int perLevel) {
		return new Enchantment.Cost(base, perLevel);
	}

	/**
	 * Создаёт определение зачарования с явным списком основных предметов.
	 *
	 * @param supportedItems предметы, которые поддерживают зачарование
	 * @param primaryItems   предметы, для которых зачарование является основным (влияет на шанс в столе)
	 * @param weight         вес при случайном выборе
	 * @param maxLevel       максимальный уровень зачарования
	 * @param minCost        минимальная стоимость для каждого уровня
	 * @param maxCost        максимальная стоимость для каждого уровня
	 * @param anvilCost      стоимость объединения на наковальне
	 * @param slots          слоты экипировки, в которых зачарование активно
	 */
	public static Enchantment.Definition definition(
			RegistryEntryList<Item> supportedItems,
			RegistryEntryList<Item> primaryItems,
			int weight,
			int maxLevel,
			Enchantment.Cost minCost,
			Enchantment.Cost maxCost,
			int anvilCost,
			AttributeModifierSlot... slots
	) {
		return new Enchantment.Definition(
				supportedItems,
				Optional.of(primaryItems),
				weight,
				maxLevel,
				minCost,
				maxCost,
				anvilCost,
				List.of(slots)
		);
	}

	/**
	 * Создаёт определение зачарования без явного списка основных предметов.
	 * В этом случае все поддерживаемые предметы считаются основными.
	 */
	public static Enchantment.Definition definition(
			RegistryEntryList<Item> supportedItems,
			int weight,
			int maxLevel,
			Enchantment.Cost minCost,
			Enchantment.Cost maxCost,
			int anvilCost,
			AttributeModifierSlot... slots
	) {
		return new Enchantment.Definition(
				supportedItems,
				Optional.empty(),
				weight,
				maxLevel,
				minCost,
				maxCost,
				anvilCost,
				List.of(slots)
		);
	}

	/**
	 * Возвращает карту слот → предмет для всех слотов сущности, в которых активно это зачарование.
	 *
	 * @param entity сущность, чья экипировка проверяется
	 * @return карта активных слотов с предметами
	 */
	public Map<EquipmentSlot, ItemStack> getEquipment(LivingEntity entity) {
		Map<EquipmentSlot, ItemStack> equipment = Maps.newEnumMap(EquipmentSlot.class);

		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (!slotMatches(slot)) {
				continue;
			}

			ItemStack stack = entity.getEquippedStack(slot);

			if (!stack.isEmpty()) {
				equipment.put(slot, stack);
			}
		}

		return equipment;
	}

	public RegistryEntryList<Item> getApplicableItems() {
		return definition.supportedItems();
	}

	/**
	 * Проверяет, активно ли зачарование в указанном слоте экипировки.
	 *
	 * @param slot проверяемый слот
	 * @return {@code true}, если хотя бы один слот из определения совпадает
	 */
	public boolean slotMatches(EquipmentSlot slot) {
		return definition.slots().stream().anyMatch(s -> s.matches(slot));
	}

	/**
	 * Проверяет, является ли предмет основным для этого зачарования.
	 * Основной предмет — тот, для которого зачарование появляется в столе зачарований.
	 */
	public boolean isPrimaryItem(ItemStack stack) {
		return isSupportedItem(stack)
				&& (definition.primaryItems.isEmpty() || stack.isIn(definition.primaryItems.get()));
	}

	public boolean isSupportedItem(ItemStack stack) {
		return stack.isIn(definition.supportedItems);
	}

	public int getWeight() {
		return definition.weight();
	}

	public int getAnvilCost() {
		return definition.anvilCost();
	}

	public int getMinLevel() {
		return 1;
	}

	public int getMaxLevel() {
		return definition.maxLevel();
	}

	public int getMinPower(int level) {
		return definition.minCost().forLevel(level);
	}

	public int getMaxPower(int level) {
		return definition.maxCost().forLevel(level);
	}

	@Override
	public String toString() {
		return "Enchantment " + description.getString();
	}

	/**
	 * Проверяет, можно ли объединить два зачарования (не конфликтуют ли они).
	 * Зачарования конфликтуют, если они одинаковы или входят в эксклюзивные наборы друг друга.
	 */
	public static boolean canBeCombined(RegistryEntry<Enchantment> first, RegistryEntry<Enchantment> second) {
		return !first.equals(second)
				&& !first.value().exclusiveSet.contains(second)
				&& !second.value().exclusiveSet.contains(first);
	}

	/**
	 * Формирует отображаемое имя зачарования с уровнем.
	 * Проклятия окрашиваются красным, обычные зачарования — серым.
	 * Уровень не отображается, если зачарование одноуровневое и уровень равен 1.
	 */
	public static Text getName(RegistryEntry<Enchantment> enchantment, int level) {
		MutableText name = enchantment.value().description.copy();

		name = enchantment.isIn(EnchantmentTags.CURSE)
				? Texts.setStyleIfAbsent(name, Style.EMPTY.withColor(Formatting.RED))
				: Texts.setStyleIfAbsent(name, Style.EMPTY.withColor(Formatting.GRAY));

		if (level != 1 || enchantment.value().getMaxLevel() != 1) {
			name.append(ScreenTexts.SPACE).append(Text.translatable("enchantment.level." + level));
		}

		return name;
	}

	public boolean isAcceptableItem(ItemStack stack) {
		return definition.supportedItems().contains(stack.getRegistryEntry());
	}

	public <T> List<T> getEffect(ComponentType<List<T>> type) {
		return effects.getOrDefault(type, List.of());
	}

	/**
	 * Проверяет, даёт ли зачарование иммунитет к указанному источнику урона.
	 *
	 * @param world       серверный мир
	 * @param level       уровень зачарования
	 * @param user        сущность с зачарованием
	 * @param damageSource источник урона
	 * @return {@code true}, если хотя бы один эффект иммунитета срабатывает
	 */
	public boolean hasDamageImmunityTo(
			ServerWorld world,
			int level,
			Entity user,
			DamageSource damageSource
	) {
		LootContext lootContext = createEnchantedDamageLootContext(world, level, user, damageSource);

		for (EnchantmentEffectEntry<DamageImmunityEnchantmentEffect> entry
				: getEffect(EnchantmentEffectComponentTypes.DAMAGE_IMMUNITY)) {
			if (entry.test(lootContext)) {
				return true;
			}
		}

		return false;
	}

	public void modifyDamageProtection(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			DamageSource damageSource,
			MutableFloat damageProtection
	) {
		LootContext lootContext = createEnchantedDamageLootContext(world, level, user, damageSource);

		for (EnchantmentEffectEntry<EnchantmentValueEffect> entry
				: getEffect(EnchantmentEffectComponentTypes.DAMAGE_PROTECTION)) {
			if (entry.test(lootContext)) {
				damageProtection.setValue(
						entry.effect().apply(level, user.getRandom(), damageProtection.floatValue())
				);
			}
		}
	}

	public void modifyItemDamage(ServerWorld world, int level, ItemStack stack, MutableFloat itemDamage) {
		modifyValue(EnchantmentEffectComponentTypes.ITEM_DAMAGE, world, level, stack, itemDamage);
	}

	public void modifyAmmoUse(ServerWorld world, int level, ItemStack projectileStack, MutableFloat ammoUse) {
		modifyValue(EnchantmentEffectComponentTypes.AMMO_USE, world, level, projectileStack, ammoUse);
	}

	public void modifyProjectilePiercing(
			ServerWorld world,
			int level,
			ItemStack stack,
			MutableFloat projectilePiercing
	) {
		modifyValue(EnchantmentEffectComponentTypes.PROJECTILE_PIERCING, world, level, stack, projectilePiercing);
	}

	public void modifyBlockExperience(
			ServerWorld world,
			int level,
			ItemStack stack,
			MutableFloat blockExperience
	) {
		modifyValue(EnchantmentEffectComponentTypes.BLOCK_EXPERIENCE, world, level, stack, blockExperience);
	}

	public void modifyMobExperience(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			MutableFloat mobExperience
	) {
		modifyValue(EnchantmentEffectComponentTypes.MOB_EXPERIENCE, world, level, stack, user, mobExperience);
	}

	public void modifyRepairWithExperience(
			ServerWorld world,
			int level,
			ItemStack stack,
			MutableFloat repairWithExperience
	) {
		modifyValue(EnchantmentEffectComponentTypes.REPAIR_WITH_XP, world, level, stack, repairWithExperience);
	}

	public void modifyTridentReturnAcceleration(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			MutableFloat tridentReturnAcceleration
	) {
		modifyValue(
				EnchantmentEffectComponentTypes.TRIDENT_RETURN_ACCELERATION,
				world,
				level,
				stack,
				user,
				tridentReturnAcceleration
		);
	}

	public void modifyTridentSpinAttackStrength(Random random, int level, MutableFloat tridentSpinAttackStrength) {
		modifyValue(
				EnchantmentEffectComponentTypes.TRIDENT_SPIN_ATTACK_STRENGTH,
				random,
				level,
				tridentSpinAttackStrength
		);
	}

	public void modifyFishingTimeReduction(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			MutableFloat fishingTimeReduction
	) {
		modifyValue(
				EnchantmentEffectComponentTypes.FISHING_TIME_REDUCTION,
				world,
				level,
				stack,
				user,
				fishingTimeReduction
		);
	}

	public void modifyFishingLuckBonus(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			MutableFloat fishingLuckBonus
	) {
		modifyValue(
				EnchantmentEffectComponentTypes.FISHING_LUCK_BONUS,
				world,
				level,
				stack,
				user,
				fishingLuckBonus
		);
	}

	public void modifyDamage(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			DamageSource damageSource,
			MutableFloat damage
	) {
		modifyValue(EnchantmentEffectComponentTypes.DAMAGE, world, level, stack, user, damageSource, damage);
	}

	public void modifySmashDamagePerFallenBlock(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			DamageSource damageSource,
			MutableFloat smashDamagePerFallenBlock
	) {
		modifyValue(
				EnchantmentEffectComponentTypes.SMASH_DAMAGE_PER_FALLEN_BLOCK,
				world,
				level,
				stack,
				user,
				damageSource,
				smashDamagePerFallenBlock
		);
	}

	public void modifyKnockback(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			DamageSource damageSource,
			MutableFloat knockback
	) {
		modifyValue(EnchantmentEffectComponentTypes.KNOCKBACK, world, level, stack, user, damageSource, knockback);
	}

	public void modifyArmorEffectiveness(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			DamageSource damageSource,
			MutableFloat armorEffectiveness
	) {
		modifyValue(
				EnchantmentEffectComponentTypes.ARMOR_EFFECTIVENESS,
				world,
				level,
				stack,
				user,
				damageSource,
				armorEffectiveness
		);
	}

	/**
	 * Вызывается после нанесения урона цели. Применяет POST_ATTACK эффекты
	 * для указанной роли (атакующий или жертва).
	 *
	 * @param target      роль, для которой проверяются эффекты (ATTACKER или VICTIM)
	 * @param user        сущность, получившая урон (жертва атаки)
	 * @param damageSource источник урона
	 */
	public void onTargetDamaged(
			ServerWorld world,
			int level,
			EnchantmentEffectContext context,
			EnchantmentEffectTarget target,
			Entity user,
			DamageSource damageSource
	) {
		for (TargetedEnchantmentEffect<EnchantmentEntityEffect> effect
				: getEffect(EnchantmentEffectComponentTypes.POST_ATTACK)) {
			if (target == effect.enchanted()) {
				applyTargetedEffect(effect, world, level, context, user, damageSource);
			}
		}
	}

	/**
	 * Применяет направленный эффект зачарования, определяя целевую сущность
	 * по роли (ATTACKER, DAMAGING_ENTITY или VICTIM).
	 */
	public static void applyTargetedEffect(
			TargetedEnchantmentEffect<EnchantmentEntityEffect> effect,
			ServerWorld world,
			int level,
			EnchantmentEffectContext context,
			Entity user,
			DamageSource damageSource
	) {
		if (!effect.test(createEnchantedDamageLootContext(world, level, user, damageSource))) {
			return;
		}

		Entity target = switch (effect.affected()) {
			case ATTACKER -> damageSource.getAttacker();
			case DAMAGING_ENTITY -> damageSource.getSource();
			case VICTIM -> user;
		};

		if (target != null) {
			effect.effect().apply(world, level, context, target, target.getEntityPos());
		}
	}

	/**
	 * Вызывается при пробивающей атаке (POST_PIERCING_ATTACK).
	 */
	public void onPiercingAttack(ServerWorld world, int level, EnchantmentEffectContext context, Entity attacker) {
		applyEffects(
				getEffect(EnchantmentEffectComponentTypes.POST_PIERCING_ATTACK),
				createEnchantedEntityLootContext(world, level, attacker, attacker.getEntityPos()),
				effect -> effect.apply(world, level, context, attacker, attacker.getEntityPos())
		);
	}

	public void modifyProjectileCount(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			MutableFloat projectileCount
	) {
		modifyValue(EnchantmentEffectComponentTypes.PROJECTILE_COUNT, world, level, stack, user, projectileCount);
	}

	public void modifyProjectileSpread(
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			MutableFloat projectileSpread
	) {
		modifyValue(EnchantmentEffectComponentTypes.PROJECTILE_SPREAD, world, level, stack, user, projectileSpread);
	}

	public void modifyCrossbowChargeTime(Random random, int level, MutableFloat crossbowChargeTime) {
		modifyValue(EnchantmentEffectComponentTypes.CROSSBOW_CHARGE_TIME, random, level, crossbowChargeTime);
	}

	/**
	 * Применяет одиночный (не списочный) эффект изменения значения.
	 * Используется для компонентов типа {@link EnchantmentValueEffect}, хранящихся напрямую (не в списке).
	 */
	public void modifyValue(
			ComponentType<EnchantmentValueEffect> type,
			Random random,
			int level,
			MutableFloat value
	) {
		EnchantmentValueEffect effect = effects.get(type);

		if (effect != null) {
			value.setValue(effect.apply(level, random, value.floatValue()));
		}
	}

	public void onTick(ServerWorld world, int level, EnchantmentEffectContext context, Entity user) {
		applyEffects(
				getEffect(EnchantmentEffectComponentTypes.TICK),
				createEnchantedEntityLootContext(world, level, user, user.getEntityPos()),
				effect -> effect.apply(world, level, context, user, user.getEntityPos())
		);
	}

	public void onProjectileSpawned(ServerWorld world, int level, EnchantmentEffectContext context, Entity user) {
		applyEffects(
				getEffect(EnchantmentEffectComponentTypes.PROJECTILE_SPAWNED),
				createEnchantedEntityLootContext(world, level, user, user.getEntityPos()),
				effect -> effect.apply(world, level, context, user, user.getEntityPos())
		);
	}

	public void onHitBlock(
			ServerWorld world,
			int level,
			EnchantmentEffectContext context,
			Entity enchantedEntity,
			Vec3d pos,
			BlockState state
	) {
		applyEffects(
				getEffect(EnchantmentEffectComponentTypes.HIT_BLOCK),
				createHitBlockLootContext(world, level, enchantedEntity, pos, state),
				effect -> effect.apply(world, level, context, enchantedEntity, pos)
		);
	}

	public final void modifyValue(
			ComponentType<List<EnchantmentEffectEntry<EnchantmentValueEffect>>> type,
			ServerWorld world,
			int level,
			ItemStack stack,
			MutableFloat value
	) {
		applyEffects(
				getEffect(type),
				createEnchantedItemLootContext(world, level, stack),
				effect -> value.setValue(effect.apply(level, world.getRandom(), value.floatValue()))
		);
	}

	public final void modifyValue(
			ComponentType<List<EnchantmentEffectEntry<EnchantmentValueEffect>>> type,
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			MutableFloat value
	) {
		applyEffects(
				getEffect(type),
				createEnchantedEntityLootContext(world, level, user, user.getEntityPos()),
				effect -> value.setValue(effect.apply(level, user.getRandom(), value.floatValue()))
		);
	}

	public final void modifyValue(
			ComponentType<List<EnchantmentEffectEntry<EnchantmentValueEffect>>> type,
			ServerWorld world,
			int level,
			ItemStack stack,
			Entity user,
			DamageSource damageSource,
			MutableFloat value
	) {
		applyEffects(
				getEffect(type),
				createEnchantedDamageLootContext(world, level, user, damageSource),
				effect -> value.setValue(effect.apply(level, user.getRandom(), value.floatValue()))
		);
	}

	/**
	 * Создаёт контекст лута для эффектов, связанных с нанесением урона.
	 * Включает параметры: сущность, уровень зачарования, позицию, источник урона,
	 * атакующего и прямого атакующего.
	 */
	public static LootContext createEnchantedDamageLootContext(
			ServerWorld world,
			int level,
			Entity entity,
			DamageSource damageSource
	) {
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(world)
				.add(LootContextParameters.THIS_ENTITY, entity)
				.add(LootContextParameters.ENCHANTMENT_LEVEL, level)
				.add(LootContextParameters.ORIGIN, entity.getEntityPos())
				.add(LootContextParameters.DAMAGE_SOURCE, damageSource)
				.addOptional(LootContextParameters.ATTACKING_ENTITY, damageSource.getAttacker())
				.addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, damageSource.getSource())
				.build(LootContextTypes.ENCHANTED_DAMAGE);

		return new LootContext.Builder(lootWorldContext).build(Optional.empty());
	}

	/**
	 * Создаёт контекст лута для эффектов, привязанных к инструменту/предмету.
	 * Включает параметры: инструмент и уровень зачарования.
	 */
	public static LootContext createEnchantedItemLootContext(ServerWorld world, int level, ItemStack stack) {
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(world)
				.add(LootContextParameters.TOOL, stack)
				.add(LootContextParameters.ENCHANTMENT_LEVEL, level)
				.build(LootContextTypes.ENCHANTED_ITEM);

		return new LootContext.Builder(lootWorldContext).build(Optional.empty());
	}

	/**
	 * Создаёт контекст лута для локационных эффектов зачарования.
	 * Параметр {@code enchantmentActive} указывает, был ли эффект уже активен в предыдущем тике.
	 */
	public static LootContext createEnchantedLocationLootContext(
			ServerWorld world,
			int level,
			Entity entity,
			boolean enchantmentActive
	) {
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(world)
				.add(LootContextParameters.THIS_ENTITY, entity)
				.add(LootContextParameters.ENCHANTMENT_LEVEL, level)
				.add(LootContextParameters.ORIGIN, entity.getEntityPos())
				.add(LootContextParameters.ENCHANTMENT_ACTIVE, enchantmentActive)
				.build(LootContextTypes.ENCHANTED_LOCATION);

		return new LootContext.Builder(lootWorldContext).build(Optional.empty());
	}

	/**
	 * Создаёт контекст лута для эффектов, привязанных к сущности и позиции.
	 */
	public static LootContext createEnchantedEntityLootContext(
			ServerWorld world,
			int level,
			Entity entity,
			Vec3d pos
	) {
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(world)
				.add(LootContextParameters.THIS_ENTITY, entity)
				.add(LootContextParameters.ENCHANTMENT_LEVEL, level)
				.add(LootContextParameters.ORIGIN, pos)
				.build(LootContextTypes.ENCHANTED_ENTITY);

		return new LootContext.Builder(lootWorldContext).build(Optional.empty());
	}

	/**
	 * Создаёт контекст лута для эффектов при попадании снаряда в блок.
	 */
	public static LootContext createHitBlockLootContext(
			ServerWorld world,
			int level,
			Entity entity,
			Vec3d pos,
			BlockState state
	) {
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(world)
				.add(LootContextParameters.THIS_ENTITY, entity)
				.add(LootContextParameters.ENCHANTMENT_LEVEL, level)
				.add(LootContextParameters.ORIGIN, pos)
				.add(LootContextParameters.BLOCK_STATE, state)
				.build(LootContextTypes.HIT_BLOCK);

		return new LootContext.Builder(lootWorldContext).build(Optional.empty());
	}

	/**
	 * Применяет все эффекты из списка, прошедшие проверку условий лута.
	 *
	 * @param entries        список записей эффектов с условиями
	 * @param lootContext    контекст лута для проверки условий
	 * @param effectConsumer потребитель, применяющий эффект
	 */
	public static <T> void applyEffects(
			List<EnchantmentEffectEntry<T>> entries,
			LootContext lootContext,
			Consumer<T> effectConsumer
	) {
		for (EnchantmentEffectEntry<T> entry : entries) {
			if (entry.test(lootContext)) {
				effectConsumer.accept(entry.effect());
			}
		}
	}

	/**
	 * Применяет или снимает локационные эффекты зачарования в зависимости от текущего слота
	 * и результата проверки условий. Управляет набором активных эффектов для данного слота.
	 *
	 * @param world   серверный мир
	 * @param level   уровень зачарования
	 * @param context контекст эффекта (содержит слот и предмет)
	 * @param user    сущность-носитель зачарования
	 */
	public void applyLocationBasedEffects(
			ServerWorld world,
			int level,
			EnchantmentEffectContext context,
			LivingEntity user
	) {
		EquipmentSlot equipmentSlot = context.slot();

		if (equipmentSlot == null) {
			return;
		}

		Map<Enchantment, Set<EnchantmentLocationBasedEffect>> effectMap =
				user.getLocationBasedEnchantmentEffects(equipmentSlot);

		if (!slotMatches(equipmentSlot)) {
			Set<EnchantmentLocationBasedEffect> removed = effectMap.remove(this);

			if (removed != null) {
				removed.forEach(effect -> effect.remove(context, user, user.getEntityPos(), level));
			}

			return;
		}

		Set<EnchantmentLocationBasedEffect> activeEffects = effectMap.get(this);

		for (EnchantmentEffectEntry<EnchantmentLocationBasedEffect> entry
				: getEffect(EnchantmentEffectComponentTypes.LOCATION_CHANGED)) {
			EnchantmentLocationBasedEffect locationEffect = entry.effect();
			boolean wasActive = activeEffects != null && activeEffects.contains(locationEffect);

			if (entry.test(createEnchantedLocationLootContext(world, level, user, wasActive))) {
				if (!wasActive) {
					if (activeEffects == null) {
						activeEffects = new ObjectArraySet<>();
						effectMap.put(this, activeEffects);
					}

					activeEffects.add(locationEffect);
				}

				locationEffect.apply(world, level, context, user, user.getEntityPos(), !wasActive);
			} else if (activeEffects != null && activeEffects.remove(locationEffect)) {
				locationEffect.remove(context, user, user.getEntityPos(), level);
			}
		}

		if (activeEffects != null && activeEffects.isEmpty()) {
			effectMap.remove(this);
		}
	}

	/**
	 * Снимает все активные локационные эффекты зачарования для указанного слота.
	 */
	public void removeLocationBasedEffects(int level, EnchantmentEffectContext context, LivingEntity user) {
		EquipmentSlot equipmentSlot = context.slot();

		if (equipmentSlot == null) {
			return;
		}

		Set<EnchantmentLocationBasedEffect> activeEffects =
				user.getLocationBasedEnchantmentEffects(equipmentSlot).remove(this);

		if (activeEffects == null) {
			return;
		}

		for (EnchantmentLocationBasedEffect effect : activeEffects) {
			effect.remove(context, user, user.getEntityPos(), level);
		}
	}

	public static Enchantment.Builder builder(Enchantment.Definition definition) {
		return new Enchantment.Builder(definition);
	}

	/**
	 * Строитель зачарования. Позволяет декларативно задавать эксклюзивные наборы
	 * и добавлять компонентные эффекты перед финальной сборкой через {@link #build(Identifier)}.
	 */
	public static class Builder {

		private final Enchantment.Definition definition;
		private RegistryEntryList<Enchantment> exclusiveSet = RegistryEntryList.of();
		private final Map<ComponentType<?>, List<?>> effectLists = new HashMap<>();
		private final ComponentMap.Builder effectMap = ComponentMap.builder();

		public Builder(Enchantment.Definition properties) {
			definition = properties;
		}

		public Enchantment.Builder exclusiveSet(RegistryEntryList<Enchantment> exclusiveSet) {
			this.exclusiveSet = exclusiveSet;
			return this;
		}

		public <E> Enchantment.Builder addEffect(
				ComponentType<List<EnchantmentEffectEntry<E>>> effectType,
				E effect,
				LootCondition.Builder requirements
		) {
			getEffectsList(effectType).add(new EnchantmentEffectEntry<>(effect, Optional.of(requirements.build())));
			return this;
		}

		public <E> Enchantment.Builder addEffect(
				ComponentType<List<EnchantmentEffectEntry<E>>> effectType,
				E effect
		) {
			getEffectsList(effectType).add(new EnchantmentEffectEntry<>(effect, Optional.empty()));
			return this;
		}

		public <E> Enchantment.Builder addEffect(
				ComponentType<List<TargetedEnchantmentEffect<E>>> type,
				EnchantmentEffectTarget enchanted,
				EnchantmentEffectTarget affected,
				E effect,
				LootCondition.Builder requirements
		) {
			getEffectsList(type).add(new TargetedEnchantmentEffect<>(
					enchanted,
					affected,
					effect,
					Optional.of(requirements.build())
			));
			return this;
		}

		public <E> Enchantment.Builder addEffect(
				ComponentType<List<TargetedEnchantmentEffect<E>>> type,
				EnchantmentEffectTarget enchanted,
				EnchantmentEffectTarget affected,
				E effect
		) {
			getEffectsList(type).add(new TargetedEnchantmentEffect<>(enchanted, affected, effect, Optional.empty()));
			return this;
		}

		public Enchantment.Builder addEffect(
				ComponentType<List<AttributeEnchantmentEffect>> type,
				AttributeEnchantmentEffect effect
		) {
			getEffectsList(type).add(effect);
			return this;
		}

		public <E> Enchantment.Builder addNonListEffect(ComponentType<E> type, E effect) {
			effectMap.add(type, effect);
			return this;
		}

		public Enchantment.Builder addEffect(ComponentType<Unit> type) {
			effectMap.add(type, Unit.INSTANCE);
			return this;
		}

		@SuppressWarnings("unchecked")
		private <E> List<E> getEffectsList(ComponentType<List<E>> type) {
			return (List<E>) effectLists.computeIfAbsent(type, key -> {
				ArrayList<E> list = new ArrayList<>();
				effectMap.add(type, list);
				return list;
			});
		}

		/**
		 * Собирает зачарование, генерируя ключ перевода из переданного идентификатора.
		 *
		 * @param id идентификатор зачарования в реестре
		 * @return готовый экземпляр {@link Enchantment}
		 */
		public Enchantment build(Identifier id) {
			return new Enchantment(
					Text.translatable(Util.createTranslationKey("enchantment", id)),
					definition,
					exclusiveSet,
					effectMap.build()
			);
		}
	}

	/**
	 * Описывает стоимость зачарования в единицах «силы зачарования» для конкретного уровня.
	 * Формула: {@code base + perLevelAboveFirst * (level - 1)}.
	 */
	public record Cost(int base, int perLevelAboveFirst) {

		public static final Codec<Enchantment.Cost> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.INT.fieldOf("base").forGetter(Enchantment.Cost::base),
						Codec.INT.fieldOf("per_level_above_first").forGetter(Enchantment.Cost::perLevelAboveFirst)
				).apply(instance, Enchantment.Cost::new)
		);

		/** Вычисляет стоимость для указанного уровня зачарования. */
		public int forLevel(int level) {
			return base + perLevelAboveFirst * (level - 1);
		}
	}

	/**
	 * Содержит статические параметры зачарования: поддерживаемые предметы, вес,
	 * диапазон уровней, стоимость и активные слоты экипировки.
	 */
	public record Definition(
			RegistryEntryList<Item> supportedItems,
			Optional<RegistryEntryList<Item>> primaryItems,
			int weight,
			int maxLevel,
			Enchantment.Cost minCost,
			Enchantment.Cost maxCost,
			int anvilCost,
			List<AttributeModifierSlot> slots
	) {

		public static final MapCodec<Enchantment.Definition> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						RegistryCodecs.entryList(RegistryKeys.ITEM)
								.fieldOf("supported_items")
								.forGetter(Enchantment.Definition::supportedItems),
						RegistryCodecs.entryList(RegistryKeys.ITEM)
								.optionalFieldOf("primary_items")
								.forGetter(Enchantment.Definition::primaryItems),
						Codecs.rangedInt(1, 1024).fieldOf("weight").forGetter(Enchantment.Definition::weight),
						Codecs.rangedInt(1, MAX_LEVEL).fieldOf("max_level").forGetter(Enchantment.Definition::maxLevel),
						Enchantment.Cost.CODEC.fieldOf("min_cost").forGetter(Enchantment.Definition::minCost),
						Enchantment.Cost.CODEC.fieldOf("max_cost").forGetter(Enchantment.Definition::maxCost),
						Codecs.NON_NEGATIVE_INT.fieldOf("anvil_cost").forGetter(Enchantment.Definition::anvilCost),
						AttributeModifierSlot.CODEC.listOf().fieldOf("slots").forGetter(Enchantment.Definition::slots)
				).apply(instance, Enchantment.Definition::new)
		);
	}
}
