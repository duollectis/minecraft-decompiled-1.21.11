package net.minecraft.client.data;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code TextureMap}.
 */
public class TextureMap {

	private final Map<TextureKey, Identifier> entries = Maps.newHashMap();
	private final Set<TextureKey> inherited = Sets.newHashSet();

	/**
	 * Put.
	 *
	 * @param key key
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public TextureMap put(TextureKey key, Identifier id) {
		this.entries.put(key, id);
		return this;
	}

	/**
	 * Register.
	 *
	 * @param key key
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public TextureMap register(TextureKey key, Identifier id) {
		this.entries.put(key, id);
		this.inherited.add(key);
		return this;
	}

	public Stream<TextureKey> getInherited() {
		return this.inherited.stream();
	}

	/**
	 * Copy.
	 *
	 * @param parent parent
	 * @param child child
	 *
	 * @return TextureMap — результат операции
	 */
	public TextureMap copy(TextureKey parent, TextureKey child) {
		this.entries.put(child, this.entries.get(parent));
		return this;
	}

	/**
	 * Inherit.
	 *
	 * @param parent parent
	 * @param child child
	 *
	 * @return TextureMap — результат операции
	 */
	public TextureMap inherit(TextureKey parent, TextureKey child) {
		this.entries.put(child, this.entries.get(parent));
		this.inherited.add(child);
		return this;
	}

	public Identifier getTexture(TextureKey key) {
		for (TextureKey textureKey = key; textureKey != null; textureKey = textureKey.getParent()) {
			Identifier identifier = this.entries.get(textureKey);
			if (identifier != null) {
				return identifier;
			}
		}

		throw new IllegalStateException("Can't find texture for slot " + key);
	}

	/**
	 * Создаёт копию and add.
	 *
	 * @param key key
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public TextureMap copyAndAdd(TextureKey key, Identifier id) {
		TextureMap textureMap = new TextureMap();
		textureMap.entries.putAll(this.entries);
		textureMap.inherited.addAll(this.inherited);
		textureMap.put(key, id);
		return textureMap;
	}

	/**
	 * All.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap all(Block block) {
		Identifier identifier = getId(block);
		return all(identifier);
	}

	/**
	 * Texture.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap texture(Block block) {
		Identifier identifier = getId(block);
		return texture(identifier);
	}

	/**
	 * Texture.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap texture(Identifier id) {
		return new TextureMap().put(TextureKey.TEXTURE, id);
	}

	/**
	 * All.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap all(Identifier id) {
		return new TextureMap().put(TextureKey.ALL, id);
	}

	/**
	 * Cross.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap cross(Block block) {
		return of(TextureKey.CROSS, getId(block));
	}

	/**
	 * Side.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap side(Block block) {
		return of(TextureKey.SIDE, getId(block));
	}

	/**
	 * Cross and cross emissive.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap crossAndCrossEmissive(Block block) {
		return new TextureMap()
				.put(TextureKey.CROSS, getId(block))
				.put(TextureKey.CROSS_EMISSIVE, getSubId(block, "_emissive"));
	}

	/**
	 * Cross.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap cross(Identifier id) {
		return of(TextureKey.CROSS, id);
	}

	/**
	 * Plant.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap plant(Block block) {
		return of(TextureKey.PLANT, getId(block));
	}

	/**
	 * Plant and cross emissive.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap plantAndCrossEmissive(Block block) {
		return new TextureMap()
				.put(TextureKey.PLANT, getId(block))
				.put(TextureKey.CROSS_EMISSIVE, getSubId(block, "_emissive"));
	}

	/**
	 * Plant.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap plant(Identifier id) {
		return of(TextureKey.PLANT, id);
	}

	/**
	 * Rail.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap rail(Block block) {
		return of(TextureKey.RAIL, getId(block));
	}

	/**
	 * Rail.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap rail(Identifier id) {
		return of(TextureKey.RAIL, id);
	}

	/**
	 * Wool.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap wool(Block block) {
		return of(TextureKey.WOOL, getId(block));
	}

	/**
	 * Flowerbed.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap flowerbed(Block block) {
		return new TextureMap().put(TextureKey.FLOWERBED, getId(block)).put(TextureKey.STEM, getSubId(block, "_stem"));
	}

	/**
	 * Wool.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap wool(Identifier id) {
		return of(TextureKey.WOOL, id);
	}

	/**
	 * Stem.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap stem(Block block) {
		return of(TextureKey.STEM, getId(block));
	}

	/**
	 * Stem and upper.
	 *
	 * @param stem stem
	 * @param upper upper
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap stemAndUpper(Block stem, Block upper) {
		return new TextureMap().put(TextureKey.STEM, getId(stem)).put(TextureKey.UPPERSTEM, getId(upper));
	}

	/**
	 * Pattern.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap pattern(Block block) {
		return of(TextureKey.PATTERN, getId(block));
	}

	/**
	 * Fan.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap fan(Block block) {
		return of(TextureKey.FAN, getId(block));
	}

	/**
	 * Crop.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap crop(Identifier id) {
		return of(TextureKey.CROP, id);
	}

	/**
	 * Pane and top for edge.
	 *
	 * @param block block
	 * @param top top
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap paneAndTopForEdge(Block block, Block top) {
		return new TextureMap().put(TextureKey.PANE, getId(block)).put(TextureKey.EDGE, getSubId(top, "_top"));
	}

	/**
	 * Of.
	 *
	 * @param key key
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap of(TextureKey key, Identifier id) {
		return new TextureMap().put(key, id);
	}

	/**
	 * Side end.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sideEnd(Block block) {
		return new TextureMap()
				.put(TextureKey.SIDE, getSubId(block, "_side"))
				.put(TextureKey.END, getSubId(block, "_top"));
	}

	/**
	 * Side and top.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sideAndTop(Block block) {
		return new TextureMap()
				.put(TextureKey.SIDE, getSubId(block, "_side"))
				.put(TextureKey.TOP, getSubId(block, "_top"));
	}

	/**
	 * Potted azalea bush.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap pottedAzaleaBush(Block block) {
		return new TextureMap()
				.put(TextureKey.PLANT, getSubId(block, "_plant"))
				.put(TextureKey.SIDE, getSubId(block, "_side"))
				.put(TextureKey.TOP, getSubId(block, "_top"));
	}

	/**
	 * Side and end for top.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sideAndEndForTop(Block block) {
		return new TextureMap()
				.put(TextureKey.SIDE, getId(block))
				.put(TextureKey.END, getSubId(block, "_top"))
				.put(TextureKey.PARTICLE, getId(block));
	}

	/**
	 * Side end.
	 *
	 * @param side side
	 * @param end end
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sideEnd(Identifier side, Identifier end) {
		return new TextureMap().put(TextureKey.SIDE, side).put(TextureKey.END, end);
	}

	/**
	 * Texture side top.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap textureSideTop(Block block) {
		return new TextureMap()
				.put(TextureKey.TEXTURE, getId(block))
				.put(TextureKey.SIDE, getSubId(block, "_side"))
				.put(TextureKey.TOP, getSubId(block, "_top"));
	}

	/**
	 * Texture particle.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap textureParticle(Block block) {
		return new TextureMap()
				.put(TextureKey.TEXTURE, getId(block))
				.put(TextureKey.PARTICLE, getSubId(block, "_particle"));
	}

	/**
	 * Side top bottom.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sideTopBottom(Block block) {
		return new TextureMap()
				.put(TextureKey.SIDE, getSubId(block, "_side"))
				.put(TextureKey.TOP, getSubId(block, "_top"))
				.put(TextureKey.BOTTOM, getSubId(block, "_bottom"));
	}

	/**
	 * Wall side top bottom.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap wallSideTopBottom(Block block) {
		Identifier identifier = getId(block);
		return new TextureMap()
				.put(TextureKey.WALL, identifier)
				.put(TextureKey.SIDE, identifier)
				.put(TextureKey.TOP, getSubId(block, "_top"))
				.put(TextureKey.BOTTOM, getSubId(block, "_bottom"));
	}

	/**
	 * Wall side end.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap wallSideEnd(Block block) {
		Identifier identifier = getId(block);
		return new TextureMap()
				.put(TextureKey.TEXTURE, identifier)
				.put(TextureKey.WALL, identifier)
				.put(TextureKey.SIDE, identifier)
				.put(TextureKey.END, getSubId(block, "_top"));
	}

	/**
	 * Top bottom.
	 *
	 * @param top top
	 * @param bottom bottom
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap topBottom(Identifier top, Identifier bottom) {
		return new TextureMap().put(TextureKey.TOP, top).put(TextureKey.BOTTOM, bottom);
	}

	/**
	 * Top bottom.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap topBottom(Block block) {
		return new TextureMap()
				.put(TextureKey.TOP, getSubId(block, "_top"))
				.put(TextureKey.BOTTOM, getSubId(block, "_bottom"));
	}

	/**
	 * Particle.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap particle(Block block) {
		return new TextureMap().put(TextureKey.PARTICLE, getId(block));
	}

	/**
	 * Particle.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap particle(Identifier id) {
		return new TextureMap().put(TextureKey.PARTICLE, id);
	}

	/**
	 * Fire0.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap fire0(Block block) {
		return new TextureMap().put(TextureKey.FIRE, getSubId(block, "_0"));
	}

	/**
	 * Fire1.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap fire1(Block block) {
		return new TextureMap().put(TextureKey.FIRE, getSubId(block, "_1"));
	}

	/**
	 * Lantern.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap lantern(Block block) {
		return new TextureMap().put(TextureKey.LANTERN, getId(block));
	}

	/**
	 * Torch.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap torch(Block block) {
		return new TextureMap().put(TextureKey.TORCH, getId(block));
	}

	/**
	 * Torch.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap torch(Identifier id) {
		return new TextureMap().put(TextureKey.TORCH, id);
	}

	/**
	 * Trial spawner.
	 *
	 * @param block block
	 * @param side side
	 * @param top top
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap trialSpawner(Block block, String side, String top) {
		return new TextureMap()
				.put(TextureKey.SIDE, getSubId(block, side))
				.put(TextureKey.TOP, getSubId(block, top))
				.put(TextureKey.BOTTOM, getSubId(block, "_bottom"));
	}

	/**
	 * Vault.
	 *
	 * @param block block
	 * @param front front
	 * @param side side
	 * @param top top
	 * @param bottom bottom
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap vault(Block block, String front, String side, String top, String bottom) {
		return new TextureMap()
				.put(TextureKey.FRONT, getSubId(block, front))
				.put(TextureKey.SIDE, getSubId(block, side))
				.put(TextureKey.TOP, getSubId(block, top))
				.put(TextureKey.BOTTOM, getSubId(block, bottom));
	}

	/**
	 * Particle.
	 *
	 * @param item item
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap particle(Item item) {
		return new TextureMap().put(TextureKey.PARTICLE, getId(item));
	}

	/**
	 * Side front back.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sideFrontBack(Block block) {
		return new TextureMap()
				.put(TextureKey.SIDE, getSubId(block, "_side"))
				.put(TextureKey.FRONT, getSubId(block, "_front"))
				.put(TextureKey.BACK, getSubId(block, "_back"));
	}

	/**
	 * Side front top bottom.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sideFrontTopBottom(Block block) {
		return new TextureMap()
				.put(TextureKey.SIDE, getSubId(block, "_side"))
				.put(TextureKey.FRONT, getSubId(block, "_front"))
				.put(TextureKey.TOP, getSubId(block, "_top"))
				.put(TextureKey.BOTTOM, getSubId(block, "_bottom"));
	}

	/**
	 * Side front top.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sideFrontTop(Block block) {
		return new TextureMap()
				.put(TextureKey.SIDE, getSubId(block, "_side"))
				.put(TextureKey.FRONT, getSubId(block, "_front"))
				.put(TextureKey.TOP, getSubId(block, "_top"));
	}

	/**
	 * Side front end.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sideFrontEnd(Block block) {
		return new TextureMap()
				.put(TextureKey.SIDE, getSubId(block, "_side"))
				.put(TextureKey.FRONT, getSubId(block, "_front"))
				.put(TextureKey.END, getSubId(block, "_end"));
	}

	/**
	 * Top.
	 *
	 * @param top top
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap top(Block top) {
		return new TextureMap().put(TextureKey.TOP, getSubId(top, "_top"));
	}

	/**
	 * Front side with custom bottom.
	 *
	 * @param block block
	 * @param bottom bottom
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap frontSideWithCustomBottom(Block block, Block bottom) {
		return new TextureMap()
				.put(TextureKey.PARTICLE, getSubId(block, "_front"))
				.put(TextureKey.DOWN, getId(bottom))
				.put(TextureKey.UP, getSubId(block, "_top"))
				.put(TextureKey.NORTH, getSubId(block, "_front"))
				.put(TextureKey.EAST, getSubId(block, "_side"))
				.put(TextureKey.SOUTH, getSubId(block, "_side"))
				.put(TextureKey.WEST, getSubId(block, "_front"));
	}

	/**
	 * Front top side.
	 *
	 * @param frontTopSideBlock front top side block
	 * @param downBlock down block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap frontTopSide(Block frontTopSideBlock, Block downBlock) {
		return new TextureMap()
				.put(TextureKey.PARTICLE, getSubId(frontTopSideBlock, "_front"))
				.put(TextureKey.DOWN, getId(downBlock))
				.put(TextureKey.UP, getSubId(frontTopSideBlock, "_top"))
				.put(TextureKey.NORTH, getSubId(frontTopSideBlock, "_front"))
				.put(TextureKey.SOUTH, getSubId(frontTopSideBlock, "_front"))
				.put(TextureKey.EAST, getSubId(frontTopSideBlock, "_side"))
				.put(TextureKey.WEST, getSubId(frontTopSideBlock, "_side"));
	}

	/**
	 * Sniffer egg.
	 *
	 * @param age age
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap snifferEgg(String age) {
		return new TextureMap()
				.put(TextureKey.PARTICLE, getSubId(Blocks.SNIFFER_EGG, age + "_north"))
				.put(TextureKey.BOTTOM, getSubId(Blocks.SNIFFER_EGG, age + "_bottom"))
				.put(TextureKey.TOP, getSubId(Blocks.SNIFFER_EGG, age + "_top"))
				.put(TextureKey.NORTH, getSubId(Blocks.SNIFFER_EGG, age + "_north"))
				.put(TextureKey.SOUTH, getSubId(Blocks.SNIFFER_EGG, age + "_south"))
				.put(TextureKey.EAST, getSubId(Blocks.SNIFFER_EGG, age + "_east"))
				.put(TextureKey.WEST, getSubId(Blocks.SNIFFER_EGG, age + "_west"));
	}

	/**
	 * Dried ghast.
	 *
	 * @param hydration hydration
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap driedGhast(String hydration) {
		return new TextureMap()
				.put(TextureKey.PARTICLE, getSubId(Blocks.DRIED_GHAST, hydration + "_north"))
				.put(TextureKey.BOTTOM, getSubId(Blocks.DRIED_GHAST, hydration + "_bottom"))
				.put(TextureKey.TOP, getSubId(Blocks.DRIED_GHAST, hydration + "_top"))
				.put(TextureKey.NORTH, getSubId(Blocks.DRIED_GHAST, hydration + "_north"))
				.put(TextureKey.SOUTH, getSubId(Blocks.DRIED_GHAST, hydration + "_south"))
				.put(TextureKey.EAST, getSubId(Blocks.DRIED_GHAST, hydration + "_east"))
				.put(TextureKey.WEST, getSubId(Blocks.DRIED_GHAST, hydration + "_west"))
				.put(TextureKey.TENTACLES, getSubId(Blocks.DRIED_GHAST, hydration + "_tentacles"));
	}

	/**
	 * Campfire.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap campfire(Block block) {
		return new TextureMap()
				.put(TextureKey.LIT_LOG, getSubId(block, "_log_lit"))
				.put(TextureKey.FIRE, getSubId(block, "_fire"));
	}

	/**
	 * Проверяет возможность dle cake.
	 *
	 * @param block block
	 * @param lit lit
	 *
	 * @return TextureMap — {@code true} если условие выполнено
	 */
	public static TextureMap candleCake(Block block, boolean lit) {
		return new TextureMap()
				.put(TextureKey.PARTICLE, getSubId(Blocks.CAKE, "_side"))
				.put(TextureKey.BOTTOM, getSubId(Blocks.CAKE, "_bottom"))
				.put(TextureKey.TOP, getSubId(Blocks.CAKE, "_top"))
				.put(TextureKey.SIDE, getSubId(Blocks.CAKE, "_side"))
				.put(TextureKey.CANDLE, getSubId(block, lit ? "_lit" : ""));
	}

	/**
	 * Cauldron.
	 *
	 * @param content content
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap cauldron(Identifier content) {
		return new TextureMap()
				.put(TextureKey.PARTICLE, getSubId(Blocks.CAULDRON, "_side"))
				.put(TextureKey.SIDE, getSubId(Blocks.CAULDRON, "_side"))
				.put(TextureKey.TOP, getSubId(Blocks.CAULDRON, "_top"))
				.put(TextureKey.BOTTOM, getSubId(Blocks.CAULDRON, "_bottom"))
				.put(TextureKey.INSIDE, getSubId(Blocks.CAULDRON, "_inner"))
				.put(TextureKey.CONTENT, content);
	}

	/**
	 * Sculk shrieker.
	 *
	 * @param canSummon can summon
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap sculkShrieker(boolean canSummon) {
		String string = canSummon ? "_can_summon" : "";
		return new TextureMap()
				.put(TextureKey.PARTICLE, getSubId(Blocks.SCULK_SHRIEKER, "_bottom"))
				.put(TextureKey.SIDE, getSubId(Blocks.SCULK_SHRIEKER, "_side"))
				.put(TextureKey.TOP, getSubId(Blocks.SCULK_SHRIEKER, "_top"))
				.put(TextureKey.INNER_TOP, getSubId(Blocks.SCULK_SHRIEKER, string + "_inner_top"))
				.put(TextureKey.BOTTOM, getSubId(Blocks.SCULK_SHRIEKER, "_bottom"));
	}

	/**
	 * Bars.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap bars(Block block) {
		return new TextureMap().put(TextureKey.BARS, getId(block)).put(TextureKey.EDGE, getId(block));
	}

	/**
	 * Layer0.
	 *
	 * @param item item
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap layer0(Item item) {
		return new TextureMap().put(TextureKey.LAYER0, getId(item));
	}

	/**
	 * Layer0.
	 *
	 * @param block block
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap layer0(Block block) {
		return new TextureMap().put(TextureKey.LAYER0, getId(block));
	}

	/**
	 * Layer0.
	 *
	 * @param id id
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap layer0(Identifier id) {
		return new TextureMap().put(TextureKey.LAYER0, id);
	}

	/**
	 * Layered.
	 *
	 * @param layer0 layer0
	 * @param layer1 layer1
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap layered(Identifier layer0, Identifier layer1) {
		return new TextureMap().put(TextureKey.LAYER0, layer0).put(TextureKey.LAYER1, layer1);
	}

	/**
	 * Layered.
	 *
	 * @param layer0 layer0
	 * @param layer1 layer1
	 * @param layer2 layer2
	 *
	 * @return TextureMap — результат операции
	 */
	public static TextureMap layered(Identifier layer0, Identifier layer1, Identifier layer2) {
		return new TextureMap()
				.put(TextureKey.LAYER0, layer0)
				.put(TextureKey.LAYER1, layer1)
				.put(TextureKey.LAYER2, layer2);
	}

	public static Identifier getId(Block block) {
		Identifier identifier = Registries.BLOCK.getId(block);
		return identifier.withPrefixedPath("block/");
	}

	public static Identifier getSubId(Block block, String suffix) {
		Identifier identifier = Registries.BLOCK.getId(block);
		return identifier.withPath(path -> "block/" + path + suffix);
	}

	public static Identifier getId(Item item) {
		Identifier identifier = Registries.ITEM.getId(item);
		return identifier.withPrefixedPath("item/");
	}

	public static Identifier getSubId(Item item, String suffix) {
		Identifier identifier = Registries.ITEM.getId(item);
		return identifier.withPath(path -> "item/" + path + suffix);
	}
}
