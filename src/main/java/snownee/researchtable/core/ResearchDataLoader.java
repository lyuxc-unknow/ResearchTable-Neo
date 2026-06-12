package snownee.researchtable.core;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.CompoundFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.api.ICriterion;
import snownee.researchtable.api.IReward;
import snownee.researchtable.plugin.astage.CriterionStages;
import snownee.researchtable.plugin.astage.RewardUnlockStages;
import snownee.researchtable.plugin.forge.ConditionForgeEnergy;
import snownee.researchtable.plugin.minecraft.ConditionExperience;
import snownee.researchtable.plugin.minecraft.ConditionFluid;
import snownee.researchtable.plugin.minecraft.ConditionItem;
import snownee.researchtable.plugin.minecraft.CriterionBiome;
import snownee.researchtable.plugin.minecraft.CriterionDimension;
import snownee.researchtable.plugin.minecraft.CriterionTime;
import snownee.researchtable.plugin.minecraft.CriterionWeather;
import snownee.researchtable.plugin.minecraft.RewardExperience;

public final class ResearchDataLoader {
	private static final String CATEGORY_DIR = "researchtable/categories";
	private static final String RESEARCH_DIR = "researchtable/researches";
	private static final String SCORE_DIR = "researchtable/score_indicators";
	private static final ItemStack DEFAULT_CATEGORY_ICON = new ItemStack(Items.BOOK);

	private ResearchDataLoader() {
	}

	public record Result(List<Research> researches, @Nullable String scoreFormattingText, String[] scores) {
		public Result {
			researches = List.copyOf(researches);
			scores = scores.clone();
		}
	}

	public static Result load(ResourceManager resourceManager) {
		Context context = new Context();
		loadCategories(resourceManager, context);
		ScoreIndicator scoreIndicator = loadScoreIndicator(resourceManager);
		List<Research> researches = loadResearches(resourceManager, context);
		return new Result(
				researches,
				scoreIndicator == null ? null : scoreIndicator.formattingText,
				scoreIndicator == null ? new String[0] : scoreIndicator.scores);
	}

	private static void loadCategories(ResourceManager resourceManager, Context context) {
		Map<ResourceLocation, Resource> resources = resourceManager.listResources(
				CATEGORY_DIR,
				location -> location.getPath().endsWith(".json"));
		resources.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
				.forEach(entry -> {
					try {
						JsonObject json = readObject(entry.getValue());
						ResourceLocation id = relativeId(entry.getKey(), CATEGORY_DIR);
						context.categories.put(id, parseCategory(json, id));
					} catch (Exception e) {
						ResearchTable.LOGGER.error("Failed to load research category {}", entry.getKey(), e);
					}
				});
	}

	@Nullable
	private static ScoreIndicator loadScoreIndicator(ResourceManager resourceManager) {
		Map<ResourceLocation, Resource> resources = resourceManager.listResources(
				SCORE_DIR,
				location -> location.getPath().endsWith(".json"));
		ScoreIndicator scoreIndicator = null;
		for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
				.toList()) {
			try {
				JsonObject json = readObject(entry.getValue());
				String formattingText = getString(json, "formatting_text", getString(json, "format", null));
				if (formattingText == null) {
					throw new JsonParseException("Missing required field 'formatting_text'");
				}
				scoreIndicator = new ScoreIndicator(formattingText, getStringArray(json, "scores"));
			} catch (Exception e) {
				ResearchTable.LOGGER.error("Failed to load research score indicator {}", entry.getKey(), e);
			}
		}
		return scoreIndicator;
	}

	private static List<Research> loadResearches(ResourceManager resourceManager, Context context) {
		Map<ResourceLocation, Resource> resources = resourceManager.listResources(
				RESEARCH_DIR,
				location -> location.getPath().endsWith(".json"));
		List<Research> researches = new ArrayList<>(resources.size());
		resources.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
				.forEach(entry -> {
					try {
						JsonObject json = readObject(entry.getValue());
						String name = getString(json, "name", relativeId(entry.getKey(), RESEARCH_DIR).toString());
						researches.add(parseResearch(name, json, context));
					} catch (Exception e) {
						ResearchTable.LOGGER.error("Failed to load research {}", entry.getKey(), e);
					}
				});
		return researches;
	}

	private static Research parseResearch(String name, JsonObject json, Context context) {
		ResearchCategory category = parseResearchCategory(json.get("category"), context);
		String title = getString(json, "title", ResearchTable.MODID + ".title.missing");
		String description = getString(json, "description", ResearchTable.MODID + ".description.missing");
		List<ItemStack> icons = parseItemStackList(json.get("icons"));
		List<ICriterion> criteria = parseCriteria(json);
		List<ICondition<?>> conditions = parseConditions(json.get("conditions"));
		List<IReward> triggers = parseRewards(json.get("triggers"));
		List<IReward> rewards = parseRewards(json.get("rewards"));

		int maxCount = getInt(json, "max_count", getInt(json, "maxCount", 1));
		if (maxCount > 0) {
			criteria.add(new CriterionResearchCount(name, maxCount));
		}

		return new Research(name, category, title, description, criteria, triggers, rewards, conditions, icons);
	}

	private static ResearchCategory parseResearchCategory(@Nullable JsonElement element, Context context) {
		if (element == null || element.isJsonNull()) {
			return context.categories.computeIfAbsent(
					ResearchTable.id("default"),
					id -> new ResearchCategory(DEFAULT_CATEGORY_ICON, "item.minecraft.book"));
		}
		if (element.isJsonPrimitive()) {
			ResourceLocation id = parseId(element.getAsString());
			ResearchCategory category = context.categories.get(id);
			if (category == null) {
				throw new JsonParseException("Unknown research category: " + id);
			}
			return category;
		}
		if (!element.isJsonObject()) {
			throw new JsonParseException("Expected category to be a string or object");
		}
		JsonObject json = element.getAsJsonObject();
		ResourceLocation id = json.has("id") ? parseId(json.get("id").getAsString()) : null;
		ResearchCategory category = parseCategory(json, id);
		if (id != null) {
			context.categories.put(id, category);
		}
		return category;
	}

	private static ResearchCategory parseCategory(JsonObject json, @Nullable ResourceLocation id) {
		ItemStack icon = json.has("icon") ? parseItemStack(json.get("icon"), 1) : DEFAULT_CATEGORY_ICON;
		String name = getString(json, "name", id == null ? null : id.toLanguageKey("researchtable.category"));
		return new ResearchCategory(icon, name);
	}

	private static List<ICriterion> parseCriteria(JsonObject json) {
		List<ICriterion> criteria = new ArrayList<>();
		JsonObject source = json.has("criteria") && json.get("criteria").isJsonObject()
				? json.getAsJsonObject("criteria")
				: json;

		addResearchCriteria(criteria, source);
		addScoreCriteria(criteria, source);
		if (source.has("biomes")) {
			criteria.add(new CriterionBiome(parseIdSet(source.get("biomes"))));
		}
		if (source.has("dimensions")) {
			criteria.add(new CriterionDimension(parseIdSet(source.get("dimensions"))));
		}
		if (source.has("weather")) {
			criteria.add(new CriterionWeather(parseWeather(source.get("weather").getAsString())));
		}
		if (source.has("time")) {
			JsonObject time = expectObject(source.get("time"), "time");
			criteria.add(new CriterionTime(getInt(time, "min", 0), getInt(time, "max", 23999)));
		}
		if (ModList.get().isLoaded("astages")) {
			addStageCriteria(criteria, source);
		}
		return criteria;
	}

	private static void addResearchCriteria(List<ICriterion> criteria, JsonObject source) {
		if (source.has("required_researches")) {
			Set<String> researches = parseStringSet(source.get("required_researches"));
			criteria.add(new CriterionResearches(researches, researches.size()));
		}
		if (source.has("optional_researches")) {
			JsonObject optional = expectObject(source.get("optional_researches"), "optional_researches");
			criteria.add(new CriterionResearches(parseStringSet(optional.get("researches")), getInt(optional, "amount", 1)));
		}
	}

	private static void addScoreCriteria(List<ICriterion> criteria, JsonObject source) {
		if (source.has("score")) {
			criteria.add(parseScoreCriterion(expectObject(source.get("score"), "score")));
		}
		if (source.has("scores")) {
			for (JsonElement element : expectArray(source.get("scores"), "scores")) {
				criteria.add(parseScoreCriterion(expectObject(element, "scores[]")));
			}
		}
	}

	private static ICriterion parseScoreCriterion(JsonObject json) {
		String score = requireString(json, "score");
		String failingText = getString(json, "failing_text", getString(json, "failingText", ""));
		int min = getInt(json, "min", Integer.MIN_VALUE);
		int max = getInt(json, "max", Integer.MAX_VALUE);
		if (max < min) {
			max = min;
		}
		return new CriterionScore(score, min, max, failingText);
	}

	private static void addStageCriteria(List<ICriterion> criteria, JsonObject source) {
		if (source.has("required_stages")) {
			Set<String> stages = parseStringSet(source.get("required_stages"));
			criteria.add(new CriterionStages(stages, stages.size()));
		}
		if (source.has("optional_stages")) {
			JsonObject optional = expectObject(source.get("optional_stages"), "optional_stages");
			criteria.add(new CriterionStages(parseStringSet(optional.get("stages")), getInt(optional, "amount", 1)));
		}
	}

	private static List<ICondition<?>> parseConditions(@Nullable JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return List.of();
		}
		List<ICondition<?>> conditions = new ArrayList<>();
		for (JsonElement entry : expectArray(element, "conditions")) {
			JsonObject json = expectObject(entry, "conditions[]");
			String type = requireString(json, "type").toLowerCase(Locale.ROOT);
			switch (type) {
				case "item" -> conditions.add(parseItemCondition(json));
				case "fluid" -> conditions.add(parseFluidCondition(json));
				case "energy", "forge_energy" -> conditions.add(new ConditionForgeEnergy(getLong(json, "amount", getLong(json, "count", 0))));
				case "xp", "experience" -> conditions.add(new ConditionExperience(getInt(json, "amount", getInt(json, "count", 0))));
				default -> throw new JsonParseException("Unknown condition type: " + type);
			}
		}
		return conditions;
	}

	private static ICondition<?> parseItemCondition(JsonObject json) {
		JsonElement ingredientElement = json.has("ingredient") ? json.get("ingredient") : json.get("item");
		if (ingredientElement == null) {
			throw new JsonParseException("Item condition requires 'ingredient' or 'item'");
		}
		Ingredient ingredient = parseIngredient(ingredientElement);
		long fallback = getFirstItemCount(ingredient);
		if (ingredientElement.isJsonObject()) {
			JsonObject ingredientJson = ingredientElement.getAsJsonObject();
			fallback = getLong(ingredientJson, "amount", getLong(ingredientJson, "count", fallback));
		}
		long amount = getLong(json, "amount", getLong(json, "count", fallback));
		ConditionItem condition = new ConditionItem(ingredient, amount);
		String customName = getString(json, "name", getString(json, "custom_name", null));
		if (customName != null && !customName.isEmpty()) {
			condition.setCustomName(customName);
		}
		return condition;
	}

	private static ICondition<?> parseFluidCondition(JsonObject json) {
		JsonElement fluidElement = json.has("ingredient") ? json.get("ingredient") : json.get("fluid");
		if (fluidElement == null) {
			throw new JsonParseException("Fluid condition requires 'ingredient' or 'fluid'");
		}
		FluidIngredient ingredient = parseFluidIngredient(fluidElement);
		long fallback = FluidType.BUCKET_VOLUME;
		if (fluidElement.isJsonObject()) {
			JsonObject fluidJson = fluidElement.getAsJsonObject();
			fallback = getLong(fluidJson, "amount", getLong(fluidJson, "count", fallback));
		}
		long amount = getLong(json, "amount", getLong(json, "count", fallback));
		return new ConditionFluid(ingredient, amount);
	}

	private static List<IReward> parseRewards(@Nullable JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return List.of();
		}
		List<IReward> rewards = new ArrayList<>();
		for (JsonElement entry : expectArray(element, "rewards/triggers")) {
			JsonObject json = expectObject(entry, "rewards/triggers[]");
			String type = requireString(json, "type").toLowerCase(Locale.ROOT);
			switch (type) {
				case "command", "commands" -> rewards.add(new RewardExecute(getStringArray(json, json.has("commands") ? "commands" : "command")));
				case "item", "items" -> rewards.add(new RewardItems(parseRewardItems(json.has("items") ? json.get("items") : json.get("item"))));
				case "xp", "experience" -> rewards.add(new RewardExperience(getInt(json, "amount", getInt(json, "points", 0)), false));
				case "xp_levels", "experience_levels", "levels" -> rewards.add(new RewardExperience(getInt(json, "amount", getInt(json, "levels", 0)), true));
				case "stages" -> {
					if (!ModList.get().isLoaded("astages")) {
						throw new JsonParseException("Reward type 'stages' requires AStages");
					}
					rewards.add(new RewardUnlockStages(getStringArray(json, "stages")));
				}
				default -> throw new JsonParseException("Unknown reward/trigger type: " + type);
			}
		}
		return rewards;
	}

	private static NonNullList<ItemStack> parseRewardItems(@Nullable JsonElement element) {
		NonNullList<ItemStack> items = NonNullList.create();
		if (element == null || element.isJsonNull()) {
			return items;
		}
		for (ItemStack stack : parseItemStackList(element)) {
			if (!stack.isEmpty()) {
				items.add(stack.copy());
			}
		}
		return items;
	}

	private static Ingredient parseIngredient(JsonElement element) {
		if (element.isJsonArray()) {
			List<Ingredient> ingredients = new ArrayList<>();
			for (JsonElement child : element.getAsJsonArray()) {
				Ingredient ingredient = parseIngredient(child);
				if (!ingredient.isEmpty()) {
					ingredients.add(ingredient);
				}
			}
			return switch (ingredients.size()) {
				case 0 -> Ingredient.EMPTY;
				case 1 -> ingredients.getFirst();
				default -> new CompoundIngredient(ingredients).toVanilla();
			};
		}
		if (element.isJsonPrimitive()) {
			String value = element.getAsString();
			if (value.startsWith("#")) {
				return Ingredient.of(ItemTags.create(parseId(value.substring(1))));
			}
			return Ingredient.of(parseItem(value));
		}
		JsonObject json = expectObject(element, "ingredient");
		if (json.has("ingredient")) {
			return parseIngredient(json.get("ingredient"));
		}
		if (json.has("ingredients")) {
			return parseIngredient(json.get("ingredients"));
		}
		if (json.has("items")) {
			return parseIngredient(json.get("items"));
		}
		if (json.has("tag")) {
			return Ingredient.of(ItemTags.create(parseId(json.get("tag").getAsString())));
		}
		if (json.has("item")) {
			return Ingredient.of(parseItem(json.get("item").getAsString()));
		}
		throw new JsonParseException("Expected item ingredient");
	}

	private static FluidIngredient parseFluidIngredient(JsonElement element) {
		if (element.isJsonArray()) {
			List<FluidIngredient> ingredients = new ArrayList<>();
			for (JsonElement child : element.getAsJsonArray()) {
				FluidIngredient ingredient = parseFluidIngredient(child);
				if (!ingredient.isEmpty()) {
					ingredients.add(ingredient);
				}
			}
			return CompoundFluidIngredient.of(ingredients);
		}
		if (element.isJsonPrimitive()) {
			String value = element.getAsString();
			if (value.startsWith("#")) {
				return FluidIngredient.tag(net.minecraft.tags.FluidTags.create(parseId(value.substring(1))));
			}
			return FluidIngredient.of(parseFluid(value));
		}
		JsonObject json = expectObject(element, "fluid ingredient");
		if (json.has("ingredient")) {
			return parseFluidIngredient(json.get("ingredient"));
		}
		if (json.has("ingredients")) {
			return parseFluidIngredient(json.get("ingredients"));
		}
		if (json.has("fluids")) {
			return parseFluidIngredient(json.get("fluids"));
		}
		if (json.has("tag")) {
			return FluidIngredient.tag(net.minecraft.tags.FluidTags.create(parseId(json.get("tag").getAsString())));
		}
		if (json.has("fluid")) {
			return FluidIngredient.of(parseFluid(json.get("fluid").getAsString()));
		}
		throw new JsonParseException("Expected fluid ingredient");
	}

	private static List<ItemStack> parseItemStackList(@Nullable JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return List.of();
		}
		List<ItemStack> items = new ArrayList<>();
		if (element.isJsonArray()) {
			for (JsonElement child : element.getAsJsonArray()) {
				ItemStack stack = parseItemStack(child, 1);
				if (!stack.isEmpty()) {
					items.add(stack);
				}
			}
		} else {
			ItemStack stack = parseItemStack(element, 1);
			if (!stack.isEmpty()) {
				items.add(stack);
			}
		}
		return items;
	}

	private static ItemStack parseItemStack(JsonElement element, int defaultCount) {
		if (element == null || element.isJsonNull()) {
			return ItemStack.EMPTY;
		}
		if (element.isJsonPrimitive()) {
			ParsedStack parsed = parseStackString(element.getAsString(), defaultCount);
			return new ItemStack(parseItem(parsed.id), parsed.count);
		}
		JsonObject json = expectObject(element, "item stack");
		String id = getString(json, "item", getString(json, "id", null));
		if (id == null) {
			throw new JsonParseException("Item stack requires 'item' or 'id'");
		}
		return new ItemStack(parseItem(id), getInt(json, "count", defaultCount));
	}

	private static ParsedStack parseStackString(String value, int defaultCount) {
		String s = value.trim();
		int count = defaultCount;
		int x = s.indexOf('x');
		if (x > 0) {
			String before = s.substring(0, x).trim();
			if (before.chars().allMatch(Character::isDigit)) {
				count = Integer.parseInt(before);
				s = s.substring(x + 1).trim();
			}
		}
		return new ParsedStack(s, Math.max(0, count));
	}

	private static long getFirstItemCount(Ingredient ingredient) {
		ItemStack[] items = ingredient.getItems();
		if (items.length == 0) {
			throw new JsonParseException("Ingredient has no matching items");
		}
		return Math.max(1, items[0].getCount());
	}

	private static Item parseItem(String id) {
		ResourceLocation location = parseId(id);
		if (!BuiltInRegistries.ITEM.containsKey(location)) {
			throw new JsonParseException("Unknown item: " + location);
		}
		Item item = BuiltInRegistries.ITEM.get(location);
		if (item == Items.AIR && !location.equals(ResourceLocation.withDefaultNamespace("air"))) {
			throw new JsonParseException("Unknown item: " + location);
		}
		return item;
	}

	private static Fluid parseFluid(String id) {
		ResourceLocation location = parseId(id);
		if (!BuiltInRegistries.FLUID.containsKey(location)) {
			throw new JsonParseException("Unknown fluid: " + location);
		}
		Fluid fluid = BuiltInRegistries.FLUID.get(location);
		if (fluid == Fluids.EMPTY && !location.equals(ResourceLocation.withDefaultNamespace("empty"))) {
			throw new JsonParseException("Unknown fluid: " + location);
		}
		return fluid;
	}

	private static Set<ResourceLocation> parseIdSet(JsonElement element) {
		Set<ResourceLocation> ids = new LinkedHashSet<>();
		for (String value : parseStringSet(element)) {
			ids.add(parseId(value));
		}
		return ids;
	}

	private static Set<String> parseStringSet(@Nullable JsonElement element) {
		Set<String> values = new LinkedHashSet<>();
		if (element == null || element.isJsonNull()) {
			return values;
		}
		if (element.isJsonArray()) {
			for (JsonElement child : element.getAsJsonArray()) {
				values.add(child.getAsString());
			}
		} else {
			values.add(element.getAsString());
		}
		return values;
	}

	private static CriterionWeather.Weather parseWeather(String value) {
		try {
			return CriterionWeather.Weather.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new JsonParseException("Unknown weather '" + value + "', expected clear, rain, or thunder");
		}
	}

	private static JsonObject readObject(Resource resource) throws Exception {
		try (Reader reader = resource.openAsReader(); JsonReader jsonReader = new JsonReader(reader)) {
			JsonElement element = com.google.gson.JsonParser.parseReader(jsonReader);
			return expectObject(element, "root");
		}
	}

	private static ResourceLocation relativeId(ResourceLocation location, String prefix) {
		String path = location.getPath();
		if (!path.startsWith(prefix + "/") || !path.endsWith(".json")) {
			throw new JsonParseException("Unexpected path: " + location);
		}
		String relative = path.substring(prefix.length() + 1, path.length() - ".json".length());
		return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), relative);
	}

	private static ResourceLocation parseId(String id) {
		ResourceLocation location = ResourceLocation.tryParse(id);
		if (location == null) {
			throw new JsonParseException("Invalid resource location: " + id);
		}
		return location;
	}

	private static JsonObject expectObject(JsonElement element, String name) {
		if (element == null || !element.isJsonObject()) {
			throw new JsonParseException("Expected " + name + " to be an object");
		}
		return element.getAsJsonObject();
	}

	private static JsonArray expectArray(JsonElement element, String name) {
		if (element == null || !element.isJsonArray()) {
			throw new JsonParseException("Expected " + name + " to be an array");
		}
		return element.getAsJsonArray();
	}

	private static String requireString(JsonObject json, String key) {
		String value = getString(json, key, null);
		if (value == null) {
			throw new JsonParseException("Missing required field '" + key + "'");
		}
		return value;
	}

	@Nullable
	private static String getString(JsonObject json, String key, @Nullable String fallback) {
		JsonElement element = json.get(key);
		return element instanceof JsonPrimitive primitive ? primitive.getAsString() : fallback;
	}

	private static String[] getStringArray(JsonObject json, String key) {
		JsonElement element = json.get(key);
		if (element == null || element.isJsonNull()) {
			return new String[0];
		}
		List<String> strings = new ArrayList<>();
		if (element.isJsonArray()) {
			for (JsonElement child : element.getAsJsonArray()) {
				strings.add(child.getAsString());
			}
		} else {
			strings.add(element.getAsString());
		}
		return strings.toArray(String[]::new);
	}

	private static int getInt(JsonObject json, String key, int fallback) {
		JsonElement element = json.get(key);
		return element instanceof JsonPrimitive primitive ? primitive.getAsInt() : fallback;
	}

	private static long getLong(JsonObject json, String key, long fallback) {
		JsonElement element = json.get(key);
		return element instanceof JsonPrimitive primitive ? primitive.getAsLong() : fallback;
	}

	private static final class Context {
		private final Map<ResourceLocation, ResearchCategory> categories = new LinkedHashMap<>();
	}

	private record ParsedStack(String id, int count) {
	}

	private record ScoreIndicator(String formattingText, String[] scores) {
	}
}
