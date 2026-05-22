package net.minecraft.item;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Spawner;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.component.*;
import net.minecraft.component.type.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.*;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.dynamic.NullOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Представляет стек предметов — конкретный экземпляр предмета с количеством и компонентами данных.
 * <p>Является центральным объектом инвентаря: хранит тип предмета ({@link Item}),
 * количество единиц и произвольный набор компонентов ({@link ComponentMap}),
 * переопределяющих дефолтные значения предмета.</p>
 * <p>Экземпляр {@code ItemStack.EMPTY} используется как нулевой объект вместо {@code null}.</p>
 */
public final class ItemStack implements ComponentHolder, FabricItemStack {

	private static final List<Text> OPERATOR_WARNINGS = List.of(
			Text.translatable("item.op_warning.line1").formatted(Formatting.RED, Formatting.BOLD),
			Text.translatable("item.op_warning.line2").formatted(Formatting.RED),
			Text.translatable("item.op_warning.line3").formatted(Formatting.RED)
	);
	private static final Text UNBREAKABLE_TEXT = Text.translatable("item.unbreakable").formatted(Formatting.BLUE);
	private static final Text INTANGIBLE_TEXT = Text.translatable("item.intangible").formatted(Formatting.GRAY);
	private static final Text DISABLED_TEXT = Text.translatable("item.disabled").formatted(Formatting.RED);

	public static final MapCodec<ItemStack> MAP_CODEC = MapCodec.recursive(
			"ItemStack",
			codec -> RecordCodecBuilder.mapCodec(
					instance -> instance.group(
							                    Item.ENTRY_CODEC.fieldOf("id").forGetter(ItemStack::getRegistryEntry),
							                    Codecs.rangedInt(1, 99).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
							                    ComponentChanges.CODEC
									                    .optionalFieldOf("components", ComponentChanges.EMPTY)
									                    .forGetter(stack -> stack.components.getChanges())
					                    )
					                    .apply(instance, ItemStack::new)
			)
	);
	public static final Codec<ItemStack> CODEC = Codec.lazyInitialized(MAP_CODEC::codec);
	public static final Codec<ItemStack> UNCOUNTED_CODEC = Codec.lazyInitialized(
			() -> RecordCodecBuilder.create(
					instance -> instance.group(
							                    Item.ENTRY_CODEC.fieldOf("id").forGetter(ItemStack::getRegistryEntry),
							                    ComponentChanges.CODEC
									                    .optionalFieldOf("components", ComponentChanges.EMPTY)
									                    .forGetter(stack -> stack.components.getChanges())
					                    )
					                    .apply(instance, (item, components) -> new ItemStack(item, 1, components))
			)
	);
	public static final Codec<ItemStack> VALIDATED_CODEC = CODEC.validate(ItemStack::validate);
	public static final Codec<ItemStack> VALIDATED_UNCOUNTED_CODEC = UNCOUNTED_CODEC.validate(ItemStack::validate);
	public static final Codec<ItemStack> OPTIONAL_CODEC = Codecs.optional(CODEC)
	                                                            .xmap(
			                                                            optional -> optional.orElse(ItemStack.EMPTY),
			                                                            stack -> stack.isEmpty()
			                                                                     ? Optional.empty()
			                                                                     : Optional.of(stack)
	                                                            );
	public static final Codec<ItemStack> REGISTRY_ENTRY_CODEC =
			Item.ENTRY_CODEC.xmap(ItemStack::new, ItemStack::getRegistryEntry);

	public static final PacketCodec<RegistryByteBuf, ItemStack> OPTIONAL_PACKET_CODEC =
			createOptionalPacketCodec(ComponentChanges.PACKET_CODEC);
	public static final PacketCodec<RegistryByteBuf, ItemStack> LENGTH_PREPENDED_OPTIONAL_PACKET_CODEC =
			createOptionalPacketCodec(ComponentChanges.LENGTH_PREPENDED_PACKET_CODEC);

	public static final PacketCodec<RegistryByteBuf, ItemStack> PACKET_CODEC =
			new PacketCodec<>() {
				@Override
				public ItemStack decode(RegistryByteBuf buf) {
					ItemStack stack = ItemStack.OPTIONAL_PACKET_CODEC.decode(buf);
					if (stack.isEmpty()) {
						throw new DecoderException("Empty ItemStack not allowed");
					}

					return stack;
				}

				@Override
				public void encode(RegistryByteBuf buf, ItemStack stack) {
					if (stack.isEmpty()) {
						throw new EncoderException("Empty ItemStack not allowed");
					}

					ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, stack);
				}
			};

	public static final PacketCodec<RegistryByteBuf, List<ItemStack>> OPTIONAL_LIST_PACKET_CODEC =
			OPTIONAL_PACKET_CODEC.collect(PacketCodecs.toCollection(DefaultedList::ofSize));

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final ItemStack EMPTY = new ItemStack((Void) null);

	private int count;
	private int bobbingAnimationTime;
	@Deprecated
	private final @Nullable Item item;
	final MergedComponentMap components;
	private @Nullable Entity holder;

	/**
	 * Валидирует стек предмета: проверяет совместимость компонентов и допустимость количества.
	 *
	 * @param stack стек для валидации
	 * @return успех или ошибка с описанием нарушения
	 */
	public static DataResult<ItemStack> validate(ItemStack stack) {
		DataResult<Unit> componentResult = validateComponents(stack.getComponents());
		if (componentResult.isError()) {
			return componentResult.map(v -> stack);
		}

		return stack.getCount() > stack.getMaxCount()
		       ? DataResult.error(() -> "Item stack with stack size of " + stack.getCount()
		                                + " was larger than maximum: " + stack.getMaxCount())
		       : DataResult.success(stack);
	}

	/**
	 * Создаёт пакетный кодек для опционального стека предмета.
	 * <p>Пустой стек кодируется как count=0 без данных предмета.</p>
	 *
	 * @param componentsCodec кодек для изменений компонентов
	 * @return пакетный кодек опционального стека
	 */
	private static PacketCodec<RegistryByteBuf, ItemStack> createOptionalPacketCodec(
			PacketCodec<RegistryByteBuf, ComponentChanges> componentsCodec
	) {
		return new PacketCodec<>() {
			@Override
			public ItemStack decode(RegistryByteBuf buf) {
				int count = buf.readVarInt();
				if (count <= 0) {
					return ItemStack.EMPTY;
				}

				RegistryEntry<Item> registryEntry = Item.ENTRY_PACKET_CODEC.decode(buf);
				ComponentChanges componentChanges = componentsCodec.decode(buf);
				return new ItemStack(registryEntry, count, componentChanges);
			}

			@Override
			public void encode(RegistryByteBuf buf, ItemStack stack) {
				if (stack.isEmpty()) {
					buf.writeVarInt(0);
					return;
				}

				buf.writeVarInt(stack.getCount());
				Item.ENTRY_PACKET_CODEC.encode(buf, stack.getRegistryEntry());
				componentsCodec.encode(buf, stack.components.getChanges());
			}
		};
	}

	/**
	 * Создаёт пакетный кодек с дополнительной валидацией через сериализацию в NullOps.
	 * <p>Используется для проверки корректности стека при получении от клиента.</p>
	 *
	 * @param baseCodec базовый кодек стека
	 * @return кодек с дополнительной валидацией
	 */
	public static PacketCodec<RegistryByteBuf, ItemStack> createExtraValidatingPacketCodec(
			PacketCodec<RegistryByteBuf, ItemStack> baseCodec
	) {
		return new PacketCodec<>() {
			@Override
			public ItemStack decode(RegistryByteBuf buf) {
				ItemStack stack = baseCodec.decode(buf);
				if (!stack.isEmpty()) {
					RegistryOps<Unit> registryOps = buf.getRegistryManager().getOps(NullOps.INSTANCE);
					ItemStack.CODEC.encodeStart(registryOps, stack).getOrThrow(DecoderException::new);
				}

				return stack;
			}

			@Override
			public void encode(RegistryByteBuf buf, ItemStack stack) {
				baseCodec.encode(buf, stack);
			}
		};
	}

	public Optional<TooltipData> getTooltipData() {
		return getItem().getTooltipData(this);
	}

	@Override
	public ComponentMap getComponents() {
		return !isEmpty() ? components : ComponentMap.EMPTY;
	}

	public ComponentMap getDefaultComponents() {
		return !isEmpty() ? getItem().getComponents() : ComponentMap.EMPTY;
	}

	public ComponentChanges getComponentChanges() {
		return !isEmpty() ? components.getChanges() : ComponentChanges.EMPTY;
	}

	public ComponentMap getImmutableComponents() {
		return !isEmpty() ? components.immutableCopy() : ComponentMap.EMPTY;
	}

	public boolean hasChangedComponent(ComponentType<?> type) {
		return !isEmpty() && components.hasChanged(type);
	}

	public ItemStack(ItemConvertible item) {
		this(item, 1);
	}

	public ItemStack(RegistryEntry<Item> entry) {
		this(entry.value(), 1);
	}

	public ItemStack(RegistryEntry<Item> item, int count, ComponentChanges changes) {
		this(item.value(), count, MergedComponentMap.create(item.value().getComponents(), changes));
	}

	public ItemStack(RegistryEntry<Item> itemEntry, int count) {
		this(itemEntry.value(), count);
	}

	public ItemStack(ItemConvertible item, int count) {
		this(item, count, new MergedComponentMap(item.asItem().getComponents()));
	}

	private ItemStack(ItemConvertible item, int count, MergedComponentMap components) {
		this.item = item.asItem();
		this.count = count;
		this.components = components;
	}

	private ItemStack(@Nullable Void v) {
		item = null;
		components = new MergedComponentMap(ComponentMap.EMPTY);
	}

	/**
	 * Валидирует набор компонентов стека: проверяет совместимость прочности и стакаемости,
	 * а также допустимость количества предметов в контейнерах.
	 *
	 * @param components набор компонентов для проверки
	 * @return успех или ошибка с описанием нарушения
	 */
	public static DataResult<Unit> validateComponents(ComponentMap components) {
		if (components.contains(DataComponentTypes.MAX_DAMAGE)
				&& components.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1) > 1) {
			return DataResult.error(() -> "Item cannot be both damageable and stackable");
		}

		ContainerComponent container =
				components.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);

		for (ItemStack itemStack : container.iterateNonEmpty()) {
			int count = itemStack.getCount();
			int maxCount = itemStack.getMaxCount();
			if (count > maxCount) {
				return DataResult.error(() -> "Item stack with count of " + count + " was larger than maximum: " + maxCount);
			}
		}

		return DataResult.success(Unit.INSTANCE);
	}

	public boolean isEmpty() {
		return this == EMPTY || item == Items.AIR || count <= 0;
	}

	public boolean isItemEnabled(FeatureSet enabledFeatures) {
		return isEmpty() || getItem().isEnabled(enabledFeatures);
	}

	/**
	 * Отделяет указанное количество предметов от стека и возвращает их как новый стек.
	 *
	 * @param amount количество предметов для отделения
	 * @return новый стек с отделёнными предметами
	 */
	public ItemStack split(int amount) {
		int splitAmount = Math.min(amount, getCount());
		ItemStack result = copyWithCount(splitAmount);
		decrement(splitAmount);
		return result;
	}

	/**
	 * Копирует стек и обнуляет количество в текущем.
	 *
	 * @return копия стека с исходным количеством, текущий стек становится пустым
	 */
	public ItemStack copyAndEmpty() {
		if (isEmpty()) {
			return EMPTY;
		}

		ItemStack copy = copy();
		setCount(0);
		return copy;
	}

	public Item getItem() {
		return isEmpty() ? Items.AIR : item;
	}

	public RegistryEntry<Item> getRegistryEntry() {
		return getItem().getRegistryEntry();
	}

	public boolean isIn(TagKey<Item> tag) {
		return getItem().getRegistryEntry().isIn(tag);
	}

	public boolean isOf(Item item) {
		return getItem() == item;
	}

	public boolean itemMatches(Predicate<RegistryEntry<Item>> predicate) {
		return predicate.test(getItem().getRegistryEntry());
	}

	public boolean itemMatches(RegistryEntry<Item> itemEntry) {
		return getItem().getRegistryEntry() == itemEntry;
	}

	public boolean isIn(RegistryEntryList<Item> registryEntryList) {
		return registryEntryList.contains(getRegistryEntry());
	}

	public Stream<TagKey<Item>> streamTags() {
		return getItem().getRegistryEntry().streamTags();
	}

	/**
	 * Использует предмет на блоке. Проверяет права игрока на изменение мира
	 * и наличие компонента {@code CAN_PLACE_ON} перед делегированием в {@link Item#useOnBlock}.
	 *
	 * @param context контекст использования предмета на блоке
	 * @return результат действия
	 */
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		BlockPos blockPos = context.getBlockPos();

		if (player != null
				&& !player.getAbilities().allowModifyWorld
				&& !canPlaceOn(new CachedBlockPosition(context.getWorld(), blockPos, false))) {
			return ActionResult.PASS;
		}

		Item heldItem = getItem();
		ActionResult actionResult = heldItem.useOnBlock(context);

		if (player != null && actionResult instanceof ActionResult.Success success
				&& success.shouldIncrementStat()) {
			player.incrementStat(Stats.USED.getOrCreateStat(heldItem));
		}

		return actionResult;
	}

	public float getMiningSpeedMultiplier(BlockState state) {
		return getItem().getMiningSpeed(this, state);
	}

	/**
	 * Использует предмет игроком. Если предмет не требует длительного использования,
	 * автоматически применяет остаток и кулдаун после успешного действия.
	 *
	 * @param world мир
	 * @param user  игрок
	 * @param hand  рука
	 * @return результат действия
	 */
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack snapshot = copy();
		boolean isInstant = getMaxUseTime(user) <= 0;
		ActionResult actionResult = getItem().use(world, user, hand);

		return isInstant && actionResult instanceof ActionResult.Success success
		       ? success.withNewHandStack(
				success.getNewHandStack() == null
				? applyRemainderAndCooldown(user, snapshot)
				: success.getNewHandStack().applyRemainderAndCooldown(user, snapshot)
		)
		       : actionResult;
	}

	/**
	 * Завершает длительное использование предмета и применяет остаток и кулдаун.
	 *
	 * @param world мир
	 * @param user  сущность, использующая предмет
	 * @return итоговый стек после завершения использования
	 */
	public ItemStack finishUsing(World world, LivingEntity user) {
		ItemStack snapshot = copy();
		ItemStack result = getItem().finishUsing(this, world, user);
		return result.applyRemainderAndCooldown(user, snapshot);
	}

	/**
	 * Применяет компонент {@code USE_REMAINDER} (замена предмета после использования)
	 * и {@code USE_COOLDOWN} (кулдаун) к стеку.
	 *
	 * @param user  сущность, использовавшая предмет
	 * @param stack снимок стека до использования
	 * @return итоговый стек (может быть заменён остатком)
	 */
	private ItemStack applyRemainderAndCooldown(LivingEntity user, ItemStack stack) {
		UseRemainderComponent remainder = stack.get(DataComponentTypes.USE_REMAINDER);
		UseCooldownComponent cooldown = stack.get(DataComponentTypes.USE_COOLDOWN);
		int originalCount = stack.getCount();

		ItemStack result = this;
		if (remainder != null) {
			result = remainder.convert(this, originalCount, user.isInCreativeMode(), user::giveOrDropStack);
		}

		if (cooldown != null) {
			cooldown.set(stack, user);
		}

		return result;
	}

	public int getMaxCount() {
		return getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);
	}

	public boolean isStackable() {
		return getMaxCount() > 1 && (!isDamageable() || !isDamaged());
	}

	public boolean isDamageable() {
		return contains(DataComponentTypes.MAX_DAMAGE)
				&& !contains(DataComponentTypes.UNBREAKABLE)
				&& contains(DataComponentTypes.DAMAGE);
	}

	public boolean isDamaged() {
		return isDamageable() && getDamage() > 0;
	}

	public int getDamage() {
		return MathHelper.clamp(getOrDefault(DataComponentTypes.DAMAGE, 0), 0, getMaxDamage());
	}

	public void setDamage(int damage) {
		set(DataComponentTypes.DAMAGE, MathHelper.clamp(damage, 0, getMaxDamage()));
	}

	public int getMaxDamage() {
		return getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
	}

	public boolean shouldBreak() {
		return isDamageable() && getDamage() >= getMaxDamage();
	}

	public boolean willBreakNextUse() {
		return isDamageable() && getDamage() >= getMaxDamage() - 1;
	}

	public void damage(
			int amount,
			ServerWorld world,
			@Nullable ServerPlayerEntity player,
			Consumer<Item> breakCallback
	) {
		int actualDamage = calculateDamage(amount, world, player);
		if (actualDamage != 0) {
			onDurabilityChange(getDamage() + actualDamage, player, breakCallback);
		}
	}

	/**
	 * Вычисляет фактический урон прочности с учётом зачарований и режима творчества.
	 *
	 * @param baseDamage базовый урон прочности
	 * @param world      серверный мир (нужен для зачарований)
	 * @param player     игрок или {@code null}
	 * @return фактический урон прочности (0 если предмет нельзя повредить)
	 */
	private int calculateDamage(int baseDamage, ServerWorld world, @Nullable ServerPlayerEntity player) {
		if (!isDamageable()) {
			return 0;
		}

		if (player != null && player.isInCreativeMode()) {
			return 0;
		}

		return baseDamage > 0 ? EnchantmentHelper.getItemDamage(world, this, baseDamage) : baseDamage;
	}

	private void onDurabilityChange(int damage, @Nullable ServerPlayerEntity player, Consumer<Item> breakCallback) {
		if (player != null) {
			Criteria.ITEM_DURABILITY_CHANGED.trigger(player, this, damage);
		}

		setDamage(damage);

		if (shouldBreak()) {
			Item brokenItem = getItem();
			decrement(1);
			breakCallback.accept(brokenItem);
		}
	}

	/**
	 * Наносит урон прочности предмету игрока, не позволяя сломать его в руке
	 * (урон ограничивается до {@code maxDamage - 1}).
	 *
	 * @param amount количество урона прочности
	 * @param player игрок-владелец предмета
	 */
	public void damage(int amount, PlayerEntity player) {
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}

		int actualDamage = calculateDamage(amount, serverPlayer.getEntityWorld(), serverPlayer);
		if (actualDamage == 0) {
			return;
		}

		int clampedDamage = Math.min(getDamage() + actualDamage, getMaxDamage() - 1);
		onDurabilityChange(clampedDamage, serverPlayer, item -> {});
	}

	public void damage(int amount, LivingEntity entity, Hand hand) {
		damage(amount, entity, hand.getEquipmentSlot());
	}

	public void damage(int amount, LivingEntity entity, EquipmentSlot slot) {
		if (entity.getEntityWorld() instanceof ServerWorld serverWorld) {
			damage(
					amount,
					serverWorld,
					entity instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null,
					brokenItem -> entity.sendEquipmentBreakStatus(brokenItem, slot)
			);
		}
	}

	/**
	 * Наносит урон прочности и возвращает новый стек если предмет сломался.
	 * <p>При поломке создаёт стек из {@code itemAfterBreaking} с перенесёнными компонентами.</p>
	 *
	 * @param amount           количество урона прочности
	 * @param itemAfterBreaking предмет, который появится после поломки
	 * @param entity           сущность-владелец
	 * @param slot             слот экипировки
	 * @return текущий стек или новый стек после поломки
	 */
	public ItemStack damage(int amount, ItemConvertible itemAfterBreaking, LivingEntity entity, EquipmentSlot slot) {
		damage(amount, entity, slot);
		if (!isEmpty()) {
			return this;
		}

		ItemStack replacement = copyComponentsToNewStackIgnoreEmpty(itemAfterBreaking, 1);
		if (replacement.isDamageable()) {
			replacement.setDamage(0);
		}

		return replacement;
	}

	public boolean isItemBarVisible() {
		return getItem().isItemBarVisible(this);
	}

	public int getItemBarStep() {
		return getItem().getItemBarStep(this);
	}

	public int getItemBarColor() {
		return getItem().getItemBarColor(this);
	}

	public boolean onStackClicked(Slot slot, ClickType clickType, PlayerEntity player) {
		return getItem().onStackClicked(this, slot, clickType, player);
	}

	public boolean onClicked(
			ItemStack stack,
			Slot slot,
			ClickType clickType,
			PlayerEntity player,
			StackReference cursorStackReference
	) {
		return getItem().onClicked(this, stack, slot, clickType, player, cursorStackReference);
	}

	/**
	 * Вызывается после удара по сущности данным предметом.
	 * <p>Если у предмета есть компонент {@code WEAPON}, увеличивает статистику использования.</p>
	 *
	 * @param target цель удара
	 * @param user   атакующая сущность
	 * @return {@code true} если предмет является оружием
	 */
	public boolean postHit(LivingEntity target, LivingEntity user) {
		Item heldItem = getItem();
		heldItem.postHit(this, target, user);

		if (!contains(DataComponentTypes.WEAPON)) {
			return false;
		}

		if (user instanceof PlayerEntity player) {
			player.incrementStat(Stats.USED.getOrCreateStat(heldItem));
		}

		return true;
	}

	public void postDamageEntity(LivingEntity target, LivingEntity user) {
		getItem().postDamageEntity(this, target, user);
		WeaponComponent weapon = get(DataComponentTypes.WEAPON);
		if (weapon != null) {
			damage(weapon.itemDamagePerAttack(), user, EquipmentSlot.MAINHAND);
		}
	}

	public void postMine(World world, BlockState state, BlockPos pos, PlayerEntity miner) {
		Item heldItem = getItem();
		if (heldItem.postMine(this, world, state, pos, miner)) {
			miner.incrementStat(Stats.USED.getOrCreateStat(heldItem));
		}
	}

	public boolean isSuitableFor(BlockState state) {
		return getItem().isCorrectForDrops(this, state);
	}

	/**
	 * Использует предмет на сущности. Сначала проверяет компонент {@code EQUIPPABLE}
	 * с флагом {@code equipOnInteract}, затем делегирует в {@link Item#useOnEntity}.
	 *
	 * @param user   игрок
	 * @param entity целевая сущность
	 * @param hand   рука
	 * @return результат действия
	 */
	public ActionResult useOnEntity(PlayerEntity user, LivingEntity entity, Hand hand) {
		EquippableComponent equippable = get(DataComponentTypes.EQUIPPABLE);
		if (equippable != null && equippable.equipOnInteract()) {
			ActionResult actionResult = equippable.equipOnInteract(user, entity, this);
			if (actionResult != ActionResult.PASS) {
				return actionResult;
			}
		}

		return getItem().useOnEntity(this, user, entity, hand);
	}
public ItemStack copy() {
	if (isEmpty()) {
		return EMPTY;
	}

	ItemStack copy = new ItemStack(getItem(), count, components.copy());
	copy.setBobbingAnimationTime(getBobbingAnimationTime());
	return copy;
}

public ItemStack copyWithCount(int count) {
	if (isEmpty()) {
		return EMPTY;
	}

	ItemStack copy = copy();
	copy.setCount(count);
	return copy;
}

public ItemStack withItem(ItemConvertible item) {
	return copyComponentsToNewStack(item, getCount());
}

public ItemStack copyComponentsToNewStack(ItemConvertible item, int count) {
	return isEmpty() ? EMPTY : copyComponentsToNewStackIgnoreEmpty(item, count);
}

private ItemStack copyComponentsToNewStackIgnoreEmpty(ItemConvertible item, int count) {
	return new ItemStack(item.asItem().getRegistryEntry(), count, components.getChanges());
}

/**
 * Проверяет полное равенство двух стеков: тип предмета, количество и компоненты.
 *
 * @param left  первый стек
 * @param right второй стек
 * @return {@code true} если стеки полностью идентичны
 */
public static boolean areEqual(ItemStack left, ItemStack right) {
	if (left == right) {
		return true;
	}

	return left.getCount() == right.getCount() && areItemsAndComponentsEqual(left, right);
}

@Deprecated
public static boolean stacksEqual(List<ItemStack> left, List<ItemStack> right) {
	if (left.size() != right.size()) {
		return false;
	}

	for (int index = 0; index < left.size(); index++) {
		if (!areEqual(left.get(index), right.get(index))) {
			return false;
		}
	}

	return true;
}

public static boolean areItemsEqual(ItemStack left, ItemStack right) {
	return left.isOf(right.getItem());
}

public static boolean areItemsAndComponentsEqual(ItemStack stack, ItemStack otherStack) {
	if (!stack.isOf(otherStack.getItem())) {
		return false;
	}

	return stack.isEmpty() && otherStack.isEmpty() || Objects.equals(stack.components, otherStack.components);
}

/**
 * Определяет, нужно ли пропустить анимацию руки при смене стека в слоте.
 * <p>Пропускает анимацию если стеки идентичны или отличаются только компонентами
 * из списка {@code skippedComponent} (например, кулдаун или анимация).</p>
 *
 * @param from             предыдущий стек
 * @param to               новый стек
 * @param skippedComponent предикат компонентов, изменение которых не вызывает анимацию
 * @return {@code true} если анимацию следует пропустить
 */
public static boolean shouldSkipHandAnimationOnSwap(
		ItemStack from,
		ItemStack to,
		Predicate<ComponentType<?>> skippedComponent
) {
	if (from == to) {
		return true;
	}

	if (from.getCount() != to.getCount()) {
		return false;
	}

	if (!from.isOf(to.getItem())) {
		return false;
	}

	if (from.isEmpty() && to.isEmpty()) {
		return true;
	}

	if (from.components.size() != to.components.size()) {
		return false;
	}

	for (ComponentType<?> componentType : from.components.getTypes()) {
		Object fromValue = from.components.get(componentType);
		Object toValue = to.components.get(componentType);
		if (fromValue == null || toValue == null) {
			return false;
		}

		if (!Objects.equals(fromValue, toValue) && !skippedComponent.test(componentType)) {
			return false;
		}
	}

	return true;
}

public static MapCodec<ItemStack> createOptionalCodec(String fieldName) {
	return CODEC
			.lenientOptionalFieldOf(fieldName)
			.xmap(
					optional -> optional.orElse(EMPTY),
					stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack)
			);
}

/**
 * Вычисляет хэш-код стека предмета на основе типа предмета и его компонентов.
 *
 * @param stack стек или {@code null}
 * @return хэш-код стека, 0 для {@code null}
 */
public static int hashCode(@Nullable ItemStack stack) {
	if (stack == null) {
		return 0;
	}

	int hash = 31 + stack.getItem().hashCode();
	return 31 * hash + stack.getComponents().hashCode();
}

@Deprecated
public static int listHashCode(List<ItemStack> stacks) {
	int hash = 0;
	for (ItemStack stack : stacks) {
		hash = hash * 31 + hashCode(stack);
	}

	return hash;
}

@Override
public String toString() {
	return getCount() + " " + getItem();
}

public void inventoryTick(World world, Entity entity, @Nullable EquipmentSlot slot) {
	if (bobbingAnimationTime > 0) {
		bobbingAnimationTime--;
	}

	if (world instanceof ServerWorld serverWorld) {
		getItem().inventoryTick(this, serverWorld, entity, slot);
	}
}

public void onCraftByPlayer(PlayerEntity player, int amount) {
	player.increaseStat(Stats.CRAFTED.getOrCreateStat(getItem()), amount);
	getItem().onCraftByPlayer(this, player);
}

public void onCraftByCrafter(World world) {
	getItem().onCraft(this, world);
}

public int getMaxUseTime(LivingEntity user) {
	return getItem().getMaxUseTime(this, user);
}

public UseAction getUseAction() {
	return getItem().getUseAction(this);
}

public void onStoppedUsing(World world, LivingEntity user, int remainingUseTicks) {
	ItemStack snapshot = copy();
	if (!getItem().onStoppedUsing(this, world, user, remainingUseTicks)) {
		return;
	}

	ItemStack result = applyRemainderAndCooldown(user, snapshot);
	if (result != this) {
		user.setStackInHand(user.getActiveHand(), result);
	}
}

/**
 * Испускает игровое событие использования, если компонент {@code USE_EFFECTS}
 * разрешает вибрации взаимодействия.
 *
 * @param user      сущность, использующая предмет
 * @param gameEvent событие для испускания
 */
public void emitUseGameEvent(Entity user, RegistryEntry.Reference<GameEvent> gameEvent) {
	UseEffectsComponent useEffects = get(DataComponentTypes.USE_EFFECTS);
	if (useEffects != null && useEffects.interactVibrations()) {
		user.emitGameEvent(gameEvent);
	}
}

public boolean isUsedOnRelease() {
	return getItem().isUsedOnRelease(this);
}

public <T> @Nullable T set(ComponentType<T> type, @Nullable T value) {
	return components.set(type, value);
}

public <T> @Nullable T set(Component<T> component) {
	return components.set(component);
}

public <T> void copy(ComponentType<T> type, ComponentsAccess from) {
	set(type, from.get(type));
}

public <T, U> @Nullable T apply(ComponentType<T> type, T defaultValue, U change, BiFunction<T, U, T> applier) {
	return set(type, applier.apply(getOrDefault(type, defaultValue), change));
}

public <T> @Nullable T apply(ComponentType<T> type, T defaultValue, UnaryOperator<T> applier) {
	T current = getOrDefault(type, defaultValue);
	return set(type, applier.apply(current));
}

public <T> @Nullable T remove(ComponentType<? extends T> type) {
	return components.remove(type);
}

/**
 * Применяет изменения компонентов с валидацией. При ошибке откатывает изменения
 * и логирует сообщение об ошибке.
 *
 * @param changes изменения компонентов для применения
 */
public void applyChanges(ComponentChanges changes) {
	ComponentChanges previousChanges = components.getChanges();
	components.applyChanges(changes);
	Optional<Error<ItemStack>> error = validate(this).error();
	if (error.isPresent()) {
		LOGGER.error("Failed to apply component patch '{}' to item: '{}'", changes, error.get().message());
		components.setChanges(previousChanges);
	}
}

public void applyUnvalidatedChanges(ComponentChanges changes) {
	components.applyChanges(changes);
}

public void applyComponentsFrom(ComponentMap components) {
	this.components.setAll(components);
}

public Text getName() {
	Text customName = getCustomName();
	return customName != null ? customName : getItemName();
}

public @Nullable Text getCustomName() {
	Text customName = get(DataComponentTypes.CUSTOM_NAME);
	if (customName != null) {
		return customName;
	}

	WrittenBookContentComponent writtenBook = get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
	if (writtenBook != null) {
		String title = writtenBook.title().raw();
		if (!StringHelper.isBlank(title)) {
			return Text.literal(title);
		}
	}

	return null;
}

public Text getItemName() {
	return getItem().getName(this);
}

public Text getFormattedName() {
	MutableText name = Text.empty().append(getName()).formatted(getRarity().getFormatting());
	if (contains(DataComponentTypes.CUSTOM_NAME)) {
		name.formatted(Formatting.ITALIC);
	}

	return name;
}

public <T extends TooltipAppender> void appendComponentTooltip(
		ComponentType<T> componentType,
		Item.TooltipContext context,
		TooltipDisplayComponent displayComponent,
		Consumer<Text> textConsumer,
		TooltipType type
) {
	T tooltipAppender = (T) get(componentType);
	if (tooltipAppender != null && displayComponent.shouldDisplay(componentType)) {
		tooltipAppender.appendTooltip(context, textConsumer, type, components);
	}
}

/**
 * Формирует полный список строк подсказки для предмета.
 * <p>Если подсказка скрыта ({@code hideTooltip}), возвращает предупреждения оператора
 * или пустой список. Иначе собирает все компоненты подсказки.</p>
 *
 * @param context контекст подсказки
 * @param player  игрок или {@code null}
 * @param type    тип подсказки (обычная, расширенная, творческая)
 * @return список строк подсказки
 */
public List<Text> getTooltip(Item.TooltipContext context, @Nullable PlayerEntity player, TooltipType type) {
	TooltipDisplayComponent displayComponent =
			getOrDefault(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT);

	if (!type.isCreative() && displayComponent.hideTooltip()) {
		boolean showWarnings = getItem().shouldShowOperatorBlockWarnings(this, player);
		return showWarnings ? OPERATOR_WARNINGS : List.of();
	}

	List<Text> lines = Lists.newArrayList();
	lines.add(getFormattedName());
	appendTooltip(context, displayComponent, player, type, lines::add);
	return lines;
}

public void appendTooltip(
		Item.TooltipContext context,
		TooltipDisplayComponent displayComponent,
		@Nullable PlayerEntity player,
		TooltipType type,
		Consumer<Text> textConsumer
) {
	getItem().appendTooltip(this, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.TROPICAL_FISH_PATTERN, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.INSTRUMENT, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.MAP_ID, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.BEES, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.CONTAINER_LOOT, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.CONTAINER, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.BANNER_PATTERNS, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.POT_DECORATIONS, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.WRITTEN_BOOK_CONTENT, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.CHARGED_PROJECTILES, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.FIREWORKS, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.FIREWORK_EXPLOSION, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.POTION_CONTENTS, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.JUKEBOX_PLAYABLE, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.TRIM, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.STORED_ENCHANTMENTS, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.ENCHANTMENTS, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.DYED_COLOR, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.PROFILE, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.LORE, context, displayComponent, textConsumer, type);
	appendAttributeModifiersTooltip(textConsumer, displayComponent, player);
	appendTooltipIfComponentExists(DataComponentTypes.INTANGIBLE_PROJECTILE, INTANGIBLE_TEXT, displayComponent, textConsumer);
	appendTooltipIfComponentExists(DataComponentTypes.UNBREAKABLE, UNBREAKABLE_TEXT, displayComponent, textConsumer);
	appendComponentTooltip(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.BLOCK_STATE, context, displayComponent, textConsumer, type);
	appendComponentTooltip(DataComponentTypes.ENTITY_DATA, context, displayComponent, textConsumer, type);

	if ((isOf(Items.SPAWNER) || isOf(Items.TRIAL_SPAWNER))
			&& displayComponent.shouldDisplay(DataComponentTypes.BLOCK_ENTITY_DATA)) {
		TypedEntityData<BlockEntityType<?>> blockEntityData = get(DataComponentTypes.BLOCK_ENTITY_DATA);
		Spawner.appendSpawnDataToTooltip(blockEntityData, textConsumer, "SpawnData");
	}

	BlockPredicatesComponent canBreak = get(DataComponentTypes.CAN_BREAK);
	if (canBreak != null && displayComponent.shouldDisplay(DataComponentTypes.CAN_BREAK)) {
		textConsumer.accept(ScreenTexts.EMPTY);
		textConsumer.accept(BlockPredicatesComponent.CAN_BREAK_TEXT);
		canBreak.addTooltips(textConsumer);
	}

	BlockPredicatesComponent canPlaceOn = get(DataComponentTypes.CAN_PLACE_ON);
	if (canPlaceOn != null && displayComponent.shouldDisplay(DataComponentTypes.CAN_PLACE_ON)) {
		textConsumer.accept(ScreenTexts.EMPTY);
		textConsumer.accept(BlockPredicatesComponent.CAN_PLACE_TEXT);
		canPlaceOn.addTooltips(textConsumer);
	}

	if (type.isAdvanced()) {
		if (isDamaged() && displayComponent.shouldDisplay(DataComponentTypes.DAMAGE)) {
			textConsumer.accept(Text.translatable(
					"item.durability",
					getMaxDamage() - getDamage(),
					getMaxDamage()
			));
		}

		textConsumer.accept(Text
				.literal(Registries.ITEM.getId(getItem()).toString())
				.formatted(Formatting.DARK_GRAY));

		int componentCount = components.size();
		if (componentCount > 0) {
			textConsumer.accept(Text.translatable("item.components", componentCount).formatted(Formatting.DARK_GRAY));
		}
	}

	if (player != null && !getItem().isEnabled(player.getEntityWorld().getEnabledFeatures())) {
		textConsumer.accept(DISABLED_TEXT);
	}

	if (getItem().shouldShowOperatorBlockWarnings(this, player)) {
		OPERATOR_WARNINGS.forEach(textConsumer);
	}
}

private void appendTooltipIfComponentExists(
		ComponentType<?> type,
		Text tooltip,
		TooltipDisplayComponent displayComponent,
		Consumer<Text> textConsumer
) {
	if (contains(type) && displayComponent.shouldDisplay(type)) {
		textConsumer.accept(tooltip);
	}
}

private void appendAttributeModifiersTooltip(
		Consumer<Text> textConsumer,
		TooltipDisplayComponent displayComponent,
		@Nullable PlayerEntity player
) {
	if (!displayComponent.shouldDisplay(DataComponentTypes.ATTRIBUTE_MODIFIERS)) {
		return;
	}

	for (AttributeModifierSlot slot : AttributeModifierSlot.values()) {
		MutableBoolean isFirstEntry = new MutableBoolean(true);
		applyAttributeModifier(
				slot, (attribute, modifier, display) -> {
					if (display == AttributeModifiersComponent.Display.getHidden()) {
						return;
					}

					if (isFirstEntry.isTrue()) {
						textConsumer.accept(ScreenTexts.EMPTY);
						textConsumer.accept(Text
								.translatable("item.modifiers." + slot.asString())
								.formatted(Formatting.GRAY));
						isFirstEntry.setFalse();
					}

					display.addTooltip(textConsumer, player, attribute, modifier);
				}
		);
	}
}

public boolean hasGlint() {
	Boolean glintOverride = get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
	return glintOverride != null ? glintOverride : getItem().hasGlint(this);
}

public Rarity getRarity() {
	Rarity rarity = getOrDefault(DataComponentTypes.RARITY, Rarity.COMMON);
	if (!hasEnchantments()) {
		return rarity;
	}

	return switch (rarity) {
		case COMMON, UNCOMMON -> Rarity.RARE;
		case RARE -> Rarity.EPIC;
		default -> rarity;
	};
}

public boolean isEnchantable() {
	if (!contains(DataComponentTypes.ENCHANTABLE)) {
		return false;
	}

	ItemEnchantmentsComponent enchantments = get(DataComponentTypes.ENCHANTMENTS);
	return enchantments != null && enchantments.isEmpty();
}

public void addEnchantment(RegistryEntry<Enchantment> enchantment, int level) {
	EnchantmentHelper.apply(this, builder -> builder.add(enchantment, level));
}

public boolean hasEnchantments() {
	return !getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT).isEmpty();
}

public ItemEnchantmentsComponent getEnchantments() {
	return getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
}

public boolean isInFrame() {
	return holder instanceof ItemFrameEntity;
}

public void setHolder(@Nullable Entity holder) {
	if (!isEmpty()) {
		this.holder = holder;
	}
}

public @Nullable ItemFrameEntity getFrame() {
	return holder instanceof ItemFrameEntity frame ? frame : null;
}

public @Nullable Entity getHolder() {
	return !isEmpty() ? holder : null;
}

public void applyAttributeModifier(
		AttributeModifierSlot slot,
		TriConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier, AttributeModifiersComponent.Display> consumer
) {
	AttributeModifiersComponent attributeModifiers =
			getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
	attributeModifiers.applyModifiers(slot, consumer);
	EnchantmentHelper.applyAttributeModifiers(
			this,
			slot,
			(attribute, modifier) -> consumer.accept(
					attribute,
					modifier,
					AttributeModifiersComponent.Display.getDefault()
			)
	);
}

public void applyAttributeModifiers(
		EquipmentSlot slot,
		BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> consumer
) {
	AttributeModifiersComponent attributeModifiers =
			getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
	attributeModifiers.applyModifiers(slot, consumer);
	EnchantmentHelper.applyAttributeModifiers(this, slot, consumer);
}

/**
 * Создаёт текст с hover-событием, показывающим информацию о предмете при наведении.
 *
 * @return текст с именем предмета и hover-событием
 */
public Text toHoverableText() {
	MutableText name = Text.empty().append(getName());
	if (contains(DataComponentTypes.CUSTOM_NAME)) {
		name.formatted(Formatting.ITALIC);
	}

	MutableText bracketed = Texts.bracketed(name);
	if (!isEmpty()) {
		bracketed
				.formatted(getRarity().getFormatting())
				.styled(style -> style.withHoverEvent(new HoverEvent.ShowItem(this)));
	}

	return bracketed;
}

public SwingAnimationComponent getSwingAnimation() {
	return getOrDefault(DataComponentTypes.SWING_ANIMATION, SwingAnimationComponent.DEFAULT);
}

public boolean canPlaceOn(CachedBlockPosition pos) {
	BlockPredicatesComponent canPlaceOn = get(DataComponentTypes.CAN_PLACE_ON);
	return canPlaceOn != null && canPlaceOn.check(pos);
}

public boolean canBreak(CachedBlockPosition pos) {
	BlockPredicatesComponent canBreak = get(DataComponentTypes.CAN_BREAK);
	return canBreak != null && canBreak.check(pos);
}

public int getBobbingAnimationTime() {
	return bobbingAnimationTime;
}

public void setBobbingAnimationTime(int bobbingAnimationTime) {
	this.bobbingAnimationTime = bobbingAnimationTime;
}

public int getCount() {
	return isEmpty() ? 0 : count;
}

public void setCount(int count) {
	this.count = count;
}

public void capCount(int maxCount) {
	if (!isEmpty() && getCount() > maxCount) {
		setCount(maxCount);
	}
}

public void increment(int amount) {
	setCount(getCount() + amount);
}

public void decrement(int amount) {
	increment(-amount);
}

public void decrementUnlessCreative(int amount, @Nullable LivingEntity entity) {
	if (entity == null || !entity.isInCreativeMode()) {
		decrement(amount);
	}
}

public ItemStack splitUnlessCreative(int amount, @Nullable LivingEntity entity) {
	ItemStack split = copyWithCount(amount);
	decrementUnlessCreative(amount, entity);
	return split;
}

public void usageTick(World world, LivingEntity user, int remainingUseTicks) {
	ConsumableComponent consumable = get(DataComponentTypes.CONSUMABLE);
	if (consumable != null && consumable.shouldSpawnParticlesAndPlaySounds(remainingUseTicks)) {
		consumable.spawnParticlesAndPlaySound(user.getRandom(), user, this, 5);
	}

	KineticWeaponComponent kineticWeapon = get(DataComponentTypes.KINETIC_WEAPON);
	if (kineticWeapon != null && !world.isClient()) {
		kineticWeapon.usageTick(this, remainingUseTicks, user, user.getActiveHand().getEquipmentSlot());
	} else {
		getItem().usageTick(world, user, this, remainingUseTicks);
	}
}

public void onItemEntityDestroyed(ItemEntity entity) {
	getItem().onItemEntityDestroyed(entity);
}

public boolean takesDamageFrom(DamageSource source) {
	DamageResistantComponent resistant = get(DataComponentTypes.DAMAGE_RESISTANT);
	return resistant == null || !resistant.resists(source);
}

public boolean canRepairWith(ItemStack ingredient) {
	RepairableComponent repairable = get(DataComponentTypes.REPAIRABLE);
	return repairable != null && repairable.matches(ingredient);
}

public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity player) {
	return getItem().canMine(this, state, world, pos, player);
}

/**
 * Определяет источник урона для данного предмета.
 * <p>Сначала проверяет компонент {@code DAMAGE_TYPE}, затем метод предмета,
 * и в крайнем случае использует {@code fallbackSupplier}.</p>
 *
 * @param attacker         атакующая сущность
 * @param fallbackSupplier поставщик источника урона по умолчанию
 * @return источник урона
 */
public DamageSource getDamageSource(LivingEntity attacker, Supplier<DamageSource> fallbackSupplier) {
	return Optional.ofNullable(get(DataComponentTypes.DAMAGE_TYPE))
	               .flatMap(ref -> ref.resolveEntry(attacker.getRegistryManager()))
	               .map(typeEntry -> new DamageSource((RegistryEntry<DamageType>) typeEntry, attacker))
	               .or(() -> Optional.ofNullable(getItem().getDamageSource(attacker)))
	               .orElseGet(fallbackSupplier);
}
}
		
