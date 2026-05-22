package net.minecraft.datafixer;

import com.mojang.datafixers.DSL.TypeReference;

/**
 * Класс TypeReferences.
 */
public class TypeReferences {

	public static final TypeReference LEVEL = create("level");
	public static final TypeReference LIGHTWEIGHT_LEVEL = create("lightweight_level");
	public static final TypeReference PLAYER = create("player");
	public static final TypeReference CHUNK = create("chunk");
	public static final TypeReference HOTBAR = create("hotbar");
	public static final TypeReference OPTIONS = create("options");
	public static final TypeReference STRUCTURE = create("structure");
	public static final TypeReference STATS = create("stats");
	public static final TypeReference SAVED_DATA_COMMAND_STORAGE = create("saved_data/command_storage");
	public static final TypeReference TICKETS_SAVED_DATA = create("saved_data/tickets");
	public static final TypeReference SAVED_DATA_MAP_DATA = create("saved_data/map_data");
	public static final TypeReference SAVED_DATA_IDCOUNTS = create("saved_data/idcounts");
	public static final TypeReference SAVED_DATA_RAIDS = create("saved_data/raids");
	public static final TypeReference SAVED_DATA_RANDOM_SEQUENCES = create("saved_data/random_sequences");
	public static final TypeReference SAVED_DATA_SCOREBOARD = create("saved_data/scoreboard");
	public static final TypeReference STOPWATCHES_SAVED_DATA = create("saved_data/stopwatches");
	public static final TypeReference
			SAVED_DATA_STRUCTURE_FEATURE_INDICES =
			create("saved_data/structure_feature_indices");
	public static final TypeReference WORLD_BORDER_SAVED_DATA = create("saved_data/world_border");
	public static final TypeReference ADVANCEMENTS = create("advancements");
	public static final TypeReference POI_CHUNK = create("poi_chunk");
	public static final TypeReference ENTITY_CHUNK = create("entity_chunk");
	public static final TypeReference DEBUG_PROFILE = create("debug_profile");
	public static final TypeReference BLOCK_ENTITY = create("block_entity");
	public static final TypeReference ITEM_STACK = create("item_stack");
	public static final TypeReference BLOCK_STATE = create("block_state");
	public static final TypeReference FLAT_BLOCK_STATE = create("flat_block_state");
	public static final TypeReference DATA_COMPONENTS = create("data_components");
	public static final TypeReference VILLAGER_TRADE = create("villager_trade");
	public static final TypeReference PARTICLE = create("particle");
	public static final TypeReference TEXT_COMPONENT = create("text_component");
	public static final TypeReference ENTITY_EQUIPMENT = create("entity_equipment");
	public static final TypeReference ENTITY_NAME = create("entity_name");
	public static final TypeReference ENTITY_TREE = create("entity_tree");
	public static final TypeReference ENTITY = create("entity");
	public static final TypeReference BLOCK_NAME = create("block_name");
	public static final TypeReference ITEM_NAME = create("item_name");
	public static final TypeReference GAME_EVENT_NAME = create("game_event_name");
	public static final TypeReference UNTAGGED_SPAWNER = create("untagged_spawner");
	public static final TypeReference STRUCTURE_FEATURE = create("structure_feature");
	public static final TypeReference OBJECTIVE = create("objective");
	public static final TypeReference TEAM = create("team");
	public static final TypeReference RECIPE = create("recipe");
	public static final TypeReference BIOME = create("biome");
	public static final TypeReference
			MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST =
			create("multi_noise_biome_source_parameter_list");
	public static final TypeReference WORLD_GEN_SETTINGS = create("world_gen_settings");

	public static TypeReference create(String typeName) {
		return new TypeReference() {
			public String typeName() {
				return typeName;
			}

			@Override
			public String toString() {
				return "@" + typeName;
			}
		};
	}
}
