import mods.researchtable.ResearchTable;

// ResearchTable CraftTweaker / ZenCode API coverage script.
// The suite intentionally owns the whole research registry during script reloads.
ResearchTable.removeAll();
ResearchTable.scoreIndicator("CRT score test: kills=%s, deaths=%s", "kills", "deaths");

var general = ResearchTable.addCategory(<item:minecraft:book>, "item.minecraft.book");
var conditions = ResearchTable.addCategory(<item:minecraft:chest>, "block.minecraft.chest");
var world = ResearchTable.addCategory(<item:minecraft:compass>, "item.minecraft.compass");
var rewards = ResearchTable.addCategory(<item:minecraft:emerald>, "item.minecraft.emerald");
var stages = ResearchTable.addCategory(<item:minecraft:experience_bottle>, "item.minecraft.experience_bottle");
var unnamed = ResearchTable.addCategory(<item:minecraft:barrier>);
var removedCategory = ResearchTable.addCategory(<item:minecraft:structure_void>, "block.minecraft.structure_void");

// Static remove(String) smoke test. This research should not appear after scripts load.
ResearchTable.builder("crt_removed_by_remove", removedCategory)
  .setTitle("CRT removed by remove()")
  .setDescription("If this appears, ResearchTable.remove(String) failed.")
  .setIcons(<item:minecraft:structure_void>)
  .build();
ResearchTable.remove("crt_removed_by_remove");

// builder(String, ResearchCategory), addCategory(IItemStack, optional String), build()
// Also covers default title / description / icon fallback.
ResearchTable.builder("crt_00_minimal_defaults", unnamed)
  .build();

// setTitle(String), setDescription(String), setIcons(IIngredient...)
ResearchTable.builder("crt_01_title_description_icons", general)
  .setTitle("CRT 01 - title, description, and rotating icons")
  .setDescription("Covers setTitle, setDescription, and setIcons with multiple item ingredients.")
  .setIcons(<item:minecraft:book>, <item:minecraft:paper>, <item:minecraft:map>)
  .build();

// setMaxCount(int)
ResearchTable.builder("crt_02_max_count", general)
  .setTitle("CRT 02 - max count")
  .setDescription("Covers setMaxCount(3). Complete this up to three times.")
  .setIcons(<item:minecraft:clock>)
  .setMaxCount(3)
  .build();

// setNoMaxCount()
ResearchTable.builder("crt_03_no_max_count", general)
  .setTitle("CRT 03 - no max count")
  .setDescription("Covers setNoMaxCount(). This research stays repeatable.")
  .setIcons(<item:minecraft:repeater>)
  .setNoMaxCount()
  .build();

// setRequiredResearches(String...)
ResearchTable.builder("crt_04_required_researches", general)
  .setTitle("CRT 04 - required researches")
  .setDescription("Covers setRequiredResearches. Requires CRT 01 and CRT 02.")
  .setIcons(<item:minecraft:lectern>)
  .setRequiredResearches("crt_01_title_description_icons", "crt_02_max_count")
  .build();

// setOptionalResearches(int, String...)
ResearchTable.builder("crt_05_optional_researches", general)
  .setTitle("CRT 05 - optional researches")
  .setDescription("Covers setOptionalResearches. Requires any two of CRT 01, CRT 02, and CRT 03.")
  .setIcons(<item:minecraft:writable_book>)
  .setOptionalResearches(2, "crt_01_title_description_icons", "crt_02_max_count", "crt_03_no_max_count")
  .build();

// setRequiredScore(String, String, int)
ResearchTable.builder("crt_06_required_score_min", general)
  .setTitle("CRT 06 - required score minimum")
  .setDescription("Covers setRequiredScore(score, failingText, min). Requires kills score to be at least 10.")
  .setIcons(<item:minecraft:target>)
  .setRequiredScore("kills", "researchtable.crt.score.kills_at_least_10", 10)
  .build();

// setRequiredScore(String, String, int, int)
ResearchTable.builder("crt_07_required_score_range", general)
  .setTitle("CRT 07 - required score range")
  .setDescription("Covers setRequiredScore(score, failingText, min, max). Requires deaths score from 0 to 5.")
  .setIcons(<item:minecraft:crossbow>)
  .setRequiredScore("deaths", "researchtable.crt.score.deaths_0_5", 0, 5)
  .build();

// addCondition(IIngredient...)
ResearchTable.builder("crt_08_add_condition_varargs", conditions)
  .setTitle("CRT 08 - item condition varargs")
  .setDescription("Covers addCondition(IIngredient...). Submit 2 coal and 3 charcoal.")
  .setIcons(<item:minecraft:coal>, <item:minecraft:charcoal>)
  .addCondition(<item:minecraft:coal> * 2, <item:minecraft:charcoal> * 3)
  .build();

// addCondition(IIngredient)
ResearchTable.builder("crt_09_add_condition_single", conditions)
  .setTitle("CRT 09 - item condition single")
  .setDescription("Covers addCondition(IIngredient). Submit 4 apples.")
  .setIcons(<item:minecraft:apple>)
  .addCondition([<item:minecraft:apple> * 4])
  .build();

// addCondition(IIngredient, String)
ResearchTable.builder("crt_10_add_condition_custom_name", conditions)
  .setTitle("CRT 10 - item condition custom name")
  .setDescription("Covers addCondition(IIngredient, String). The row should display the bread translation.")
  .setIcons(<item:minecraft:bread>)
  .addCondition(<item:minecraft:bread> * 2, "item.minecraft.bread")
  .build();

// addItemCondition(IIngredient)
ResearchTable.builder("crt_11_add_item_condition", conditions)
  .setTitle("CRT 11 - add item condition")
  .setDescription("Covers addItemCondition(IIngredient). Submit 8 iron ingots.")
  .setIcons(<item:minecraft:iron_ingot>)
  .addItemCondition(<item:minecraft:iron_ingot> * 8)
  .build();

// addItemCondition(IIngredient, String)
ResearchTable.builder("crt_12_add_item_condition_custom_name", conditions)
  .setTitle("CRT 12 - add item condition custom name")
  .setDescription("Covers addItemCondition(IIngredient, String). The row should display the gold ingot translation.")
  .setIcons(<item:minecraft:gold_ingot>)
  .addItemCondition(<item:minecraft:gold_ingot> * 6, "item.minecraft.gold_ingot")
  .build();

// addFluidCondition(IFluidStack)
ResearchTable.builder("crt_13_fluid_condition_stack_amount", conditions)
  .setTitle("CRT 13 - fluid condition from stack amount")
  .setDescription("Covers addFluidCondition(IFluidStack). Fill 1000 mB water.")
  .setIcons(<item:minecraft:water_bucket>)
  .addFluidCondition(<fluid:minecraft:water> * 1000)
  .build();

// addFluidCondition(IFluidStack, int)
ResearchTable.builder("crt_14_fluid_condition_explicit_amount", conditions)
  .setTitle("CRT 14 - fluid condition explicit amount")
  .setDescription("Covers addFluidCondition(IFluidStack, int). Fill 500 mB lava.")
  .setIcons(<item:minecraft:lava_bucket>)
  .addFluidCondition(<fluid:minecraft:lava> * 1000, 500)
  .build();

// addEnergyCondition(int)
ResearchTable.builder("crt_15_energy_condition_int", conditions)
  .setTitle("CRT 15 - energy condition int")
  .setDescription("Covers addEnergyCondition(int). Submit 1000 FE.")
  .setIcons(<item:minecraft:redstone>)
  .addEnergyCondition(1000)
  .build();

// addEnergyCondition(long)
ResearchTable.builder("crt_16_energy_condition_long", conditions)
  .setTitle("CRT 16 - energy condition long")
  .setDescription("Covers addEnergyCondition(long). Submit 3000000000 FE.")
  .setIcons(<item:minecraft:redstone_block>)
  .addEnergyCondition(3000000000)
  .build();

// setRequiredBiomes(String...)
ResearchTable.builder("crt_17_required_biomes", world)
  .setTitle("CRT 17 - required biomes")
  .setDescription("Covers setRequiredBiomes. Stand in plains or forest.")
  .setIcons(<item:minecraft:grass_block>)
  .setRequiredBiomes("minecraft:plains", "minecraft:forest")
  .build();

// setRequiredDimensions(String...)
ResearchTable.builder("crt_18_required_dimensions", world)
  .setTitle("CRT 18 - required dimensions")
  .setDescription("Covers setRequiredDimensions. Requires the overworld.")
  .setIcons(<item:minecraft:compass>)
  .setRequiredDimensions("minecraft:overworld")
  .build();

// setRequiredWeather(String) for clear.
ResearchTable.builder("crt_19_required_weather_clear", world)
  .setTitle("CRT 19 - required weather clear")
  .setDescription("Covers setRequiredWeather(\"clear\").")
  .setIcons(<item:minecraft:sunflower>)
  .setRequiredWeather("clear")
  .build();

// setRequiredWeather(String) for rain.
ResearchTable.builder("crt_20_required_weather_rain", world)
  .setTitle("CRT 20 - required weather rain")
  .setDescription("Covers setRequiredWeather(\"rain\").")
  .setIcons(<item:minecraft:water_bucket>)
  .setRequiredWeather("rain")
  .build();

// setRequiredWeather(String) for thunder.
ResearchTable.builder("crt_21_required_weather_thunder", world)
  .setTitle("CRT 21 - required weather thunder")
  .setDescription("Covers setRequiredWeather(\"thunder\").")
  .setIcons(<item:minecraft:lightning_rod>)
  .setRequiredWeather("thunder")
  .build();

// setRequiredTime(int, int), normal daytime range.
ResearchTable.builder("crt_22_required_time_day", world)
  .setTitle("CRT 22 - required time day")
  .setDescription("Covers setRequiredTime with a normal non-wrapping range.")
  .setIcons(<item:minecraft:clock>)
  .setRequiredTime(0, 12000)
  .build();

// setRequiredTime(int, int), wrapping midnight range.
ResearchTable.builder("crt_23_required_time_night_wrap", world)
  .setTitle("CRT 23 - required time night wrap")
  .setDescription("Covers setRequiredTime with a wrapping range.")
  .setIcons(<item:minecraft:crying_obsidian>)
  .setRequiredTime(13000, 23000)
  .build();

// addXPCondition(int)
ResearchTable.builder("crt_24_xp_condition", conditions)
  .setTitle("CRT 24 - XP condition")
  .setDescription("Covers addXPCondition. Submit 50 experience points.")
  .setIcons(<item:minecraft:experience_bottle>)
  .addXPCondition(50)
  .build();

// setRewardCommands(String...)
ResearchTable.builder("crt_25_reward_commands", rewards)
  .setTitle("CRT 25 - reward commands")
  .setDescription("Covers setRewardCommands with multiple commands.")
  .setIcons(<item:minecraft:command_block>)
  .setRewardCommands("/say CRT reward command 1", "/say CRT reward command 2")
  .build();

// setRewardItems(IItemStack...)
ResearchTable.builder("crt_26_reward_items", rewards)
  .setTitle("CRT 26 - reward items")
  .setDescription("Covers setRewardItems with multiple item stacks.")
  .setIcons(<item:minecraft:diamond>)
  .setRewardItems(<item:minecraft:diamond> * 2, <item:minecraft:emerald> * 4)
  .build();

// setTriggerCommands(String...)
ResearchTable.builder("crt_27_trigger_commands", rewards)
  .setTitle("CRT 27 - trigger commands")
  .setDescription("Covers setTriggerCommands. The command runs when research starts.")
  .setIcons(<item:minecraft:chain_command_block>)
  .setTriggerCommands("/say CRT trigger command 1", "/say CRT trigger command 2")
  .build();

// setTriggerItems(IItemStack...)
ResearchTable.builder("crt_28_trigger_items", rewards)
  .setTitle("CRT 28 - trigger items")
  .setDescription("Covers setTriggerItems. Items are granted when research starts.")
  .setIcons(<item:minecraft:golden_apple>)
  .setTriggerItems(<item:minecraft:cookie> * 3, <item:minecraft:honey_bottle>)
  .build();

// setRewardXP(int)
ResearchTable.builder("crt_29_reward_xp_points", rewards)
  .setTitle("CRT 29 - reward XP points")
  .setDescription("Covers setRewardXP.")
  .setIcons(<item:minecraft:experience_bottle>)
  .setRewardXP(100)
  .build();

// setRewardXPLevels(int)
ResearchTable.builder("crt_30_reward_xp_levels", rewards)
  .setTitle("CRT 30 - reward XP levels")
  .setDescription("Covers setRewardXPLevels.")
  .setIcons(<item:minecraft:enchanted_book>)
  .setRewardXPLevels(2)
  .build();

// setTriggerXP(int)
ResearchTable.builder("crt_31_trigger_xp_points", rewards)
  .setTitle("CRT 31 - trigger XP points")
  .setDescription("Covers setTriggerXP.")
  .setIcons(<item:minecraft:glass_bottle>)
  .setTriggerXP(25)
  .build();

// setTriggerXPLevels(int)
ResearchTable.builder("crt_32_trigger_xp_levels", rewards)
  .setTitle("CRT 32 - trigger XP levels")
  .setDescription("Covers setTriggerXPLevels.")
  .setIcons(<item:minecraft:lapis_lazuli>)
  .setTriggerXPLevels(1)
  .build();

// setRequiredStages(String...)
ResearchTable.builder("crt_33_required_stages", stages)
  .setTitle("CRT 33 - required stages")
  .setDescription("Covers setRequiredStages. Requires AStages stages crt_stage_alpha and crt_stage_beta.")
  .setIcons(<item:minecraft:amethyst_shard>)
  .setRequiredStages("crt_stage_alpha", "crt_stage_beta")
  .build();

// setOptionalStages(int, String...)
ResearchTable.builder("crt_34_optional_stages", stages)
  .setTitle("CRT 34 - optional stages")
  .setDescription("Covers setOptionalStages. Requires any two of three AStages stages.")
  .setIcons(<item:minecraft:echo_shard>)
  .setOptionalStages(2, "crt_stage_alpha", "crt_stage_beta", "crt_stage_gamma")
  .build();

// setRewardStages(String...)
ResearchTable.builder("crt_35_reward_stages", stages)
  .setTitle("CRT 35 - reward stages")
  .setDescription("Covers setRewardStages. Completing grants crt_stage_alpha and crt_stage_beta.")
  .setIcons(<item:minecraft:nether_star>)
  .setRewardStages("crt_stage_alpha", "crt_stage_beta")
  .build();

// setTriggerStages(String...)
ResearchTable.builder("crt_36_trigger_stages", stages)
  .setTitle("CRT 36 - trigger stages")
  .setDescription("Covers setTriggerStages. Starting grants crt_stage_gamma.")
  .setIcons(<item:minecraft:ender_eye>)
  .setTriggerStages("crt_stage_gamma")
  .build();

// Combined integration smoke test with prerequisites, world gates, submit costs, triggers, and rewards.
ResearchTable.builder("crt_37_full_chain_smoke", rewards)
  .setTitle("CRT 37 - full chain smoke test")
  .setDescription("Combines research prerequisites, world criteria, item/fluid/energy/XP costs, trigger rewards, and completion rewards.")
  .setIcons(<item:minecraft:netherite_ingot>, <item:minecraft:diamond>, <item:minecraft:emerald>)
  .setRequiredResearches("crt_01_title_description_icons")
  .setOptionalResearches(1, "crt_02_max_count", "crt_03_no_max_count")
  .setRequiredDimensions("minecraft:overworld")
  .setRequiredWeather("clear")
  .setRequiredTime(0, 23999)
  .addItemCondition(<item:minecraft:copper_ingot> * 4, "item.minecraft.copper_ingot")
  .addFluidCondition(<fluid:minecraft:water> * 250)
  .addEnergyCondition(250)
  .addXPCondition(10)
  .setTriggerItems(<item:minecraft:bread>)
  .setTriggerXP(5)
  .setRewardItems(<item:minecraft:emerald> * 1)
  .setRewardXP(20)
  .setRewardCommands("/say CRT full chain smoke test completed")
  .setNoMaxCount()
  .build();
