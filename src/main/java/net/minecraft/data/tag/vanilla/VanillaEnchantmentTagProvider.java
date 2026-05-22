package net.minecraft.data.tag.vanilla;

import net.minecraft.data.DataOutput;
import net.minecraft.data.tag.EnchantmentTagProvider;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.EnchantmentTags;

import java.util.concurrent.CompletableFuture;

/**
 * {@code VanillaEnchantmentTagProvider}.
 */
public class VanillaEnchantmentTagProvider extends EnchantmentTagProvider {

	public VanillaEnchantmentTagProvider(
			DataOutput dataOutput,
			CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture
	) {
		super(dataOutput, completableFuture);
	}

	@Override
	protected void configure(RegistryWrapper.WrapperLookup registries) {
		this.createTooltipOrderTag(
				registries,
				Enchantments.BINDING_CURSE,
				Enchantments.VANISHING_CURSE,
				Enchantments.RIPTIDE,
				Enchantments.CHANNELING,
				Enchantments.WIND_BURST,
				Enchantments.FROST_WALKER,
				Enchantments.LUNGE,
				Enchantments.SHARPNESS,
				Enchantments.SMITE,
				Enchantments.BANE_OF_ARTHROPODS,
				Enchantments.IMPALING,
				Enchantments.POWER,
				Enchantments.DENSITY,
				Enchantments.BREACH,
				Enchantments.PIERCING,
				Enchantments.SWEEPING_EDGE,
				Enchantments.MULTISHOT,
				Enchantments.FIRE_ASPECT,
				Enchantments.FLAME,
				Enchantments.KNOCKBACK,
				Enchantments.PUNCH,
				Enchantments.PROTECTION,
				Enchantments.BLAST_PROTECTION,
				Enchantments.FIRE_PROTECTION,
				Enchantments.PROJECTILE_PROTECTION,
				Enchantments.FEATHER_FALLING,
				Enchantments.FORTUNE,
				Enchantments.LOOTING,
				Enchantments.SILK_TOUCH,
				Enchantments.LUCK_OF_THE_SEA,
				Enchantments.EFFICIENCY,
				Enchantments.QUICK_CHARGE,
				Enchantments.LURE,
				Enchantments.RESPIRATION,
				Enchantments.AQUA_AFFINITY,
				Enchantments.SOUL_SPEED,
				Enchantments.SWIFT_SNEAK,
				Enchantments.DEPTH_STRIDER,
				Enchantments.THORNS,
				Enchantments.LOYALTY,
				Enchantments.UNBREAKING,
				Enchantments.INFINITY,
				Enchantments.MENDING
		);
		builder(EnchantmentTags.ARMOR_EXCLUSIVE_SET)
		    .add(
				    Enchantments.PROTECTION,
				    Enchantments.BLAST_PROTECTION,
				    Enchantments.FIRE_PROTECTION,
				    Enchantments.PROJECTILE_PROTECTION
		    );
		builder(EnchantmentTags.BOOTS_EXCLUSIVE_SET).add(Enchantments.FROST_WALKER, Enchantments.DEPTH_STRIDER);
		builder(EnchantmentTags.BOW_EXCLUSIVE_SET).add(Enchantments.INFINITY, Enchantments.MENDING);
		builder(EnchantmentTags.CROSSBOW_EXCLUSIVE_SET).add(Enchantments.MULTISHOT, Enchantments.PIERCING);
		builder(EnchantmentTags.DAMAGE_EXCLUSIVE_SET)
		    .add(
				    Enchantments.SHARPNESS,
				    Enchantments.SMITE,
				    Enchantments.BANE_OF_ARTHROPODS,
				    Enchantments.IMPALING,
				    Enchantments.DENSITY,
				    Enchantments.BREACH
		    );
		builder(EnchantmentTags.MINING_EXCLUSIVE_SET).add(Enchantments.FORTUNE, Enchantments.SILK_TOUCH);
		builder(EnchantmentTags.RIPTIDE_EXCLUSIVE_SET).add(Enchantments.LOYALTY, Enchantments.CHANNELING);
		builder(EnchantmentTags.TREASURE)
		    .add(
				    Enchantments.BINDING_CURSE,
				    Enchantments.VANISHING_CURSE,
				    Enchantments.SWIFT_SNEAK,
				    Enchantments.SOUL_SPEED,
				    Enchantments.FROST_WALKER,
				    Enchantments.MENDING,
				    Enchantments.WIND_BURST
		    );
		builder(EnchantmentTags.NON_TREASURE)
		    .add(
				    Enchantments.PROTECTION,
				    Enchantments.FIRE_PROTECTION,
				    Enchantments.FEATHER_FALLING,
				    Enchantments.BLAST_PROTECTION,
				    Enchantments.PROJECTILE_PROTECTION,
				    Enchantments.RESPIRATION,
				    Enchantments.AQUA_AFFINITY,
				    Enchantments.THORNS,
				    Enchantments.DEPTH_STRIDER,
				    Enchantments.SHARPNESS,
				    Enchantments.SMITE,
				    Enchantments.BANE_OF_ARTHROPODS,
				    Enchantments.KNOCKBACK,
				    Enchantments.FIRE_ASPECT,
				    Enchantments.LOOTING,
				    Enchantments.SWEEPING_EDGE,
				    Enchantments.EFFICIENCY,
				    Enchantments.SILK_TOUCH,
				    Enchantments.UNBREAKING,
				    Enchantments.FORTUNE,
				    Enchantments.POWER,
				    Enchantments.PUNCH,
				    Enchantments.FLAME,
				    Enchantments.INFINITY,
				    Enchantments.LUCK_OF_THE_SEA,
				    Enchantments.LURE,
				    Enchantments.LOYALTY,
				    Enchantments.IMPALING,
				    Enchantments.RIPTIDE,
				    Enchantments.CHANNELING,
				    Enchantments.MULTISHOT,
				    Enchantments.QUICK_CHARGE,
				    Enchantments.PIERCING,
				    Enchantments.DENSITY,
				    Enchantments.BREACH,
				    Enchantments.LUNGE
		    );
		builder(EnchantmentTags.DOUBLE_TRADE_PRICE).addTag(EnchantmentTags.TREASURE);
		builder(EnchantmentTags.IN_ENCHANTING_TABLE).addTag(EnchantmentTags.NON_TREASURE);
		builder(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT).addTag(EnchantmentTags.NON_TREASURE);
		builder(EnchantmentTags.ON_TRADED_EQUIPMENT).addTag(EnchantmentTags.NON_TREASURE);
		builder(EnchantmentTags.ON_RANDOM_LOOT)
		    .addTag(EnchantmentTags.NON_TREASURE)
		    .add(
				    Enchantments.BINDING_CURSE,
				    Enchantments.VANISHING_CURSE,
				    Enchantments.FROST_WALKER,
				    Enchantments.MENDING
		    );
		builder(EnchantmentTags.TRADEABLE)
		    .addTag(EnchantmentTags.NON_TREASURE)
		    .add(
				    Enchantments.BINDING_CURSE,
				    Enchantments.VANISHING_CURSE,
				    Enchantments.FROST_WALKER,
				    Enchantments.MENDING
		    );
		builder(EnchantmentTags.CURSE).add(Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE);
		builder(EnchantmentTags.SMELTS_LOOT).add(Enchantments.FIRE_ASPECT);
		builder(EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING).add(Enchantments.SILK_TOUCH);
		builder(EnchantmentTags.PREVENTS_DECORATED_POT_SHATTERING).add(Enchantments.SILK_TOUCH);
		builder(EnchantmentTags.PREVENTS_ICE_MELTING).add(Enchantments.SILK_TOUCH);
		builder(EnchantmentTags.PREVENTS_INFESTED_SPAWNS).add(Enchantments.SILK_TOUCH);
	}
}
