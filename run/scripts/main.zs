import mods.researchtable.ResearchTable;

var cap1 = ResearchTable.addCategory(<item:minecraft:grass_block>, "hello");

ResearchTable.builder("testResearch1", cap1)
  .setIcons(<item:minecraft:grass_block> * 5)
  .setTitle("mind2.title1") // I18n support: use language key
  .setDescription("mind2.description")
  .setRequiredBiomes("minecraft:nether_wastes")
  .setRequiredDimensions("minecraft:the_nether")
  .setRequiredWeather("clear")
  .addXPCondition(10000)
  .setRewardCommands("/tellraw @a {\"text\":\"wow, \",\"extra\":[{\"selector\":\"@s\"},{\"text\":\" has found a gold!\"}]}")
  .setRewardItems(<item:minecraft:gold_ingot> * 64)
  .build();
