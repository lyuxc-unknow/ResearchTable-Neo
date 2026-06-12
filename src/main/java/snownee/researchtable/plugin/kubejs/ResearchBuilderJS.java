package snownee.researchtable.plugin.kubejs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import dev.latvian.mods.kubejs.fluid.FluidWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.SizedIngredientWrapper;
import dev.latvian.mods.kubejs.util.ListJS;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.api.ICriterion;
import snownee.researchtable.api.IReward;
import snownee.researchtable.core.CriterionResearchCount;
import snownee.researchtable.core.CriterionResearches;
import snownee.researchtable.core.CriterionScore;
import snownee.researchtable.core.Research;
import snownee.researchtable.core.ResearchCategory;
import snownee.researchtable.core.ResearchList;
import snownee.researchtable.core.RewardExecute;
import snownee.researchtable.core.RewardItems;
import snownee.researchtable.plugin.astage.CriterionStages;
import snownee.researchtable.plugin.astage.RewardUnlockStages;
import snownee.researchtable.plugin.forge.ConditionForgeEnergy;
import snownee.researchtable.plugin.minecraft.ConditionExperience;
import snownee.researchtable.plugin.minecraft.CriterionBiome;
import snownee.researchtable.plugin.minecraft.CriterionDimension;
import snownee.researchtable.plugin.minecraft.CriterionTime;
import snownee.researchtable.plugin.minecraft.CriterionWeather;
import snownee.researchtable.plugin.minecraft.RewardExperience;

public class ResearchBuilderJS {
	private static final String KEY_NO_TITLE = ResearchTable.MODID + ".title.missing";
	private static final String KEY_NO_DESCRIPTION = ResearchTable.MODID + ".description.missing";

	private final String name;
	private final ResearchCategory category;
	private final List<ICriterion> criteria = new ArrayList<>();
	private final List<IReward> triggers = new ArrayList<>();
	private final List<IReward> rewards = new ArrayList<>();
	private final List<ICondition<?>> conditions = new ArrayList<>(4);
	@Nullable
	private List<ItemStack> icons;
	private String title;
	private String description;
	private int maxCount = 1;

	public ResearchBuilderJS(String name, ResearchCategoryJS category) {
		this.name = name;
		this.category = category.category;
	}

	public ResearchBuilderJS addCriterion(ICriterion criterion) {
		criteria.add(Objects.requireNonNull(criterion, "criterion"));
		return this;
	}

	public ResearchBuilderJS addTrigger(IReward trigger) {
		triggers.add(Objects.requireNonNull(trigger, "trigger"));
		return this;
	}

	public ResearchBuilderJS addReward(IReward reward) {
		rewards.add(Objects.requireNonNull(reward, "reward"));
		return this;
	}

	public ResearchBuilderJS addConditionInternal(ICondition<?> condition) {
		conditions.add(Objects.requireNonNull(condition, "condition"));
		return this;
	}

	public ResearchBuilderJS setIcons(Context cx, Object[] items) {
		NonNullList<ItemStack> actualItems = NonNullList.create();
		for (Object item : items) {
			Ingredient ingredient = IngredientWrapper.wrap(cx, item);
			for (ItemStack stack : ingredient.getItems()) {
				if (!stack.isEmpty()) {
					actualItems.add(stack.copy());
				}
			}
		}
		icons = List.copyOf(actualItems);
		return this;
	}

	public ResearchBuilderJS setRequiredResearches(Object[] researches) {
		Set<String> set = toStringSet(researches);
		return addCriterion(new CriterionResearches(set, set.size()));
	}

	public ResearchBuilderJS setOptionalResearches(int amount, Object[] researches) {
		return addCriterion(new CriterionResearches(toStringSet(researches), amount));
	}

	public ResearchBuilderJS setRequiredScore(String score, String failingText, int min) {
		return setRequiredScore(score, failingText, min, Integer.MAX_VALUE);
	}

	public ResearchBuilderJS setRequiredScore(String score, String failingText, int min, int max) {
		if (max < min) {
			max = min;
		}
		return addCriterion(new CriterionScore(score, min, max, failingText));
	}

	public ResearchBuilderJS setRewardCommands(Object[] commands) {
		return addReward(new RewardExecute(toStringArray(commands)));
	}

	public ResearchBuilderJS setRewardItems(Context cx, Object[] items) {
		NonNullList<ItemStack> rawItems = NonNullList.create();
		for (Object item : flatten(items)) {
			ItemStack stack = ItemWrapper.wrap(cx, item);
			if (!stack.isEmpty()) {
				rawItems.add(stack.copy());
			}
		}
		return addReward(new RewardItems(rawItems));
	}

	public ResearchBuilderJS setTriggerCommands(Object[] commands) {
		return addTrigger(new RewardExecute(toStringArray(commands)));
	}

	public ResearchBuilderJS setTriggerItems(Context cx, Object[] items) {
		NonNullList<ItemStack> rawItems = NonNullList.create();
		for (Object item : flatten(items)) {
			ItemStack stack = ItemWrapper.wrap(cx, item);
			if (!stack.isEmpty()) {
				rawItems.add(stack.copy());
			}
		}
		return addTrigger(new RewardItems(rawItems));
	}

	public ResearchBuilderJS setTitle(String title) {
		this.title = title;
		return this;
	}

	public ResearchBuilderJS setDescription(String description) {
		this.description = description;
		return this;
	}

	public ResearchBuilderJS addCondition(Context cx, Object[] ingredients) {
		for (Object ingredient : ingredients) {
			pushItem(cx, ingredient, null);
		}
		return this;
	}

	public ResearchBuilderJS addCondition(Context cx, Object ingredient) {
		return pushItem(cx, ingredient, null);
	}

	public ResearchBuilderJS addCondition(Context cx, Object ingredient, String customName) {
		return pushItem(cx, ingredient, customName);
	}

	public ResearchBuilderJS addItemCondition(Context cx, Object ingredient) {
		return pushItem(cx, ingredient, null);
	}

	public ResearchBuilderJS addItemCondition(Context cx, Object ingredient, String customName) {
		return pushItem(cx, ingredient, customName);
	}

	private ResearchBuilderJS pushItem(Context cx, Object ingredient, @Nullable String customName) {
		SizedIngredient sized = SizedIngredientWrapper.wrap(cx, ingredient);
		if (sized.getItems().length == 0) {
			throw new IllegalArgumentException("Ingredient has no matching items: " + ingredient);
		}
		ConditionKubeJSItem cond = new ConditionKubeJSItem(sized);
		if (customName != null && !customName.isEmpty()) {
			cond.setCustomName(customName);
		}
		return addConditionInternal(cond);
	}

	public ResearchBuilderJS addFluidCondition(Context cx, Object fluid) {
		Object unwrapped = unwrap(fluid);
		if (unwrapped instanceof FluidStack stack) {
			return addConditionInternal(new ConditionKubeJSFluid(stack, stack.getAmount()));
		}
		return addConditionInternal(new ConditionKubeJSFluid(FluidWrapper.wrapSizedIngredient(cx, fluid)));
	}

	public ResearchBuilderJS addFluidCondition(Context cx, Object fluid, int amount) {
		Object unwrapped = unwrap(fluid);
		if (unwrapped instanceof FluidStack stack) {
			return addConditionInternal(new ConditionKubeJSFluid(stack, amount));
		}
		SizedFluidIngredient sized = FluidWrapper.wrapSizedIngredient(cx, fluid);
		return addConditionInternal(new ConditionKubeJSFluid(sized.ingredient(), amount, List.of(sized.getFluids())));
	}

	public ResearchBuilderJS addEnergyCondition(int amount) {
		return addConditionInternal(new ConditionForgeEnergy(amount));
	}

	public ResearchBuilderJS addEnergyCondition(long amount) {
		return addConditionInternal(new ConditionForgeEnergy(amount));
	}

	public ResearchBuilderJS setRequiredBiomes(Object[] biomes) {
		return addCriterion(new CriterionBiome(parseIds(biomes)));
	}

	public ResearchBuilderJS setRequiredDimensions(Object[] dimensions) {
		return addCriterion(new CriterionDimension(parseIds(dimensions)));
	}

	public ResearchBuilderJS setRequiredWeather(String weather) {
		CriterionWeather.Weather w;
		try {
			w = CriterionWeather.Weather.valueOf(weather.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Unknown weather '" + weather + "', expected one of: clear, rain, thunder");
		}
		return addCriterion(new CriterionWeather(w));
	}

	public ResearchBuilderJS setRequiredTime(int min, int max) {
		return addCriterion(new CriterionTime(min, max));
	}

	public ResearchBuilderJS addXPCondition(int amount) {
		return addConditionInternal(new ConditionExperience(amount));
	}

	public ResearchBuilderJS setRewardXP(int points) {
		return addReward(new RewardExperience(points, false));
	}

	public ResearchBuilderJS setRewardXPLevels(int levels) {
		return addReward(new RewardExperience(levels, true));
	}

	public ResearchBuilderJS setTriggerXP(int points) {
		return addTrigger(new RewardExperience(points, false));
	}

	public ResearchBuilderJS setTriggerXPLevels(int levels) {
		return addTrigger(new RewardExperience(levels, true));
	}

	public ResearchBuilderJS setRewardStages(Object[] stages) {
		ensureAStagesLoaded();
		return addReward(new RewardUnlockStages(toStringArray(stages)));
	}

	public ResearchBuilderJS setTriggerStages(Object[] stages) {
		ensureAStagesLoaded();
		return addTrigger(new RewardUnlockStages(toStringArray(stages)));
	}

	public ResearchBuilderJS setRequiredStages(Object[] stages) {
		ensureAStagesLoaded();
		Set<String> set = toStringSet(stages);
		return addCriterion(new CriterionStages(set, set.size()));
	}

	public ResearchBuilderJS setOptionalStages(int amount, Object[] stages) {
		ensureAStagesLoaded();
		return addCriterion(new CriterionStages(toStringSet(stages), amount));
	}

	public ResearchBuilderJS setMaxCount(int count) {
		this.maxCount = Math.max(0, count);
		return this;
	}

	public ResearchBuilderJS setNoMaxCount() {
		return setMaxCount(0);
	}

	public boolean build() {
		String actualTitle = title == null ? KEY_NO_TITLE : title;
		String actualDescription = description == null ? KEY_NO_DESCRIPTION : description;
		List<ICriterion> actualCriteria = new ArrayList<>(criteria);
		if (maxCount > 0) {
			actualCriteria.add(new CriterionResearchCount(name, maxCount));
		}
		Research research = new Research(
				name,
				category,
				actualTitle,
				actualDescription,
				actualCriteria,
				triggers,
				rewards,
				conditions,
				icons);
		return ResearchList.add(research);
	}

	static String[] toStringArray(Object[] values) {
		return toStrings(values).toArray(String[]::new);
	}

	private static Set<String> toStringSet(Object[] values) {
		return new LinkedHashSet<>(toStrings(values));
	}

	private static List<String> toStrings(Object[] values) {
		List<String> out = new ArrayList<>();
		for (Object value : flatten(values)) {
			if (value != null) {
				out.add(String.valueOf(value));
			}
		}
		return out;
	}

	private static List<?> flatten(Object[] values) {
		if (values == null || values.length == 0) {
			return List.of();
		}
		List<Object> out = new ArrayList<>();
		for (Object value : values) {
			List<?> list = ListJS.of(value);
			if (list != null && !(value instanceof CharSequence)) {
				out.addAll(list);
			} else if (value != null) {
				out.add(value);
			}
		}
		return out;
	}

	private static Set<ResourceLocation> parseIds(Object[] ids) {
		Set<ResourceLocation> set = new LinkedHashSet<>();
		for (String id : toStrings(ids)) {
			ResourceLocation parsed = ResourceLocation.tryParse(id);
			if (parsed == null) {
				throw new IllegalArgumentException("Invalid ResourceLocation: " + id);
			}
			set.add(parsed);
		}
		return set;
	}

	private static Object unwrap(Object value) {
		while (value instanceof Wrapper wrapper) {
			value = wrapper.unwrap();
		}
		return value;
	}

	private static void ensureAStagesLoaded() {
		if (!ModList.get().isLoaded("astages")) {
			throw new IllegalStateException("AStages is not loaded");
		}
	}
}
