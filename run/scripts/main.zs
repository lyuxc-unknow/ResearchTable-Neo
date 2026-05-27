import mods.researchtable.ResearchTable;

var cap1 = ResearchTable.addCategory(<item:minecraft:grass_block>, "hello");
var cap2 = ResearchTable.addCategory(<item:minecraft:dirt>, "dirt");

ResearchTable.builder("testResearch1", cap1)
  .setIcons(<item:minecraft:grass_block> * 5)
  .setTitle("研究1标题") // I18n support: use language key
  .setDescription("研究1描述")
  .setRequiredBiomes("minecraft:nether_wastes")
  .setRequiredDimensions("minecraft:the_nether")
  .setRequiredWeather("clear")
  .addXPCondition(10000)
  .setRewardCommands("/tellraw @a {\"text\":\"wow, \",\"extra\":[{\"selector\":\"@s\"},{\"text\":\" has found a gold!\"}]}")
  .setRewardItems(<item:minecraft:gold_ingot> * 64)
  .build();

ResearchTable.builder("testResearch2", cap2)
  .setIcons(<item:minecraft:gold_ingot>)
  .setTitle("研究2标题") // I18n support: use language key
  .setDescription("研究2描述")
  .setRequiredResearches("testResearch1")
  .setRequiredStages("world")
  .setRewardStages("hello")
  .setMaxCount(10)
  .build();
