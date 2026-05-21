package net.minecraft.village;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * {@code VillagerProfession}.
 */
public record VillagerProfession(
		Text id,
		Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation,
		Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation,
		ImmutableSet<Item> gatherableItems,
		ImmutableSet<Block> secondaryJobSites,
		@Nullable SoundEvent workSound
) {

	public static final Predicate<RegistryEntry<PointOfInterestType>> IS_ACQUIRABLE_JOB_SITE = poiType -> poiType.isIn(
			PointOfInterestTypeTags.ACQUIRABLE_JOB_SITE
	);
	public static final RegistryKey<VillagerProfession> NONE = of("none");
	public static final RegistryKey<VillagerProfession> ARMORER = of("armorer");
	public static final RegistryKey<VillagerProfession> BUTCHER = of("butcher");
	public static final RegistryKey<VillagerProfession> CARTOGRAPHER = of("cartographer");
	public static final RegistryKey<VillagerProfession> CLERIC = of("cleric");
	public static final RegistryKey<VillagerProfession> FARMER = of("farmer");
	public static final RegistryKey<VillagerProfession> FISHERMAN = of("fisherman");
	public static final RegistryKey<VillagerProfession> FLETCHER = of("fletcher");
	public static final RegistryKey<VillagerProfession> LEATHERWORKER = of("leatherworker");
	public static final RegistryKey<VillagerProfession> LIBRARIAN = of("librarian");
	public static final RegistryKey<VillagerProfession> MASON = of("mason");
	public static final RegistryKey<VillagerProfession> NITWIT = of("nitwit");
	public static final RegistryKey<VillagerProfession> SHEPHERD = of("shepherd");
	public static final RegistryKey<VillagerProfession> TOOLSMITH = of("toolsmith");
	public static final RegistryKey<VillagerProfession> WEAPONSMITH = of("weaponsmith");

	private static RegistryKey<VillagerProfession> of(String id) {
		return RegistryKey.of(RegistryKeys.VILLAGER_PROFESSION, Identifier.ofVanilla(id));
	}

	private static VillagerProfession register(
			Registry<VillagerProfession> registry,
			RegistryKey<VillagerProfession> key,
			RegistryKey<PointOfInterestType> heldWorkstation,
			@Nullable SoundEvent workSound
	) {
		return register(
				registry,
				key,
				entry -> entry.matchesKey(heldWorkstation),
				entry -> entry.matchesKey(heldWorkstation),
				workSound
		);
	}

	private static VillagerProfession register(
			Registry<VillagerProfession> registry,
			RegistryKey<VillagerProfession> key,
			Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation,
			Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation,
			@Nullable SoundEvent workSound
	) {
		return register(
				registry,
				key,
				heldWorkstation,
				acquirableWorkstation,
				ImmutableSet.of(),
				ImmutableSet.of(),
				workSound
		);
	}

	private static VillagerProfession register(
			Registry<VillagerProfession> registry,
			RegistryKey<VillagerProfession> key,
			RegistryKey<PointOfInterestType> heldWorkstation,
			ImmutableSet<Item> gatherableItems,
			ImmutableSet<Block> secondaryJobSites,
			@Nullable SoundEvent workSound
	) {
		return register(
				registry,
				key,
				entry -> entry.matchesKey(heldWorkstation),
				entry -> entry.matchesKey(heldWorkstation),
				gatherableItems,
				secondaryJobSites,
				workSound
		);
	}

	private static VillagerProfession register(
			Registry<VillagerProfession> registry,
			RegistryKey<VillagerProfession> key,
			Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation,
			Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation,
			ImmutableSet<Item> gatherableItems,
			ImmutableSet<Block> secondaryJobSites,
			@Nullable SoundEvent workSound
	) {
		return Registry.register(
				registry,
				key,
				new VillagerProfession(
						Text.translatable(
								"entity." + key.getValue().getNamespace() + ".villager." + key.getValue().getPath()),
						heldWorkstation,
						acquirableWorkstation,
						gatherableItems,
						secondaryJobSites,
						workSound
				)
		);
	}

	public static VillagerProfession registerAndGetDefault(Registry<VillagerProfession> registry) {
		register(registry, NONE, PointOfInterestType.NONE, IS_ACQUIRABLE_JOB_SITE, null);
		register(registry, ARMORER, PointOfInterestTypes.ARMORER, SoundEvents.ENTITY_VILLAGER_WORK_ARMORER);
		register(registry, BUTCHER, PointOfInterestTypes.BUTCHER, SoundEvents.ENTITY_VILLAGER_WORK_BUTCHER);
		register(
				registry,
				CARTOGRAPHER,
				PointOfInterestTypes.CARTOGRAPHER,
				SoundEvents.ENTITY_VILLAGER_WORK_CARTOGRAPHER
		);
		register(registry, CLERIC, PointOfInterestTypes.CLERIC, SoundEvents.ENTITY_VILLAGER_WORK_CLERIC);
		register(
				registry,
				FARMER,
				PointOfInterestTypes.FARMER,
				ImmutableSet.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.BONE_MEAL),
				ImmutableSet.of(Blocks.FARMLAND),
				SoundEvents.ENTITY_VILLAGER_WORK_FARMER
		);
		register(registry, FISHERMAN, PointOfInterestTypes.FISHERMAN, SoundEvents.ENTITY_VILLAGER_WORK_FISHERMAN);
		register(registry, FLETCHER, PointOfInterestTypes.FLETCHER, SoundEvents.ENTITY_VILLAGER_WORK_FLETCHER);
		register(
				registry,
				LEATHERWORKER,
				PointOfInterestTypes.LEATHERWORKER,
				SoundEvents.ENTITY_VILLAGER_WORK_LEATHERWORKER
		);
		register(registry, LIBRARIAN, PointOfInterestTypes.LIBRARIAN, SoundEvents.ENTITY_VILLAGER_WORK_LIBRARIAN);
		register(registry, MASON, PointOfInterestTypes.MASON, SoundEvents.ENTITY_VILLAGER_WORK_MASON);
		register(registry, NITWIT, PointOfInterestType.NONE, PointOfInterestType.NONE, null);
		register(registry, SHEPHERD, PointOfInterestTypes.SHEPHERD, SoundEvents.ENTITY_VILLAGER_WORK_SHEPHERD);
		register(registry, TOOLSMITH, PointOfInterestTypes.TOOLSMITH, SoundEvents.ENTITY_VILLAGER_WORK_TOOLSMITH);
		return register(
				registry,
				WEAPONSMITH,
				PointOfInterestTypes.WEAPONSMITH,
				SoundEvents.ENTITY_VILLAGER_WORK_WEAPONSMITH
		);
	}
}
