package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.nbt.NbtOps;
import org.jspecify.annotations.Nullable;

public class BlockStateFlattening {
   private static final @Nullable Dynamic<?>[] OLD_STATE_TO_DYNAMIC = new Dynamic[4096];
   private static final @Nullable Dynamic<?>[] OLD_BLOCK_TO_DYNAMIC = new Dynamic[256];
   private static final Object2IntMap<Dynamic<?>> OLD_STATE_TO_ID = (Object2IntMap<Dynamic<?>>)DataFixUtils.make(
      new Object2IntOpenHashMap(), object2IntOpenHashMap -> object2IntOpenHashMap.defaultReturnValue(-1)
   );
   private static final Object2IntMap<String> OLD_BLOCK_TO_ID = (Object2IntMap<String>)DataFixUtils.make(
      new Object2IntOpenHashMap(), object2IntOpenHashMap -> object2IntOpenHashMap.defaultReturnValue(-1)
   );
   static final String FILTER_ME = "%%FILTER_ME%%";
   private static final String NAME_KEY = "Name";
   private static final String PROPERTIES_KEY = "Properties";
   private static final Map<String, String> AGE_0 = Map.of("age", "0");
   private static final Map<String, String> AGE_0_FACING_EAST = Map.of("age", "0", "facing", "east");
   private static final Map<String, String> AGE_0_FACING_NORTH = Map.of("age", "0", "facing", "north");
   private static final Map<String, String> AGE_0_FACING_SOUTH = Map.of("age", "0", "facing", "south");
   private static final Map<String, String> AGE_0_FACING_WEST = Map.of("age", "0", "facing", "west");
   private static final Map<String, String> AGE_1 = Map.of("age", "1");
   private static final Map<String, String> AGE_10 = Map.of("age", "10");
   private static final Map<String, String> AGE_11 = Map.of("age", "11");
   private static final Map<String, String> AGE_12 = Map.of("age", "12");
   private static final Map<String, String> AGE_13 = Map.of("age", "13");
   private static final Map<String, String> AGE_14 = Map.of("age", "14");
   private static final Map<String, String> AGE_15 = Map.of("age", "15");
   private static final Map<String, String> AGE_1_FACING_EAST = Map.of("age", "1", "facing", "east");
   private static final Map<String, String> AGE_1_FACING_NORTH = Map.of("age", "1", "facing", "north");
   private static final Map<String, String> AGE_1_FACING_SOUTH = Map.of("age", "1", "facing", "south");
   private static final Map<String, String> AGE_1_FACING_WEST = Map.of("age", "1", "facing", "west");
   private static final Map<String, String> AGE_2 = Map.of("age", "2");
   private static final Map<String, String> AGE_2_FACING_EAST = Map.of("age", "2", "facing", "east");
   private static final Map<String, String> AGE_2_FACING_NORTH = Map.of("age", "2", "facing", "north");
   private static final Map<String, String> AGE_2_FACING_SOUTH = Map.of("age", "2", "facing", "south");
   private static final Map<String, String> AGE_2_FACING_WEST = Map.of("age", "2", "facing", "west");
   private static final Map<String, String> AGE_3 = Map.of("age", "3");
   private static final Map<String, String> AGE_4 = Map.of("age", "4");
   private static final Map<String, String> AGE_5 = Map.of("age", "5");
   private static final Map<String, String> AGE_6 = Map.of("age", "6");
   private static final Map<String, String> AGE_7 = Map.of("age", "7");
   private static final Map<String, String> AGE_8 = Map.of("age", "8");
   private static final Map<String, String> AGE_9 = Map.of("age", "9");
   private static final Map<String, String> AXIS_X = Map.of("axis", "x");
   private static final Map<String, String> AXIS_Y = Map.of("axis", "y");
   private static final Map<String, String> AXIS_Z = Map.of("axis", "z");
   private static final Map<String, String> CHECK_DECAY_FALSE_DECAYABLE_FALSE = Map.of("check_decay", "false", "decayable", "false");
   private static final Map<String, String> CHECK_DECAY_FALSE_DECAYABLE_TRUE = Map.of("check_decay", "false", "decayable", "true");
   private static final Map<String, String> CHECK_DECAY_TRUE_DECAYABLE_FALSE = Map.of("check_decay", "true", "decayable", "false");
   private static final Map<String, String> CHECK_DECAY_TRUE_DECAYABLE_TRUE = Map.of("check_decay", "true", "decayable", "true");
   private static final Map<String, String> COLOR_BLACK = Map.of("color", "black");
   private static final Map<String, String> COLOR_BLUE = Map.of("color", "blue");
   private static final Map<String, String> COLOR_BROWN = Map.of("color", "brown");
   private static final Map<String, String> COLOR_CYAN = Map.of("color", "cyan");
   private static final Map<String, String> COLOR_GRAY = Map.of("color", "gray");
   private static final Map<String, String> COLOR_GREEN = Map.of("color", "green");
   private static final Map<String, String> COLOR_LIGHT_BLUE = Map.of("color", "light_blue");
   private static final Map<String, String> COLOR_LIME = Map.of("color", "lime");
   private static final Map<String, String> COLOR_MAGENTA = Map.of("color", "magenta");
   private static final Map<String, String> COLOR_ORANGE = Map.of("color", "orange");
   private static final Map<String, String> COLOR_PINK = Map.of("color", "pink");
   private static final Map<String, String> COLOR_PURPLE = Map.of("color", "purple");
   private static final Map<String, String> COLOR_RED = Map.of("color", "red");
   private static final Map<String, String> COLOR_SILVER = Map.of("color", "silver");
   private static final Map<String, String> COLOR_WHITE = Map.of("color", "white");
   private static final Map<String, String> COLOR_YELLOW = Map.of("color", "yellow");
   private static final Map<String, String> ATTACHED_FALSE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> ATTACHED_FALSE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> ATTACHED_FALSE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> ATTACHED_FALSE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> ATTACHED_TRUE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> ATTACHED_TRUE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> ATTACHED_TRUE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> FACE_CEILING_FACING_NORTH_POWERED_FALSE = Map.of("face", "ceiling", "facing", "north", "powered", "false");
   private static final Map<String, String> FACE_CEILING_FACING_NORTH_POWERED_TRUE = Map.of("face", "ceiling", "facing", "north", "powered", "true");
   private static final Map<String, String> FACE_FLOOR_FACING_NORTH_POWERED_FALSE = Map.of("face", "floor", "facing", "north", "powered", "false");
   private static final Map<String, String> FACE_FLOOR_FACING_NORTH_POWERED_TRUE = Map.of("face", "floor", "facing", "north", "powered", "true");
   private static final Map<String, String> FACE_WALL_FACING_EAST_POWERED_FALSE = Map.of("face", "wall", "facing", "east", "powered", "false");
   private static final Map<String, String> FACE_WALL_FACING_NORTH_POWERED_FALSE = Map.of("face", "wall", "facing", "north", "powered", "false");
   private static final Map<String, String> FACE_WALL_FACING_SOUTH_POWERED_FALSE = Map.of("face", "wall", "facing", "south", "powered", "false");
   private static final Map<String, String> FACE_WALL_FACING_WEST_POWERED_FALSE = Map.of("face", "wall", "facing", "west", "powered", "false");
   private static final Map<String, String> FACE_WALL_FACING_EAST_POWERED_TRUE = Map.of("face", "wall", "facing", "east", "powered", "true");
   private static final Map<String, String> FACE_WALL_FACING_NORTH_POWERED_TRUE = Map.of("face", "wall", "facing", "north", "powered", "true");
   private static final Map<String, String> FACE_WALL_FACING_SOUTH_POWERED_TRUE = Map.of("face", "wall", "facing", "south", "powered", "true");
   private static final Map<String, String> FACE_WALL_FACING_WEST_POWERED_TRUE = Map.of("face", "wall", "facing", "west", "powered", "true");
   private static final Map<String, String> FACING_DOWN = Map.of("facing", "down");
   private static final Map<String, String> CONDITIONAL_FALSE_FACING_DOWN = Map.of("conditional", "false", "facing", "down");
   private static final Map<String, String> CONDITIONAL_TRUE_FACING_DOWN = Map.of("conditional", "true", "facing", "down");
   private static final Map<String, String> EXTENDED_FALSE_FACING_DOWN = Map.of("extended", "false", "facing", "down");
   private static final Map<String, String> EXTENDED_TRUE_FACING_DOWN = Map.of("extended", "true", "facing", "down");
   private static final Map<String, String> FACING_DOWN_POWERED_FALSE = Map.of("facing", "down", "powered", "false");
   private static final Map<String, String> FACING_DOWN_POWERED_TRUE = Map.of("facing", "down", "powered", "true");
   private static final Map<String, String> FACING_EAST = Map.of("facing", "east");
   private static final Map<String, String> CONDITIONAL_FALSE_FACING_EAST = Map.of("conditional", "false", "facing", "east");
   private static final Map<String, String> CONDITIONAL_TRUE_FACING_EAST = Map.of("conditional", "true", "facing", "east");
   private static final Map<String, String> EXTENDED_FALSE_FACING_EAST = Map.of("extended", "false", "facing", "east");
   private static final Map<String, String> EXTENDED_TRUE_FACING_EAST = Map.of("extended", "true", "facing", "east");
   private static final Map<String, String> FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_POWERED_FALSE = Map.of("facing", "east", "powered", "false");
   private static final Map<String, String> FACING_EAST_POWERED_TRUE = Map.of("facing", "east", "powered", "true");
   private static final Map<String, String> FACING_NORTH = Map.of("facing", "north");
   private static final Map<String, String> CONDITIONAL_FALSE_FACING_NORTH = Map.of("conditional", "false", "facing", "north");
   private static final Map<String, String> CONDITIONAL_TRUE_FACING_NORTH = Map.of("conditional", "true", "facing", "north");
   private static final Map<String, String> EXTENDED_FALSE_FACING_NORTH = Map.of("extended", "false", "facing", "north");
   private static final Map<String, String> EXTENDED_TRUE_FACING_NORTH = Map.of("extended", "true", "facing", "north");
   private static final Map<String, String> FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_POWERED_FALSE = Map.of("facing", "north", "powered", "false");
   private static final Map<String, String> FACING_NORTH_POWERED_TRUE = Map.of("facing", "north", "powered", "true");
   private static final Map<String, String> FACING_SOUTH = Map.of("facing", "south");
   private static final Map<String, String> CONDITIONAL_FALSE_FACING_SOUTH = Map.of("conditional", "false", "facing", "south");
   private static final Map<String, String> CONDITIONAL_TRUE_FACING_SOUTH = Map.of("conditional", "true", "facing", "south");
   private static final Map<String, String> EXTENDED_FALSE_FACING_SOUTH = Map.of("extended", "false", "facing", "south");
   private static final Map<String, String> EXTENDED_TRUE_FACING_SOUTH = Map.of("extended", "true", "facing", "south");
   private static final Map<String, String> FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_POWERED_FALSE = Map.of("facing", "south", "powered", "false");
   private static final Map<String, String> FACING_SOUTH_POWERED_TRUE = Map.of("facing", "south", "powered", "true");
   private static final Map<String, String> FACING_UP = Map.of("facing", "up");
   private static final Map<String, String> CONDITIONAL_FALSE_FACING_UP = Map.of("conditional", "false", "facing", "up");
   private static final Map<String, String> CONDITIONAL_TRUE_FACING_UP = Map.of("conditional", "true", "facing", "up");
   private static final Map<String, String> EXTENDED_FALSE_FACING_UP = Map.of("extended", "false", "facing", "up");
   private static final Map<String, String> EXTENDED_TRUE_FACING_UP = Map.of("extended", "true", "facing", "up");
   private static final Map<String, String> FACING_UP_POWERED_FALSE = Map.of("facing", "up", "powered", "false");
   private static final Map<String, String> FACING_UP_POWERED_TRUE = Map.of("facing", "up", "powered", "true");
   private static final Map<String, String> FACING_WEST = Map.of("facing", "west");
   private static final Map<String, String> CONDITIONAL_FALSE_FACING_WEST = Map.of("conditional", "false", "facing", "west");
   private static final Map<String, String> CONDITIONAL_TRUE_FACING_WEST = Map.of("conditional", "true", "facing", "west");
   private static final Map<String, String> EXTENDED_FALSE_FACING_WEST = Map.of("extended", "false", "facing", "west");
   private static final Map<String, String> EXTENDED_TRUE_FACING_WEST = Map.of("extended", "true", "facing", "west");
   private static final Map<String, String> FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_POWERED_FALSE = Map.of("facing", "west", "powered", "false");
   private static final Map<String, String> FACING_WEST_POWERED_TRUE = Map.of("facing", "west", "powered", "true");
   private static final Map<String, String> FACING_EAST_HALF_BOTTOM_OPEN_FALSE = Map.of("facing", "east", "half", "bottom", "open", "false");
   private static final Map<String, String> FACING_NORTH_HALF_BOTTOM_OPEN_FALSE = Map.of("facing", "north", "half", "bottom", "open", "false");
   private static final Map<String, String> FACING_SOUTH_HALF_BOTTOM_OPEN_FALSE = Map.of("facing", "south", "half", "bottom", "open", "false");
   private static final Map<String, String> FACING_WEST_HALF_BOTTOM_OPEN_FALSE = Map.of("facing", "west", "half", "bottom", "open", "false");
   private static final Map<String, String> FACING_EAST_HALF_BOTTOM_OPEN_TRUE = Map.of("facing", "east", "half", "bottom", "open", "true");
   private static final Map<String, String> FACING_NORTH_HALF_BOTTOM_OPEN_TRUE = Map.of("facing", "north", "half", "bottom", "open", "true");
   private static final Map<String, String> FACING_SOUTH_HALF_BOTTOM_OPEN_TRUE = Map.of("facing", "south", "half", "bottom", "open", "true");
   private static final Map<String, String> FACING_WEST_HALF_BOTTOM_OPEN_TRUE = Map.of("facing", "west", "half", "bottom", "open", "true");
   private static final Map<String, String> FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT = Map.of("facing", "east", "half", "bottom", "shape", "inner_left");
   private static final Map<String, String> FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT = Map.of("facing", "north", "half", "bottom", "shape", "inner_left");
   private static final Map<String, String> FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT = Map.of("facing", "south", "half", "bottom", "shape", "inner_left");
   private static final Map<String, String> FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT = Map.of("facing", "west", "half", "bottom", "shape", "inner_left");
   private static final Map<String, String> FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT = Map.of("facing", "east", "half", "bottom", "shape", "inner_right");
   private static final Map<String, String> FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT = Map.of("facing", "north", "half", "bottom", "shape", "inner_right");
   private static final Map<String, String> FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT = Map.of("facing", "south", "half", "bottom", "shape", "inner_right");
   private static final Map<String, String> FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT = Map.of("facing", "west", "half", "bottom", "shape", "inner_right");
   private static final Map<String, String> FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT = Map.of("facing", "east", "half", "bottom", "shape", "outer_left");
   private static final Map<String, String> FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT = Map.of("facing", "north", "half", "bottom", "shape", "outer_left");
   private static final Map<String, String> FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT = Map.of("facing", "south", "half", "bottom", "shape", "outer_left");
   private static final Map<String, String> FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT = Map.of("facing", "west", "half", "bottom", "shape", "outer_left");
   private static final Map<String, String> FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT = Map.of("facing", "east", "half", "bottom", "shape", "outer_right");
   private static final Map<String, String> FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT = Map.of("facing", "north", "half", "bottom", "shape", "outer_right");
   private static final Map<String, String> FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT = Map.of("facing", "south", "half", "bottom", "shape", "outer_right");
   private static final Map<String, String> FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT = Map.of("facing", "west", "half", "bottom", "shape", "outer_right");
   private static final Map<String, String> FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT = Map.of("facing", "east", "half", "bottom", "shape", "straight");
   private static final Map<String, String> FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT = Map.of("facing", "north", "half", "bottom", "shape", "straight");
   private static final Map<String, String> FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT = Map.of("facing", "south", "half", "bottom", "shape", "straight");
   private static final Map<String, String> FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT = Map.of("facing", "west", "half", "bottom", "shape", "straight");
   private static final Map<String, String> HALF_LOWER = Map.of("half", "lower");
   private static final Map<String, String> FACING_EAST_HALF_TOP_OPEN_FALSE = Map.of("facing", "east", "half", "top", "open", "false");
   private static final Map<String, String> FACING_NORTH_HALF_TOP_OPEN_FALSE = Map.of("facing", "north", "half", "top", "open", "false");
   private static final Map<String, String> FACING_SOUTH_HALF_TOP_OPEN_FALSE = Map.of("facing", "south", "half", "top", "open", "false");
   private static final Map<String, String> FACING_WEST_HALF_TOP_OPEN_FALSE = Map.of("facing", "west", "half", "top", "open", "false");
   private static final Map<String, String> FACING_EAST_HALF_TOP_OPEN_TRUE = Map.of("facing", "east", "half", "top", "open", "true");
   private static final Map<String, String> FACING_NORTH_HALF_TOP_OPEN_TRUE = Map.of("facing", "north", "half", "top", "open", "true");
   private static final Map<String, String> FACING_SOUTH_HALF_TOP_OPEN_TRUE = Map.of("facing", "south", "half", "top", "open", "true");
   private static final Map<String, String> FACING_WEST_HALF_TOP_OPEN_TRUE = Map.of("facing", "west", "half", "top", "open", "true");
   private static final Map<String, String> FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT = Map.of("facing", "east", "half", "top", "shape", "inner_left");
   private static final Map<String, String> FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT = Map.of("facing", "north", "half", "top", "shape", "inner_left");
   private static final Map<String, String> FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT = Map.of("facing", "south", "half", "top", "shape", "inner_left");
   private static final Map<String, String> FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT = Map.of("facing", "west", "half", "top", "shape", "inner_left");
   private static final Map<String, String> FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT = Map.of("facing", "east", "half", "top", "shape", "inner_right");
   private static final Map<String, String> FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT = Map.of("facing", "north", "half", "top", "shape", "inner_right");
   private static final Map<String, String> FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT = Map.of("facing", "south", "half", "top", "shape", "inner_right");
   private static final Map<String, String> FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT = Map.of("facing", "west", "half", "top", "shape", "inner_right");
   private static final Map<String, String> FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT = Map.of("facing", "east", "half", "top", "shape", "outer_left");
   private static final Map<String, String> FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT = Map.of("facing", "north", "half", "top", "shape", "outer_left");
   private static final Map<String, String> FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT = Map.of("facing", "south", "half", "top", "shape", "outer_left");
   private static final Map<String, String> FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT = Map.of("facing", "west", "half", "top", "shape", "outer_left");
   private static final Map<String, String> FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT = Map.of("facing", "east", "half", "top", "shape", "outer_right");
   private static final Map<String, String> FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT = Map.of("facing", "north", "half", "top", "shape", "outer_right");
   private static final Map<String, String> FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT = Map.of("facing", "south", "half", "top", "shape", "outer_right");
   private static final Map<String, String> FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT = Map.of("facing", "west", "half", "top", "shape", "outer_right");
   private static final Map<String, String> FACING_EAST_HALF_TOP_SHAPE_STRAIGHT = Map.of("facing", "east", "half", "top", "shape", "straight");
   private static final Map<String, String> FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT = Map.of("facing", "north", "half", "top", "shape", "straight");
   private static final Map<String, String> FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT = Map.of("facing", "south", "half", "top", "shape", "straight");
   private static final Map<String, String> FACING_WEST_HALF_TOP_SHAPE_STRAIGHT = Map.of("facing", "west", "half", "top", "shape", "straight");
   private static final Map<String, String> HALF_UPPER = Map.of("half", "upper");
   private static final Map<String, String> LEVEL_0 = Map.of("level", "0");
   private static final Map<String, String> LEVEL_1 = Map.of("level", "1");
   private static final Map<String, String> LEVEL_10 = Map.of("level", "10");
   private static final Map<String, String> LEVEL_11 = Map.of("level", "11");
   private static final Map<String, String> LEVEL_12 = Map.of("level", "12");
   private static final Map<String, String> LEVEL_13 = Map.of("level", "13");
   private static final Map<String, String> LEVEL_14 = Map.of("level", "14");
   private static final Map<String, String> LEVEL_15 = Map.of("level", "15");
   private static final Map<String, String> LEVEL_2 = Map.of("level", "2");
   private static final Map<String, String> LEVEL_3 = Map.of("level", "3");
   private static final Map<String, String> LEVEL_4 = Map.of("level", "4");
   private static final Map<String, String> LEVEL_5 = Map.of("level", "5");
   private static final Map<String, String> LEVEL_6 = Map.of("level", "6");
   private static final Map<String, String> LEVEL_7 = Map.of("level", "7");
   private static final Map<String, String> LEVEL_8 = Map.of("level", "8");
   private static final Map<String, String> LEVEL_9 = Map.of("level", "9");
   private static final Map<String, String> LIT_FALSE = Map.of("lit", "false");
   private static final Map<String, String> LIT_TRUE = Map.of("lit", "true");
   private static final Map<String, String> DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_FALSE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_FALSE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_TRUE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_TRUE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_FALSE = Map.of(
      "down", "false", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_FALSE = Map.of(
      "down", "false", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_FALSE = Map.of(
      "down", "false", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_TRUE = Map.of(
      "down", "false", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_FALSE_WEST_TRUE = Map.of(
      "down", "false", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true"
   );
   private static final Map<String, String> DOWN_FALSE_EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_FALSE = Map.of(
      "down", "false", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false"
   );
   private static final Map<String, String> DOWN_TRUE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_TRUE_WEST_TRUE = Map.of(
      "down", "true", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"
   );
   private static final Map<String, String> POWERED_FALSE = Map.of("powered", "false");
   private static final Map<String, String> FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "east", "in_wall", "false", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "north", "in_wall", "false", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "south", "in_wall", "false", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "west", "in_wall", "false", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "east", "in_wall", "false", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "north", "in_wall", "false", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "south", "in_wall", "false", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "west", "in_wall", "false", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "east", "in_wall", "true", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "north", "in_wall", "true", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "south", "in_wall", "true", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE = Map.of(
      "facing", "west", "in_wall", "true", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "east", "in_wall", "true", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "north", "in_wall", "true", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "south", "in_wall", "true", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE = Map.of(
      "facing", "west", "in_wall", "true", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_MODE_COMPARE_POWERED_FALSE = Map.of("facing", "east", "mode", "compare", "powered", "false");
   private static final Map<String, String> FACING_NORTH_MODE_COMPARE_POWERED_FALSE = Map.of("facing", "north", "mode", "compare", "powered", "false");
   private static final Map<String, String> FACING_SOUTH_MODE_COMPARE_POWERED_FALSE = Map.of("facing", "south", "mode", "compare", "powered", "false");
   private static final Map<String, String> FACING_WEST_MODE_COMPARE_POWERED_FALSE = Map.of("facing", "west", "mode", "compare", "powered", "false");
   private static final Map<String, String> FACING_EAST_MODE_SUBTRACT_POWERED_FALSE = Map.of("facing", "east", "mode", "subtract", "powered", "false");
   private static final Map<String, String> FACING_NORTH_MODE_SUBTRACT_POWERED_FALSE = Map.of("facing", "north", "mode", "subtract", "powered", "false");
   private static final Map<String, String> FACING_SOUTH_MODE_SUBTRACT_POWERED_FALSE = Map.of("facing", "south", "mode", "subtract", "powered", "false");
   private static final Map<String, String> FACING_WEST_MODE_SUBTRACT_POWERED_FALSE = Map.of("facing", "west", "mode", "subtract", "powered", "false");
   private static final Map<String, String> POWERED_TRUE = Map.of("powered", "true");
   private static final Map<String, String> FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "east", "in_wall", "false", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "north", "in_wall", "false", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "south", "in_wall", "false", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "west", "in_wall", "false", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "east", "in_wall", "false", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "north", "in_wall", "false", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "south", "in_wall", "false", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "west", "in_wall", "false", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "east", "in_wall", "true", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "north", "in_wall", "true", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "south", "in_wall", "true", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE = Map.of(
      "facing", "west", "in_wall", "true", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "east", "in_wall", "true", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "north", "in_wall", "true", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "south", "in_wall", "true", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE = Map.of(
      "facing", "west", "in_wall", "true", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_MODE_COMPARE_POWERED_TRUE = Map.of("facing", "east", "mode", "compare", "powered", "true");
   private static final Map<String, String> FACING_NORTH_MODE_COMPARE_POWERED_TRUE = Map.of("facing", "north", "mode", "compare", "powered", "true");
   private static final Map<String, String> FACING_SOUTH_MODE_COMPARE_POWERED_TRUE = Map.of("facing", "south", "mode", "compare", "powered", "true");
   private static final Map<String, String> FACING_WEST_MODE_COMPARE_POWERED_TRUE = Map.of("facing", "west", "mode", "compare", "powered", "true");
   private static final Map<String, String> FACING_EAST_MODE_SUBTRACT_POWERED_TRUE = Map.of("facing", "east", "mode", "subtract", "powered", "true");
   private static final Map<String, String> FACING_NORTH_MODE_SUBTRACT_POWERED_TRUE = Map.of("facing", "north", "mode", "subtract", "powered", "true");
   private static final Map<String, String> FACING_SOUTH_MODE_SUBTRACT_POWERED_TRUE = Map.of("facing", "south", "mode", "subtract", "powered", "true");
   private static final Map<String, String> FACING_WEST_MODE_SUBTRACT_POWERED_TRUE = Map.of("facing", "west", "mode", "subtract", "powered", "true");
   private static final Map<String, String> POWER_0 = Map.of("power", "0");
   private static final Map<String, String> POWER_1 = Map.of("power", "1");
   private static final Map<String, String> POWER_10 = Map.of("power", "10");
   private static final Map<String, String> POWER_11 = Map.of("power", "11");
   private static final Map<String, String> POWER_12 = Map.of("power", "12");
   private static final Map<String, String> POWER_13 = Map.of("power", "13");
   private static final Map<String, String> POWER_14 = Map.of("power", "14");
   private static final Map<String, String> POWER_15 = Map.of("power", "15");
   private static final Map<String, String> POWER_2 = Map.of("power", "2");
   private static final Map<String, String> POWER_3 = Map.of("power", "3");
   private static final Map<String, String> POWER_4 = Map.of("power", "4");
   private static final Map<String, String> POWER_5 = Map.of("power", "5");
   private static final Map<String, String> POWER_6 = Map.of("power", "6");
   private static final Map<String, String> POWER_7 = Map.of("power", "7");
   private static final Map<String, String> POWER_8 = Map.of("power", "8");
   private static final Map<String, String> POWER_9 = Map.of("power", "9");
   private static final Map<String, String> ROTATION_0 = Map.of("rotation", "0");
   private static final Map<String, String> ROTATION_1 = Map.of("rotation", "1");
   private static final Map<String, String> ROTATION_10 = Map.of("rotation", "10");
   private static final Map<String, String> ROTATION_11 = Map.of("rotation", "11");
   private static final Map<String, String> ROTATION_12 = Map.of("rotation", "12");
   private static final Map<String, String> ROTATION_13 = Map.of("rotation", "13");
   private static final Map<String, String> ROTATION_14 = Map.of("rotation", "14");
   private static final Map<String, String> ROTATION_15 = Map.of("rotation", "15");
   private static final Map<String, String> ROTATION_2 = Map.of("rotation", "2");
   private static final Map<String, String> ROTATION_3 = Map.of("rotation", "3");
   private static final Map<String, String> ROTATION_4 = Map.of("rotation", "4");
   private static final Map<String, String> ROTATION_5 = Map.of("rotation", "5");
   private static final Map<String, String> ROTATION_6 = Map.of("rotation", "6");
   private static final Map<String, String> ROTATION_7 = Map.of("rotation", "7");
   private static final Map<String, String> ROTATION_8 = Map.of("rotation", "8");
   private static final Map<String, String> ROTATION_9 = Map.of("rotation", "9");
   private static final Map<String, String> POWERED_FALSE_SHAPE_ASCENDING_EAST = Map.of("powered", "false", "shape", "ascending_east");
   private static final Map<String, String> POWERED_TRUE_SHAPE_ASCENDING_EAST = Map.of("powered", "true", "shape", "ascending_east");
   private static final Map<String, String> POWERED_FALSE_SHAPE_ASCENDING_NORTH = Map.of("powered", "false", "shape", "ascending_north");
   private static final Map<String, String> POWERED_TRUE_SHAPE_ASCENDING_NORTH = Map.of("powered", "true", "shape", "ascending_north");
   private static final Map<String, String> POWERED_FALSE_SHAPE_ASCENDING_SOUTH = Map.of("powered", "false", "shape", "ascending_south");
   private static final Map<String, String> POWERED_TRUE_SHAPE_ASCENDING_SOUTH = Map.of("powered", "true", "shape", "ascending_south");
   private static final Map<String, String> POWERED_FALSE_SHAPE_ASCENDING_WEST = Map.of("powered", "false", "shape", "ascending_west");
   private static final Map<String, String> POWERED_TRUE_SHAPE_ASCENDING_WEST = Map.of("powered", "true", "shape", "ascending_west");
   private static final Map<String, String> POWERED_FALSE_SHAPE_EAST_WEST = Map.of("powered", "false", "shape", "east_west");
   private static final Map<String, String> POWERED_TRUE_SHAPE_EAST_WEST = Map.of("powered", "true", "shape", "east_west");
   private static final Map<String, String> POWERED_FALSE_SHAPE_NORTH_SOUTH = Map.of("powered", "false", "shape", "north_south");
   private static final Map<String, String> POWERED_TRUE_SHAPE_NORTH_SOUTH = Map.of("powered", "true", "shape", "north_south");
   private static final Map<String, String> SNOWY_FALSE = Map.of("snowy", "false");
   private static final Map<String, String> STAGE_0 = Map.of("stage", "0");
   private static final Map<String, String> STAGE_1 = Map.of("stage", "1");
   private static final Map<String, String> FACING_DOWN_TRIGGERED_FALSE = Map.of("facing", "down", "triggered", "false");
   private static final Map<String, String> FACING_EAST_TRIGGERED_FALSE = Map.of("facing", "east", "triggered", "false");
   private static final Map<String, String> FACING_NORTH_TRIGGERED_FALSE = Map.of("facing", "north", "triggered", "false");
   private static final Map<String, String> FACING_SOUTH_TRIGGERED_FALSE = Map.of("facing", "south", "triggered", "false");
   private static final Map<String, String> FACING_UP_TRIGGERED_FALSE = Map.of("facing", "up", "triggered", "false");
   private static final Map<String, String> FACING_WEST_TRIGGERED_FALSE = Map.of("facing", "west", "triggered", "false");
   private static final Map<String, String> FACING_DOWN_TRIGGERED_TRUE = Map.of("facing", "down", "triggered", "true");
   private static final Map<String, String> FACING_EAST_TRIGGERED_TRUE = Map.of("facing", "east", "triggered", "true");
   private static final Map<String, String> FACING_NORTH_TRIGGERED_TRUE = Map.of("facing", "north", "triggered", "true");
   private static final Map<String, String> FACING_SOUTH_TRIGGERED_TRUE = Map.of("facing", "south", "triggered", "true");
   private static final Map<String, String> FACING_UP_TRIGGERED_TRUE = Map.of("facing", "up", "triggered", "true");
   private static final Map<String, String> FACING_WEST_TRIGGERED_TRUE = Map.of("facing", "west", "triggered", "true");
   private static final Map<String, String> TYPE_BOTTOM = Map.of("type", "bottom");
   private static final Map<String, String> TYPE_DOUBLE = Map.of("type", "double");
   private static final Map<String, String> TYPE_TOP = Map.of("type", "top");
   private static final Map<String, String> EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE = Map.of(
      "east", "false", "north", "false", "south", "false", "up", "false", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "east", "false", "north", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "east", "true", "north", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE = Map.of(
      "east", "false", "north", "false", "south", "true", "west", "false"
   );
   private static final Map<String, String> EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE = Map.of(
      "east", "true", "north", "false", "south", "true", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "east", "false", "north", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "east", "true", "north", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE = Map.of(
      "east", "false", "north", "true", "south", "true", "west", "false"
   );
   private static final Map<String, String> EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE = Map.of(
      "east", "true", "north", "true", "south", "true", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE = Map.of(
      "east", "false", "north", "false", "south", "false", "west", "true"
   );
   private static final Map<String, String> EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE = Map.of(
      "east", "true", "north", "false", "south", "false", "west", "true"
   );
   private static final Map<String, String> EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE = Map.of(
      "east", "false", "north", "false", "south", "true", "west", "true"
   );
   private static final Map<String, String> EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE = Map.of(
      "east", "true", "north", "false", "south", "true", "west", "true"
   );
   private static final Map<String, String> EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE = Map.of(
      "east", "false", "north", "true", "south", "false", "west", "true"
   );
   private static final Map<String, String> EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE = Map.of(
      "east", "true", "north", "true", "south", "false", "west", "true"
   );
   private static final Map<String, String> EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE = Map.of(
      "east", "false", "north", "true", "south", "true", "west", "true"
   );
   private static final Map<String, String> EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE = Map.of("east", "true", "north", "true", "south", "true", "west", "true");

   private static Dynamic<?> createStateDynamic(String name) {
      return new Dynamic(JavaOps.INSTANCE, Map.of("Name", name)).convert(NbtOps.INSTANCE);
   }

   private static Dynamic<?> createStateDynamic(String name, Map<String, String> properties) {
      return new Dynamic(JavaOps.INSTANCE, Map.of("Name", name, "Properties", properties)).convert(NbtOps.INSTANCE);
   }

   private static void putStates(int oldIdAndMeta, Dynamic<?> newStateDynamic, Dynamic<?>... oldStateDynamics) {
      OLD_STATE_TO_DYNAMIC[oldIdAndMeta] = newStateDynamic;
      int i = oldIdAndMeta >> 4;
      if (OLD_BLOCK_TO_DYNAMIC[i] == null) {
         OLD_BLOCK_TO_DYNAMIC[i] = newStateDynamic;
      }

      for (Dynamic<?> dynamic : oldStateDynamics) {
         String string = dynamic.get("Name").asString("");
         OLD_BLOCK_TO_ID.putIfAbsent(string, oldIdAndMeta);
         OLD_STATE_TO_ID.put(dynamic, oldIdAndMeta);
      }
   }

   private static void fillEmptyStates() {
      for (int i = 0; i < OLD_STATE_TO_DYNAMIC.length; i++) {
         if (OLD_STATE_TO_DYNAMIC[i] == null) {
            OLD_STATE_TO_DYNAMIC[i] = OLD_BLOCK_TO_DYNAMIC[i >> 4];
         }
      }
   }

   public static Dynamic<?> lookupState(Dynamic<?> dynamic) {
      int i = OLD_STATE_TO_ID.getInt(dynamic);
      if (i >= 0 && i < OLD_STATE_TO_DYNAMIC.length) {
         Dynamic<?> dynamic2 = OLD_STATE_TO_DYNAMIC[i];
         return dynamic2 == null ? dynamic : dynamic2;
      } else {
         return dynamic;
      }
   }

   public static String lookupBlock(String oldBlockName) {
      int i = OLD_BLOCK_TO_ID.getInt(oldBlockName);
      if (i >= 0 && i < OLD_STATE_TO_DYNAMIC.length) {
         Dynamic<?> dynamic = OLD_STATE_TO_DYNAMIC[i];
         return dynamic == null ? oldBlockName : dynamic.get("Name").asString("");
      } else {
         return oldBlockName;
      }
   }

   public static String lookupStateBlock(int stateId) {
      if (stateId >= 0 && stateId < OLD_STATE_TO_DYNAMIC.length) {
         Dynamic<?> dynamic = OLD_STATE_TO_DYNAMIC[stateId];
         return dynamic == null ? "minecraft:air" : dynamic.get("Name").asString("");
      } else {
         return "minecraft:air";
      }
   }

   public static Dynamic<?> lookupState(int stateId) {
      Dynamic<?> dynamic = null;
      if (stateId >= 0 && stateId < OLD_STATE_TO_DYNAMIC.length) {
         dynamic = OLD_STATE_TO_DYNAMIC[stateId];
      }

      return dynamic == null ? OLD_STATE_TO_DYNAMIC[0] : dynamic;
   }

   private static void putStatesFromBlocks0To15() {
      putStates(0, createStateDynamic("minecraft:air"), createStateDynamic("minecraft:air"));
      putStates(16, createStateDynamic("minecraft:stone"), createStateDynamic("minecraft:stone", Map.of("variant", "stone")));
      putStates(17, createStateDynamic("minecraft:granite"), createStateDynamic("minecraft:stone", Map.of("variant", "granite")));
      putStates(18, createStateDynamic("minecraft:polished_granite"), createStateDynamic("minecraft:stone", Map.of("variant", "smooth_granite")));
      putStates(19, createStateDynamic("minecraft:diorite"), createStateDynamic("minecraft:stone", Map.of("variant", "diorite")));
      putStates(20, createStateDynamic("minecraft:polished_diorite"), createStateDynamic("minecraft:stone", Map.of("variant", "smooth_diorite")));
      putStates(21, createStateDynamic("minecraft:andesite"), createStateDynamic("minecraft:stone", Map.of("variant", "andesite")));
      putStates(22, createStateDynamic("minecraft:polished_andesite"), createStateDynamic("minecraft:stone", Map.of("variant", "smooth_andesite")));
      putStates(
         32,
         createStateDynamic("minecraft:grass_block", SNOWY_FALSE),
         createStateDynamic("minecraft:grass", SNOWY_FALSE),
         createStateDynamic("minecraft:grass", Map.of("snowy", "true"))
      );
      putStates(
         48,
         createStateDynamic("minecraft:dirt"),
         createStateDynamic("minecraft:dirt", Map.of("snowy", "false", "variant", "dirt")),
         createStateDynamic("minecraft:dirt", Map.of("snowy", "true", "variant", "dirt"))
      );
      putStates(
         49,
         createStateDynamic("minecraft:coarse_dirt"),
         createStateDynamic("minecraft:dirt", Map.of("snowy", "false", "variant", "coarse_dirt")),
         createStateDynamic("minecraft:dirt", Map.of("snowy", "true", "variant", "coarse_dirt"))
      );
      putStates(
         50,
         createStateDynamic("minecraft:podzol", SNOWY_FALSE),
         createStateDynamic("minecraft:dirt", Map.of("snowy", "false", "variant", "podzol")),
         createStateDynamic("minecraft:dirt", Map.of("snowy", "true", "variant", "podzol"))
      );
      putStates(64, createStateDynamic("minecraft:cobblestone"), createStateDynamic("minecraft:cobblestone"));
      putStates(80, createStateDynamic("minecraft:oak_planks"), createStateDynamic("minecraft:planks", Map.of("variant", "oak")));
      putStates(81, createStateDynamic("minecraft:spruce_planks"), createStateDynamic("minecraft:planks", Map.of("variant", "spruce")));
      putStates(82, createStateDynamic("minecraft:birch_planks"), createStateDynamic("minecraft:planks", Map.of("variant", "birch")));
      putStates(83, createStateDynamic("minecraft:jungle_planks"), createStateDynamic("minecraft:planks", Map.of("variant", "jungle")));
      putStates(84, createStateDynamic("minecraft:acacia_planks"), createStateDynamic("minecraft:planks", Map.of("variant", "acacia")));
      putStates(85, createStateDynamic("minecraft:dark_oak_planks"), createStateDynamic("minecraft:planks", Map.of("variant", "dark_oak")));
      putStates(96, createStateDynamic("minecraft:oak_sapling", STAGE_0), createStateDynamic("minecraft:sapling", Map.of("stage", "0", "type", "oak")));
      putStates(97, createStateDynamic("minecraft:spruce_sapling", STAGE_0), createStateDynamic("minecraft:sapling", Map.of("stage", "0", "type", "spruce")));
      putStates(98, createStateDynamic("minecraft:birch_sapling", STAGE_0), createStateDynamic("minecraft:sapling", Map.of("stage", "0", "type", "birch")));
      putStates(99, createStateDynamic("minecraft:jungle_sapling", STAGE_0), createStateDynamic("minecraft:sapling", Map.of("stage", "0", "type", "jungle")));
      putStates(100, createStateDynamic("minecraft:acacia_sapling", STAGE_0), createStateDynamic("minecraft:sapling", Map.of("stage", "0", "type", "acacia")));
      putStates(
         101, createStateDynamic("minecraft:dark_oak_sapling", STAGE_0), createStateDynamic("minecraft:sapling", Map.of("stage", "0", "type", "dark_oak"))
      );
      putStates(104, createStateDynamic("minecraft:oak_sapling", STAGE_1), createStateDynamic("minecraft:sapling", Map.of("stage", "1", "type", "oak")));
      putStates(105, createStateDynamic("minecraft:spruce_sapling", STAGE_1), createStateDynamic("minecraft:sapling", Map.of("stage", "1", "type", "spruce")));
      putStates(106, createStateDynamic("minecraft:birch_sapling", STAGE_1), createStateDynamic("minecraft:sapling", Map.of("stage", "1", "type", "birch")));
      putStates(107, createStateDynamic("minecraft:jungle_sapling", STAGE_1), createStateDynamic("minecraft:sapling", Map.of("stage", "1", "type", "jungle")));
      putStates(108, createStateDynamic("minecraft:acacia_sapling", STAGE_1), createStateDynamic("minecraft:sapling", Map.of("stage", "1", "type", "acacia")));
      putStates(
         109, createStateDynamic("minecraft:dark_oak_sapling", STAGE_1), createStateDynamic("minecraft:sapling", Map.of("stage", "1", "type", "dark_oak"))
      );
      putStates(112, createStateDynamic("minecraft:bedrock"), createStateDynamic("minecraft:bedrock"));
      putStates(128, createStateDynamic("minecraft:water", LEVEL_0), createStateDynamic("minecraft:flowing_water", LEVEL_0));
      putStates(129, createStateDynamic("minecraft:water", LEVEL_1), createStateDynamic("minecraft:flowing_water", LEVEL_1));
      putStates(130, createStateDynamic("minecraft:water", LEVEL_2), createStateDynamic("minecraft:flowing_water", LEVEL_2));
      putStates(131, createStateDynamic("minecraft:water", LEVEL_3), createStateDynamic("minecraft:flowing_water", LEVEL_3));
      putStates(132, createStateDynamic("minecraft:water", LEVEL_4), createStateDynamic("minecraft:flowing_water", LEVEL_4));
      putStates(133, createStateDynamic("minecraft:water", LEVEL_5), createStateDynamic("minecraft:flowing_water", LEVEL_5));
      putStates(134, createStateDynamic("minecraft:water", LEVEL_6), createStateDynamic("minecraft:flowing_water", LEVEL_6));
      putStates(135, createStateDynamic("minecraft:water", LEVEL_7), createStateDynamic("minecraft:flowing_water", LEVEL_7));
      putStates(136, createStateDynamic("minecraft:water", LEVEL_8), createStateDynamic("minecraft:flowing_water", LEVEL_8));
      putStates(137, createStateDynamic("minecraft:water", LEVEL_9), createStateDynamic("minecraft:flowing_water", LEVEL_9));
      putStates(138, createStateDynamic("minecraft:water", LEVEL_10), createStateDynamic("minecraft:flowing_water", LEVEL_10));
      putStates(139, createStateDynamic("minecraft:water", LEVEL_11), createStateDynamic("minecraft:flowing_water", LEVEL_11));
      putStates(140, createStateDynamic("minecraft:water", LEVEL_12), createStateDynamic("minecraft:flowing_water", LEVEL_12));
      putStates(141, createStateDynamic("minecraft:water", LEVEL_13), createStateDynamic("minecraft:flowing_water", LEVEL_13));
      putStates(142, createStateDynamic("minecraft:water", LEVEL_14), createStateDynamic("minecraft:flowing_water", LEVEL_14));
      putStates(143, createStateDynamic("minecraft:water", LEVEL_15), createStateDynamic("minecraft:flowing_water", LEVEL_15));
      putStates(144, createStateDynamic("minecraft:water", LEVEL_0), createStateDynamic("minecraft:water", LEVEL_0));
      putStates(145, createStateDynamic("minecraft:water", LEVEL_1), createStateDynamic("minecraft:water", LEVEL_1));
      putStates(146, createStateDynamic("minecraft:water", LEVEL_2), createStateDynamic("minecraft:water", LEVEL_2));
      putStates(147, createStateDynamic("minecraft:water", LEVEL_3), createStateDynamic("minecraft:water", LEVEL_3));
      putStates(148, createStateDynamic("minecraft:water", LEVEL_4), createStateDynamic("minecraft:water", LEVEL_4));
      putStates(149, createStateDynamic("minecraft:water", LEVEL_5), createStateDynamic("minecraft:water", LEVEL_5));
      putStates(150, createStateDynamic("minecraft:water", LEVEL_6), createStateDynamic("minecraft:water", LEVEL_6));
      putStates(151, createStateDynamic("minecraft:water", LEVEL_7), createStateDynamic("minecraft:water", LEVEL_7));
      putStates(152, createStateDynamic("minecraft:water", LEVEL_8), createStateDynamic("minecraft:water", LEVEL_8));
      putStates(153, createStateDynamic("minecraft:water", LEVEL_9), createStateDynamic("minecraft:water", LEVEL_9));
      putStates(154, createStateDynamic("minecraft:water", LEVEL_10), createStateDynamic("minecraft:water", LEVEL_10));
      putStates(155, createStateDynamic("minecraft:water", LEVEL_11), createStateDynamic("minecraft:water", LEVEL_11));
      putStates(156, createStateDynamic("minecraft:water", LEVEL_12), createStateDynamic("minecraft:water", LEVEL_12));
      putStates(157, createStateDynamic("minecraft:water", LEVEL_13), createStateDynamic("minecraft:water", LEVEL_13));
      putStates(158, createStateDynamic("minecraft:water", LEVEL_14), createStateDynamic("minecraft:water", LEVEL_14));
      putStates(159, createStateDynamic("minecraft:water", LEVEL_15), createStateDynamic("minecraft:water", LEVEL_15));
      putStates(160, createStateDynamic("minecraft:lava", LEVEL_0), createStateDynamic("minecraft:flowing_lava", LEVEL_0));
      putStates(161, createStateDynamic("minecraft:lava", LEVEL_1), createStateDynamic("minecraft:flowing_lava", LEVEL_1));
      putStates(162, createStateDynamic("minecraft:lava", LEVEL_2), createStateDynamic("minecraft:flowing_lava", LEVEL_2));
      putStates(163, createStateDynamic("minecraft:lava", LEVEL_3), createStateDynamic("minecraft:flowing_lava", LEVEL_3));
      putStates(164, createStateDynamic("minecraft:lava", LEVEL_4), createStateDynamic("minecraft:flowing_lava", LEVEL_4));
      putStates(165, createStateDynamic("minecraft:lava", LEVEL_5), createStateDynamic("minecraft:flowing_lava", LEVEL_5));
      putStates(166, createStateDynamic("minecraft:lava", LEVEL_6), createStateDynamic("minecraft:flowing_lava", LEVEL_6));
      putStates(167, createStateDynamic("minecraft:lava", LEVEL_7), createStateDynamic("minecraft:flowing_lava", LEVEL_7));
      putStates(168, createStateDynamic("minecraft:lava", LEVEL_8), createStateDynamic("minecraft:flowing_lava", LEVEL_8));
      putStates(169, createStateDynamic("minecraft:lava", LEVEL_9), createStateDynamic("minecraft:flowing_lava", LEVEL_9));
      putStates(170, createStateDynamic("minecraft:lava", LEVEL_10), createStateDynamic("minecraft:flowing_lava", LEVEL_10));
      putStates(171, createStateDynamic("minecraft:lava", LEVEL_11), createStateDynamic("minecraft:flowing_lava", LEVEL_11));
      putStates(172, createStateDynamic("minecraft:lava", LEVEL_12), createStateDynamic("minecraft:flowing_lava", LEVEL_12));
      putStates(173, createStateDynamic("minecraft:lava", LEVEL_13), createStateDynamic("minecraft:flowing_lava", LEVEL_13));
      putStates(174, createStateDynamic("minecraft:lava", LEVEL_14), createStateDynamic("minecraft:flowing_lava", LEVEL_14));
      putStates(175, createStateDynamic("minecraft:lava", LEVEL_15), createStateDynamic("minecraft:flowing_lava", LEVEL_15));
      putStates(176, createStateDynamic("minecraft:lava", LEVEL_0), createStateDynamic("minecraft:lava", LEVEL_0));
      putStates(177, createStateDynamic("minecraft:lava", LEVEL_1), createStateDynamic("minecraft:lava", LEVEL_1));
      putStates(178, createStateDynamic("minecraft:lava", LEVEL_2), createStateDynamic("minecraft:lava", LEVEL_2));
      putStates(179, createStateDynamic("minecraft:lava", LEVEL_3), createStateDynamic("minecraft:lava", LEVEL_3));
      putStates(180, createStateDynamic("minecraft:lava", LEVEL_4), createStateDynamic("minecraft:lava", LEVEL_4));
      putStates(181, createStateDynamic("minecraft:lava", LEVEL_5), createStateDynamic("minecraft:lava", LEVEL_5));
      putStates(182, createStateDynamic("minecraft:lava", LEVEL_6), createStateDynamic("minecraft:lava", LEVEL_6));
      putStates(183, createStateDynamic("minecraft:lava", LEVEL_7), createStateDynamic("minecraft:lava", LEVEL_7));
      putStates(184, createStateDynamic("minecraft:lava", LEVEL_8), createStateDynamic("minecraft:lava", LEVEL_8));
      putStates(185, createStateDynamic("minecraft:lava", LEVEL_9), createStateDynamic("minecraft:lava", LEVEL_9));
      putStates(186, createStateDynamic("minecraft:lava", LEVEL_10), createStateDynamic("minecraft:lava", LEVEL_10));
      putStates(187, createStateDynamic("minecraft:lava", LEVEL_11), createStateDynamic("minecraft:lava", LEVEL_11));
      putStates(188, createStateDynamic("minecraft:lava", LEVEL_12), createStateDynamic("minecraft:lava", LEVEL_12));
      putStates(189, createStateDynamic("minecraft:lava", LEVEL_13), createStateDynamic("minecraft:lava", LEVEL_13));
      putStates(190, createStateDynamic("minecraft:lava", LEVEL_14), createStateDynamic("minecraft:lava", LEVEL_14));
      putStates(191, createStateDynamic("minecraft:lava", LEVEL_15), createStateDynamic("minecraft:lava", LEVEL_15));
      putStates(192, createStateDynamic("minecraft:sand"), createStateDynamic("minecraft:sand", Map.of("variant", "sand")));
      putStates(193, createStateDynamic("minecraft:red_sand"), createStateDynamic("minecraft:sand", Map.of("variant", "red_sand")));
      putStates(208, createStateDynamic("minecraft:gravel"), createStateDynamic("minecraft:gravel"));
      putStates(224, createStateDynamic("minecraft:gold_ore"), createStateDynamic("minecraft:gold_ore"));
      putStates(240, createStateDynamic("minecraft:iron_ore"), createStateDynamic("minecraft:iron_ore"));
   }

   private static void putStatesFromBlocks16To31() {
      putStates(256, createStateDynamic("minecraft:coal_ore"), createStateDynamic("minecraft:coal_ore"));
      putStates(272, createStateDynamic("minecraft:oak_log", AXIS_Y), createStateDynamic("minecraft:log", Map.of("axis", "y", "variant", "oak")));
      putStates(273, createStateDynamic("minecraft:spruce_log", AXIS_Y), createStateDynamic("minecraft:log", Map.of("axis", "y", "variant", "spruce")));
      putStates(274, createStateDynamic("minecraft:birch_log", AXIS_Y), createStateDynamic("minecraft:log", Map.of("axis", "y", "variant", "birch")));
      putStates(275, createStateDynamic("minecraft:jungle_log", AXIS_Y), createStateDynamic("minecraft:log", Map.of("axis", "y", "variant", "jungle")));
      putStates(276, createStateDynamic("minecraft:oak_log", AXIS_X), createStateDynamic("minecraft:log", Map.of("axis", "x", "variant", "oak")));
      putStates(277, createStateDynamic("minecraft:spruce_log", AXIS_X), createStateDynamic("minecraft:log", Map.of("axis", "x", "variant", "spruce")));
      putStates(278, createStateDynamic("minecraft:birch_log", AXIS_X), createStateDynamic("minecraft:log", Map.of("axis", "x", "variant", "birch")));
      putStates(279, createStateDynamic("minecraft:jungle_log", AXIS_X), createStateDynamic("minecraft:log", Map.of("axis", "x", "variant", "jungle")));
      putStates(280, createStateDynamic("minecraft:oak_log", AXIS_Z), createStateDynamic("minecraft:log", Map.of("axis", "z", "variant", "oak")));
      putStates(281, createStateDynamic("minecraft:spruce_log", AXIS_Z), createStateDynamic("minecraft:log", Map.of("axis", "z", "variant", "spruce")));
      putStates(282, createStateDynamic("minecraft:birch_log", AXIS_Z), createStateDynamic("minecraft:log", Map.of("axis", "z", "variant", "birch")));
      putStates(283, createStateDynamic("minecraft:jungle_log", AXIS_Z), createStateDynamic("minecraft:log", Map.of("axis", "z", "variant", "jungle")));
      putStates(284, createStateDynamic("minecraft:oak_bark"), createStateDynamic("minecraft:log", Map.of("axis", "none", "variant", "oak")));
      putStates(285, createStateDynamic("minecraft:spruce_bark"), createStateDynamic("minecraft:log", Map.of("axis", "none", "variant", "spruce")));
      putStates(286, createStateDynamic("minecraft:birch_bark"), createStateDynamic("minecraft:log", Map.of("axis", "none", "variant", "birch")));
      putStates(287, createStateDynamic("minecraft:jungle_bark"), createStateDynamic("minecraft:log", Map.of("axis", "none", "variant", "jungle")));
      putStates(
         288,
         createStateDynamic("minecraft:oak_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "false", "decayable", "true", "variant", "oak"))
      );
      putStates(
         289,
         createStateDynamic("minecraft:spruce_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "false", "decayable", "true", "variant", "spruce"))
      );
      putStates(
         290,
         createStateDynamic("minecraft:birch_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "false", "decayable", "true", "variant", "birch"))
      );
      putStates(
         291,
         createStateDynamic("minecraft:jungle_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "false", "decayable", "true", "variant", "jungle"))
      );
      putStates(
         292,
         createStateDynamic("minecraft:oak_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "false", "decayable", "false", "variant", "oak"))
      );
      putStates(
         293,
         createStateDynamic("minecraft:spruce_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "false", "decayable", "false", "variant", "spruce"))
      );
      putStates(
         294,
         createStateDynamic("minecraft:birch_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "false", "decayable", "false", "variant", "birch"))
      );
      putStates(
         295,
         createStateDynamic("minecraft:jungle_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "false", "decayable", "false", "variant", "jungle"))
      );
      putStates(
         296,
         createStateDynamic("minecraft:oak_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "true", "decayable", "true", "variant", "oak"))
      );
      putStates(
         297,
         createStateDynamic("minecraft:spruce_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "true", "decayable", "true", "variant", "spruce"))
      );
      putStates(
         298,
         createStateDynamic("minecraft:birch_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "true", "decayable", "true", "variant", "birch"))
      );
      putStates(
         299,
         createStateDynamic("minecraft:jungle_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "true", "decayable", "true", "variant", "jungle"))
      );
      putStates(
         300,
         createStateDynamic("minecraft:oak_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "true", "decayable", "false", "variant", "oak"))
      );
      putStates(
         301,
         createStateDynamic("minecraft:spruce_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "true", "decayable", "false", "variant", "spruce"))
      );
      putStates(
         302,
         createStateDynamic("minecraft:birch_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "true", "decayable", "false", "variant", "birch"))
      );
      putStates(
         303,
         createStateDynamic("minecraft:jungle_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves", Map.of("check_decay", "true", "decayable", "false", "variant", "jungle"))
      );
      putStates(304, createStateDynamic("minecraft:sponge"), createStateDynamic("minecraft:sponge", Map.of("wet", "false")));
      putStates(305, createStateDynamic("minecraft:wet_sponge"), createStateDynamic("minecraft:sponge", Map.of("wet", "true")));
      putStates(320, createStateDynamic("minecraft:glass"), createStateDynamic("minecraft:glass"));
      putStates(336, createStateDynamic("minecraft:lapis_ore"), createStateDynamic("minecraft:lapis_ore"));
      putStates(352, createStateDynamic("minecraft:lapis_block"), createStateDynamic("minecraft:lapis_block"));
      putStates(
         368, createStateDynamic("minecraft:dispenser", FACING_DOWN_TRIGGERED_FALSE), createStateDynamic("minecraft:dispenser", FACING_DOWN_TRIGGERED_FALSE)
      );
      putStates(369, createStateDynamic("minecraft:dispenser", FACING_UP_TRIGGERED_FALSE), createStateDynamic("minecraft:dispenser", FACING_UP_TRIGGERED_FALSE));
      putStates(
         370, createStateDynamic("minecraft:dispenser", FACING_NORTH_TRIGGERED_FALSE), createStateDynamic("minecraft:dispenser", FACING_NORTH_TRIGGERED_FALSE)
      );
      putStates(
         371, createStateDynamic("minecraft:dispenser", FACING_SOUTH_TRIGGERED_FALSE), createStateDynamic("minecraft:dispenser", FACING_SOUTH_TRIGGERED_FALSE)
      );
      putStates(
         372, createStateDynamic("minecraft:dispenser", FACING_WEST_TRIGGERED_FALSE), createStateDynamic("minecraft:dispenser", FACING_WEST_TRIGGERED_FALSE)
      );
      putStates(
         373, createStateDynamic("minecraft:dispenser", FACING_EAST_TRIGGERED_FALSE), createStateDynamic("minecraft:dispenser", FACING_EAST_TRIGGERED_FALSE)
      );
      putStates(
         376, createStateDynamic("minecraft:dispenser", FACING_DOWN_TRIGGERED_TRUE), createStateDynamic("minecraft:dispenser", FACING_DOWN_TRIGGERED_TRUE)
      );
      putStates(377, createStateDynamic("minecraft:dispenser", FACING_UP_TRIGGERED_TRUE), createStateDynamic("minecraft:dispenser", FACING_UP_TRIGGERED_TRUE));
      putStates(
         378, createStateDynamic("minecraft:dispenser", FACING_NORTH_TRIGGERED_TRUE), createStateDynamic("minecraft:dispenser", FACING_NORTH_TRIGGERED_TRUE)
      );
      putStates(
         379, createStateDynamic("minecraft:dispenser", FACING_SOUTH_TRIGGERED_TRUE), createStateDynamic("minecraft:dispenser", FACING_SOUTH_TRIGGERED_TRUE)
      );
      putStates(
         380, createStateDynamic("minecraft:dispenser", FACING_WEST_TRIGGERED_TRUE), createStateDynamic("minecraft:dispenser", FACING_WEST_TRIGGERED_TRUE)
      );
      putStates(
         381, createStateDynamic("minecraft:dispenser", FACING_EAST_TRIGGERED_TRUE), createStateDynamic("minecraft:dispenser", FACING_EAST_TRIGGERED_TRUE)
      );
      putStates(384, createStateDynamic("minecraft:sandstone"), createStateDynamic("minecraft:sandstone", Map.of("type", "sandstone")));
      putStates(385, createStateDynamic("minecraft:chiseled_sandstone"), createStateDynamic("minecraft:sandstone", Map.of("type", "chiseled_sandstone")));
      putStates(386, createStateDynamic("minecraft:cut_sandstone"), createStateDynamic("minecraft:sandstone", Map.of("type", "smooth_sandstone")));
      putStates(400, createStateDynamic("minecraft:note_block"), createStateDynamic("minecraft:noteblock"));
      putStates(
         416,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "south", "occupied", "false", "part", "foot")),
         createStateDynamic("minecraft:bed", Map.of("facing", "south", "occupied", "false", "part", "foot")),
         createStateDynamic("minecraft:bed", Map.of("facing", "south", "occupied", "true", "part", "foot"))
      );
      putStates(
         417,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "west", "occupied", "false", "part", "foot")),
         createStateDynamic("minecraft:bed", Map.of("facing", "west", "occupied", "false", "part", "foot")),
         createStateDynamic("minecraft:bed", Map.of("facing", "west", "occupied", "true", "part", "foot"))
      );
      putStates(
         418,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "north", "occupied", "false", "part", "foot")),
         createStateDynamic("minecraft:bed", Map.of("facing", "north", "occupied", "false", "part", "foot")),
         createStateDynamic("minecraft:bed", Map.of("facing", "north", "occupied", "true", "part", "foot"))
      );
      putStates(
         419,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "east", "occupied", "false", "part", "foot")),
         createStateDynamic("minecraft:bed", Map.of("facing", "east", "occupied", "false", "part", "foot")),
         createStateDynamic("minecraft:bed", Map.of("facing", "east", "occupied", "true", "part", "foot"))
      );
      putStates(
         424,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "south", "occupied", "false", "part", "head")),
         createStateDynamic("minecraft:bed", Map.of("facing", "south", "occupied", "false", "part", "head"))
      );
      putStates(
         425,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "west", "occupied", "false", "part", "head")),
         createStateDynamic("minecraft:bed", Map.of("facing", "west", "occupied", "false", "part", "head"))
      );
      putStates(
         426,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "north", "occupied", "false", "part", "head")),
         createStateDynamic("minecraft:bed", Map.of("facing", "north", "occupied", "false", "part", "head"))
      );
      putStates(
         427,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "east", "occupied", "false", "part", "head")),
         createStateDynamic("minecraft:bed", Map.of("facing", "east", "occupied", "false", "part", "head"))
      );
      putStates(
         428,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "south", "occupied", "true", "part", "head")),
         createStateDynamic("minecraft:bed", Map.of("facing", "south", "occupied", "true", "part", "head"))
      );
      putStates(
         429,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "west", "occupied", "true", "part", "head")),
         createStateDynamic("minecraft:bed", Map.of("facing", "west", "occupied", "true", "part", "head"))
      );
      putStates(
         430,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "north", "occupied", "true", "part", "head")),
         createStateDynamic("minecraft:bed", Map.of("facing", "north", "occupied", "true", "part", "head"))
      );
      putStates(
         431,
         createStateDynamic("minecraft:red_bed", Map.of("facing", "east", "occupied", "true", "part", "head")),
         createStateDynamic("minecraft:bed", Map.of("facing", "east", "occupied", "true", "part", "head"))
      );
      putStates(
         432,
         createStateDynamic("minecraft:powered_rail", POWERED_FALSE_SHAPE_NORTH_SOUTH),
         createStateDynamic("minecraft:golden_rail", POWERED_FALSE_SHAPE_NORTH_SOUTH)
      );
      putStates(
         433,
         createStateDynamic("minecraft:powered_rail", POWERED_FALSE_SHAPE_EAST_WEST),
         createStateDynamic("minecraft:golden_rail", POWERED_FALSE_SHAPE_EAST_WEST)
      );
      putStates(
         434,
         createStateDynamic("minecraft:powered_rail", POWERED_FALSE_SHAPE_ASCENDING_EAST),
         createStateDynamic("minecraft:golden_rail", POWERED_FALSE_SHAPE_ASCENDING_EAST)
      );
      putStates(
         435,
         createStateDynamic("minecraft:powered_rail", POWERED_FALSE_SHAPE_ASCENDING_WEST),
         createStateDynamic("minecraft:golden_rail", POWERED_FALSE_SHAPE_ASCENDING_WEST)
      );
      putStates(
         436,
         createStateDynamic("minecraft:powered_rail", POWERED_FALSE_SHAPE_ASCENDING_NORTH),
         createStateDynamic("minecraft:golden_rail", POWERED_FALSE_SHAPE_ASCENDING_NORTH)
      );
      putStates(
         437,
         createStateDynamic("minecraft:powered_rail", POWERED_FALSE_SHAPE_ASCENDING_SOUTH),
         createStateDynamic("minecraft:golden_rail", POWERED_FALSE_SHAPE_ASCENDING_SOUTH)
      );
      putStates(
         440,
         createStateDynamic("minecraft:powered_rail", POWERED_TRUE_SHAPE_NORTH_SOUTH),
         createStateDynamic("minecraft:golden_rail", POWERED_TRUE_SHAPE_NORTH_SOUTH)
      );
      putStates(
         441,
         createStateDynamic("minecraft:powered_rail", POWERED_TRUE_SHAPE_EAST_WEST),
         createStateDynamic("minecraft:golden_rail", POWERED_TRUE_SHAPE_EAST_WEST)
      );
      putStates(
         442,
         createStateDynamic("minecraft:powered_rail", POWERED_TRUE_SHAPE_ASCENDING_EAST),
         createStateDynamic("minecraft:golden_rail", POWERED_TRUE_SHAPE_ASCENDING_EAST)
      );
      putStates(
         443,
         createStateDynamic("minecraft:powered_rail", POWERED_TRUE_SHAPE_ASCENDING_WEST),
         createStateDynamic("minecraft:golden_rail", POWERED_TRUE_SHAPE_ASCENDING_WEST)
      );
      putStates(
         444,
         createStateDynamic("minecraft:powered_rail", POWERED_TRUE_SHAPE_ASCENDING_NORTH),
         createStateDynamic("minecraft:golden_rail", POWERED_TRUE_SHAPE_ASCENDING_NORTH)
      );
      putStates(
         445,
         createStateDynamic("minecraft:powered_rail", POWERED_TRUE_SHAPE_ASCENDING_SOUTH),
         createStateDynamic("minecraft:golden_rail", POWERED_TRUE_SHAPE_ASCENDING_SOUTH)
      );
      putStates(
         448,
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_NORTH_SOUTH),
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_NORTH_SOUTH)
      );
      putStates(
         449,
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_EAST_WEST),
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_EAST_WEST)
      );
      putStates(
         450,
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_ASCENDING_EAST),
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_ASCENDING_EAST)
      );
      putStates(
         451,
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_ASCENDING_WEST),
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_ASCENDING_WEST)
      );
      putStates(
         452,
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_ASCENDING_NORTH),
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_ASCENDING_NORTH)
      );
      putStates(
         453,
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_ASCENDING_SOUTH),
         createStateDynamic("minecraft:detector_rail", POWERED_FALSE_SHAPE_ASCENDING_SOUTH)
      );
      putStates(
         456,
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_NORTH_SOUTH),
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_NORTH_SOUTH)
      );
      putStates(
         457,
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_EAST_WEST),
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_EAST_WEST)
      );
      putStates(
         458,
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_ASCENDING_EAST),
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_ASCENDING_EAST)
      );
      putStates(
         459,
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_ASCENDING_WEST),
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_ASCENDING_WEST)
      );
      putStates(
         460,
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_ASCENDING_NORTH),
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_ASCENDING_NORTH)
      );
      putStates(
         461,
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_ASCENDING_SOUTH),
         createStateDynamic("minecraft:detector_rail", POWERED_TRUE_SHAPE_ASCENDING_SOUTH)
      );
      putStates(
         464,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_DOWN),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_DOWN)
      );
      putStates(
         465, createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_UP), createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_UP)
      );
      putStates(
         466,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_NORTH),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_NORTH)
      );
      putStates(
         467,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_SOUTH),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_SOUTH)
      );
      putStates(
         468,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_WEST),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_WEST)
      );
      putStates(
         469,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_EAST),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_FALSE_FACING_EAST)
      );
      putStates(
         472,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_DOWN),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_DOWN)
      );
      putStates(
         473, createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_UP), createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_UP)
      );
      putStates(
         474,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_NORTH),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_NORTH)
      );
      putStates(
         475,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_SOUTH),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_SOUTH)
      );
      putStates(
         476,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_WEST),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_WEST)
      );
      putStates(
         477,
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_EAST),
         createStateDynamic("minecraft:sticky_piston", EXTENDED_TRUE_FACING_EAST)
      );
      putStates(480, createStateDynamic("minecraft:cobweb"), createStateDynamic("minecraft:web"));
      putStates(496, createStateDynamic("minecraft:dead_bush"), createStateDynamic("minecraft:tallgrass", Map.of("type", "dead_bush")));
      putStates(497, createStateDynamic("minecraft:grass"), createStateDynamic("minecraft:tallgrass", Map.of("type", "tall_grass")));
      putStates(498, createStateDynamic("minecraft:fern"), createStateDynamic("minecraft:tallgrass", Map.of("type", "fern")));
   }

   private static void putStatesFromBlocks32To47() {
      putStates(512, createStateDynamic("minecraft:dead_bush"), createStateDynamic("minecraft:deadbush"));
      putStates(528, createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_DOWN), createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_DOWN));
      putStates(529, createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_UP), createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_UP));
      putStates(530, createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_NORTH), createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_NORTH));
      putStates(531, createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_SOUTH), createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_SOUTH));
      putStates(532, createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_WEST), createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_WEST));
      putStates(533, createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_EAST), createStateDynamic("minecraft:piston", EXTENDED_FALSE_FACING_EAST));
      putStates(536, createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_DOWN), createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_DOWN));
      putStates(537, createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_UP), createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_UP));
      putStates(538, createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_NORTH), createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_NORTH));
      putStates(539, createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_SOUTH), createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_SOUTH));
      putStates(540, createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_WEST), createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_WEST));
      putStates(541, createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_EAST), createStateDynamic("minecraft:piston", EXTENDED_TRUE_FACING_EAST));
      putStates(
         544,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "down", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "down", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "down", "short", "true", "type", "normal"))
      );
      putStates(
         545,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "up", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "up", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "up", "short", "true", "type", "normal"))
      );
      putStates(
         546,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "north", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "north", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "north", "short", "true", "type", "normal"))
      );
      putStates(
         547,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "south", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "south", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "south", "short", "true", "type", "normal"))
      );
      putStates(
         548,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "west", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "west", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "west", "short", "true", "type", "normal"))
      );
      putStates(
         549,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "east", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "east", "short", "false", "type", "normal")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "east", "short", "true", "type", "normal"))
      );
      putStates(
         552,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "down", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "down", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "down", "short", "true", "type", "sticky"))
      );
      putStates(
         553,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "up", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "up", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "up", "short", "true", "type", "sticky"))
      );
      putStates(
         554,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "north", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "north", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "north", "short", "true", "type", "sticky"))
      );
      putStates(
         555,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "south", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "south", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "south", "short", "true", "type", "sticky"))
      );
      putStates(
         556,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "west", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "west", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "west", "short", "true", "type", "sticky"))
      );
      putStates(
         557,
         createStateDynamic("minecraft:piston_head", Map.of("facing", "east", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "east", "short", "false", "type", "sticky")),
         createStateDynamic("minecraft:piston_head", Map.of("facing", "east", "short", "true", "type", "sticky"))
      );
      putStates(560, createStateDynamic("minecraft:white_wool"), createStateDynamic("minecraft:wool", COLOR_WHITE));
      putStates(561, createStateDynamic("minecraft:orange_wool"), createStateDynamic("minecraft:wool", COLOR_ORANGE));
      putStates(562, createStateDynamic("minecraft:magenta_wool"), createStateDynamic("minecraft:wool", COLOR_MAGENTA));
      putStates(563, createStateDynamic("minecraft:light_blue_wool"), createStateDynamic("minecraft:wool", COLOR_LIGHT_BLUE));
      putStates(564, createStateDynamic("minecraft:yellow_wool"), createStateDynamic("minecraft:wool", COLOR_YELLOW));
      putStates(565, createStateDynamic("minecraft:lime_wool"), createStateDynamic("minecraft:wool", COLOR_LIME));
      putStates(566, createStateDynamic("minecraft:pink_wool"), createStateDynamic("minecraft:wool", COLOR_PINK));
      putStates(567, createStateDynamic("minecraft:gray_wool"), createStateDynamic("minecraft:wool", COLOR_GRAY));
      putStates(568, createStateDynamic("minecraft:light_gray_wool"), createStateDynamic("minecraft:wool", COLOR_SILVER));
      putStates(569, createStateDynamic("minecraft:cyan_wool"), createStateDynamic("minecraft:wool", COLOR_CYAN));
      putStates(570, createStateDynamic("minecraft:purple_wool"), createStateDynamic("minecraft:wool", COLOR_PURPLE));
      putStates(571, createStateDynamic("minecraft:blue_wool"), createStateDynamic("minecraft:wool", COLOR_BLUE));
      putStates(572, createStateDynamic("minecraft:brown_wool"), createStateDynamic("minecraft:wool", COLOR_BROWN));
      putStates(573, createStateDynamic("minecraft:green_wool"), createStateDynamic("minecraft:wool", COLOR_GREEN));
      putStates(574, createStateDynamic("minecraft:red_wool"), createStateDynamic("minecraft:wool", COLOR_RED));
      putStates(575, createStateDynamic("minecraft:black_wool"), createStateDynamic("minecraft:wool", COLOR_BLACK));
      putStates(
         576,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "down", "type", "normal")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "down", "type", "normal"))
      );
      putStates(
         577,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "up", "type", "normal")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "up", "type", "normal"))
      );
      putStates(
         578,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "north", "type", "normal")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "north", "type", "normal"))
      );
      putStates(
         579,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "south", "type", "normal")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "south", "type", "normal"))
      );
      putStates(
         580,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "west", "type", "normal")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "west", "type", "normal"))
      );
      putStates(
         581,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "east", "type", "normal")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "east", "type", "normal"))
      );
      putStates(
         584,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "down", "type", "sticky")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "down", "type", "sticky"))
      );
      putStates(
         585,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "up", "type", "sticky")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "up", "type", "sticky"))
      );
      putStates(
         586,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "north", "type", "sticky")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "north", "type", "sticky"))
      );
      putStates(
         587,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "south", "type", "sticky")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "south", "type", "sticky"))
      );
      putStates(
         588,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "west", "type", "sticky")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "west", "type", "sticky"))
      );
      putStates(
         589,
         createStateDynamic("minecraft:moving_piston", Map.of("facing", "east", "type", "sticky")),
         createStateDynamic("minecraft:piston_extension", Map.of("facing", "east", "type", "sticky"))
      );
      putStates(592, createStateDynamic("minecraft:dandelion"), createStateDynamic("minecraft:yellow_flower", Map.of("type", "dandelion")));
      putStates(608, createStateDynamic("minecraft:poppy"), createStateDynamic("minecraft:red_flower", Map.of("type", "poppy")));
      putStates(609, createStateDynamic("minecraft:blue_orchid"), createStateDynamic("minecraft:red_flower", Map.of("type", "blue_orchid")));
      putStates(610, createStateDynamic("minecraft:allium"), createStateDynamic("minecraft:red_flower", Map.of("type", "allium")));
      putStates(611, createStateDynamic("minecraft:azure_bluet"), createStateDynamic("minecraft:red_flower", Map.of("type", "houstonia")));
      putStates(612, createStateDynamic("minecraft:red_tulip"), createStateDynamic("minecraft:red_flower", Map.of("type", "red_tulip")));
      putStates(613, createStateDynamic("minecraft:orange_tulip"), createStateDynamic("minecraft:red_flower", Map.of("type", "orange_tulip")));
      putStates(614, createStateDynamic("minecraft:white_tulip"), createStateDynamic("minecraft:red_flower", Map.of("type", "white_tulip")));
      putStates(615, createStateDynamic("minecraft:pink_tulip"), createStateDynamic("minecraft:red_flower", Map.of("type", "pink_tulip")));
      putStates(616, createStateDynamic("minecraft:oxeye_daisy"), createStateDynamic("minecraft:red_flower", Map.of("type", "oxeye_daisy")));
      putStates(624, createStateDynamic("minecraft:brown_mushroom"), createStateDynamic("minecraft:brown_mushroom"));
      putStates(640, createStateDynamic("minecraft:red_mushroom"), createStateDynamic("minecraft:red_mushroom"));
      putStates(656, createStateDynamic("minecraft:gold_block"), createStateDynamic("minecraft:gold_block"));
      putStates(672, createStateDynamic("minecraft:iron_block"), createStateDynamic("minecraft:iron_block"));
      putStates(
         688,
         createStateDynamic("minecraft:stone_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "stone"))
      );
      putStates(
         689,
         createStateDynamic("minecraft:sandstone_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "sandstone"))
      );
      putStates(
         690,
         createStateDynamic("minecraft:petrified_oak_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "wood_old"))
      );
      putStates(
         691,
         createStateDynamic("minecraft:cobblestone_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "cobblestone"))
      );
      putStates(
         692,
         createStateDynamic("minecraft:brick_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "brick"))
      );
      putStates(
         693,
         createStateDynamic("minecraft:stone_brick_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "stone_brick"))
      );
      putStates(
         694,
         createStateDynamic("minecraft:nether_brick_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "nether_brick"))
      );
      putStates(
         695,
         createStateDynamic("minecraft:quartz_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "quartz"))
      );
      putStates(
         696, createStateDynamic("minecraft:smooth_stone"), createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "stone"))
      );
      putStates(
         697,
         createStateDynamic("minecraft:smooth_sandstone"),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "sandstone"))
      );
      putStates(
         698,
         createStateDynamic("minecraft:petrified_oak_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "wood_old"))
      );
      putStates(
         699,
         createStateDynamic("minecraft:cobblestone_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "cobblestone"))
      );
      putStates(
         700,
         createStateDynamic("minecraft:brick_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "brick"))
      );
      putStates(
         701,
         createStateDynamic("minecraft:stone_brick_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "stone_brick"))
      );
      putStates(
         702,
         createStateDynamic("minecraft:nether_brick_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "nether_brick"))
      );
      putStates(
         703, createStateDynamic("minecraft:smooth_quartz"), createStateDynamic("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "quartz"))
      );
      putStates(
         704, createStateDynamic("minecraft:stone_slab", TYPE_BOTTOM), createStateDynamic("minecraft:stone_slab", Map.of("half", "bottom", "variant", "stone"))
      );
      putStates(
         705,
         createStateDynamic("minecraft:sandstone_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "bottom", "variant", "sandstone"))
      );
      putStates(
         706,
         createStateDynamic("minecraft:petrified_oak_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "bottom", "variant", "wood_old"))
      );
      putStates(
         707,
         createStateDynamic("minecraft:cobblestone_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "bottom", "variant", "cobblestone"))
      );
      putStates(
         708, createStateDynamic("minecraft:brick_slab", TYPE_BOTTOM), createStateDynamic("minecraft:stone_slab", Map.of("half", "bottom", "variant", "brick"))
      );
      putStates(
         709,
         createStateDynamic("minecraft:stone_brick_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "bottom", "variant", "stone_brick"))
      );
      putStates(
         710,
         createStateDynamic("minecraft:nether_brick_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "bottom", "variant", "nether_brick"))
      );
      putStates(
         711,
         createStateDynamic("minecraft:quartz_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "bottom", "variant", "quartz"))
      );
      putStates(
         712, createStateDynamic("minecraft:stone_slab", TYPE_TOP), createStateDynamic("minecraft:stone_slab", Map.of("half", "top", "variant", "stone"))
      );
      putStates(
         713,
         createStateDynamic("minecraft:sandstone_slab", TYPE_TOP),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "top", "variant", "sandstone"))
      );
      putStates(
         714,
         createStateDynamic("minecraft:petrified_oak_slab", TYPE_TOP),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "top", "variant", "wood_old"))
      );
      putStates(
         715,
         createStateDynamic("minecraft:cobblestone_slab", TYPE_TOP),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "top", "variant", "cobblestone"))
      );
      putStates(
         716, createStateDynamic("minecraft:brick_slab", TYPE_TOP), createStateDynamic("minecraft:stone_slab", Map.of("half", "top", "variant", "brick"))
      );
      putStates(
         717,
         createStateDynamic("minecraft:stone_brick_slab", TYPE_TOP),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "top", "variant", "stone_brick"))
      );
      putStates(
         718,
         createStateDynamic("minecraft:nether_brick_slab", TYPE_TOP),
         createStateDynamic("minecraft:stone_slab", Map.of("half", "top", "variant", "nether_brick"))
      );
      putStates(
         719, createStateDynamic("minecraft:quartz_slab", TYPE_TOP), createStateDynamic("minecraft:stone_slab", Map.of("half", "top", "variant", "quartz"))
      );
      putStates(720, createStateDynamic("minecraft:bricks"), createStateDynamic("minecraft:brick_block"));
      putStates(736, createStateDynamic("minecraft:tnt", Map.of("unstable", "false")), createStateDynamic("minecraft:tnt", Map.of("explode", "false")));
      putStates(737, createStateDynamic("minecraft:tnt", Map.of("unstable", "true")), createStateDynamic("minecraft:tnt", Map.of("explode", "true")));
      putStates(752, createStateDynamic("minecraft:bookshelf"), createStateDynamic("minecraft:bookshelf"));
   }

   private static void putStatesFromBlocks48To51() {
      putStates(768, createStateDynamic("minecraft:mossy_cobblestone"), createStateDynamic("minecraft:mossy_cobblestone"));
      putStates(784, createStateDynamic("minecraft:obsidian"), createStateDynamic("minecraft:obsidian"));
      putStates(801, createStateDynamic("minecraft:wall_torch", FACING_EAST), createStateDynamic("minecraft:torch", FACING_EAST));
      putStates(802, createStateDynamic("minecraft:wall_torch", FACING_WEST), createStateDynamic("minecraft:torch", FACING_WEST));
      putStates(803, createStateDynamic("minecraft:wall_torch", FACING_SOUTH), createStateDynamic("minecraft:torch", FACING_SOUTH));
      putStates(804, createStateDynamic("minecraft:wall_torch", FACING_NORTH), createStateDynamic("minecraft:torch", FACING_NORTH));
      putStates(805, createStateDynamic("minecraft:torch"), createStateDynamic("minecraft:torch", FACING_UP));
      putStates(
         816,
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         817,
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         818,
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         819,
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         820,
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         821,
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         822,
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         823,
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         824,
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         825,
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         826,
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         827,
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         828,
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         829,
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         830,
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         831,
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
   }

   private static void putStatesFromBlocks52To63() {
      putStates(832, createStateDynamic("minecraft:mob_spawner"), createStateDynamic("minecraft:mob_spawner"));
      putStates(
         848,
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         849,
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         850,
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         851,
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         852,
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         853,
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         854,
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         855,
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(866, createStateDynamic("minecraft:chest", Map.of("facing", "north", "type", "single")), createStateDynamic("minecraft:chest", FACING_NORTH));
      putStates(867, createStateDynamic("minecraft:chest", Map.of("facing", "south", "type", "single")), createStateDynamic("minecraft:chest", FACING_SOUTH));
      putStates(868, createStateDynamic("minecraft:chest", Map.of("facing", "west", "type", "single")), createStateDynamic("minecraft:chest", FACING_WEST));
      putStates(869, createStateDynamic("minecraft:chest", Map.of("facing", "east", "type", "single")), createStateDynamic("minecraft:chest", FACING_EAST));
      putStates(
         880,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "up", "west", "up"))
      );
      putStates(
         881,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "up", "west", "up"))
      );
      putStates(
         882,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "up", "west", "up"))
      );
      putStates(
         883,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "up", "west", "up"))
      );
      putStates(
         884,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "up", "west", "up"))
      );
      putStates(
         885,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "up", "west", "up"))
      );
      putStates(
         886,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "up", "west", "up"))
      );
      putStates(
         887,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "up", "west", "up"))
      );
      putStates(
         888,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "up", "west", "up"))
      );
      putStates(
         889,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "up", "west", "up"))
      );
      putStates(
         890,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "up", "west", "up"))
      );
      putStates(
         891,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "up", "west", "up"))
      );
      putStates(
         892,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "up", "west", "up"))
      );
      putStates(
         893,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "up", "west", "up"))
      );
      putStates(
         894,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "up", "west", "up"))
      );
      putStates(
         895,
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "up", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "none", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "none", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "none", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "side", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "side", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "side", "west", "up")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "up", "west", "none")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "up", "west", "side")),
         createStateDynamic("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "up", "west", "up"))
      );
      putStates(896, createStateDynamic("minecraft:diamond_ore"), createStateDynamic("minecraft:diamond_ore"));
      putStates(912, createStateDynamic("minecraft:diamond_block"), createStateDynamic("minecraft:diamond_block"));
      putStates(928, createStateDynamic("minecraft:crafting_table"), createStateDynamic("minecraft:crafting_table"));
      putStates(944, createStateDynamic("minecraft:wheat", AGE_0), createStateDynamic("minecraft:wheat", AGE_0));
      putStates(945, createStateDynamic("minecraft:wheat", AGE_1), createStateDynamic("minecraft:wheat", AGE_1));
      putStates(946, createStateDynamic("minecraft:wheat", AGE_2), createStateDynamic("minecraft:wheat", AGE_2));
      putStates(947, createStateDynamic("minecraft:wheat", AGE_3), createStateDynamic("minecraft:wheat", AGE_3));
      putStates(948, createStateDynamic("minecraft:wheat", AGE_4), createStateDynamic("minecraft:wheat", AGE_4));
      putStates(949, createStateDynamic("minecraft:wheat", AGE_5), createStateDynamic("minecraft:wheat", AGE_5));
      putStates(950, createStateDynamic("minecraft:wheat", AGE_6), createStateDynamic("minecraft:wheat", AGE_6));
      putStates(951, createStateDynamic("minecraft:wheat", AGE_7), createStateDynamic("minecraft:wheat", AGE_7));
      putStates(960, createStateDynamic("minecraft:farmland", Map.of("moisture", "0")), createStateDynamic("minecraft:farmland", Map.of("moisture", "0")));
      putStates(961, createStateDynamic("minecraft:farmland", Map.of("moisture", "1")), createStateDynamic("minecraft:farmland", Map.of("moisture", "1")));
      putStates(962, createStateDynamic("minecraft:farmland", Map.of("moisture", "2")), createStateDynamic("minecraft:farmland", Map.of("moisture", "2")));
      putStates(963, createStateDynamic("minecraft:farmland", Map.of("moisture", "3")), createStateDynamic("minecraft:farmland", Map.of("moisture", "3")));
      putStates(964, createStateDynamic("minecraft:farmland", Map.of("moisture", "4")), createStateDynamic("minecraft:farmland", Map.of("moisture", "4")));
      putStates(965, createStateDynamic("minecraft:farmland", Map.of("moisture", "5")), createStateDynamic("minecraft:farmland", Map.of("moisture", "5")));
      putStates(966, createStateDynamic("minecraft:farmland", Map.of("moisture", "6")), createStateDynamic("minecraft:farmland", Map.of("moisture", "6")));
      putStates(967, createStateDynamic("minecraft:farmland", Map.of("moisture", "7")), createStateDynamic("minecraft:farmland", Map.of("moisture", "7")));
      putStates(978, createStateDynamic("minecraft:furnace", Map.of("facing", "north", "lit", "false")), createStateDynamic("minecraft:furnace", FACING_NORTH));
      putStates(979, createStateDynamic("minecraft:furnace", Map.of("facing", "south", "lit", "false")), createStateDynamic("minecraft:furnace", FACING_SOUTH));
      putStates(980, createStateDynamic("minecraft:furnace", Map.of("facing", "west", "lit", "false")), createStateDynamic("minecraft:furnace", FACING_WEST));
      putStates(981, createStateDynamic("minecraft:furnace", Map.of("facing", "east", "lit", "false")), createStateDynamic("minecraft:furnace", FACING_EAST));
      putStates(
         994, createStateDynamic("minecraft:furnace", Map.of("facing", "north", "lit", "true")), createStateDynamic("minecraft:lit_furnace", FACING_NORTH)
      );
      putStates(
         995, createStateDynamic("minecraft:furnace", Map.of("facing", "south", "lit", "true")), createStateDynamic("minecraft:lit_furnace", FACING_SOUTH)
      );
      putStates(996, createStateDynamic("minecraft:furnace", Map.of("facing", "west", "lit", "true")), createStateDynamic("minecraft:lit_furnace", FACING_WEST));
      putStates(997, createStateDynamic("minecraft:furnace", Map.of("facing", "east", "lit", "true")), createStateDynamic("minecraft:lit_furnace", FACING_EAST));
      putStates(1008, createStateDynamic("minecraft:sign", ROTATION_0), createStateDynamic("minecraft:standing_sign", ROTATION_0));
      putStates(1009, createStateDynamic("minecraft:sign", ROTATION_1), createStateDynamic("minecraft:standing_sign", ROTATION_1));
      putStates(1010, createStateDynamic("minecraft:sign", ROTATION_2), createStateDynamic("minecraft:standing_sign", ROTATION_2));
      putStates(1011, createStateDynamic("minecraft:sign", ROTATION_3), createStateDynamic("minecraft:standing_sign", ROTATION_3));
      putStates(1012, createStateDynamic("minecraft:sign", ROTATION_4), createStateDynamic("minecraft:standing_sign", ROTATION_4));
      putStates(1013, createStateDynamic("minecraft:sign", ROTATION_5), createStateDynamic("minecraft:standing_sign", ROTATION_5));
      putStates(1014, createStateDynamic("minecraft:sign", ROTATION_6), createStateDynamic("minecraft:standing_sign", ROTATION_6));
      putStates(1015, createStateDynamic("minecraft:sign", ROTATION_7), createStateDynamic("minecraft:standing_sign", ROTATION_7));
      putStates(1016, createStateDynamic("minecraft:sign", ROTATION_8), createStateDynamic("minecraft:standing_sign", ROTATION_8));
      putStates(1017, createStateDynamic("minecraft:sign", ROTATION_9), createStateDynamic("minecraft:standing_sign", ROTATION_9));
      putStates(1018, createStateDynamic("minecraft:sign", ROTATION_10), createStateDynamic("minecraft:standing_sign", ROTATION_10));
      putStates(1019, createStateDynamic("minecraft:sign", ROTATION_11), createStateDynamic("minecraft:standing_sign", ROTATION_11));
      putStates(1020, createStateDynamic("minecraft:sign", ROTATION_12), createStateDynamic("minecraft:standing_sign", ROTATION_12));
      putStates(1021, createStateDynamic("minecraft:sign", ROTATION_13), createStateDynamic("minecraft:standing_sign", ROTATION_13));
      putStates(1022, createStateDynamic("minecraft:sign", ROTATION_14), createStateDynamic("minecraft:standing_sign", ROTATION_14));
      putStates(1023, createStateDynamic("minecraft:sign", ROTATION_15), createStateDynamic("minecraft:standing_sign", ROTATION_15));
   }

   private static void putStatesFromBlocks64To79() {
      putStates(
         1024,
         createStateDynamic("minecraft:oak_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1025,
         createStateDynamic("minecraft:oak_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1026,
         createStateDynamic("minecraft:oak_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1027,
         createStateDynamic("minecraft:oak_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1028,
         createStateDynamic("minecraft:oak_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1029,
         createStateDynamic("minecraft:oak_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1030,
         createStateDynamic("minecraft:oak_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1031,
         createStateDynamic("minecraft:oak_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1032,
         createStateDynamic("minecraft:oak_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         1033,
         createStateDynamic("minecraft:oak_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         1034,
         createStateDynamic("minecraft:oak_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1035,
         createStateDynamic("minecraft:oak_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(1036, createStateDynamic("minecraft:oak_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE));
      putStates(1037, createStateDynamic("minecraft:oak_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE));
      putStates(1038, createStateDynamic("minecraft:oak_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE));
      putStates(1039, createStateDynamic("minecraft:oak_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE));
      putStates(1042, createStateDynamic("minecraft:ladder", FACING_NORTH), createStateDynamic("minecraft:ladder", FACING_NORTH));
      putStates(1043, createStateDynamic("minecraft:ladder", FACING_SOUTH), createStateDynamic("minecraft:ladder", FACING_SOUTH));
      putStates(1044, createStateDynamic("minecraft:ladder", FACING_WEST), createStateDynamic("minecraft:ladder", FACING_WEST));
      putStates(1045, createStateDynamic("minecraft:ladder", FACING_EAST), createStateDynamic("minecraft:ladder", FACING_EAST));
      putStates(
         1056, createStateDynamic("minecraft:rail", Map.of("shape", "north_south")), createStateDynamic("minecraft:rail", Map.of("shape", "north_south"))
      );
      putStates(1057, createStateDynamic("minecraft:rail", Map.of("shape", "east_west")), createStateDynamic("minecraft:rail", Map.of("shape", "east_west")));
      putStates(
         1058, createStateDynamic("minecraft:rail", Map.of("shape", "ascending_east")), createStateDynamic("minecraft:rail", Map.of("shape", "ascending_east"))
      );
      putStates(
         1059, createStateDynamic("minecraft:rail", Map.of("shape", "ascending_west")), createStateDynamic("minecraft:rail", Map.of("shape", "ascending_west"))
      );
      putStates(
         1060,
         createStateDynamic("minecraft:rail", Map.of("shape", "ascending_north")),
         createStateDynamic("minecraft:rail", Map.of("shape", "ascending_north"))
      );
      putStates(
         1061,
         createStateDynamic("minecraft:rail", Map.of("shape", "ascending_south")),
         createStateDynamic("minecraft:rail", Map.of("shape", "ascending_south"))
      );
      putStates(1062, createStateDynamic("minecraft:rail", Map.of("shape", "south_east")), createStateDynamic("minecraft:rail", Map.of("shape", "south_east")));
      putStates(1063, createStateDynamic("minecraft:rail", Map.of("shape", "south_west")), createStateDynamic("minecraft:rail", Map.of("shape", "south_west")));
      putStates(1064, createStateDynamic("minecraft:rail", Map.of("shape", "north_west")), createStateDynamic("minecraft:rail", Map.of("shape", "north_west")));
      putStates(1065, createStateDynamic("minecraft:rail", Map.of("shape", "north_east")), createStateDynamic("minecraft:rail", Map.of("shape", "north_east")));
      putStates(
         1072,
         createStateDynamic("minecraft:cobblestone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1073,
         createStateDynamic("minecraft:cobblestone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1074,
         createStateDynamic("minecraft:cobblestone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1075,
         createStateDynamic("minecraft:cobblestone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1076,
         createStateDynamic("minecraft:cobblestone_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1077,
         createStateDynamic("minecraft:cobblestone_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1078,
         createStateDynamic("minecraft:cobblestone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1079,
         createStateDynamic("minecraft:cobblestone_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(1090, createStateDynamic("minecraft:wall_sign", FACING_NORTH), createStateDynamic("minecraft:wall_sign", FACING_NORTH));
      putStates(1091, createStateDynamic("minecraft:wall_sign", FACING_SOUTH), createStateDynamic("minecraft:wall_sign", FACING_SOUTH));
      putStates(1092, createStateDynamic("minecraft:wall_sign", FACING_WEST), createStateDynamic("minecraft:wall_sign", FACING_WEST));
      putStates(1093, createStateDynamic("minecraft:wall_sign", FACING_EAST), createStateDynamic("minecraft:wall_sign", FACING_EAST));
      putStates(
         1104,
         createStateDynamic("minecraft:lever", Map.of("face", "ceiling", "facing", "west", "powered", "false")),
         createStateDynamic("minecraft:lever", Map.of("facing", "down_x", "powered", "false"))
      );
      putStates(
         1105, createStateDynamic("minecraft:lever", FACE_WALL_FACING_EAST_POWERED_FALSE), createStateDynamic("minecraft:lever", FACING_EAST_POWERED_FALSE)
      );
      putStates(
         1106, createStateDynamic("minecraft:lever", FACE_WALL_FACING_WEST_POWERED_FALSE), createStateDynamic("minecraft:lever", FACING_WEST_POWERED_FALSE)
      );
      putStates(
         1107, createStateDynamic("minecraft:lever", FACE_WALL_FACING_SOUTH_POWERED_FALSE), createStateDynamic("minecraft:lever", FACING_SOUTH_POWERED_FALSE)
      );
      putStates(
         1108, createStateDynamic("minecraft:lever", FACE_WALL_FACING_NORTH_POWERED_FALSE), createStateDynamic("minecraft:lever", FACING_NORTH_POWERED_FALSE)
      );
      putStates(
         1109,
         createStateDynamic("minecraft:lever", FACE_FLOOR_FACING_NORTH_POWERED_FALSE),
         createStateDynamic("minecraft:lever", Map.of("facing", "up_z", "powered", "false"))
      );
      putStates(
         1110,
         createStateDynamic("minecraft:lever", Map.of("face", "floor", "facing", "west", "powered", "false")),
         createStateDynamic("minecraft:lever", Map.of("facing", "up_x", "powered", "false"))
      );
      putStates(
         1111,
         createStateDynamic("minecraft:lever", FACE_CEILING_FACING_NORTH_POWERED_FALSE),
         createStateDynamic("minecraft:lever", Map.of("facing", "down_z", "powered", "false"))
      );
      putStates(
         1112,
         createStateDynamic("minecraft:lever", Map.of("face", "ceiling", "facing", "west", "powered", "true")),
         createStateDynamic("minecraft:lever", Map.of("facing", "down_x", "powered", "true"))
      );
      putStates(
         1113, createStateDynamic("minecraft:lever", FACE_WALL_FACING_EAST_POWERED_TRUE), createStateDynamic("minecraft:lever", FACING_EAST_POWERED_TRUE)
      );
      putStates(
         1114, createStateDynamic("minecraft:lever", FACE_WALL_FACING_WEST_POWERED_TRUE), createStateDynamic("minecraft:lever", FACING_WEST_POWERED_TRUE)
      );
      putStates(
         1115, createStateDynamic("minecraft:lever", FACE_WALL_FACING_SOUTH_POWERED_TRUE), createStateDynamic("minecraft:lever", FACING_SOUTH_POWERED_TRUE)
      );
      putStates(
         1116, createStateDynamic("minecraft:lever", FACE_WALL_FACING_NORTH_POWERED_TRUE), createStateDynamic("minecraft:lever", FACING_NORTH_POWERED_TRUE)
      );
      putStates(
         1117,
         createStateDynamic("minecraft:lever", FACE_FLOOR_FACING_NORTH_POWERED_TRUE),
         createStateDynamic("minecraft:lever", Map.of("facing", "up_z", "powered", "true"))
      );
      putStates(
         1118,
         createStateDynamic("minecraft:lever", Map.of("face", "floor", "facing", "west", "powered", "true")),
         createStateDynamic("minecraft:lever", Map.of("facing", "up_x", "powered", "true"))
      );
      putStates(
         1119,
         createStateDynamic("minecraft:lever", FACE_CEILING_FACING_NORTH_POWERED_TRUE),
         createStateDynamic("minecraft:lever", Map.of("facing", "down_z", "powered", "true"))
      );
      putStates(1120, createStateDynamic("minecraft:stone_pressure_plate", POWERED_FALSE), createStateDynamic("minecraft:stone_pressure_plate", POWERED_FALSE));
      putStates(1121, createStateDynamic("minecraft:stone_pressure_plate", POWERED_TRUE), createStateDynamic("minecraft:stone_pressure_plate", POWERED_TRUE));
      putStates(
         1136,
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1137,
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1138,
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1139,
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1140,
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1141,
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1142,
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1143,
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1144,
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         1145,
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         1146,
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1147,
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(1148, createStateDynamic("minecraft:iron_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE));
      putStates(1149, createStateDynamic("minecraft:iron_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE));
      putStates(1150, createStateDynamic("minecraft:iron_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE));
      putStates(1151, createStateDynamic("minecraft:iron_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE));
      putStates(1152, createStateDynamic("minecraft:oak_pressure_plate", POWERED_FALSE), createStateDynamic("minecraft:wooden_pressure_plate", POWERED_FALSE));
      putStates(1153, createStateDynamic("minecraft:oak_pressure_plate", POWERED_TRUE), createStateDynamic("minecraft:wooden_pressure_plate", POWERED_TRUE));
      putStates(1168, createStateDynamic("minecraft:redstone_ore", LIT_FALSE), createStateDynamic("minecraft:redstone_ore"));
      putStates(1184, createStateDynamic("minecraft:redstone_ore", LIT_TRUE), createStateDynamic("minecraft:lit_redstone_ore"));
      putStates(
         1201,
         createStateDynamic("minecraft:redstone_wall_torch", Map.of("facing", "east", "lit", "false")),
         createStateDynamic("minecraft:unlit_redstone_torch", FACING_EAST)
      );
      putStates(
         1202,
         createStateDynamic("minecraft:redstone_wall_torch", Map.of("facing", "west", "lit", "false")),
         createStateDynamic("minecraft:unlit_redstone_torch", FACING_WEST)
      );
      putStates(
         1203,
         createStateDynamic("minecraft:redstone_wall_torch", Map.of("facing", "south", "lit", "false")),
         createStateDynamic("minecraft:unlit_redstone_torch", FACING_SOUTH)
      );
      putStates(
         1204,
         createStateDynamic("minecraft:redstone_wall_torch", Map.of("facing", "north", "lit", "false")),
         createStateDynamic("minecraft:unlit_redstone_torch", FACING_NORTH)
      );
      putStates(1205, createStateDynamic("minecraft:redstone_torch", LIT_FALSE), createStateDynamic("minecraft:unlit_redstone_torch", FACING_UP));
      putStates(
         1217,
         createStateDynamic("minecraft:redstone_wall_torch", Map.of("facing", "east", "lit", "true")),
         createStateDynamic("minecraft:redstone_torch", FACING_EAST)
      );
      putStates(
         1218,
         createStateDynamic("minecraft:redstone_wall_torch", Map.of("facing", "west", "lit", "true")),
         createStateDynamic("minecraft:redstone_torch", FACING_WEST)
      );
      putStates(
         1219,
         createStateDynamic("minecraft:redstone_wall_torch", Map.of("facing", "south", "lit", "true")),
         createStateDynamic("minecraft:redstone_torch", FACING_SOUTH)
      );
      putStates(
         1220,
         createStateDynamic("minecraft:redstone_wall_torch", Map.of("facing", "north", "lit", "true")),
         createStateDynamic("minecraft:redstone_torch", FACING_NORTH)
      );
      putStates(1221, createStateDynamic("minecraft:redstone_torch", LIT_TRUE), createStateDynamic("minecraft:redstone_torch", FACING_UP));
      putStates(
         1232,
         createStateDynamic("minecraft:stone_button", FACE_CEILING_FACING_NORTH_POWERED_FALSE),
         createStateDynamic("minecraft:stone_button", FACING_DOWN_POWERED_FALSE)
      );
      putStates(
         1233,
         createStateDynamic("minecraft:stone_button", FACE_WALL_FACING_EAST_POWERED_FALSE),
         createStateDynamic("minecraft:stone_button", FACING_EAST_POWERED_FALSE)
      );
      putStates(
         1234,
         createStateDynamic("minecraft:stone_button", FACE_WALL_FACING_WEST_POWERED_FALSE),
         createStateDynamic("minecraft:stone_button", FACING_WEST_POWERED_FALSE)
      );
      putStates(
         1235,
         createStateDynamic("minecraft:stone_button", FACE_WALL_FACING_SOUTH_POWERED_FALSE),
         createStateDynamic("minecraft:stone_button", FACING_SOUTH_POWERED_FALSE)
      );
      putStates(
         1236,
         createStateDynamic("minecraft:stone_button", FACE_WALL_FACING_NORTH_POWERED_FALSE),
         createStateDynamic("minecraft:stone_button", FACING_NORTH_POWERED_FALSE)
      );
      putStates(
         1237,
         createStateDynamic("minecraft:stone_button", FACE_FLOOR_FACING_NORTH_POWERED_FALSE),
         createStateDynamic("minecraft:stone_button", FACING_UP_POWERED_FALSE)
      );
      putStates(
         1240,
         createStateDynamic("minecraft:stone_button", FACE_CEILING_FACING_NORTH_POWERED_TRUE),
         createStateDynamic("minecraft:stone_button", FACING_DOWN_POWERED_TRUE)
      );
      putStates(
         1241,
         createStateDynamic("minecraft:stone_button", FACE_WALL_FACING_EAST_POWERED_TRUE),
         createStateDynamic("minecraft:stone_button", FACING_EAST_POWERED_TRUE)
      );
      putStates(
         1242,
         createStateDynamic("minecraft:stone_button", FACE_WALL_FACING_WEST_POWERED_TRUE),
         createStateDynamic("minecraft:stone_button", FACING_WEST_POWERED_TRUE)
      );
      putStates(
         1243,
         createStateDynamic("minecraft:stone_button", FACE_WALL_FACING_SOUTH_POWERED_TRUE),
         createStateDynamic("minecraft:stone_button", FACING_SOUTH_POWERED_TRUE)
      );
      putStates(
         1244,
         createStateDynamic("minecraft:stone_button", FACE_WALL_FACING_NORTH_POWERED_TRUE),
         createStateDynamic("minecraft:stone_button", FACING_NORTH_POWERED_TRUE)
      );
      putStates(
         1245,
         createStateDynamic("minecraft:stone_button", FACE_FLOOR_FACING_NORTH_POWERED_TRUE),
         createStateDynamic("minecraft:stone_button", FACING_UP_POWERED_TRUE)
      );
      putStates(1248, createStateDynamic("minecraft:snow", Map.of("layers", "1")), createStateDynamic("minecraft:snow_layer", Map.of("layers", "1")));
      putStates(1249, createStateDynamic("minecraft:snow", Map.of("layers", "2")), createStateDynamic("minecraft:snow_layer", Map.of("layers", "2")));
      putStates(1250, createStateDynamic("minecraft:snow", Map.of("layers", "3")), createStateDynamic("minecraft:snow_layer", Map.of("layers", "3")));
      putStates(1251, createStateDynamic("minecraft:snow", Map.of("layers", "4")), createStateDynamic("minecraft:snow_layer", Map.of("layers", "4")));
      putStates(1252, createStateDynamic("minecraft:snow", Map.of("layers", "5")), createStateDynamic("minecraft:snow_layer", Map.of("layers", "5")));
      putStates(1253, createStateDynamic("minecraft:snow", Map.of("layers", "6")), createStateDynamic("minecraft:snow_layer", Map.of("layers", "6")));
      putStates(1254, createStateDynamic("minecraft:snow", Map.of("layers", "7")), createStateDynamic("minecraft:snow_layer", Map.of("layers", "7")));
      putStates(1255, createStateDynamic("minecraft:snow", Map.of("layers", "8")), createStateDynamic("minecraft:snow_layer", Map.of("layers", "8")));
      putStates(1264, createStateDynamic("minecraft:ice"), createStateDynamic("minecraft:ice"));
   }

   private static void putStatesFromBlocks80To95() {
      putStates(1280, createStateDynamic("minecraft:snow_block"), createStateDynamic("minecraft:snow"));
      putStates(1296, createStateDynamic("minecraft:cactus", AGE_0), createStateDynamic("minecraft:cactus", AGE_0));
      putStates(1297, createStateDynamic("minecraft:cactus", AGE_1), createStateDynamic("minecraft:cactus", AGE_1));
      putStates(1298, createStateDynamic("minecraft:cactus", AGE_2), createStateDynamic("minecraft:cactus", AGE_2));
      putStates(1299, createStateDynamic("minecraft:cactus", AGE_3), createStateDynamic("minecraft:cactus", AGE_3));
      putStates(1300, createStateDynamic("minecraft:cactus", AGE_4), createStateDynamic("minecraft:cactus", AGE_4));
      putStates(1301, createStateDynamic("minecraft:cactus", AGE_5), createStateDynamic("minecraft:cactus", AGE_5));
      putStates(1302, createStateDynamic("minecraft:cactus", AGE_6), createStateDynamic("minecraft:cactus", AGE_6));
      putStates(1303, createStateDynamic("minecraft:cactus", AGE_7), createStateDynamic("minecraft:cactus", AGE_7));
      putStates(1304, createStateDynamic("minecraft:cactus", AGE_8), createStateDynamic("minecraft:cactus", AGE_8));
      putStates(1305, createStateDynamic("minecraft:cactus", AGE_9), createStateDynamic("minecraft:cactus", AGE_9));
      putStates(1306, createStateDynamic("minecraft:cactus", AGE_10), createStateDynamic("minecraft:cactus", AGE_10));
      putStates(1307, createStateDynamic("minecraft:cactus", AGE_11), createStateDynamic("minecraft:cactus", AGE_11));
      putStates(1308, createStateDynamic("minecraft:cactus", AGE_12), createStateDynamic("minecraft:cactus", AGE_12));
      putStates(1309, createStateDynamic("minecraft:cactus", AGE_13), createStateDynamic("minecraft:cactus", AGE_13));
      putStates(1310, createStateDynamic("minecraft:cactus", AGE_14), createStateDynamic("minecraft:cactus", AGE_14));
      putStates(1311, createStateDynamic("minecraft:cactus", AGE_15), createStateDynamic("minecraft:cactus", AGE_15));
      putStates(1312, createStateDynamic("minecraft:clay"), createStateDynamic("minecraft:clay"));
      putStates(1328, createStateDynamic("minecraft:sugar_cane", AGE_0), createStateDynamic("minecraft:reeds", AGE_0));
      putStates(1329, createStateDynamic("minecraft:sugar_cane", AGE_1), createStateDynamic("minecraft:reeds", AGE_1));
      putStates(1330, createStateDynamic("minecraft:sugar_cane", AGE_2), createStateDynamic("minecraft:reeds", AGE_2));
      putStates(1331, createStateDynamic("minecraft:sugar_cane", AGE_3), createStateDynamic("minecraft:reeds", AGE_3));
      putStates(1332, createStateDynamic("minecraft:sugar_cane", AGE_4), createStateDynamic("minecraft:reeds", AGE_4));
      putStates(1333, createStateDynamic("minecraft:sugar_cane", AGE_5), createStateDynamic("minecraft:reeds", AGE_5));
      putStates(1334, createStateDynamic("minecraft:sugar_cane", AGE_6), createStateDynamic("minecraft:reeds", AGE_6));
      putStates(1335, createStateDynamic("minecraft:sugar_cane", AGE_7), createStateDynamic("minecraft:reeds", AGE_7));
      putStates(1336, createStateDynamic("minecraft:sugar_cane", AGE_8), createStateDynamic("minecraft:reeds", AGE_8));
      putStates(1337, createStateDynamic("minecraft:sugar_cane", AGE_9), createStateDynamic("minecraft:reeds", AGE_9));
      putStates(1338, createStateDynamic("minecraft:sugar_cane", AGE_10), createStateDynamic("minecraft:reeds", AGE_10));
      putStates(1339, createStateDynamic("minecraft:sugar_cane", AGE_11), createStateDynamic("minecraft:reeds", AGE_11));
      putStates(1340, createStateDynamic("minecraft:sugar_cane", AGE_12), createStateDynamic("minecraft:reeds", AGE_12));
      putStates(1341, createStateDynamic("minecraft:sugar_cane", AGE_13), createStateDynamic("minecraft:reeds", AGE_13));
      putStates(1342, createStateDynamic("minecraft:sugar_cane", AGE_14), createStateDynamic("minecraft:reeds", AGE_14));
      putStates(1343, createStateDynamic("minecraft:sugar_cane", AGE_15), createStateDynamic("minecraft:reeds", AGE_15));
      putStates(
         1344, createStateDynamic("minecraft:jukebox", Map.of("has_record", "false")), createStateDynamic("minecraft:jukebox", Map.of("has_record", "false"))
      );
      putStates(
         1345, createStateDynamic("minecraft:jukebox", Map.of("has_record", "true")), createStateDynamic("minecraft:jukebox", Map.of("has_record", "true"))
      );
      putStates(
         1360,
         createStateDynamic("minecraft:oak_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE)
      );
      putStates(1376, createStateDynamic("minecraft:carved_pumpkin", FACING_SOUTH), createStateDynamic("minecraft:pumpkin", FACING_SOUTH));
      putStates(1377, createStateDynamic("minecraft:carved_pumpkin", FACING_WEST), createStateDynamic("minecraft:pumpkin", FACING_WEST));
      putStates(1378, createStateDynamic("minecraft:carved_pumpkin", FACING_NORTH), createStateDynamic("minecraft:pumpkin", FACING_NORTH));
      putStates(1379, createStateDynamic("minecraft:carved_pumpkin", FACING_EAST), createStateDynamic("minecraft:pumpkin", FACING_EAST));
      putStates(1392, createStateDynamic("minecraft:netherrack"), createStateDynamic("minecraft:netherrack"));
      putStates(1408, createStateDynamic("minecraft:soul_sand"), createStateDynamic("minecraft:soul_sand"));
      putStates(1424, createStateDynamic("minecraft:glowstone"), createStateDynamic("minecraft:glowstone"));
      putStates(1441, createStateDynamic("minecraft:portal", AXIS_X), createStateDynamic("minecraft:portal", AXIS_X));
      putStates(1442, createStateDynamic("minecraft:portal", AXIS_Z), createStateDynamic("minecraft:portal", AXIS_Z));
      putStates(1456, createStateDynamic("minecraft:jack_o_lantern", FACING_SOUTH), createStateDynamic("minecraft:lit_pumpkin", FACING_SOUTH));
      putStates(1457, createStateDynamic("minecraft:jack_o_lantern", FACING_WEST), createStateDynamic("minecraft:lit_pumpkin", FACING_WEST));
      putStates(1458, createStateDynamic("minecraft:jack_o_lantern", FACING_NORTH), createStateDynamic("minecraft:lit_pumpkin", FACING_NORTH));
      putStates(1459, createStateDynamic("minecraft:jack_o_lantern", FACING_EAST), createStateDynamic("minecraft:lit_pumpkin", FACING_EAST));
      putStates(1472, createStateDynamic("minecraft:cake", Map.of("bites", "0")), createStateDynamic("minecraft:cake", Map.of("bites", "0")));
      putStates(1473, createStateDynamic("minecraft:cake", Map.of("bites", "1")), createStateDynamic("minecraft:cake", Map.of("bites", "1")));
      putStates(1474, createStateDynamic("minecraft:cake", Map.of("bites", "2")), createStateDynamic("minecraft:cake", Map.of("bites", "2")));
      putStates(1475, createStateDynamic("minecraft:cake", Map.of("bites", "3")), createStateDynamic("minecraft:cake", Map.of("bites", "3")));
      putStates(1476, createStateDynamic("minecraft:cake", Map.of("bites", "4")), createStateDynamic("minecraft:cake", Map.of("bites", "4")));
      putStates(1477, createStateDynamic("minecraft:cake", Map.of("bites", "5")), createStateDynamic("minecraft:cake", Map.of("bites", "5")));
      putStates(1478, createStateDynamic("minecraft:cake", Map.of("bites", "6")), createStateDynamic("minecraft:cake", Map.of("bites", "6")));
      putStates(
         1488,
         createStateDynamic("minecraft:repeater", Map.of("delay", "1", "facing", "south", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "south", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "south", "locked", "true"))
      );
      putStates(
         1489,
         createStateDynamic("minecraft:repeater", Map.of("delay", "1", "facing", "west", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "west", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "west", "locked", "true"))
      );
      putStates(
         1490,
         createStateDynamic("minecraft:repeater", Map.of("delay", "1", "facing", "north", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "north", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "north", "locked", "true"))
      );
      putStates(
         1491,
         createStateDynamic("minecraft:repeater", Map.of("delay", "1", "facing", "east", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "east", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "east", "locked", "true"))
      );
      putStates(
         1492,
         createStateDynamic("minecraft:repeater", Map.of("delay", "2", "facing", "south", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "south", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "south", "locked", "true"))
      );
      putStates(
         1493,
         createStateDynamic("minecraft:repeater", Map.of("delay", "2", "facing", "west", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "west", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "west", "locked", "true"))
      );
      putStates(
         1494,
         createStateDynamic("minecraft:repeater", Map.of("delay", "2", "facing", "north", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "north", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "north", "locked", "true"))
      );
      putStates(
         1495,
         createStateDynamic("minecraft:repeater", Map.of("delay", "2", "facing", "east", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "east", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "east", "locked", "true"))
      );
      putStates(
         1496,
         createStateDynamic("minecraft:repeater", Map.of("delay", "3", "facing", "south", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "south", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "south", "locked", "true"))
      );
      putStates(
         1497,
         createStateDynamic("minecraft:repeater", Map.of("delay", "3", "facing", "west", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "west", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "west", "locked", "true"))
      );
      putStates(
         1498,
         createStateDynamic("minecraft:repeater", Map.of("delay", "3", "facing", "north", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "north", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "north", "locked", "true"))
      );
      putStates(
         1499,
         createStateDynamic("minecraft:repeater", Map.of("delay", "3", "facing", "east", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "east", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "east", "locked", "true"))
      );
      putStates(
         1500,
         createStateDynamic("minecraft:repeater", Map.of("delay", "4", "facing", "south", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "south", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "south", "locked", "true"))
      );
      putStates(
         1501,
         createStateDynamic("minecraft:repeater", Map.of("delay", "4", "facing", "west", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "west", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "west", "locked", "true"))
      );
      putStates(
         1502,
         createStateDynamic("minecraft:repeater", Map.of("delay", "4", "facing", "north", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "north", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "north", "locked", "true"))
      );
      putStates(
         1503,
         createStateDynamic("minecraft:repeater", Map.of("delay", "4", "facing", "east", "locked", "false", "powered", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "east", "locked", "false")),
         createStateDynamic("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "east", "locked", "true"))
      );
      putStates(
         1504,
         createStateDynamic("minecraft:repeater", Map.of("delay", "1", "facing", "south", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "1", "facing", "south", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "1", "facing", "south", "locked", "true"))
      );
      putStates(
         1505,
         createStateDynamic("minecraft:repeater", Map.of("delay", "1", "facing", "west", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "1", "facing", "west", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "1", "facing", "west", "locked", "true"))
      );
      putStates(
         1506,
         createStateDynamic("minecraft:repeater", Map.of("delay", "1", "facing", "north", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "1", "facing", "north", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "1", "facing", "north", "locked", "true"))
      );
      putStates(
         1507,
         createStateDynamic("minecraft:repeater", Map.of("delay", "1", "facing", "east", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "1", "facing", "east", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "1", "facing", "east", "locked", "true"))
      );
      putStates(
         1508,
         createStateDynamic("minecraft:repeater", Map.of("delay", "2", "facing", "south", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "2", "facing", "south", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "2", "facing", "south", "locked", "true"))
      );
      putStates(
         1509,
         createStateDynamic("minecraft:repeater", Map.of("delay", "2", "facing", "west", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "2", "facing", "west", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "2", "facing", "west", "locked", "true"))
      );
      putStates(
         1510,
         createStateDynamic("minecraft:repeater", Map.of("delay", "2", "facing", "north", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "2", "facing", "north", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "2", "facing", "north", "locked", "true"))
      );
      putStates(
         1511,
         createStateDynamic("minecraft:repeater", Map.of("delay", "2", "facing", "east", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "2", "facing", "east", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "2", "facing", "east", "locked", "true"))
      );
      putStates(
         1512,
         createStateDynamic("minecraft:repeater", Map.of("delay", "3", "facing", "south", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "3", "facing", "south", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "3", "facing", "south", "locked", "true"))
      );
      putStates(
         1513,
         createStateDynamic("minecraft:repeater", Map.of("delay", "3", "facing", "west", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "3", "facing", "west", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "3", "facing", "west", "locked", "true"))
      );
      putStates(
         1514,
         createStateDynamic("minecraft:repeater", Map.of("delay", "3", "facing", "north", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "3", "facing", "north", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "3", "facing", "north", "locked", "true"))
      );
      putStates(
         1515,
         createStateDynamic("minecraft:repeater", Map.of("delay", "3", "facing", "east", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "3", "facing", "east", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "3", "facing", "east", "locked", "true"))
      );
      putStates(
         1516,
         createStateDynamic("minecraft:repeater", Map.of("delay", "4", "facing", "south", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "4", "facing", "south", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "4", "facing", "south", "locked", "true"))
      );
      putStates(
         1517,
         createStateDynamic("minecraft:repeater", Map.of("delay", "4", "facing", "west", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "4", "facing", "west", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "4", "facing", "west", "locked", "true"))
      );
      putStates(
         1518,
         createStateDynamic("minecraft:repeater", Map.of("delay", "4", "facing", "north", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "4", "facing", "north", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "4", "facing", "north", "locked", "true"))
      );
      putStates(
         1519,
         createStateDynamic("minecraft:repeater", Map.of("delay", "4", "facing", "east", "locked", "false", "powered", "true")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "4", "facing", "east", "locked", "false")),
         createStateDynamic("minecraft:powered_repeater", Map.of("delay", "4", "facing", "east", "locked", "true"))
      );
      putStates(1520, createStateDynamic("minecraft:white_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_WHITE));
      putStates(1521, createStateDynamic("minecraft:orange_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_ORANGE));
      putStates(1522, createStateDynamic("minecraft:magenta_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_MAGENTA));
      putStates(1523, createStateDynamic("minecraft:light_blue_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_LIGHT_BLUE));
      putStates(1524, createStateDynamic("minecraft:yellow_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_YELLOW));
      putStates(1525, createStateDynamic("minecraft:lime_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_LIME));
      putStates(1526, createStateDynamic("minecraft:pink_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_PINK));
      putStates(1527, createStateDynamic("minecraft:gray_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_GRAY));
      putStates(1528, createStateDynamic("minecraft:light_gray_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_SILVER));
      putStates(1529, createStateDynamic("minecraft:cyan_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_CYAN));
      putStates(1530, createStateDynamic("minecraft:purple_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_PURPLE));
      putStates(1531, createStateDynamic("minecraft:blue_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_BLUE));
      putStates(1532, createStateDynamic("minecraft:brown_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_BROWN));
      putStates(1533, createStateDynamic("minecraft:green_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_GREEN));
      putStates(1534, createStateDynamic("minecraft:red_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_RED));
      putStates(1535, createStateDynamic("minecraft:black_stained_glass"), createStateDynamic("minecraft:stained_glass", COLOR_BLACK));
   }

   private static void putStatesFromBlocks96To111() {
      putStates(
         1536,
         createStateDynamic("minecraft:oak_trapdoor", FACING_NORTH_HALF_BOTTOM_OPEN_FALSE),
         createStateDynamic("minecraft:trapdoor", FACING_NORTH_HALF_BOTTOM_OPEN_FALSE)
      );
      putStates(
         1537,
         createStateDynamic("minecraft:oak_trapdoor", FACING_SOUTH_HALF_BOTTOM_OPEN_FALSE),
         createStateDynamic("minecraft:trapdoor", FACING_SOUTH_HALF_BOTTOM_OPEN_FALSE)
      );
      putStates(
         1538,
         createStateDynamic("minecraft:oak_trapdoor", FACING_WEST_HALF_BOTTOM_OPEN_FALSE),
         createStateDynamic("minecraft:trapdoor", FACING_WEST_HALF_BOTTOM_OPEN_FALSE)
      );
      putStates(
         1539,
         createStateDynamic("minecraft:oak_trapdoor", FACING_EAST_HALF_BOTTOM_OPEN_FALSE),
         createStateDynamic("minecraft:trapdoor", FACING_EAST_HALF_BOTTOM_OPEN_FALSE)
      );
      putStates(
         1540,
         createStateDynamic("minecraft:oak_trapdoor", FACING_NORTH_HALF_BOTTOM_OPEN_TRUE),
         createStateDynamic("minecraft:trapdoor", FACING_NORTH_HALF_BOTTOM_OPEN_TRUE)
      );
      putStates(
         1541,
         createStateDynamic("minecraft:oak_trapdoor", FACING_SOUTH_HALF_BOTTOM_OPEN_TRUE),
         createStateDynamic("minecraft:trapdoor", FACING_SOUTH_HALF_BOTTOM_OPEN_TRUE)
      );
      putStates(
         1542,
         createStateDynamic("minecraft:oak_trapdoor", FACING_WEST_HALF_BOTTOM_OPEN_TRUE),
         createStateDynamic("minecraft:trapdoor", FACING_WEST_HALF_BOTTOM_OPEN_TRUE)
      );
      putStates(
         1543,
         createStateDynamic("minecraft:oak_trapdoor", FACING_EAST_HALF_BOTTOM_OPEN_TRUE),
         createStateDynamic("minecraft:trapdoor", FACING_EAST_HALF_BOTTOM_OPEN_TRUE)
      );
      putStates(
         1544,
         createStateDynamic("minecraft:oak_trapdoor", FACING_NORTH_HALF_TOP_OPEN_FALSE),
         createStateDynamic("minecraft:trapdoor", FACING_NORTH_HALF_TOP_OPEN_FALSE)
      );
      putStates(
         1545,
         createStateDynamic("minecraft:oak_trapdoor", FACING_SOUTH_HALF_TOP_OPEN_FALSE),
         createStateDynamic("minecraft:trapdoor", FACING_SOUTH_HALF_TOP_OPEN_FALSE)
      );
      putStates(
         1546,
         createStateDynamic("minecraft:oak_trapdoor", FACING_WEST_HALF_TOP_OPEN_FALSE),
         createStateDynamic("minecraft:trapdoor", FACING_WEST_HALF_TOP_OPEN_FALSE)
      );
      putStates(
         1547,
         createStateDynamic("minecraft:oak_trapdoor", FACING_EAST_HALF_TOP_OPEN_FALSE),
         createStateDynamic("minecraft:trapdoor", FACING_EAST_HALF_TOP_OPEN_FALSE)
      );
      putStates(
         1548,
         createStateDynamic("minecraft:oak_trapdoor", FACING_NORTH_HALF_TOP_OPEN_TRUE),
         createStateDynamic("minecraft:trapdoor", FACING_NORTH_HALF_TOP_OPEN_TRUE)
      );
      putStates(
         1549,
         createStateDynamic("minecraft:oak_trapdoor", FACING_SOUTH_HALF_TOP_OPEN_TRUE),
         createStateDynamic("minecraft:trapdoor", FACING_SOUTH_HALF_TOP_OPEN_TRUE)
      );
      putStates(
         1550,
         createStateDynamic("minecraft:oak_trapdoor", FACING_WEST_HALF_TOP_OPEN_TRUE),
         createStateDynamic("minecraft:trapdoor", FACING_WEST_HALF_TOP_OPEN_TRUE)
      );
      putStates(
         1551,
         createStateDynamic("minecraft:oak_trapdoor", FACING_EAST_HALF_TOP_OPEN_TRUE),
         createStateDynamic("minecraft:trapdoor", FACING_EAST_HALF_TOP_OPEN_TRUE)
      );
      putStates(1552, createStateDynamic("minecraft:infested_stone"), createStateDynamic("minecraft:monster_egg", Map.of("variant", "stone")));
      putStates(1553, createStateDynamic("minecraft:infested_cobblestone"), createStateDynamic("minecraft:monster_egg", Map.of("variant", "cobblestone")));
      putStates(1554, createStateDynamic("minecraft:infested_stone_bricks"), createStateDynamic("minecraft:monster_egg", Map.of("variant", "stone_brick")));
      putStates(
         1555, createStateDynamic("minecraft:infested_mossy_stone_bricks"), createStateDynamic("minecraft:monster_egg", Map.of("variant", "mossy_brick"))
      );
      putStates(
         1556, createStateDynamic("minecraft:infested_cracked_stone_bricks"), createStateDynamic("minecraft:monster_egg", Map.of("variant", "cracked_brick"))
      );
      putStates(
         1557, createStateDynamic("minecraft:infested_chiseled_stone_bricks"), createStateDynamic("minecraft:monster_egg", Map.of("variant", "chiseled_brick"))
      );
      putStates(1568, createStateDynamic("minecraft:stone_bricks"), createStateDynamic("minecraft:stonebrick", Map.of("variant", "stonebrick")));
      putStates(1569, createStateDynamic("minecraft:mossy_stone_bricks"), createStateDynamic("minecraft:stonebrick", Map.of("variant", "mossy_stonebrick")));
      putStates(1570, createStateDynamic("minecraft:cracked_stone_bricks"), createStateDynamic("minecraft:stonebrick", Map.of("variant", "cracked_stonebrick")));
      putStates(
         1571, createStateDynamic("minecraft:chiseled_stone_bricks"), createStateDynamic("minecraft:stonebrick", Map.of("variant", "chiseled_stonebrick"))
      );
      putStates(
         1584,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "all_inside"))
      );
      putStates(
         1585,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "north_west"))
      );
      putStates(
         1586,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "north"))
      );
      putStates(
         1587,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "north_east"))
      );
      putStates(
         1588,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "west"))
      );
      putStates(
         1589,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "center"))
      );
      putStates(
         1590,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "east"))
      );
      putStates(
         1591,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "south_west"))
      );
      putStates(
         1592,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "south"))
      );
      putStates(
         1593,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "south_east"))
      );
      putStates(
         1594,
         createStateDynamic("minecraft:mushroom_stem", DOWN_FALSE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "stem"))
      );
      putStates(1595, createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE));
      putStates(1596, createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE));
      putStates(1597, createStateDynamic("minecraft:brown_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE));
      putStates(
         1598,
         createStateDynamic("minecraft:brown_mushroom_block", DOWN_TRUE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "all_outside"))
      );
      putStates(
         1599,
         createStateDynamic("minecraft:mushroom_stem", DOWN_TRUE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:brown_mushroom_block", Map.of("variant", "all_stem"))
      );
      putStates(
         1600,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "all_inside"))
      );
      putStates(
         1601,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "north_west"))
      );
      putStates(
         1602,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "north"))
      );
      putStates(
         1603,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "north_east"))
      );
      putStates(
         1604,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "west"))
      );
      putStates(
         1605,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "center"))
      );
      putStates(
         1606,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "east"))
      );
      putStates(
         1607,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "south_west"))
      );
      putStates(
         1608,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "south"))
      );
      putStates(
         1609,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "south_east"))
      );
      putStates(
         1610,
         createStateDynamic("minecraft:mushroom_stem", DOWN_FALSE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "stem"))
      );
      putStates(1611, createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE));
      putStates(1612, createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE));
      putStates(1613, createStateDynamic("minecraft:red_mushroom_block", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE));
      putStates(
         1614,
         createStateDynamic("minecraft:red_mushroom_block", DOWN_TRUE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "all_outside"))
      );
      putStates(
         1615,
         createStateDynamic("minecraft:mushroom_stem", DOWN_TRUE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:red_mushroom_block", Map.of("variant", "all_stem"))
      );
      putStates(
         1616,
         createStateDynamic("minecraft:iron_bars", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:iron_bars", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:iron_bars", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:iron_bars", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:iron_bars", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:iron_bars", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:iron_bars", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:iron_bars", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:iron_bars", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:iron_bars", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:iron_bars", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:iron_bars", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:iron_bars", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:iron_bars", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:iron_bars", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:iron_bars", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:iron_bars", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE)
      );
      putStates(
         1632,
         createStateDynamic("minecraft:glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:glass_pane", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:glass_pane", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:glass_pane", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:glass_pane", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:glass_pane", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:glass_pane", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:glass_pane", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:glass_pane", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:glass_pane", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:glass_pane", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:glass_pane", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:glass_pane", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE)
      );
      putStates(1648, createStateDynamic("minecraft:melon_block"), createStateDynamic("minecraft:melon_block"));
      putStates(
         1664,
         createStateDynamic("minecraft:pumpkin_stem", AGE_0),
         createStateDynamic("minecraft:pumpkin_stem", AGE_0_FACING_EAST),
         createStateDynamic("minecraft:pumpkin_stem", AGE_0_FACING_NORTH),
         createStateDynamic("minecraft:pumpkin_stem", AGE_0_FACING_SOUTH),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "0", "facing", "up")),
         createStateDynamic("minecraft:pumpkin_stem", AGE_0_FACING_WEST)
      );
      putStates(
         1665,
         createStateDynamic("minecraft:pumpkin_stem", AGE_1),
         createStateDynamic("minecraft:pumpkin_stem", AGE_1_FACING_EAST),
         createStateDynamic("minecraft:pumpkin_stem", AGE_1_FACING_NORTH),
         createStateDynamic("minecraft:pumpkin_stem", AGE_1_FACING_SOUTH),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "1", "facing", "up")),
         createStateDynamic("minecraft:pumpkin_stem", AGE_1_FACING_WEST)
      );
      putStates(
         1666,
         createStateDynamic("minecraft:pumpkin_stem", AGE_2),
         createStateDynamic("minecraft:pumpkin_stem", AGE_2_FACING_EAST),
         createStateDynamic("minecraft:pumpkin_stem", AGE_2_FACING_NORTH),
         createStateDynamic("minecraft:pumpkin_stem", AGE_2_FACING_SOUTH),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "2", "facing", "up")),
         createStateDynamic("minecraft:pumpkin_stem", AGE_2_FACING_WEST)
      );
      putStates(
         1667,
         createStateDynamic("minecraft:pumpkin_stem", AGE_3),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "east")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "north")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "south")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "up")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "west"))
      );
      putStates(
         1668,
         createStateDynamic("minecraft:pumpkin_stem", AGE_4),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "east")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "north")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "south")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "up")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "west"))
      );
      putStates(
         1669,
         createStateDynamic("minecraft:pumpkin_stem", AGE_5),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "east")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "north")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "south")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "up")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "west"))
      );
      putStates(
         1670,
         createStateDynamic("minecraft:pumpkin_stem", AGE_6),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "east")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "north")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "south")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "up")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "west"))
      );
      putStates(
         1671,
         createStateDynamic("minecraft:pumpkin_stem", AGE_7),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "east")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "north")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "south")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "up")),
         createStateDynamic("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "west"))
      );
      putStates(
         1680,
         createStateDynamic("minecraft:melon_stem", AGE_0),
         createStateDynamic("minecraft:melon_stem", AGE_0_FACING_EAST),
         createStateDynamic("minecraft:melon_stem", AGE_0_FACING_NORTH),
         createStateDynamic("minecraft:melon_stem", AGE_0_FACING_SOUTH),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "0", "facing", "up")),
         createStateDynamic("minecraft:melon_stem", AGE_0_FACING_WEST)
      );
      putStates(
         1681,
         createStateDynamic("minecraft:melon_stem", AGE_1),
         createStateDynamic("minecraft:melon_stem", AGE_1_FACING_EAST),
         createStateDynamic("minecraft:melon_stem", AGE_1_FACING_NORTH),
         createStateDynamic("minecraft:melon_stem", AGE_1_FACING_SOUTH),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "1", "facing", "up")),
         createStateDynamic("minecraft:melon_stem", AGE_1_FACING_WEST)
      );
      putStates(
         1682,
         createStateDynamic("minecraft:melon_stem", AGE_2),
         createStateDynamic("minecraft:melon_stem", AGE_2_FACING_EAST),
         createStateDynamic("minecraft:melon_stem", AGE_2_FACING_NORTH),
         createStateDynamic("minecraft:melon_stem", AGE_2_FACING_SOUTH),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "2", "facing", "up")),
         createStateDynamic("minecraft:melon_stem", AGE_2_FACING_WEST)
      );
      putStates(
         1683,
         createStateDynamic("minecraft:melon_stem", AGE_3),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "3", "facing", "east")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "3", "facing", "north")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "3", "facing", "south")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "3", "facing", "up")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "3", "facing", "west"))
      );
      putStates(
         1684,
         createStateDynamic("minecraft:melon_stem", AGE_4),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "4", "facing", "east")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "4", "facing", "north")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "4", "facing", "south")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "4", "facing", "up")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "4", "facing", "west"))
      );
      putStates(
         1685,
         createStateDynamic("minecraft:melon_stem", AGE_5),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "5", "facing", "east")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "5", "facing", "north")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "5", "facing", "south")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "5", "facing", "up")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "5", "facing", "west"))
      );
      putStates(
         1686,
         createStateDynamic("minecraft:melon_stem", AGE_6),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "6", "facing", "east")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "6", "facing", "north")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "6", "facing", "south")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "6", "facing", "up")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "6", "facing", "west"))
      );
      putStates(
         1687,
         createStateDynamic("minecraft:melon_stem", AGE_7),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "7", "facing", "east")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "7", "facing", "north")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "7", "facing", "south")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "7", "facing", "up")),
         createStateDynamic("minecraft:melon_stem", Map.of("age", "7", "facing", "west"))
      );
      putStates(
         1696,
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:vine", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "west", "false"))
      );
      putStates(
         1697,
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "west", "false"))
      );
      putStates(
         1698,
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "west", "true"))
      );
      putStates(
         1699,
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         1700,
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "west", "false"))
      );
      putStates(
         1701,
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "west", "false"))
      );
      putStates(
         1702,
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "west", "true"))
      );
      putStates(
         1703,
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         1704,
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "west", "false"))
      );
      putStates(
         1705,
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "west", "false"))
      );
      putStates(
         1706,
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "west", "true"))
      );
      putStates(
         1707,
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         1708,
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "west", "false"))
      );
      putStates(
         1709,
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "west", "false"))
      );
      putStates(
         1710,
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "west", "true"))
      );
      putStates(
         1711,
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      putStates(
         1712,
         createStateDynamic("minecraft:oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         1713,
         createStateDynamic("minecraft:oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         1714,
         createStateDynamic("minecraft:oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         1715,
         createStateDynamic("minecraft:oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         1716,
         createStateDynamic("minecraft:oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         1717,
         createStateDynamic("minecraft:oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         1718,
         createStateDynamic("minecraft:oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         1719,
         createStateDynamic("minecraft:oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         1720,
         createStateDynamic("minecraft:oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1721,
         createStateDynamic("minecraft:oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1722,
         createStateDynamic("minecraft:oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1723,
         createStateDynamic("minecraft:oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         1724,
         createStateDynamic("minecraft:oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1725,
         createStateDynamic("minecraft:oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1726,
         createStateDynamic("minecraft:oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1727,
         createStateDynamic("minecraft:oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         1728,
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1729,
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1730,
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1731,
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1732,
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1733,
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1734,
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1735,
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1744,
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1745,
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1746,
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1747,
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1748,
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1749,
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1750,
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1751,
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:stone_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1760,
         createStateDynamic("minecraft:mycelium", SNOWY_FALSE),
         createStateDynamic("minecraft:mycelium", SNOWY_FALSE),
         createStateDynamic("minecraft:mycelium", Map.of("snowy", "true"))
      );
      putStates(1776, createStateDynamic("minecraft:lily_pad"), createStateDynamic("minecraft:waterlily"));
   }

   private static void putStatesFromBlocks112To127() {
      putStates(1792, createStateDynamic("minecraft:nether_bricks"), createStateDynamic("minecraft:nether_brick"));
      putStates(
         1808,
         createStateDynamic("minecraft:nether_brick_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:nether_brick_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE)
      );
      putStates(
         1824,
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1825,
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1826,
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1827,
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         1828,
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1829,
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1830,
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         1831,
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:nether_brick_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(1840, createStateDynamic("minecraft:nether_wart", AGE_0), createStateDynamic("minecraft:nether_wart", AGE_0));
      putStates(1841, createStateDynamic("minecraft:nether_wart", AGE_1), createStateDynamic("minecraft:nether_wart", AGE_1));
      putStates(1842, createStateDynamic("minecraft:nether_wart", AGE_2), createStateDynamic("minecraft:nether_wart", AGE_2));
      putStates(1843, createStateDynamic("minecraft:nether_wart", AGE_3), createStateDynamic("minecraft:nether_wart", AGE_3));
      putStates(1856, createStateDynamic("minecraft:enchanting_table"), createStateDynamic("minecraft:enchanting_table"));
      putStates(
         1872,
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "false")),
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "false"))
      );
      putStates(
         1873,
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "false", "has_bottle_2", "false")),
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "false", "has_bottle_2", "false"))
      );
      putStates(
         1874,
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "true", "has_bottle_2", "false")),
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "true", "has_bottle_2", "false"))
      );
      putStates(
         1875,
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "true", "has_bottle_2", "false")),
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "true", "has_bottle_2", "false"))
      );
      putStates(
         1876,
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "true")),
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "true"))
      );
      putStates(
         1877,
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "false", "has_bottle_2", "true")),
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "false", "has_bottle_2", "true"))
      );
      putStates(
         1878,
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "true", "has_bottle_2", "true")),
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "true", "has_bottle_2", "true"))
      );
      putStates(
         1879,
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "true", "has_bottle_2", "true")),
         createStateDynamic("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "true", "has_bottle_2", "true"))
      );
      putStates(1888, createStateDynamic("minecraft:cauldron", LEVEL_0), createStateDynamic("minecraft:cauldron", LEVEL_0));
      putStates(1889, createStateDynamic("minecraft:cauldron", LEVEL_1), createStateDynamic("minecraft:cauldron", LEVEL_1));
      putStates(1890, createStateDynamic("minecraft:cauldron", LEVEL_2), createStateDynamic("minecraft:cauldron", LEVEL_2));
      putStates(1891, createStateDynamic("minecraft:cauldron", LEVEL_3), createStateDynamic("minecraft:cauldron", LEVEL_3));
      putStates(1904, createStateDynamic("minecraft:end_portal"), createStateDynamic("minecraft:end_portal"));
      putStates(
         1920,
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "south")),
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "south"))
      );
      putStates(
         1921,
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "west")),
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "west"))
      );
      putStates(
         1922,
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "north")),
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "north"))
      );
      putStates(
         1923,
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "east")),
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "east"))
      );
      putStates(
         1924,
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "south")),
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "south"))
      );
      putStates(
         1925,
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "west")),
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "west"))
      );
      putStates(
         1926,
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "north")),
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "north"))
      );
      putStates(
         1927,
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "east")),
         createStateDynamic("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "east"))
      );
      putStates(1936, createStateDynamic("minecraft:end_stone"), createStateDynamic("minecraft:end_stone"));
      putStates(1952, createStateDynamic("minecraft:dragon_egg"), createStateDynamic("minecraft:dragon_egg"));
      putStates(1968, createStateDynamic("minecraft:redstone_lamp", LIT_FALSE), createStateDynamic("minecraft:redstone_lamp"));
      putStates(1984, createStateDynamic("minecraft:redstone_lamp", LIT_TRUE), createStateDynamic("minecraft:lit_redstone_lamp"));
      putStates(2000, createStateDynamic("minecraft:oak_slab", TYPE_DOUBLE), createStateDynamic("minecraft:double_wooden_slab", Map.of("variant", "oak")));
      putStates(2001, createStateDynamic("minecraft:spruce_slab", TYPE_DOUBLE), createStateDynamic("minecraft:double_wooden_slab", Map.of("variant", "spruce")));
      putStates(2002, createStateDynamic("minecraft:birch_slab", TYPE_DOUBLE), createStateDynamic("minecraft:double_wooden_slab", Map.of("variant", "birch")));
      putStates(2003, createStateDynamic("minecraft:jungle_slab", TYPE_DOUBLE), createStateDynamic("minecraft:double_wooden_slab", Map.of("variant", "jungle")));
      putStates(2004, createStateDynamic("minecraft:acacia_slab", TYPE_DOUBLE), createStateDynamic("minecraft:double_wooden_slab", Map.of("variant", "acacia")));
      putStates(
         2005, createStateDynamic("minecraft:dark_oak_slab", TYPE_DOUBLE), createStateDynamic("minecraft:double_wooden_slab", Map.of("variant", "dark_oak"))
      );
      putStates(
         2016, createStateDynamic("minecraft:oak_slab", TYPE_BOTTOM), createStateDynamic("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "oak"))
      );
      putStates(
         2017,
         createStateDynamic("minecraft:spruce_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "spruce"))
      );
      putStates(
         2018,
         createStateDynamic("minecraft:birch_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "birch"))
      );
      putStates(
         2019,
         createStateDynamic("minecraft:jungle_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "jungle"))
      );
      putStates(
         2020,
         createStateDynamic("minecraft:acacia_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "acacia"))
      );
      putStates(
         2021,
         createStateDynamic("minecraft:dark_oak_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "dark_oak"))
      );
      putStates(2024, createStateDynamic("minecraft:oak_slab", TYPE_TOP), createStateDynamic("minecraft:wooden_slab", Map.of("half", "top", "variant", "oak")));
      putStates(
         2025, createStateDynamic("minecraft:spruce_slab", TYPE_TOP), createStateDynamic("minecraft:wooden_slab", Map.of("half", "top", "variant", "spruce"))
      );
      putStates(
         2026, createStateDynamic("minecraft:birch_slab", TYPE_TOP), createStateDynamic("minecraft:wooden_slab", Map.of("half", "top", "variant", "birch"))
      );
      putStates(
         2027, createStateDynamic("minecraft:jungle_slab", TYPE_TOP), createStateDynamic("minecraft:wooden_slab", Map.of("half", "top", "variant", "jungle"))
      );
      putStates(
         2028, createStateDynamic("minecraft:acacia_slab", TYPE_TOP), createStateDynamic("minecraft:wooden_slab", Map.of("half", "top", "variant", "acacia"))
      );
      putStates(
         2029,
         createStateDynamic("minecraft:dark_oak_slab", TYPE_TOP),
         createStateDynamic("minecraft:wooden_slab", Map.of("half", "top", "variant", "dark_oak"))
      );
      putStates(2032, createStateDynamic("minecraft:cocoa", AGE_0_FACING_SOUTH), createStateDynamic("minecraft:cocoa", AGE_0_FACING_SOUTH));
      putStates(2033, createStateDynamic("minecraft:cocoa", AGE_0_FACING_WEST), createStateDynamic("minecraft:cocoa", AGE_0_FACING_WEST));
      putStates(2034, createStateDynamic("minecraft:cocoa", AGE_0_FACING_NORTH), createStateDynamic("minecraft:cocoa", AGE_0_FACING_NORTH));
      putStates(2035, createStateDynamic("minecraft:cocoa", AGE_0_FACING_EAST), createStateDynamic("minecraft:cocoa", AGE_0_FACING_EAST));
      putStates(2036, createStateDynamic("minecraft:cocoa", AGE_1_FACING_SOUTH), createStateDynamic("minecraft:cocoa", AGE_1_FACING_SOUTH));
      putStates(2037, createStateDynamic("minecraft:cocoa", AGE_1_FACING_WEST), createStateDynamic("minecraft:cocoa", AGE_1_FACING_WEST));
      putStates(2038, createStateDynamic("minecraft:cocoa", AGE_1_FACING_NORTH), createStateDynamic("minecraft:cocoa", AGE_1_FACING_NORTH));
      putStates(2039, createStateDynamic("minecraft:cocoa", AGE_1_FACING_EAST), createStateDynamic("minecraft:cocoa", AGE_1_FACING_EAST));
      putStates(2040, createStateDynamic("minecraft:cocoa", AGE_2_FACING_SOUTH), createStateDynamic("minecraft:cocoa", AGE_2_FACING_SOUTH));
      putStates(2041, createStateDynamic("minecraft:cocoa", AGE_2_FACING_WEST), createStateDynamic("minecraft:cocoa", AGE_2_FACING_WEST));
      putStates(2042, createStateDynamic("minecraft:cocoa", AGE_2_FACING_NORTH), createStateDynamic("minecraft:cocoa", AGE_2_FACING_NORTH));
      putStates(2043, createStateDynamic("minecraft:cocoa", AGE_2_FACING_EAST), createStateDynamic("minecraft:cocoa", AGE_2_FACING_EAST));
   }

   private static void putStatesFromBlocks128To143() {
      putStates(
         2048,
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2049,
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2050,
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2051,
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2052,
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2053,
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2054,
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2055,
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(2064, createStateDynamic("minecraft:emerald_ore"), createStateDynamic("minecraft:emerald_ore"));
      putStates(2082, createStateDynamic("minecraft:ender_chest", FACING_NORTH), createStateDynamic("minecraft:ender_chest", FACING_NORTH));
      putStates(2083, createStateDynamic("minecraft:ender_chest", FACING_SOUTH), createStateDynamic("minecraft:ender_chest", FACING_SOUTH));
      putStates(2084, createStateDynamic("minecraft:ender_chest", FACING_WEST), createStateDynamic("minecraft:ender_chest", FACING_WEST));
      putStates(2085, createStateDynamic("minecraft:ender_chest", FACING_EAST), createStateDynamic("minecraft:ender_chest", FACING_EAST));
      putStates(
         2096,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "south", "powered", "false")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "south", "powered", "false"))
      );
      putStates(
         2097,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "west", "powered", "false")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "west", "powered", "false"))
      );
      putStates(
         2098,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "north", "powered", "false")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "north", "powered", "false"))
      );
      putStates(
         2099,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "east", "powered", "false")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "east", "powered", "false"))
      );
      putStates(
         2100,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "south", "powered", "false")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "south", "powered", "false"))
      );
      putStates(
         2101,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "west", "powered", "false")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "west", "powered", "false"))
      );
      putStates(
         2102,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "north", "powered", "false")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "north", "powered", "false"))
      );
      putStates(
         2103,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "east", "powered", "false")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "east", "powered", "false"))
      );
      putStates(
         2104,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "south", "powered", "true")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "south", "powered", "true"))
      );
      putStates(
         2105,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "west", "powered", "true")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "west", "powered", "true"))
      );
      putStates(
         2106,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "north", "powered", "true")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "north", "powered", "true"))
      );
      putStates(
         2107,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "east", "powered", "true")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "east", "powered", "true"))
      );
      putStates(
         2108,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "south", "powered", "true")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "south", "powered", "true"))
      );
      putStates(
         2109,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "west", "powered", "true")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "west", "powered", "true"))
      );
      putStates(
         2110,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "north", "powered", "true")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "north", "powered", "true"))
      );
      putStates(
         2111,
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "east", "powered", "true")),
         createStateDynamic("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "east", "powered", "true"))
      );
      putStates(
         2112,
         createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "true")
         )
      );
      putStates(
         2113,
         createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "true")
         )
      );
      putStates(2114, createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE));
      putStates(2115, createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE));
      putStates(
         2116,
         createStateDynamic("minecraft:tripwire", ATTACHED_TRUE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:tripwire", ATTACHED_TRUE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "true")
         )
      );
      putStates(
         2117,
         createStateDynamic("minecraft:tripwire", ATTACHED_TRUE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:tripwire", ATTACHED_TRUE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "true")
         )
      );
      putStates(2118, createStateDynamic("minecraft:tripwire", ATTACHED_TRUE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE));
      putStates(2119, createStateDynamic("minecraft:tripwire", ATTACHED_TRUE_DISARMED_FALSE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE));
      putStates(
         2120,
         createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "true")
         )
      );
      putStates(
         2121,
         createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "true")
         )
      );
      putStates(2122, createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE));
      putStates(2123, createStateDynamic("minecraft:tripwire", ATTACHED_FALSE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_TRUE_SOUTH_FALSE_WEST_FALSE));
      putStates(
         2124,
         createStateDynamic("minecraft:tripwire", ATTACHED_TRUE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:tripwire", ATTACHED_TRUE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "true")
         )
      );
      putStates(
         2125,
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         createStateDynamic(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "true")
         )
      );
      putStates(2126, createStateDynamic("minecraft:tripwire", ATTACHED_TRUE_DISARMED_TRUE_EAST_FALSE_NORTH_FALSE_POWERED_FALSE_SOUTH_FALSE_WEST_FALSE));
      putStates(2128, createStateDynamic("minecraft:emerald_block"), createStateDynamic("minecraft:emerald_block"));
      putStates(
         2144,
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2145,
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2146,
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2147,
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2148,
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2149,
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2150,
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2151,
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:spruce_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2160,
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2161,
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2162,
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2163,
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2164,
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2165,
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2166,
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2167,
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:birch_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2176,
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2177,
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2178,
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2179,
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2180,
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2181,
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2182,
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2183,
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:jungle_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2192,
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_DOWN),
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_DOWN)
      );
      putStates(
         2193,
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_UP),
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_UP)
      );
      putStates(
         2194,
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_NORTH),
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_NORTH)
      );
      putStates(
         2195,
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_SOUTH),
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_SOUTH)
      );
      putStates(
         2196,
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_WEST),
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_WEST)
      );
      putStates(
         2197,
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_EAST),
         createStateDynamic("minecraft:command_block", CONDITIONAL_FALSE_FACING_EAST)
      );
      putStates(
         2200,
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_DOWN),
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_DOWN)
      );
      putStates(
         2201,
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_UP),
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_UP)
      );
      putStates(
         2202,
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_NORTH),
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_NORTH)
      );
      putStates(
         2203,
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_SOUTH),
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_SOUTH)
      );
      putStates(
         2204,
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_WEST),
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_WEST)
      );
      putStates(
         2205,
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_EAST),
         createStateDynamic("minecraft:command_block", CONDITIONAL_TRUE_FACING_EAST)
      );
      putStates(2208, createStateDynamic("minecraft:beacon"), createStateDynamic("minecraft:beacon"));
      putStates(
         2224,
         createStateDynamic("minecraft:cobblestone_wall", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "false", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "false", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "true", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "true", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "false", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "false", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "true", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "true", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "false", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "false", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "true", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "true", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "false", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "false", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "true", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "true", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "variant", "cobblestone", "west", "true")
         )
      );
      putStates(
         2225,
         createStateDynamic("minecraft:mossy_cobblestone_wall", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         createStateDynamic(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         )
      );
      putStates(
         2240,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "0")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "0"))
      );
      putStates(
         2241,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "1")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "1"))
      );
      putStates(
         2242,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "2")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "2"))
      );
      putStates(
         2243,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "3")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "3"))
      );
      putStates(
         2244,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "4")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "4"))
      );
      putStates(
         2245,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "5")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "5"))
      );
      putStates(
         2246,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "6")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "6"))
      );
      putStates(
         2247,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "7")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "7"))
      );
      putStates(
         2248,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "8")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "8"))
      );
      putStates(
         2249,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "9")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "9"))
      );
      putStates(
         2250,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "10")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "10"))
      );
      putStates(
         2251,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "11")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "11"))
      );
      putStates(
         2252,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "12")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "12"))
      );
      putStates(
         2253,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "13")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "13"))
      );
      putStates(
         2254,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "14")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "14"))
      );
      putStates(
         2255,
         createStateDynamic("minecraft:potted_cactus"),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "15")),
         createStateDynamic("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "15"))
      );
      putStates(2256, createStateDynamic("minecraft:carrots", AGE_0), createStateDynamic("minecraft:carrots", AGE_0));
      putStates(2257, createStateDynamic("minecraft:carrots", AGE_1), createStateDynamic("minecraft:carrots", AGE_1));
      putStates(2258, createStateDynamic("minecraft:carrots", AGE_2), createStateDynamic("minecraft:carrots", AGE_2));
      putStates(2259, createStateDynamic("minecraft:carrots", AGE_3), createStateDynamic("minecraft:carrots", AGE_3));
      putStates(2260, createStateDynamic("minecraft:carrots", AGE_4), createStateDynamic("minecraft:carrots", AGE_4));
      putStates(2261, createStateDynamic("minecraft:carrots", AGE_5), createStateDynamic("minecraft:carrots", AGE_5));
      putStates(2262, createStateDynamic("minecraft:carrots", AGE_6), createStateDynamic("minecraft:carrots", AGE_6));
      putStates(2263, createStateDynamic("minecraft:carrots", AGE_7), createStateDynamic("minecraft:carrots", AGE_7));
      putStates(2272, createStateDynamic("minecraft:potatoes", AGE_0), createStateDynamic("minecraft:potatoes", AGE_0));
      putStates(2273, createStateDynamic("minecraft:potatoes", AGE_1), createStateDynamic("minecraft:potatoes", AGE_1));
      putStates(2274, createStateDynamic("minecraft:potatoes", AGE_2), createStateDynamic("minecraft:potatoes", AGE_2));
      putStates(2275, createStateDynamic("minecraft:potatoes", AGE_3), createStateDynamic("minecraft:potatoes", AGE_3));
      putStates(2276, createStateDynamic("minecraft:potatoes", AGE_4), createStateDynamic("minecraft:potatoes", AGE_4));
      putStates(2277, createStateDynamic("minecraft:potatoes", AGE_5), createStateDynamic("minecraft:potatoes", AGE_5));
      putStates(2278, createStateDynamic("minecraft:potatoes", AGE_6), createStateDynamic("minecraft:potatoes", AGE_6));
      putStates(2279, createStateDynamic("minecraft:potatoes", AGE_7), createStateDynamic("minecraft:potatoes", AGE_7));
      putStates(
         2288,
         createStateDynamic("minecraft:oak_button", FACE_CEILING_FACING_NORTH_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_button", FACING_DOWN_POWERED_FALSE)
      );
      putStates(
         2289,
         createStateDynamic("minecraft:oak_button", FACE_WALL_FACING_EAST_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_button", FACING_EAST_POWERED_FALSE)
      );
      putStates(
         2290,
         createStateDynamic("minecraft:oak_button", FACE_WALL_FACING_WEST_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_button", FACING_WEST_POWERED_FALSE)
      );
      putStates(
         2291,
         createStateDynamic("minecraft:oak_button", FACE_WALL_FACING_SOUTH_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_button", FACING_SOUTH_POWERED_FALSE)
      );
      putStates(
         2292,
         createStateDynamic("minecraft:oak_button", FACE_WALL_FACING_NORTH_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_button", FACING_NORTH_POWERED_FALSE)
      );
      putStates(
         2293,
         createStateDynamic("minecraft:oak_button", FACE_FLOOR_FACING_NORTH_POWERED_FALSE),
         createStateDynamic("minecraft:wooden_button", FACING_UP_POWERED_FALSE)
      );
      putStates(
         2296,
         createStateDynamic("minecraft:oak_button", FACE_CEILING_FACING_NORTH_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_button", FACING_DOWN_POWERED_TRUE)
      );
      putStates(
         2297,
         createStateDynamic("minecraft:oak_button", FACE_WALL_FACING_EAST_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_button", FACING_EAST_POWERED_TRUE)
      );
      putStates(
         2298,
         createStateDynamic("minecraft:oak_button", FACE_WALL_FACING_WEST_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_button", FACING_WEST_POWERED_TRUE)
      );
      putStates(
         2299,
         createStateDynamic("minecraft:oak_button", FACE_WALL_FACING_SOUTH_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_button", FACING_SOUTH_POWERED_TRUE)
      );
      putStates(
         2300,
         createStateDynamic("minecraft:oak_button", FACE_WALL_FACING_NORTH_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_button", FACING_NORTH_POWERED_TRUE)
      );
      putStates(
         2301,
         createStateDynamic("minecraft:oak_button", FACE_FLOOR_FACING_NORTH_POWERED_TRUE),
         createStateDynamic("minecraft:wooden_button", FACING_UP_POWERED_TRUE)
      );
   }

   private static void putStatesFromBlocks144To159() {
      putStates(
         2304,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "down", "nodrop", "false")),
         createStateDynamic("minecraft:skull", Map.of("facing", "down", "nodrop", "false"))
      );
      putStates(
         2305,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "up", "nodrop", "false")),
         createStateDynamic("minecraft:skull", Map.of("facing", "up", "nodrop", "false"))
      );
      putStates(
         2306,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "north", "nodrop", "false")),
         createStateDynamic("minecraft:skull", Map.of("facing", "north", "nodrop", "false"))
      );
      putStates(
         2307,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "south", "nodrop", "false")),
         createStateDynamic("minecraft:skull", Map.of("facing", "south", "nodrop", "false"))
      );
      putStates(
         2308,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "west", "nodrop", "false")),
         createStateDynamic("minecraft:skull", Map.of("facing", "west", "nodrop", "false"))
      );
      putStates(
         2309,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "east", "nodrop", "false")),
         createStateDynamic("minecraft:skull", Map.of("facing", "east", "nodrop", "false"))
      );
      putStates(
         2312,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "down", "nodrop", "true")),
         createStateDynamic("minecraft:skull", Map.of("facing", "down", "nodrop", "true"))
      );
      putStates(
         2313,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "up", "nodrop", "true")),
         createStateDynamic("minecraft:skull", Map.of("facing", "up", "nodrop", "true"))
      );
      putStates(
         2314,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "north", "nodrop", "true")),
         createStateDynamic("minecraft:skull", Map.of("facing", "north", "nodrop", "true"))
      );
      putStates(
         2315,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "south", "nodrop", "true")),
         createStateDynamic("minecraft:skull", Map.of("facing", "south", "nodrop", "true"))
      );
      putStates(
         2316,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "west", "nodrop", "true")),
         createStateDynamic("minecraft:skull", Map.of("facing", "west", "nodrop", "true"))
      );
      putStates(
         2317,
         createStateDynamic("%%FILTER_ME%%", Map.of("facing", "east", "nodrop", "true")),
         createStateDynamic("minecraft:skull", Map.of("facing", "east", "nodrop", "true"))
      );
      putStates(2320, createStateDynamic("minecraft:anvil", FACING_SOUTH), createStateDynamic("minecraft:anvil", Map.of("damage", "0", "facing", "south")));
      putStates(2321, createStateDynamic("minecraft:anvil", FACING_WEST), createStateDynamic("minecraft:anvil", Map.of("damage", "0", "facing", "west")));
      putStates(2322, createStateDynamic("minecraft:anvil", FACING_NORTH), createStateDynamic("minecraft:anvil", Map.of("damage", "0", "facing", "north")));
      putStates(2323, createStateDynamic("minecraft:anvil", FACING_EAST), createStateDynamic("minecraft:anvil", Map.of("damage", "0", "facing", "east")));
      putStates(
         2324, createStateDynamic("minecraft:chipped_anvil", FACING_SOUTH), createStateDynamic("minecraft:anvil", Map.of("damage", "1", "facing", "south"))
      );
      putStates(
         2325, createStateDynamic("minecraft:chipped_anvil", FACING_WEST), createStateDynamic("minecraft:anvil", Map.of("damage", "1", "facing", "west"))
      );
      putStates(
         2326, createStateDynamic("minecraft:chipped_anvil", FACING_NORTH), createStateDynamic("minecraft:anvil", Map.of("damage", "1", "facing", "north"))
      );
      putStates(
         2327, createStateDynamic("minecraft:chipped_anvil", FACING_EAST), createStateDynamic("minecraft:anvil", Map.of("damage", "1", "facing", "east"))
      );
      putStates(
         2328, createStateDynamic("minecraft:damaged_anvil", FACING_SOUTH), createStateDynamic("minecraft:anvil", Map.of("damage", "2", "facing", "south"))
      );
      putStates(
         2329, createStateDynamic("minecraft:damaged_anvil", FACING_WEST), createStateDynamic("minecraft:anvil", Map.of("damage", "2", "facing", "west"))
      );
      putStates(
         2330, createStateDynamic("minecraft:damaged_anvil", FACING_NORTH), createStateDynamic("minecraft:anvil", Map.of("damage", "2", "facing", "north"))
      );
      putStates(
         2331, createStateDynamic("minecraft:damaged_anvil", FACING_EAST), createStateDynamic("minecraft:anvil", Map.of("damage", "2", "facing", "east"))
      );
      putStates(
         2338,
         createStateDynamic("minecraft:trapped_chest", Map.of("facing", "north", "type", "single")),
         createStateDynamic("minecraft:trapped_chest", FACING_NORTH)
      );
      putStates(
         2339,
         createStateDynamic("minecraft:trapped_chest", Map.of("facing", "south", "type", "single")),
         createStateDynamic("minecraft:trapped_chest", FACING_SOUTH)
      );
      putStates(
         2340,
         createStateDynamic("minecraft:trapped_chest", Map.of("facing", "west", "type", "single")),
         createStateDynamic("minecraft:trapped_chest", FACING_WEST)
      );
      putStates(
         2341,
         createStateDynamic("minecraft:trapped_chest", Map.of("facing", "east", "type", "single")),
         createStateDynamic("minecraft:trapped_chest", FACING_EAST)
      );
      putStates(
         2352, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_0), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_0)
      );
      putStates(
         2353, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_1), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_1)
      );
      putStates(
         2354, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_2), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_2)
      );
      putStates(
         2355, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_3), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_3)
      );
      putStates(
         2356, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_4), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_4)
      );
      putStates(
         2357, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_5), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_5)
      );
      putStates(
         2358, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_6), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_6)
      );
      putStates(
         2359, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_7), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_7)
      );
      putStates(
         2360, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_8), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_8)
      );
      putStates(
         2361, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_9), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_9)
      );
      putStates(
         2362, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_10), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_10)
      );
      putStates(
         2363, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_11), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_11)
      );
      putStates(
         2364, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_12), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_12)
      );
      putStates(
         2365, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_13), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_13)
      );
      putStates(
         2366, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_14), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_14)
      );
      putStates(
         2367, createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_15), createStateDynamic("minecraft:light_weighted_pressure_plate", POWER_15)
      );
      putStates(
         2368, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_0), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_0)
      );
      putStates(
         2369, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_1), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_1)
      );
      putStates(
         2370, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_2), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_2)
      );
      putStates(
         2371, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_3), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_3)
      );
      putStates(
         2372, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_4), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_4)
      );
      putStates(
         2373, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_5), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_5)
      );
      putStates(
         2374, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_6), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_6)
      );
      putStates(
         2375, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_7), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_7)
      );
      putStates(
         2376, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_8), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_8)
      );
      putStates(
         2377, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_9), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_9)
      );
      putStates(
         2378, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_10), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_10)
      );
      putStates(
         2379, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_11), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_11)
      );
      putStates(
         2380, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_12), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_12)
      );
      putStates(
         2381, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_13), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_13)
      );
      putStates(
         2382, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_14), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_14)
      );
      putStates(
         2383, createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_15), createStateDynamic("minecraft:heavy_weighted_pressure_plate", POWER_15)
      );
      putStates(
         2384,
         createStateDynamic("minecraft:comparator", FACING_SOUTH_MODE_COMPARE_POWERED_FALSE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_SOUTH_MODE_COMPARE_POWERED_FALSE)
      );
      putStates(
         2385,
         createStateDynamic("minecraft:comparator", FACING_WEST_MODE_COMPARE_POWERED_FALSE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_WEST_MODE_COMPARE_POWERED_FALSE)
      );
      putStates(
         2386,
         createStateDynamic("minecraft:comparator", FACING_NORTH_MODE_COMPARE_POWERED_FALSE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_NORTH_MODE_COMPARE_POWERED_FALSE)
      );
      putStates(
         2387,
         createStateDynamic("minecraft:comparator", FACING_EAST_MODE_COMPARE_POWERED_FALSE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_EAST_MODE_COMPARE_POWERED_FALSE)
      );
      putStates(
         2388,
         createStateDynamic("minecraft:comparator", FACING_SOUTH_MODE_SUBTRACT_POWERED_FALSE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_SOUTH_MODE_SUBTRACT_POWERED_FALSE)
      );
      putStates(
         2389,
         createStateDynamic("minecraft:comparator", FACING_WEST_MODE_SUBTRACT_POWERED_FALSE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_WEST_MODE_SUBTRACT_POWERED_FALSE)
      );
      putStates(
         2390,
         createStateDynamic("minecraft:comparator", FACING_NORTH_MODE_SUBTRACT_POWERED_FALSE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_NORTH_MODE_SUBTRACT_POWERED_FALSE)
      );
      putStates(
         2391,
         createStateDynamic("minecraft:comparator", FACING_EAST_MODE_SUBTRACT_POWERED_FALSE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_EAST_MODE_SUBTRACT_POWERED_FALSE)
      );
      putStates(
         2392,
         createStateDynamic("minecraft:comparator", FACING_SOUTH_MODE_COMPARE_POWERED_TRUE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_SOUTH_MODE_COMPARE_POWERED_TRUE)
      );
      putStates(
         2393,
         createStateDynamic("minecraft:comparator", FACING_WEST_MODE_COMPARE_POWERED_TRUE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_WEST_MODE_COMPARE_POWERED_TRUE)
      );
      putStates(
         2394,
         createStateDynamic("minecraft:comparator", FACING_NORTH_MODE_COMPARE_POWERED_TRUE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_NORTH_MODE_COMPARE_POWERED_TRUE)
      );
      putStates(
         2395,
         createStateDynamic("minecraft:comparator", FACING_EAST_MODE_COMPARE_POWERED_TRUE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_EAST_MODE_COMPARE_POWERED_TRUE)
      );
      putStates(
         2396,
         createStateDynamic("minecraft:comparator", FACING_SOUTH_MODE_SUBTRACT_POWERED_TRUE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_SOUTH_MODE_SUBTRACT_POWERED_TRUE)
      );
      putStates(
         2397,
         createStateDynamic("minecraft:comparator", FACING_WEST_MODE_SUBTRACT_POWERED_TRUE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_WEST_MODE_SUBTRACT_POWERED_TRUE)
      );
      putStates(
         2398,
         createStateDynamic("minecraft:comparator", FACING_NORTH_MODE_SUBTRACT_POWERED_TRUE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_NORTH_MODE_SUBTRACT_POWERED_TRUE)
      );
      putStates(
         2399,
         createStateDynamic("minecraft:comparator", FACING_EAST_MODE_SUBTRACT_POWERED_TRUE),
         createStateDynamic("minecraft:unpowered_comparator", FACING_EAST_MODE_SUBTRACT_POWERED_TRUE)
      );
      putStates(
         2400,
         createStateDynamic("minecraft:comparator", FACING_SOUTH_MODE_COMPARE_POWERED_FALSE),
         createStateDynamic("minecraft:powered_comparator", FACING_SOUTH_MODE_COMPARE_POWERED_FALSE)
      );
      putStates(
         2401,
         createStateDynamic("minecraft:comparator", FACING_WEST_MODE_COMPARE_POWERED_FALSE),
         createStateDynamic("minecraft:powered_comparator", FACING_WEST_MODE_COMPARE_POWERED_FALSE)
      );
      putStates(
         2402,
         createStateDynamic("minecraft:comparator", FACING_NORTH_MODE_COMPARE_POWERED_FALSE),
         createStateDynamic("minecraft:powered_comparator", FACING_NORTH_MODE_COMPARE_POWERED_FALSE)
      );
      putStates(
         2403,
         createStateDynamic("minecraft:comparator", FACING_EAST_MODE_COMPARE_POWERED_FALSE),
         createStateDynamic("minecraft:powered_comparator", FACING_EAST_MODE_COMPARE_POWERED_FALSE)
      );
      putStates(
         2404,
         createStateDynamic("minecraft:comparator", FACING_SOUTH_MODE_SUBTRACT_POWERED_FALSE),
         createStateDynamic("minecraft:powered_comparator", FACING_SOUTH_MODE_SUBTRACT_POWERED_FALSE)
      );
      putStates(
         2405,
         createStateDynamic("minecraft:comparator", FACING_WEST_MODE_SUBTRACT_POWERED_FALSE),
         createStateDynamic("minecraft:powered_comparator", FACING_WEST_MODE_SUBTRACT_POWERED_FALSE)
      );
      putStates(
         2406,
         createStateDynamic("minecraft:comparator", FACING_NORTH_MODE_SUBTRACT_POWERED_FALSE),
         createStateDynamic("minecraft:powered_comparator", FACING_NORTH_MODE_SUBTRACT_POWERED_FALSE)
      );
      putStates(
         2407,
         createStateDynamic("minecraft:comparator", FACING_EAST_MODE_SUBTRACT_POWERED_FALSE),
         createStateDynamic("minecraft:powered_comparator", FACING_EAST_MODE_SUBTRACT_POWERED_FALSE)
      );
      putStates(
         2408,
         createStateDynamic("minecraft:comparator", FACING_SOUTH_MODE_COMPARE_POWERED_TRUE),
         createStateDynamic("minecraft:powered_comparator", FACING_SOUTH_MODE_COMPARE_POWERED_TRUE)
      );
      putStates(
         2409,
         createStateDynamic("minecraft:comparator", FACING_WEST_MODE_COMPARE_POWERED_TRUE),
         createStateDynamic("minecraft:powered_comparator", FACING_WEST_MODE_COMPARE_POWERED_TRUE)
      );
      putStates(
         2410,
         createStateDynamic("minecraft:comparator", FACING_NORTH_MODE_COMPARE_POWERED_TRUE),
         createStateDynamic("minecraft:powered_comparator", FACING_NORTH_MODE_COMPARE_POWERED_TRUE)
      );
      putStates(
         2411,
         createStateDynamic("minecraft:comparator", FACING_EAST_MODE_COMPARE_POWERED_TRUE),
         createStateDynamic("minecraft:powered_comparator", FACING_EAST_MODE_COMPARE_POWERED_TRUE)
      );
      putStates(
         2412,
         createStateDynamic("minecraft:comparator", FACING_SOUTH_MODE_SUBTRACT_POWERED_TRUE),
         createStateDynamic("minecraft:powered_comparator", FACING_SOUTH_MODE_SUBTRACT_POWERED_TRUE)
      );
      putStates(
         2413,
         createStateDynamic("minecraft:comparator", FACING_WEST_MODE_SUBTRACT_POWERED_TRUE),
         createStateDynamic("minecraft:powered_comparator", FACING_WEST_MODE_SUBTRACT_POWERED_TRUE)
      );
      putStates(
         2414,
         createStateDynamic("minecraft:comparator", FACING_NORTH_MODE_SUBTRACT_POWERED_TRUE),
         createStateDynamic("minecraft:powered_comparator", FACING_NORTH_MODE_SUBTRACT_POWERED_TRUE)
      );
      putStates(
         2415,
         createStateDynamic("minecraft:comparator", FACING_EAST_MODE_SUBTRACT_POWERED_TRUE),
         createStateDynamic("minecraft:powered_comparator", FACING_EAST_MODE_SUBTRACT_POWERED_TRUE)
      );
      putStates(
         2416,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "0")),
         createStateDynamic("minecraft:daylight_detector", POWER_0)
      );
      putStates(
         2417,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "1")),
         createStateDynamic("minecraft:daylight_detector", POWER_1)
      );
      putStates(
         2418,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "2")),
         createStateDynamic("minecraft:daylight_detector", POWER_2)
      );
      putStates(
         2419,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "3")),
         createStateDynamic("minecraft:daylight_detector", POWER_3)
      );
      putStates(
         2420,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "4")),
         createStateDynamic("minecraft:daylight_detector", POWER_4)
      );
      putStates(
         2421,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "5")),
         createStateDynamic("minecraft:daylight_detector", POWER_5)
      );
      putStates(
         2422,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "6")),
         createStateDynamic("minecraft:daylight_detector", POWER_6)
      );
      putStates(
         2423,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "7")),
         createStateDynamic("minecraft:daylight_detector", POWER_7)
      );
      putStates(
         2424,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "8")),
         createStateDynamic("minecraft:daylight_detector", POWER_8)
      );
      putStates(
         2425,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "9")),
         createStateDynamic("minecraft:daylight_detector", POWER_9)
      );
      putStates(
         2426,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "10")),
         createStateDynamic("minecraft:daylight_detector", POWER_10)
      );
      putStates(
         2427,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "11")),
         createStateDynamic("minecraft:daylight_detector", POWER_11)
      );
      putStates(
         2428,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "12")),
         createStateDynamic("minecraft:daylight_detector", POWER_12)
      );
      putStates(
         2429,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "13")),
         createStateDynamic("minecraft:daylight_detector", POWER_13)
      );
      putStates(
         2430,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "14")),
         createStateDynamic("minecraft:daylight_detector", POWER_14)
      );
      putStates(
         2431,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "false", "power", "15")),
         createStateDynamic("minecraft:daylight_detector", POWER_15)
      );
      putStates(2432, createStateDynamic("minecraft:redstone_block"), createStateDynamic("minecraft:redstone_block"));
      putStates(2448, createStateDynamic("minecraft:nether_quartz_ore"), createStateDynamic("minecraft:quartz_ore"));
      putStates(
         2464,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "down")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "down"))
      );
      putStates(
         2466,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "north")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "north"))
      );
      putStates(
         2467,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "south")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "south"))
      );
      putStates(
         2468,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "west")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "west"))
      );
      putStates(
         2469,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "east")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "true", "facing", "east"))
      );
      putStates(
         2472,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "down")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "down"))
      );
      putStates(
         2474,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "north")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "north"))
      );
      putStates(
         2475,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "south")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "south"))
      );
      putStates(
         2476,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "west")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "west"))
      );
      putStates(
         2477,
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "east")),
         createStateDynamic("minecraft:hopper", Map.of("enabled", "false", "facing", "east"))
      );
      putStates(2480, createStateDynamic("minecraft:quartz_block"), createStateDynamic("minecraft:quartz_block", Map.of("variant", "default")));
      putStates(2481, createStateDynamic("minecraft:chiseled_quartz_block"), createStateDynamic("minecraft:quartz_block", Map.of("variant", "chiseled")));
      putStates(2482, createStateDynamic("minecraft:quartz_pillar", AXIS_Y), createStateDynamic("minecraft:quartz_block", Map.of("variant", "lines_y")));
      putStates(2483, createStateDynamic("minecraft:quartz_pillar", AXIS_X), createStateDynamic("minecraft:quartz_block", Map.of("variant", "lines_x")));
      putStates(2484, createStateDynamic("minecraft:quartz_pillar", AXIS_Z), createStateDynamic("minecraft:quartz_block", Map.of("variant", "lines_z")));
      putStates(
         2496,
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2497,
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2498,
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2499,
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2500,
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2501,
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2502,
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2503,
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:quartz_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2512,
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_NORTH_SOUTH),
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_NORTH_SOUTH)
      );
      putStates(
         2513,
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_EAST_WEST),
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_EAST_WEST)
      );
      putStates(
         2514,
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_ASCENDING_EAST),
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_ASCENDING_EAST)
      );
      putStates(
         2515,
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_ASCENDING_WEST),
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_ASCENDING_WEST)
      );
      putStates(
         2516,
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_ASCENDING_NORTH),
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_ASCENDING_NORTH)
      );
      putStates(
         2517,
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_ASCENDING_SOUTH),
         createStateDynamic("minecraft:activator_rail", POWERED_FALSE_SHAPE_ASCENDING_SOUTH)
      );
      putStates(
         2520,
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_NORTH_SOUTH),
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_NORTH_SOUTH)
      );
      putStates(
         2521,
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_EAST_WEST),
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_EAST_WEST)
      );
      putStates(
         2522,
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_ASCENDING_EAST),
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_ASCENDING_EAST)
      );
      putStates(
         2523,
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_ASCENDING_WEST),
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_ASCENDING_WEST)
      );
      putStates(
         2524,
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_ASCENDING_NORTH),
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_ASCENDING_NORTH)
      );
      putStates(
         2525,
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_ASCENDING_SOUTH),
         createStateDynamic("minecraft:activator_rail", POWERED_TRUE_SHAPE_ASCENDING_SOUTH)
      );
      putStates(
         2528, createStateDynamic("minecraft:dropper", FACING_DOWN_TRIGGERED_FALSE), createStateDynamic("minecraft:dropper", FACING_DOWN_TRIGGERED_FALSE)
      );
      putStates(2529, createStateDynamic("minecraft:dropper", FACING_UP_TRIGGERED_FALSE), createStateDynamic("minecraft:dropper", FACING_UP_TRIGGERED_FALSE));
      putStates(
         2530, createStateDynamic("minecraft:dropper", FACING_NORTH_TRIGGERED_FALSE), createStateDynamic("minecraft:dropper", FACING_NORTH_TRIGGERED_FALSE)
      );
      putStates(
         2531, createStateDynamic("minecraft:dropper", FACING_SOUTH_TRIGGERED_FALSE), createStateDynamic("minecraft:dropper", FACING_SOUTH_TRIGGERED_FALSE)
      );
      putStates(
         2532, createStateDynamic("minecraft:dropper", FACING_WEST_TRIGGERED_FALSE), createStateDynamic("minecraft:dropper", FACING_WEST_TRIGGERED_FALSE)
      );
      putStates(
         2533, createStateDynamic("minecraft:dropper", FACING_EAST_TRIGGERED_FALSE), createStateDynamic("minecraft:dropper", FACING_EAST_TRIGGERED_FALSE)
      );
      putStates(2536, createStateDynamic("minecraft:dropper", FACING_DOWN_TRIGGERED_TRUE), createStateDynamic("minecraft:dropper", FACING_DOWN_TRIGGERED_TRUE));
      putStates(2537, createStateDynamic("minecraft:dropper", FACING_UP_TRIGGERED_TRUE), createStateDynamic("minecraft:dropper", FACING_UP_TRIGGERED_TRUE));
      putStates(
         2538, createStateDynamic("minecraft:dropper", FACING_NORTH_TRIGGERED_TRUE), createStateDynamic("minecraft:dropper", FACING_NORTH_TRIGGERED_TRUE)
      );
      putStates(
         2539, createStateDynamic("minecraft:dropper", FACING_SOUTH_TRIGGERED_TRUE), createStateDynamic("minecraft:dropper", FACING_SOUTH_TRIGGERED_TRUE)
      );
      putStates(2540, createStateDynamic("minecraft:dropper", FACING_WEST_TRIGGERED_TRUE), createStateDynamic("minecraft:dropper", FACING_WEST_TRIGGERED_TRUE));
      putStates(2541, createStateDynamic("minecraft:dropper", FACING_EAST_TRIGGERED_TRUE), createStateDynamic("minecraft:dropper", FACING_EAST_TRIGGERED_TRUE));
      putStates(2544, createStateDynamic("minecraft:white_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_WHITE));
      putStates(2545, createStateDynamic("minecraft:orange_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_ORANGE));
      putStates(2546, createStateDynamic("minecraft:magenta_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_MAGENTA));
      putStates(2547, createStateDynamic("minecraft:light_blue_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_LIGHT_BLUE));
      putStates(2548, createStateDynamic("minecraft:yellow_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_YELLOW));
      putStates(2549, createStateDynamic("minecraft:lime_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_LIME));
      putStates(2550, createStateDynamic("minecraft:pink_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_PINK));
      putStates(2551, createStateDynamic("minecraft:gray_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_GRAY));
      putStates(2552, createStateDynamic("minecraft:light_gray_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_SILVER));
      putStates(2553, createStateDynamic("minecraft:cyan_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_CYAN));
      putStates(2554, createStateDynamic("minecraft:purple_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_PURPLE));
      putStates(2555, createStateDynamic("minecraft:blue_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_BLUE));
      putStates(2556, createStateDynamic("minecraft:brown_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_BROWN));
      putStates(2557, createStateDynamic("minecraft:green_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_GREEN));
      putStates(2558, createStateDynamic("minecraft:red_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_RED));
      putStates(2559, createStateDynamic("minecraft:black_terracotta"), createStateDynamic("minecraft:stained_hardened_clay", COLOR_BLACK));
   }

   private static void putStatesFromBlocks160To175() {
      putStates(
         2560,
         createStateDynamic("minecraft:white_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2561,
         createStateDynamic("minecraft:orange_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2562,
         createStateDynamic("minecraft:magenta_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2563,
         createStateDynamic("minecraft:light_blue_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2564,
         createStateDynamic("minecraft:yellow_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2565,
         createStateDynamic("minecraft:lime_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2566,
         createStateDynamic("minecraft:pink_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2567,
         createStateDynamic("minecraft:gray_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2568,
         createStateDynamic("minecraft:light_gray_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2569,
         createStateDynamic("minecraft:cyan_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2570,
         createStateDynamic("minecraft:purple_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2571,
         createStateDynamic("minecraft:blue_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2572,
         createStateDynamic("minecraft:brown_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2573,
         createStateDynamic("minecraft:green_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2574,
         createStateDynamic("minecraft:red_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2575,
         createStateDynamic("minecraft:black_stained_glass_pane", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "true", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "false", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "false", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "false", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "false", "south", "true", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "true", "south", "false", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "true", "south", "false", "west", "true")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "true", "south", "true", "west", "false")),
         createStateDynamic("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      putStates(
         2576,
         createStateDynamic("minecraft:acacia_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves2", Map.of("check_decay", "false", "decayable", "true", "variant", "acacia"))
      );
      putStates(
         2577,
         createStateDynamic("minecraft:dark_oak_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves2", Map.of("check_decay", "false", "decayable", "true", "variant", "dark_oak"))
      );
      putStates(
         2580,
         createStateDynamic("minecraft:acacia_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves2", Map.of("check_decay", "false", "decayable", "false", "variant", "acacia"))
      );
      putStates(
         2581,
         createStateDynamic("minecraft:dark_oak_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves2", Map.of("check_decay", "false", "decayable", "false", "variant", "dark_oak"))
      );
      putStates(
         2584,
         createStateDynamic("minecraft:acacia_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves2", Map.of("check_decay", "true", "decayable", "true", "variant", "acacia"))
      );
      putStates(
         2585,
         createStateDynamic("minecraft:dark_oak_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         createStateDynamic("minecraft:leaves2", Map.of("check_decay", "true", "decayable", "true", "variant", "dark_oak"))
      );
      putStates(
         2588,
         createStateDynamic("minecraft:acacia_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves2", Map.of("check_decay", "true", "decayable", "false", "variant", "acacia"))
      );
      putStates(
         2589,
         createStateDynamic("minecraft:dark_oak_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         createStateDynamic("minecraft:leaves2", Map.of("check_decay", "true", "decayable", "false", "variant", "dark_oak"))
      );
      putStates(2592, createStateDynamic("minecraft:acacia_log", AXIS_Y), createStateDynamic("minecraft:log2", Map.of("axis", "y", "variant", "acacia")));
      putStates(2593, createStateDynamic("minecraft:dark_oak_log", AXIS_Y), createStateDynamic("minecraft:log2", Map.of("axis", "y", "variant", "dark_oak")));
      putStates(2596, createStateDynamic("minecraft:acacia_log", AXIS_X), createStateDynamic("minecraft:log2", Map.of("axis", "x", "variant", "acacia")));
      putStates(2597, createStateDynamic("minecraft:dark_oak_log", AXIS_X), createStateDynamic("minecraft:log2", Map.of("axis", "x", "variant", "dark_oak")));
      putStates(2600, createStateDynamic("minecraft:acacia_log", AXIS_Z), createStateDynamic("minecraft:log2", Map.of("axis", "z", "variant", "acacia")));
      putStates(2601, createStateDynamic("minecraft:dark_oak_log", AXIS_Z), createStateDynamic("minecraft:log2", Map.of("axis", "z", "variant", "dark_oak")));
      putStates(2604, createStateDynamic("minecraft:acacia_bark"), createStateDynamic("minecraft:log2", Map.of("axis", "none", "variant", "acacia")));
      putStates(2605, createStateDynamic("minecraft:dark_oak_bark"), createStateDynamic("minecraft:log2", Map.of("axis", "none", "variant", "dark_oak")));
      putStates(
         2608,
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2609,
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2610,
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2611,
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2612,
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2613,
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2614,
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2615,
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:acacia_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2624,
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2625,
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2626,
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2627,
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2628,
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2629,
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2630,
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2631,
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:dark_oak_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(2640, createStateDynamic("minecraft:slime_block"), createStateDynamic("minecraft:slime"));
      putStates(2656, createStateDynamic("minecraft:barrier"), createStateDynamic("minecraft:barrier"));
      putStates(
         2672,
         createStateDynamic("minecraft:iron_trapdoor", FACING_NORTH_HALF_BOTTOM_OPEN_FALSE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_NORTH_HALF_BOTTOM_OPEN_FALSE)
      );
      putStates(
         2673,
         createStateDynamic("minecraft:iron_trapdoor", FACING_SOUTH_HALF_BOTTOM_OPEN_FALSE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_SOUTH_HALF_BOTTOM_OPEN_FALSE)
      );
      putStates(
         2674,
         createStateDynamic("minecraft:iron_trapdoor", FACING_WEST_HALF_BOTTOM_OPEN_FALSE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_WEST_HALF_BOTTOM_OPEN_FALSE)
      );
      putStates(
         2675,
         createStateDynamic("minecraft:iron_trapdoor", FACING_EAST_HALF_BOTTOM_OPEN_FALSE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_EAST_HALF_BOTTOM_OPEN_FALSE)
      );
      putStates(
         2676,
         createStateDynamic("minecraft:iron_trapdoor", FACING_NORTH_HALF_BOTTOM_OPEN_TRUE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_NORTH_HALF_BOTTOM_OPEN_TRUE)
      );
      putStates(
         2677,
         createStateDynamic("minecraft:iron_trapdoor", FACING_SOUTH_HALF_BOTTOM_OPEN_TRUE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_SOUTH_HALF_BOTTOM_OPEN_TRUE)
      );
      putStates(
         2678,
         createStateDynamic("minecraft:iron_trapdoor", FACING_WEST_HALF_BOTTOM_OPEN_TRUE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_WEST_HALF_BOTTOM_OPEN_TRUE)
      );
      putStates(
         2679,
         createStateDynamic("minecraft:iron_trapdoor", FACING_EAST_HALF_BOTTOM_OPEN_TRUE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_EAST_HALF_BOTTOM_OPEN_TRUE)
      );
      putStates(
         2680,
         createStateDynamic("minecraft:iron_trapdoor", FACING_NORTH_HALF_TOP_OPEN_FALSE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_NORTH_HALF_TOP_OPEN_FALSE)
      );
      putStates(
         2681,
         createStateDynamic("minecraft:iron_trapdoor", FACING_SOUTH_HALF_TOP_OPEN_FALSE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_SOUTH_HALF_TOP_OPEN_FALSE)
      );
      putStates(
         2682,
         createStateDynamic("minecraft:iron_trapdoor", FACING_WEST_HALF_TOP_OPEN_FALSE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_WEST_HALF_TOP_OPEN_FALSE)
      );
      putStates(
         2683,
         createStateDynamic("minecraft:iron_trapdoor", FACING_EAST_HALF_TOP_OPEN_FALSE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_EAST_HALF_TOP_OPEN_FALSE)
      );
      putStates(
         2684,
         createStateDynamic("minecraft:iron_trapdoor", FACING_NORTH_HALF_TOP_OPEN_TRUE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_NORTH_HALF_TOP_OPEN_TRUE)
      );
      putStates(
         2685,
         createStateDynamic("minecraft:iron_trapdoor", FACING_SOUTH_HALF_TOP_OPEN_TRUE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_SOUTH_HALF_TOP_OPEN_TRUE)
      );
      putStates(
         2686,
         createStateDynamic("minecraft:iron_trapdoor", FACING_WEST_HALF_TOP_OPEN_TRUE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_WEST_HALF_TOP_OPEN_TRUE)
      );
      putStates(
         2687,
         createStateDynamic("minecraft:iron_trapdoor", FACING_EAST_HALF_TOP_OPEN_TRUE),
         createStateDynamic("minecraft:iron_trapdoor", FACING_EAST_HALF_TOP_OPEN_TRUE)
      );
      putStates(2688, createStateDynamic("minecraft:prismarine"), createStateDynamic("minecraft:prismarine", Map.of("variant", "prismarine")));
      putStates(2689, createStateDynamic("minecraft:prismarine_bricks"), createStateDynamic("minecraft:prismarine", Map.of("variant", "prismarine_bricks")));
      putStates(2690, createStateDynamic("minecraft:dark_prismarine"), createStateDynamic("minecraft:prismarine", Map.of("variant", "dark_prismarine")));
      putStates(2704, createStateDynamic("minecraft:sea_lantern"), createStateDynamic("minecraft:sea_lantern"));
      putStates(2720, createStateDynamic("minecraft:hay_block", AXIS_Y), createStateDynamic("minecraft:hay_block", AXIS_Y));
      putStates(2724, createStateDynamic("minecraft:hay_block", AXIS_X), createStateDynamic("minecraft:hay_block", AXIS_X));
      putStates(2728, createStateDynamic("minecraft:hay_block", AXIS_Z), createStateDynamic("minecraft:hay_block", AXIS_Z));
      putStates(2736, createStateDynamic("minecraft:white_carpet"), createStateDynamic("minecraft:carpet", COLOR_WHITE));
      putStates(2737, createStateDynamic("minecraft:orange_carpet"), createStateDynamic("minecraft:carpet", COLOR_ORANGE));
      putStates(2738, createStateDynamic("minecraft:magenta_carpet"), createStateDynamic("minecraft:carpet", COLOR_MAGENTA));
      putStates(2739, createStateDynamic("minecraft:light_blue_carpet"), createStateDynamic("minecraft:carpet", COLOR_LIGHT_BLUE));
      putStates(2740, createStateDynamic("minecraft:yellow_carpet"), createStateDynamic("minecraft:carpet", COLOR_YELLOW));
      putStates(2741, createStateDynamic("minecraft:lime_carpet"), createStateDynamic("minecraft:carpet", COLOR_LIME));
      putStates(2742, createStateDynamic("minecraft:pink_carpet"), createStateDynamic("minecraft:carpet", COLOR_PINK));
      putStates(2743, createStateDynamic("minecraft:gray_carpet"), createStateDynamic("minecraft:carpet", COLOR_GRAY));
      putStates(2744, createStateDynamic("minecraft:light_gray_carpet"), createStateDynamic("minecraft:carpet", COLOR_SILVER));
      putStates(2745, createStateDynamic("minecraft:cyan_carpet"), createStateDynamic("minecraft:carpet", COLOR_CYAN));
      putStates(2746, createStateDynamic("minecraft:purple_carpet"), createStateDynamic("minecraft:carpet", COLOR_PURPLE));
      putStates(2747, createStateDynamic("minecraft:blue_carpet"), createStateDynamic("minecraft:carpet", COLOR_BLUE));
      putStates(2748, createStateDynamic("minecraft:brown_carpet"), createStateDynamic("minecraft:carpet", COLOR_BROWN));
      putStates(2749, createStateDynamic("minecraft:green_carpet"), createStateDynamic("minecraft:carpet", COLOR_GREEN));
      putStates(2750, createStateDynamic("minecraft:red_carpet"), createStateDynamic("minecraft:carpet", COLOR_RED));
      putStates(2751, createStateDynamic("minecraft:black_carpet"), createStateDynamic("minecraft:carpet", COLOR_BLACK));
      putStates(2752, createStateDynamic("minecraft:terracotta"), createStateDynamic("minecraft:hardened_clay"));
      putStates(2768, createStateDynamic("minecraft:coal_block"), createStateDynamic("minecraft:coal_block"));
      putStates(2784, createStateDynamic("minecraft:packed_ice"), createStateDynamic("minecraft:packed_ice"));
      putStates(
         2800,
         createStateDynamic("minecraft:sunflower", HALF_LOWER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "sunflower")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "sunflower")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "sunflower")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "sunflower"))
      );
      putStates(
         2801,
         createStateDynamic("minecraft:lilac", HALF_LOWER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "syringa")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "syringa")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "syringa")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "syringa"))
      );
      putStates(
         2802,
         createStateDynamic("minecraft:tall_grass", HALF_LOWER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "double_grass")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "double_grass")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "double_grass")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "double_grass"))
      );
      putStates(
         2803,
         createStateDynamic("minecraft:large_fern", HALF_LOWER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "double_fern")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "double_fern")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "double_fern")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "double_fern"))
      );
      putStates(
         2804,
         createStateDynamic("minecraft:rose_bush", HALF_LOWER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "double_rose")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "double_rose")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "double_rose")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "double_rose"))
      );
      putStates(
         2805,
         createStateDynamic("minecraft:peony", HALF_LOWER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "paeonia")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "paeonia")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "paeonia")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "paeonia"))
      );
      putStates(
         2808,
         createStateDynamic("minecraft:peony", HALF_UPPER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "double_fern")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "double_grass")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "double_rose")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "paeonia")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "sunflower")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "syringa"))
      );
      putStates(
         2809,
         createStateDynamic("minecraft:peony", HALF_UPPER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "double_fern")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "double_grass")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "double_rose")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "paeonia")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "sunflower")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "syringa"))
      );
      putStates(
         2810,
         createStateDynamic("minecraft:peony", HALF_UPPER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "double_fern")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "double_grass")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "double_rose")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "paeonia")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "sunflower")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "syringa"))
      );
      putStates(
         2811,
         createStateDynamic("minecraft:peony", HALF_UPPER),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "double_fern")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "double_grass")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "double_rose")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "paeonia")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "sunflower")),
         createStateDynamic("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "syringa"))
      );
   }

   private static void putStatesFromBlocks176To191() {
      putStates(2816, createStateDynamic("minecraft:white_banner", ROTATION_0), createStateDynamic("minecraft:standing_banner", ROTATION_0));
      putStates(2817, createStateDynamic("minecraft:white_banner", ROTATION_1), createStateDynamic("minecraft:standing_banner", ROTATION_1));
      putStates(2818, createStateDynamic("minecraft:white_banner", ROTATION_2), createStateDynamic("minecraft:standing_banner", ROTATION_2));
      putStates(2819, createStateDynamic("minecraft:white_banner", ROTATION_3), createStateDynamic("minecraft:standing_banner", ROTATION_3));
      putStates(2820, createStateDynamic("minecraft:white_banner", ROTATION_4), createStateDynamic("minecraft:standing_banner", ROTATION_4));
      putStates(2821, createStateDynamic("minecraft:white_banner", ROTATION_5), createStateDynamic("minecraft:standing_banner", ROTATION_5));
      putStates(2822, createStateDynamic("minecraft:white_banner", ROTATION_6), createStateDynamic("minecraft:standing_banner", ROTATION_6));
      putStates(2823, createStateDynamic("minecraft:white_banner", ROTATION_7), createStateDynamic("minecraft:standing_banner", ROTATION_7));
      putStates(2824, createStateDynamic("minecraft:white_banner", ROTATION_8), createStateDynamic("minecraft:standing_banner", ROTATION_8));
      putStates(2825, createStateDynamic("minecraft:white_banner", ROTATION_9), createStateDynamic("minecraft:standing_banner", ROTATION_9));
      putStates(2826, createStateDynamic("minecraft:white_banner", ROTATION_10), createStateDynamic("minecraft:standing_banner", ROTATION_10));
      putStates(2827, createStateDynamic("minecraft:white_banner", ROTATION_11), createStateDynamic("minecraft:standing_banner", ROTATION_11));
      putStates(2828, createStateDynamic("minecraft:white_banner", ROTATION_12), createStateDynamic("minecraft:standing_banner", ROTATION_12));
      putStates(2829, createStateDynamic("minecraft:white_banner", ROTATION_13), createStateDynamic("minecraft:standing_banner", ROTATION_13));
      putStates(2830, createStateDynamic("minecraft:white_banner", ROTATION_14), createStateDynamic("minecraft:standing_banner", ROTATION_14));
      putStates(2831, createStateDynamic("minecraft:white_banner", ROTATION_15), createStateDynamic("minecraft:standing_banner", ROTATION_15));
      putStates(2834, createStateDynamic("minecraft:white_wall_banner", FACING_NORTH), createStateDynamic("minecraft:wall_banner", FACING_NORTH));
      putStates(2835, createStateDynamic("minecraft:white_wall_banner", FACING_SOUTH), createStateDynamic("minecraft:wall_banner", FACING_SOUTH));
      putStates(2836, createStateDynamic("minecraft:white_wall_banner", FACING_WEST), createStateDynamic("minecraft:wall_banner", FACING_WEST));
      putStates(2837, createStateDynamic("minecraft:white_wall_banner", FACING_EAST), createStateDynamic("minecraft:wall_banner", FACING_EAST));
      putStates(
         2848,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "0")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_0)
      );
      putStates(
         2849,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "1")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_1)
      );
      putStates(
         2850,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "2")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_2)
      );
      putStates(
         2851,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "3")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_3)
      );
      putStates(
         2852,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "4")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_4)
      );
      putStates(
         2853,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "5")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_5)
      );
      putStates(
         2854,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "6")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_6)
      );
      putStates(
         2855,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "7")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_7)
      );
      putStates(
         2856,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "8")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_8)
      );
      putStates(
         2857,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "9")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_9)
      );
      putStates(
         2858,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "10")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_10)
      );
      putStates(
         2859,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "11")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_11)
      );
      putStates(
         2860,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "12")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_12)
      );
      putStates(
         2861,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "13")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_13)
      );
      putStates(
         2862,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "14")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_14)
      );
      putStates(
         2863,
         createStateDynamic("minecraft:daylight_detector", Map.of("inverted", "true", "power", "15")),
         createStateDynamic("minecraft:daylight_detector_inverted", POWER_15)
      );
      putStates(2864, createStateDynamic("minecraft:red_sandstone"), createStateDynamic("minecraft:red_sandstone", Map.of("type", "red_sandstone")));
      putStates(
         2865, createStateDynamic("minecraft:chiseled_red_sandstone"), createStateDynamic("minecraft:red_sandstone", Map.of("type", "chiseled_red_sandstone"))
      );
      putStates(2866, createStateDynamic("minecraft:cut_red_sandstone"), createStateDynamic("minecraft:red_sandstone", Map.of("type", "smooth_red_sandstone")));
      putStates(
         2880,
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2881,
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2882,
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2883,
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         2884,
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2885,
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2886,
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2887,
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:red_sandstone_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         2896,
         createStateDynamic("minecraft:red_sandstone_slab", TYPE_DOUBLE),
         createStateDynamic("minecraft:double_stone_slab2", Map.of("seamless", "false", "variant", "red_sandstone"))
      );
      putStates(
         2904,
         createStateDynamic("minecraft:smooth_red_sandstone"),
         createStateDynamic("minecraft:double_stone_slab2", Map.of("seamless", "true", "variant", "red_sandstone"))
      );
      putStates(
         2912,
         createStateDynamic("minecraft:red_sandstone_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:stone_slab2", Map.of("half", "bottom", "variant", "red_sandstone"))
      );
      putStates(
         2920,
         createStateDynamic("minecraft:red_sandstone_slab", TYPE_TOP),
         createStateDynamic("minecraft:stone_slab2", Map.of("half", "top", "variant", "red_sandstone"))
      );
      putStates(
         2928,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2929,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2930,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2931,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2932,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2933,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2934,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2935,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2936,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2937,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2938,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2939,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2940,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2941,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2942,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2943,
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2944,
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2945,
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2946,
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2947,
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2948,
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2949,
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2950,
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2951,
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2952,
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2953,
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2954,
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2955,
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2956,
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2957,
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2958,
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2959,
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2960,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2961,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2962,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2963,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2964,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2965,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2966,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2967,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2968,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2969,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2970,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2971,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2972,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2973,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2974,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2975,
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2976,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2977,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2978,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2979,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2980,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2981,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2982,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2983,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2984,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2985,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2986,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2987,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         2988,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2989,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2990,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2991,
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         2992,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2993,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2994,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2995,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_FALSE)
      );
      putStates(
         2996,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2997,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2998,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         2999,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3000,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3001,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3002,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3003,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3004,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_SOUTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3005,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_WEST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3006,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_NORTH_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3007,
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_FALSE_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_fence_gate", FACING_EAST_IN_WALL_TRUE_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3008,
         createStateDynamic("minecraft:spruce_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:spruce_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:spruce_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:spruce_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:spruce_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:spruce_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:spruce_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:spruce_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:spruce_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:spruce_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:spruce_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:spruce_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:spruce_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:spruce_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:spruce_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:spruce_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:spruce_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE)
      );
      putStates(
         3024,
         createStateDynamic("minecraft:birch_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:birch_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:birch_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:birch_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:birch_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:birch_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:birch_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:birch_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:birch_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:birch_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:birch_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:birch_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:birch_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:birch_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:birch_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:birch_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:birch_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE)
      );
      putStates(
         3040,
         createStateDynamic("minecraft:jungle_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:jungle_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:jungle_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:jungle_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:jungle_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:jungle_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:jungle_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:jungle_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:jungle_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:jungle_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:jungle_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:jungle_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:jungle_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:jungle_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:jungle_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:jungle_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:jungle_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE)
      );
      putStates(
         3056,
         createStateDynamic("minecraft:dark_oak_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:dark_oak_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE)
      );
   }

   private static void putStatesFromBlocks192To207() {
      putStates(
         3072,
         createStateDynamic("minecraft:acacia_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:acacia_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:acacia_fence", EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:acacia_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:acacia_fence", EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:acacia_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:acacia_fence", EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:acacia_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:acacia_fence", EAST_FALSE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:acacia_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:acacia_fence", EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:acacia_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:acacia_fence", EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_WEST_TRUE),
         createStateDynamic("minecraft:acacia_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:acacia_fence", EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:acacia_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:acacia_fence", EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_WEST_TRUE)
      );
      putStates(
         3088,
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3089,
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3090,
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3091,
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3092,
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3093,
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3094,
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3095,
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3096,
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3097,
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3098,
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3099,
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:spruce_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3104,
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3105,
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3106,
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3107,
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3108,
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3109,
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3110,
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3111,
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3112,
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3113,
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3114,
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3115,
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:birch_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3120,
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3121,
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3122,
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3123,
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3124,
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3125,
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3126,
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3127,
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3128,
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3129,
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3130,
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3131,
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:jungle_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3136,
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3137,
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3138,
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3139,
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3140,
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3141,
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3142,
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3143,
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3144,
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3145,
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3146,
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3147,
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:acacia_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3152,
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3153,
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3154,
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3155,
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE)
      );
      putStates(
         3156,
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3157,
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3158,
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3159,
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_LOWER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3160,
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3161,
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_FALSE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_FALSE)
      );
      putStates(
         3162,
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_UPPER_HINGE_LEFT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(
         3163,
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_EAST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_NORTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_SOUTH_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_FALSE_POWERED_TRUE),
         createStateDynamic("minecraft:dark_oak_door", FACING_WEST_HALF_UPPER_HINGE_RIGHT_OPEN_TRUE_POWERED_TRUE)
      );
      putStates(3168, createStateDynamic("minecraft:end_rod", FACING_DOWN), createStateDynamic("minecraft:end_rod", FACING_DOWN));
      putStates(3169, createStateDynamic("minecraft:end_rod", FACING_UP), createStateDynamic("minecraft:end_rod", FACING_UP));
      putStates(3170, createStateDynamic("minecraft:end_rod", FACING_NORTH), createStateDynamic("minecraft:end_rod", FACING_NORTH));
      putStates(3171, createStateDynamic("minecraft:end_rod", FACING_SOUTH), createStateDynamic("minecraft:end_rod", FACING_SOUTH));
      putStates(3172, createStateDynamic("minecraft:end_rod", FACING_WEST), createStateDynamic("minecraft:end_rod", FACING_WEST));
      putStates(3173, createStateDynamic("minecraft:end_rod", FACING_EAST), createStateDynamic("minecraft:end_rod", FACING_EAST));
      putStates(
         3184,
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_FALSE_WEST_FALSE),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")
         ),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_TRUE),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")
         ),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_TRUE),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")
         ),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_FALSE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_TRUE),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")
         ),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")
         ),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_TRUE_NORTH_FALSE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")
         ),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_TRUE_NORTH_FALSE_SOUTH_TRUE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")
         ),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_TRUE_NORTH_TRUE_SOUTH_FALSE_UP_TRUE_WEST_FALSE),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", DOWN_FALSE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_FALSE_WEST_TRUE),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")
         ),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")
         ),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")
         ),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")
         ),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")
         ),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         createStateDynamic(
            "minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")
         ),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         createStateDynamic("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         createStateDynamic("minecraft:chorus_plant", DOWN_TRUE_EAST_TRUE_NORTH_TRUE_SOUTH_TRUE_UP_TRUE_WEST_TRUE)
      );
      putStates(3200, createStateDynamic("minecraft:chorus_flower", AGE_0), createStateDynamic("minecraft:chorus_flower", AGE_0));
      putStates(3201, createStateDynamic("minecraft:chorus_flower", AGE_1), createStateDynamic("minecraft:chorus_flower", AGE_1));
      putStates(3202, createStateDynamic("minecraft:chorus_flower", AGE_2), createStateDynamic("minecraft:chorus_flower", AGE_2));
      putStates(3203, createStateDynamic("minecraft:chorus_flower", AGE_3), createStateDynamic("minecraft:chorus_flower", AGE_3));
      putStates(3204, createStateDynamic("minecraft:chorus_flower", AGE_4), createStateDynamic("minecraft:chorus_flower", AGE_4));
      putStates(3205, createStateDynamic("minecraft:chorus_flower", AGE_5), createStateDynamic("minecraft:chorus_flower", AGE_5));
      putStates(3216, createStateDynamic("minecraft:purpur_block"), createStateDynamic("minecraft:purpur_block"));
      putStates(3232, createStateDynamic("minecraft:purpur_pillar", AXIS_Y), createStateDynamic("minecraft:purpur_pillar", AXIS_Y));
      putStates(3236, createStateDynamic("minecraft:purpur_pillar", AXIS_X), createStateDynamic("minecraft:purpur_pillar", AXIS_X));
      putStates(3240, createStateDynamic("minecraft:purpur_pillar", AXIS_Z), createStateDynamic("minecraft:purpur_pillar", AXIS_Z));
      putStates(
         3248,
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         3249,
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         3250,
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         3251,
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_BOTTOM_SHAPE_STRAIGHT)
      );
      putStates(
         3252,
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_EAST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         3253,
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_WEST_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         3254,
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_SOUTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         3255,
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_TOP_SHAPE_INNER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_LEFT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_TOP_SHAPE_OUTER_RIGHT),
         createStateDynamic("minecraft:purpur_stairs", FACING_NORTH_HALF_TOP_SHAPE_STRAIGHT)
      );
      putStates(
         3264, createStateDynamic("minecraft:purpur_slab", TYPE_DOUBLE), createStateDynamic("minecraft:purpur_double_slab", Map.of("variant", "default"))
      );
      putStates(
         3280,
         createStateDynamic("minecraft:purpur_slab", TYPE_BOTTOM),
         createStateDynamic("minecraft:purpur_slab", Map.of("half", "bottom", "variant", "default"))
      );
      putStates(
         3288, createStateDynamic("minecraft:purpur_slab", TYPE_TOP), createStateDynamic("minecraft:purpur_slab", Map.of("half", "top", "variant", "default"))
      );
      putStates(3296, createStateDynamic("minecraft:end_stone_bricks"), createStateDynamic("minecraft:end_bricks"));
      putStates(3312, createStateDynamic("minecraft:beetroots", AGE_0), createStateDynamic("minecraft:beetroots", AGE_0));
      putStates(3313, createStateDynamic("minecraft:beetroots", AGE_1), createStateDynamic("minecraft:beetroots", AGE_1));
      putStates(3314, createStateDynamic("minecraft:beetroots", AGE_2), createStateDynamic("minecraft:beetroots", AGE_2));
      putStates(3315, createStateDynamic("minecraft:beetroots", AGE_3), createStateDynamic("minecraft:beetroots", AGE_3));
   }

   private static void putStatesFromBlocks208To223() {
      putStates(3328, createStateDynamic("minecraft:grass_path"), createStateDynamic("minecraft:grass_path"));
      putStates(3344, createStateDynamic("minecraft:end_gateway"), createStateDynamic("minecraft:end_gateway"));
      putStates(
         3360,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_DOWN),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_DOWN)
      );
      putStates(
         3361,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_UP),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_UP)
      );
      putStates(
         3362,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_NORTH),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_NORTH)
      );
      putStates(
         3363,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_SOUTH),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_SOUTH)
      );
      putStates(
         3364,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_WEST),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_WEST)
      );
      putStates(
         3365,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_EAST),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_FALSE_FACING_EAST)
      );
      putStates(
         3368,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_DOWN),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_DOWN)
      );
      putStates(
         3369,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_UP),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_UP)
      );
      putStates(
         3370,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_NORTH),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_NORTH)
      );
      putStates(
         3371,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_SOUTH),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_SOUTH)
      );
      putStates(
         3372,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_WEST),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_WEST)
      );
      putStates(
         3373,
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_EAST),
         createStateDynamic("minecraft:repeating_command_block", CONDITIONAL_TRUE_FACING_EAST)
      );
      putStates(
         3376,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_DOWN),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_DOWN)
      );
      putStates(
         3377,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_UP),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_UP)
      );
      putStates(
         3378,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_NORTH),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_NORTH)
      );
      putStates(
         3379,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_SOUTH),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_SOUTH)
      );
      putStates(
         3380,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_WEST),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_WEST)
      );
      putStates(
         3381,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_EAST),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_FALSE_FACING_EAST)
      );
      putStates(
         3384,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_DOWN),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_DOWN)
      );
      putStates(
         3385,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_UP),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_UP)
      );
      putStates(
         3386,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_NORTH),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_NORTH)
      );
      putStates(
         3387,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_SOUTH),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_SOUTH)
      );
      putStates(
         3388,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_WEST),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_WEST)
      );
      putStates(
         3389,
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_EAST),
         createStateDynamic("minecraft:chain_command_block", CONDITIONAL_TRUE_FACING_EAST)
      );
      putStates(3392, createStateDynamic("minecraft:frosted_ice", AGE_0), createStateDynamic("minecraft:frosted_ice", AGE_0));
      putStates(3393, createStateDynamic("minecraft:frosted_ice", AGE_1), createStateDynamic("minecraft:frosted_ice", AGE_1));
      putStates(3394, createStateDynamic("minecraft:frosted_ice", AGE_2), createStateDynamic("minecraft:frosted_ice", AGE_2));
      putStates(3395, createStateDynamic("minecraft:frosted_ice", AGE_3), createStateDynamic("minecraft:frosted_ice", AGE_3));
      putStates(3408, createStateDynamic("minecraft:magma_block"), createStateDynamic("minecraft:magma"));
      putStates(3424, createStateDynamic("minecraft:nether_wart_block"), createStateDynamic("minecraft:nether_wart_block"));
      putStates(3440, createStateDynamic("minecraft:red_nether_bricks"), createStateDynamic("minecraft:red_nether_brick"));
      putStates(3456, createStateDynamic("minecraft:bone_block", AXIS_Y), createStateDynamic("minecraft:bone_block", AXIS_Y));
      putStates(3460, createStateDynamic("minecraft:bone_block", AXIS_X), createStateDynamic("minecraft:bone_block", AXIS_X));
      putStates(3464, createStateDynamic("minecraft:bone_block", AXIS_Z), createStateDynamic("minecraft:bone_block", AXIS_Z));
      putStates(3472, createStateDynamic("minecraft:structure_void"), createStateDynamic("minecraft:structure_void"));
      putStates(3488, createStateDynamic("minecraft:observer", FACING_DOWN_POWERED_FALSE), createStateDynamic("minecraft:observer", FACING_DOWN_POWERED_FALSE));
      putStates(3489, createStateDynamic("minecraft:observer", FACING_UP_POWERED_FALSE), createStateDynamic("minecraft:observer", FACING_UP_POWERED_FALSE));
      putStates(
         3490, createStateDynamic("minecraft:observer", FACING_NORTH_POWERED_FALSE), createStateDynamic("minecraft:observer", FACING_NORTH_POWERED_FALSE)
      );
      putStates(
         3491, createStateDynamic("minecraft:observer", FACING_SOUTH_POWERED_FALSE), createStateDynamic("minecraft:observer", FACING_SOUTH_POWERED_FALSE)
      );
      putStates(3492, createStateDynamic("minecraft:observer", FACING_WEST_POWERED_FALSE), createStateDynamic("minecraft:observer", FACING_WEST_POWERED_FALSE));
      putStates(3493, createStateDynamic("minecraft:observer", FACING_EAST_POWERED_FALSE), createStateDynamic("minecraft:observer", FACING_EAST_POWERED_FALSE));
      putStates(3496, createStateDynamic("minecraft:observer", FACING_DOWN_POWERED_TRUE), createStateDynamic("minecraft:observer", FACING_DOWN_POWERED_TRUE));
      putStates(3497, createStateDynamic("minecraft:observer", FACING_UP_POWERED_TRUE), createStateDynamic("minecraft:observer", FACING_UP_POWERED_TRUE));
      putStates(3498, createStateDynamic("minecraft:observer", FACING_NORTH_POWERED_TRUE), createStateDynamic("minecraft:observer", FACING_NORTH_POWERED_TRUE));
      putStates(3499, createStateDynamic("minecraft:observer", FACING_SOUTH_POWERED_TRUE), createStateDynamic("minecraft:observer", FACING_SOUTH_POWERED_TRUE));
      putStates(3500, createStateDynamic("minecraft:observer", FACING_WEST_POWERED_TRUE), createStateDynamic("minecraft:observer", FACING_WEST_POWERED_TRUE));
      putStates(3501, createStateDynamic("minecraft:observer", FACING_EAST_POWERED_TRUE), createStateDynamic("minecraft:observer", FACING_EAST_POWERED_TRUE));
      putStates(3504, createStateDynamic("minecraft:white_shulker_box", FACING_DOWN), createStateDynamic("minecraft:white_shulker_box", FACING_DOWN));
      putStates(3505, createStateDynamic("minecraft:white_shulker_box", FACING_UP), createStateDynamic("minecraft:white_shulker_box", FACING_UP));
      putStates(3506, createStateDynamic("minecraft:white_shulker_box", FACING_NORTH), createStateDynamic("minecraft:white_shulker_box", FACING_NORTH));
      putStates(3507, createStateDynamic("minecraft:white_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:white_shulker_box", FACING_SOUTH));
      putStates(3508, createStateDynamic("minecraft:white_shulker_box", FACING_WEST), createStateDynamic("minecraft:white_shulker_box", FACING_WEST));
      putStates(3509, createStateDynamic("minecraft:white_shulker_box", FACING_EAST), createStateDynamic("minecraft:white_shulker_box", FACING_EAST));
      putStates(3520, createStateDynamic("minecraft:orange_shulker_box", FACING_DOWN), createStateDynamic("minecraft:orange_shulker_box", FACING_DOWN));
      putStates(3521, createStateDynamic("minecraft:orange_shulker_box", FACING_UP), createStateDynamic("minecraft:orange_shulker_box", FACING_UP));
      putStates(3522, createStateDynamic("minecraft:orange_shulker_box", FACING_NORTH), createStateDynamic("minecraft:orange_shulker_box", FACING_NORTH));
      putStates(3523, createStateDynamic("minecraft:orange_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:orange_shulker_box", FACING_SOUTH));
      putStates(3524, createStateDynamic("minecraft:orange_shulker_box", FACING_WEST), createStateDynamic("minecraft:orange_shulker_box", FACING_WEST));
      putStates(3525, createStateDynamic("minecraft:orange_shulker_box", FACING_EAST), createStateDynamic("minecraft:orange_shulker_box", FACING_EAST));
      putStates(3536, createStateDynamic("minecraft:magenta_shulker_box", FACING_DOWN), createStateDynamic("minecraft:magenta_shulker_box", FACING_DOWN));
      putStates(3537, createStateDynamic("minecraft:magenta_shulker_box", FACING_UP), createStateDynamic("minecraft:magenta_shulker_box", FACING_UP));
      putStates(3538, createStateDynamic("minecraft:magenta_shulker_box", FACING_NORTH), createStateDynamic("minecraft:magenta_shulker_box", FACING_NORTH));
      putStates(3539, createStateDynamic("minecraft:magenta_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:magenta_shulker_box", FACING_SOUTH));
      putStates(3540, createStateDynamic("minecraft:magenta_shulker_box", FACING_WEST), createStateDynamic("minecraft:magenta_shulker_box", FACING_WEST));
      putStates(3541, createStateDynamic("minecraft:magenta_shulker_box", FACING_EAST), createStateDynamic("minecraft:magenta_shulker_box", FACING_EAST));
      putStates(3552, createStateDynamic("minecraft:light_blue_shulker_box", FACING_DOWN), createStateDynamic("minecraft:light_blue_shulker_box", FACING_DOWN));
      putStates(3553, createStateDynamic("minecraft:light_blue_shulker_box", FACING_UP), createStateDynamic("minecraft:light_blue_shulker_box", FACING_UP));
      putStates(
         3554, createStateDynamic("minecraft:light_blue_shulker_box", FACING_NORTH), createStateDynamic("minecraft:light_blue_shulker_box", FACING_NORTH)
      );
      putStates(
         3555, createStateDynamic("minecraft:light_blue_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:light_blue_shulker_box", FACING_SOUTH)
      );
      putStates(3556, createStateDynamic("minecraft:light_blue_shulker_box", FACING_WEST), createStateDynamic("minecraft:light_blue_shulker_box", FACING_WEST));
      putStates(3557, createStateDynamic("minecraft:light_blue_shulker_box", FACING_EAST), createStateDynamic("minecraft:light_blue_shulker_box", FACING_EAST));
      putStates(3568, createStateDynamic("minecraft:yellow_shulker_box", FACING_DOWN), createStateDynamic("minecraft:yellow_shulker_box", FACING_DOWN));
      putStates(3569, createStateDynamic("minecraft:yellow_shulker_box", FACING_UP), createStateDynamic("minecraft:yellow_shulker_box", FACING_UP));
      putStates(3570, createStateDynamic("minecraft:yellow_shulker_box", FACING_NORTH), createStateDynamic("minecraft:yellow_shulker_box", FACING_NORTH));
      putStates(3571, createStateDynamic("minecraft:yellow_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:yellow_shulker_box", FACING_SOUTH));
      putStates(3572, createStateDynamic("minecraft:yellow_shulker_box", FACING_WEST), createStateDynamic("minecraft:yellow_shulker_box", FACING_WEST));
      putStates(3573, createStateDynamic("minecraft:yellow_shulker_box", FACING_EAST), createStateDynamic("minecraft:yellow_shulker_box", FACING_EAST));
   }

   private static void putStatesFromBlocks224To239() {
      putStates(3584, createStateDynamic("minecraft:lime_shulker_box", FACING_DOWN), createStateDynamic("minecraft:lime_shulker_box", FACING_DOWN));
      putStates(3585, createStateDynamic("minecraft:lime_shulker_box", FACING_UP), createStateDynamic("minecraft:lime_shulker_box", FACING_UP));
      putStates(3586, createStateDynamic("minecraft:lime_shulker_box", FACING_NORTH), createStateDynamic("minecraft:lime_shulker_box", FACING_NORTH));
      putStates(3587, createStateDynamic("minecraft:lime_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:lime_shulker_box", FACING_SOUTH));
      putStates(3588, createStateDynamic("minecraft:lime_shulker_box", FACING_WEST), createStateDynamic("minecraft:lime_shulker_box", FACING_WEST));
      putStates(3589, createStateDynamic("minecraft:lime_shulker_box", FACING_EAST), createStateDynamic("minecraft:lime_shulker_box", FACING_EAST));
      putStates(3600, createStateDynamic("minecraft:pink_shulker_box", FACING_DOWN), createStateDynamic("minecraft:pink_shulker_box", FACING_DOWN));
      putStates(3601, createStateDynamic("minecraft:pink_shulker_box", FACING_UP), createStateDynamic("minecraft:pink_shulker_box", FACING_UP));
      putStates(3602, createStateDynamic("minecraft:pink_shulker_box", FACING_NORTH), createStateDynamic("minecraft:pink_shulker_box", FACING_NORTH));
      putStates(3603, createStateDynamic("minecraft:pink_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:pink_shulker_box", FACING_SOUTH));
      putStates(3604, createStateDynamic("minecraft:pink_shulker_box", FACING_WEST), createStateDynamic("minecraft:pink_shulker_box", FACING_WEST));
      putStates(3605, createStateDynamic("minecraft:pink_shulker_box", FACING_EAST), createStateDynamic("minecraft:pink_shulker_box", FACING_EAST));
      putStates(3616, createStateDynamic("minecraft:gray_shulker_box", FACING_DOWN), createStateDynamic("minecraft:gray_shulker_box", FACING_DOWN));
      putStates(3617, createStateDynamic("minecraft:gray_shulker_box", FACING_UP), createStateDynamic("minecraft:gray_shulker_box", FACING_UP));
      putStates(3618, createStateDynamic("minecraft:gray_shulker_box", FACING_NORTH), createStateDynamic("minecraft:gray_shulker_box", FACING_NORTH));
      putStates(3619, createStateDynamic("minecraft:gray_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:gray_shulker_box", FACING_SOUTH));
      putStates(3620, createStateDynamic("minecraft:gray_shulker_box", FACING_WEST), createStateDynamic("minecraft:gray_shulker_box", FACING_WEST));
      putStates(3621, createStateDynamic("minecraft:gray_shulker_box", FACING_EAST), createStateDynamic("minecraft:gray_shulker_box", FACING_EAST));
      putStates(3632, createStateDynamic("minecraft:light_gray_shulker_box", FACING_DOWN), createStateDynamic("minecraft:silver_shulker_box", FACING_DOWN));
      putStates(3633, createStateDynamic("minecraft:light_gray_shulker_box", FACING_UP), createStateDynamic("minecraft:silver_shulker_box", FACING_UP));
      putStates(3634, createStateDynamic("minecraft:light_gray_shulker_box", FACING_NORTH), createStateDynamic("minecraft:silver_shulker_box", FACING_NORTH));
      putStates(3635, createStateDynamic("minecraft:light_gray_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:silver_shulker_box", FACING_SOUTH));
      putStates(3636, createStateDynamic("minecraft:light_gray_shulker_box", FACING_WEST), createStateDynamic("minecraft:silver_shulker_box", FACING_WEST));
      putStates(3637, createStateDynamic("minecraft:light_gray_shulker_box", FACING_EAST), createStateDynamic("minecraft:silver_shulker_box", FACING_EAST));
      putStates(3648, createStateDynamic("minecraft:cyan_shulker_box", FACING_DOWN), createStateDynamic("minecraft:cyan_shulker_box", FACING_DOWN));
      putStates(3649, createStateDynamic("minecraft:cyan_shulker_box", FACING_UP), createStateDynamic("minecraft:cyan_shulker_box", FACING_UP));
      putStates(3650, createStateDynamic("minecraft:cyan_shulker_box", FACING_NORTH), createStateDynamic("minecraft:cyan_shulker_box", FACING_NORTH));
      putStates(3651, createStateDynamic("minecraft:cyan_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:cyan_shulker_box", FACING_SOUTH));
      putStates(3652, createStateDynamic("minecraft:cyan_shulker_box", FACING_WEST), createStateDynamic("minecraft:cyan_shulker_box", FACING_WEST));
      putStates(3653, createStateDynamic("minecraft:cyan_shulker_box", FACING_EAST), createStateDynamic("minecraft:cyan_shulker_box", FACING_EAST));
      putStates(3664, createStateDynamic("minecraft:purple_shulker_box", FACING_DOWN), createStateDynamic("minecraft:purple_shulker_box", FACING_DOWN));
      putStates(3665, createStateDynamic("minecraft:purple_shulker_box", FACING_UP), createStateDynamic("minecraft:purple_shulker_box", FACING_UP));
      putStates(3666, createStateDynamic("minecraft:purple_shulker_box", FACING_NORTH), createStateDynamic("minecraft:purple_shulker_box", FACING_NORTH));
      putStates(3667, createStateDynamic("minecraft:purple_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:purple_shulker_box", FACING_SOUTH));
      putStates(3668, createStateDynamic("minecraft:purple_shulker_box", FACING_WEST), createStateDynamic("minecraft:purple_shulker_box", FACING_WEST));
      putStates(3669, createStateDynamic("minecraft:purple_shulker_box", FACING_EAST), createStateDynamic("minecraft:purple_shulker_box", FACING_EAST));
      putStates(3680, createStateDynamic("minecraft:blue_shulker_box", FACING_DOWN), createStateDynamic("minecraft:blue_shulker_box", FACING_DOWN));
      putStates(3681, createStateDynamic("minecraft:blue_shulker_box", FACING_UP), createStateDynamic("minecraft:blue_shulker_box", FACING_UP));
      putStates(3682, createStateDynamic("minecraft:blue_shulker_box", FACING_NORTH), createStateDynamic("minecraft:blue_shulker_box", FACING_NORTH));
      putStates(3683, createStateDynamic("minecraft:blue_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:blue_shulker_box", FACING_SOUTH));
      putStates(3684, createStateDynamic("minecraft:blue_shulker_box", FACING_WEST), createStateDynamic("minecraft:blue_shulker_box", FACING_WEST));
      putStates(3685, createStateDynamic("minecraft:blue_shulker_box", FACING_EAST), createStateDynamic("minecraft:blue_shulker_box", FACING_EAST));
      putStates(3696, createStateDynamic("minecraft:brown_shulker_box", FACING_DOWN), createStateDynamic("minecraft:brown_shulker_box", FACING_DOWN));
      putStates(3697, createStateDynamic("minecraft:brown_shulker_box", FACING_UP), createStateDynamic("minecraft:brown_shulker_box", FACING_UP));
      putStates(3698, createStateDynamic("minecraft:brown_shulker_box", FACING_NORTH), createStateDynamic("minecraft:brown_shulker_box", FACING_NORTH));
      putStates(3699, createStateDynamic("minecraft:brown_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:brown_shulker_box", FACING_SOUTH));
      putStates(3700, createStateDynamic("minecraft:brown_shulker_box", FACING_WEST), createStateDynamic("minecraft:brown_shulker_box", FACING_WEST));
      putStates(3701, createStateDynamic("minecraft:brown_shulker_box", FACING_EAST), createStateDynamic("minecraft:brown_shulker_box", FACING_EAST));
      putStates(3712, createStateDynamic("minecraft:green_shulker_box", FACING_DOWN), createStateDynamic("minecraft:green_shulker_box", FACING_DOWN));
      putStates(3713, createStateDynamic("minecraft:green_shulker_box", FACING_UP), createStateDynamic("minecraft:green_shulker_box", FACING_UP));
      putStates(3714, createStateDynamic("minecraft:green_shulker_box", FACING_NORTH), createStateDynamic("minecraft:green_shulker_box", FACING_NORTH));
      putStates(3715, createStateDynamic("minecraft:green_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:green_shulker_box", FACING_SOUTH));
      putStates(3716, createStateDynamic("minecraft:green_shulker_box", FACING_WEST), createStateDynamic("minecraft:green_shulker_box", FACING_WEST));
      putStates(3717, createStateDynamic("minecraft:green_shulker_box", FACING_EAST), createStateDynamic("minecraft:green_shulker_box", FACING_EAST));
      putStates(3728, createStateDynamic("minecraft:red_shulker_box", FACING_DOWN), createStateDynamic("minecraft:red_shulker_box", FACING_DOWN));
      putStates(3729, createStateDynamic("minecraft:red_shulker_box", FACING_UP), createStateDynamic("minecraft:red_shulker_box", FACING_UP));
      putStates(3730, createStateDynamic("minecraft:red_shulker_box", FACING_NORTH), createStateDynamic("minecraft:red_shulker_box", FACING_NORTH));
      putStates(3731, createStateDynamic("minecraft:red_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:red_shulker_box", FACING_SOUTH));
      putStates(3732, createStateDynamic("minecraft:red_shulker_box", FACING_WEST), createStateDynamic("minecraft:red_shulker_box", FACING_WEST));
      putStates(3733, createStateDynamic("minecraft:red_shulker_box", FACING_EAST), createStateDynamic("minecraft:red_shulker_box", FACING_EAST));
      putStates(3744, createStateDynamic("minecraft:black_shulker_box", FACING_DOWN), createStateDynamic("minecraft:black_shulker_box", FACING_DOWN));
      putStates(3745, createStateDynamic("minecraft:black_shulker_box", FACING_UP), createStateDynamic("minecraft:black_shulker_box", FACING_UP));
      putStates(3746, createStateDynamic("minecraft:black_shulker_box", FACING_NORTH), createStateDynamic("minecraft:black_shulker_box", FACING_NORTH));
      putStates(3747, createStateDynamic("minecraft:black_shulker_box", FACING_SOUTH), createStateDynamic("minecraft:black_shulker_box", FACING_SOUTH));
      putStates(3748, createStateDynamic("minecraft:black_shulker_box", FACING_WEST), createStateDynamic("minecraft:black_shulker_box", FACING_WEST));
      putStates(3749, createStateDynamic("minecraft:black_shulker_box", FACING_EAST), createStateDynamic("minecraft:black_shulker_box", FACING_EAST));
      putStates(
         3760, createStateDynamic("minecraft:white_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:white_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         3761, createStateDynamic("minecraft:white_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:white_glazed_terracotta", FACING_WEST)
      );
      putStates(
         3762, createStateDynamic("minecraft:white_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:white_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         3763, createStateDynamic("minecraft:white_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:white_glazed_terracotta", FACING_EAST)
      );
      putStates(
         3776, createStateDynamic("minecraft:orange_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:orange_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         3777, createStateDynamic("minecraft:orange_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:orange_glazed_terracotta", FACING_WEST)
      );
      putStates(
         3778, createStateDynamic("minecraft:orange_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:orange_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         3779, createStateDynamic("minecraft:orange_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:orange_glazed_terracotta", FACING_EAST)
      );
      putStates(
         3792, createStateDynamic("minecraft:magenta_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:magenta_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         3793, createStateDynamic("minecraft:magenta_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:magenta_glazed_terracotta", FACING_WEST)
      );
      putStates(
         3794, createStateDynamic("minecraft:magenta_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:magenta_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         3795, createStateDynamic("minecraft:magenta_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:magenta_glazed_terracotta", FACING_EAST)
      );
      putStates(
         3808,
         createStateDynamic("minecraft:light_blue_glazed_terracotta", FACING_SOUTH),
         createStateDynamic("minecraft:light_blue_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         3809,
         createStateDynamic("minecraft:light_blue_glazed_terracotta", FACING_WEST),
         createStateDynamic("minecraft:light_blue_glazed_terracotta", FACING_WEST)
      );
      putStates(
         3810,
         createStateDynamic("minecraft:light_blue_glazed_terracotta", FACING_NORTH),
         createStateDynamic("minecraft:light_blue_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         3811,
         createStateDynamic("minecraft:light_blue_glazed_terracotta", FACING_EAST),
         createStateDynamic("minecraft:light_blue_glazed_terracotta", FACING_EAST)
      );
      putStates(
         3824, createStateDynamic("minecraft:yellow_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:yellow_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         3825, createStateDynamic("minecraft:yellow_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:yellow_glazed_terracotta", FACING_WEST)
      );
      putStates(
         3826, createStateDynamic("minecraft:yellow_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:yellow_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         3827, createStateDynamic("minecraft:yellow_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:yellow_glazed_terracotta", FACING_EAST)
      );
   }

   private static void putStatesFromBlocks240To255() {
      putStates(
         3840, createStateDynamic("minecraft:lime_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:lime_glazed_terracotta", FACING_SOUTH)
      );
      putStates(3841, createStateDynamic("minecraft:lime_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:lime_glazed_terracotta", FACING_WEST));
      putStates(
         3842, createStateDynamic("minecraft:lime_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:lime_glazed_terracotta", FACING_NORTH)
      );
      putStates(3843, createStateDynamic("minecraft:lime_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:lime_glazed_terracotta", FACING_EAST));
      putStates(
         3856, createStateDynamic("minecraft:pink_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:pink_glazed_terracotta", FACING_SOUTH)
      );
      putStates(3857, createStateDynamic("minecraft:pink_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:pink_glazed_terracotta", FACING_WEST));
      putStates(
         3858, createStateDynamic("minecraft:pink_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:pink_glazed_terracotta", FACING_NORTH)
      );
      putStates(3859, createStateDynamic("minecraft:pink_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:pink_glazed_terracotta", FACING_EAST));
      putStates(
         3872, createStateDynamic("minecraft:gray_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:gray_glazed_terracotta", FACING_SOUTH)
      );
      putStates(3873, createStateDynamic("minecraft:gray_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:gray_glazed_terracotta", FACING_WEST));
      putStates(
         3874, createStateDynamic("minecraft:gray_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:gray_glazed_terracotta", FACING_NORTH)
      );
      putStates(3875, createStateDynamic("minecraft:gray_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:gray_glazed_terracotta", FACING_EAST));
      putStates(
         3888,
         createStateDynamic("minecraft:light_gray_glazed_terracotta", FACING_SOUTH),
         createStateDynamic("minecraft:silver_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         3889, createStateDynamic("minecraft:light_gray_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:silver_glazed_terracotta", FACING_WEST)
      );
      putStates(
         3890,
         createStateDynamic("minecraft:light_gray_glazed_terracotta", FACING_NORTH),
         createStateDynamic("minecraft:silver_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         3891, createStateDynamic("minecraft:light_gray_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:silver_glazed_terracotta", FACING_EAST)
      );
      putStates(
         3904, createStateDynamic("minecraft:cyan_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:cyan_glazed_terracotta", FACING_SOUTH)
      );
      putStates(3905, createStateDynamic("minecraft:cyan_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:cyan_glazed_terracotta", FACING_WEST));
      putStates(
         3906, createStateDynamic("minecraft:cyan_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:cyan_glazed_terracotta", FACING_NORTH)
      );
      putStates(3907, createStateDynamic("minecraft:cyan_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:cyan_glazed_terracotta", FACING_EAST));
      putStates(
         3920, createStateDynamic("minecraft:purple_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:purple_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         3921, createStateDynamic("minecraft:purple_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:purple_glazed_terracotta", FACING_WEST)
      );
      putStates(
         3922, createStateDynamic("minecraft:purple_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:purple_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         3923, createStateDynamic("minecraft:purple_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:purple_glazed_terracotta", FACING_EAST)
      );
      putStates(
         3936, createStateDynamic("minecraft:blue_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:blue_glazed_terracotta", FACING_SOUTH)
      );
      putStates(3937, createStateDynamic("minecraft:blue_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:blue_glazed_terracotta", FACING_WEST));
      putStates(
         3938, createStateDynamic("minecraft:blue_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:blue_glazed_terracotta", FACING_NORTH)
      );
      putStates(3939, createStateDynamic("minecraft:blue_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:blue_glazed_terracotta", FACING_EAST));
      putStates(
         3952, createStateDynamic("minecraft:brown_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:brown_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         3953, createStateDynamic("minecraft:brown_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:brown_glazed_terracotta", FACING_WEST)
      );
      putStates(
         3954, createStateDynamic("minecraft:brown_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:brown_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         3955, createStateDynamic("minecraft:brown_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:brown_glazed_terracotta", FACING_EAST)
      );
      putStates(
         3968, createStateDynamic("minecraft:green_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:green_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         3969, createStateDynamic("minecraft:green_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:green_glazed_terracotta", FACING_WEST)
      );
      putStates(
         3970, createStateDynamic("minecraft:green_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:green_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         3971, createStateDynamic("minecraft:green_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:green_glazed_terracotta", FACING_EAST)
      );
      putStates(3984, createStateDynamic("minecraft:red_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:red_glazed_terracotta", FACING_SOUTH));
      putStates(3985, createStateDynamic("minecraft:red_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:red_glazed_terracotta", FACING_WEST));
      putStates(3986, createStateDynamic("minecraft:red_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:red_glazed_terracotta", FACING_NORTH));
      putStates(3987, createStateDynamic("minecraft:red_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:red_glazed_terracotta", FACING_EAST));
      putStates(
         4000, createStateDynamic("minecraft:black_glazed_terracotta", FACING_SOUTH), createStateDynamic("minecraft:black_glazed_terracotta", FACING_SOUTH)
      );
      putStates(
         4001, createStateDynamic("minecraft:black_glazed_terracotta", FACING_WEST), createStateDynamic("minecraft:black_glazed_terracotta", FACING_WEST)
      );
      putStates(
         4002, createStateDynamic("minecraft:black_glazed_terracotta", FACING_NORTH), createStateDynamic("minecraft:black_glazed_terracotta", FACING_NORTH)
      );
      putStates(
         4003, createStateDynamic("minecraft:black_glazed_terracotta", FACING_EAST), createStateDynamic("minecraft:black_glazed_terracotta", FACING_EAST)
      );
      putStates(4016, createStateDynamic("minecraft:white_concrete"), createStateDynamic("minecraft:concrete", COLOR_WHITE));
      putStates(4017, createStateDynamic("minecraft:orange_concrete"), createStateDynamic("minecraft:concrete", COLOR_ORANGE));
      putStates(4018, createStateDynamic("minecraft:magenta_concrete"), createStateDynamic("minecraft:concrete", COLOR_MAGENTA));
      putStates(4019, createStateDynamic("minecraft:light_blue_concrete"), createStateDynamic("minecraft:concrete", COLOR_LIGHT_BLUE));
      putStates(4020, createStateDynamic("minecraft:yellow_concrete"), createStateDynamic("minecraft:concrete", COLOR_YELLOW));
      putStates(4021, createStateDynamic("minecraft:lime_concrete"), createStateDynamic("minecraft:concrete", COLOR_LIME));
      putStates(4022, createStateDynamic("minecraft:pink_concrete"), createStateDynamic("minecraft:concrete", COLOR_PINK));
      putStates(4023, createStateDynamic("minecraft:gray_concrete"), createStateDynamic("minecraft:concrete", COLOR_GRAY));
      putStates(4024, createStateDynamic("minecraft:light_gray_concrete"), createStateDynamic("minecraft:concrete", COLOR_SILVER));
      putStates(4025, createStateDynamic("minecraft:cyan_concrete"), createStateDynamic("minecraft:concrete", COLOR_CYAN));
      putStates(4026, createStateDynamic("minecraft:purple_concrete"), createStateDynamic("minecraft:concrete", COLOR_PURPLE));
      putStates(4027, createStateDynamic("minecraft:blue_concrete"), createStateDynamic("minecraft:concrete", COLOR_BLUE));
      putStates(4028, createStateDynamic("minecraft:brown_concrete"), createStateDynamic("minecraft:concrete", COLOR_BROWN));
      putStates(4029, createStateDynamic("minecraft:green_concrete"), createStateDynamic("minecraft:concrete", COLOR_GREEN));
      putStates(4030, createStateDynamic("minecraft:red_concrete"), createStateDynamic("minecraft:concrete", COLOR_RED));
      putStates(4031, createStateDynamic("minecraft:black_concrete"), createStateDynamic("minecraft:concrete", COLOR_BLACK));
      putStates(4032, createStateDynamic("minecraft:white_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_WHITE));
      putStates(4033, createStateDynamic("minecraft:orange_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_ORANGE));
      putStates(4034, createStateDynamic("minecraft:magenta_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_MAGENTA));
      putStates(4035, createStateDynamic("minecraft:light_blue_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_LIGHT_BLUE));
      putStates(4036, createStateDynamic("minecraft:yellow_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_YELLOW));
      putStates(4037, createStateDynamic("minecraft:lime_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_LIME));
      putStates(4038, createStateDynamic("minecraft:pink_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_PINK));
      putStates(4039, createStateDynamic("minecraft:gray_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_GRAY));
      putStates(4040, createStateDynamic("minecraft:light_gray_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_SILVER));
      putStates(4041, createStateDynamic("minecraft:cyan_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_CYAN));
      putStates(4042, createStateDynamic("minecraft:purple_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_PURPLE));
      putStates(4043, createStateDynamic("minecraft:blue_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_BLUE));
      putStates(4044, createStateDynamic("minecraft:brown_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_BROWN));
      putStates(4045, createStateDynamic("minecraft:green_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_GREEN));
      putStates(4046, createStateDynamic("minecraft:red_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_RED));
      putStates(4047, createStateDynamic("minecraft:black_concrete_powder"), createStateDynamic("minecraft:concrete_powder", COLOR_BLACK));
      putStates(
         4080, createStateDynamic("minecraft:structure_block", Map.of("mode", "save")), createStateDynamic("minecraft:structure_block", Map.of("mode", "save"))
      );
      putStates(
         4081, createStateDynamic("minecraft:structure_block", Map.of("mode", "load")), createStateDynamic("minecraft:structure_block", Map.of("mode", "load"))
      );
      putStates(
         4082,
         createStateDynamic("minecraft:structure_block", Map.of("mode", "corner")),
         createStateDynamic("minecraft:structure_block", Map.of("mode", "corner"))
      );
      putStates(
         4083, createStateDynamic("minecraft:structure_block", Map.of("mode", "data")), createStateDynamic("minecraft:structure_block", Map.of("mode", "data"))
      );
   }

   static {
      OLD_STATE_TO_ID.defaultReturnValue(-1);
      putStatesFromBlocks0To15();
      putStatesFromBlocks16To31();
      putStatesFromBlocks32To47();
      putStatesFromBlocks48To51();
      putStatesFromBlocks52To63();
      putStatesFromBlocks64To79();
      putStatesFromBlocks80To95();
      putStatesFromBlocks96To111();
      putStatesFromBlocks112To127();
      putStatesFromBlocks128To143();
      putStatesFromBlocks144To159();
      putStatesFromBlocks160To175();
      putStatesFromBlocks176To191();
      putStatesFromBlocks192To207();
      putStatesFromBlocks208To223();
      putStatesFromBlocks224To239();
      putStatesFromBlocks240To255();
      fillEmptyStates();
   }
}
