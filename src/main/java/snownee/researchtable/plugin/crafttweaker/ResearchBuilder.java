package snownee.researchtable.plugin.crafttweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.CriterionResearchCount;
import snownee.researchtable.core.CriterionResearches;
import snownee.researchtable.core.CriterionScore;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.api.ICriterion;
import snownee.researchtable.api.IReward;
import snownee.researchtable.core.Research;
import snownee.researchtable.core.ResearchCategory;
import snownee.researchtable.core.ResearchList;
import snownee.researchtable.core.RewardExecute;
import snownee.researchtable.core.RewardItems;
import snownee.researchtable.plugin.forge.ConditionForgeEnergy;

@ZenRegister
@ZenCodeType.Name("mods.researchtable.ResearchBuilder")
public class ResearchBuilder {
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

	public ResearchBuilder(@Nonnull String name, @Nonnull ResearchCategoryWrapper category) {
		this.name = name;
		this.category = category.category;
	}

	public ResearchBuilder addCriterion(ICriterion criterion) {
		criteria.add(Objects.requireNonNull(criterion, "criterion"));
		return this;
	}

	public ResearchBuilder addTrigger(IReward trigger) {
		triggers.add(Objects.requireNonNull(trigger, "trigger"));
		return this;
	}

	public ResearchBuilder addReward(IReward reward) {
		rewards.add(Objects.requireNonNull(reward, "reward"));
		return this;
	}

	public ResearchBuilder addConditionInternal(ICondition<?> condition) {
		conditions.add(Objects.requireNonNull(condition, "condition"));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setIcons(@Nonnull IIngredient... items) {
		NonNullList<ItemStack> actualItems = NonNullList.create();
		for (IIngredient item : items) {
			IItemStack[] stacks = item.getItems();
			if (stacks == null) {
				continue;
			}
			for (IItemStack stack : stacks) {
				if (stack != null && !stack.getInternal().isEmpty()) {
					actualItems.add(stack.getInternal().copy());
				}
			}
		}
		icons = ImmutableList.copyOf(actualItems);
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setRequiredResearches(@Nonnull String... researches) {
		Set<String> set = ImmutableSet.copyOf(researches);
		return addCriterion(new CriterionResearches(set, set.size()));
	}

	@ZenCodeType.Method
	public ResearchBuilder setOptionalResearches(int amount, @Nonnull String... researches) {
		Set<String> set = ImmutableSet.copyOf(researches);
		return addCriterion(new CriterionResearches(set, amount));
	}

	@ZenCodeType.Method
	public ResearchBuilder setRequiredScore(String score, String failingText, int min) {
		return setRequiredScore(score, failingText, min, Integer.MAX_VALUE);
	}

	@ZenCodeType.Method
	public ResearchBuilder setRequiredScore(String score, String failingText, int min, int max) {
		if (max < min) {
			max = min;
		}
		return addCriterion(new CriterionScore(score, min, max, failingText));
	}

	@ZenCodeType.Method
	public ResearchBuilder setRewardCommands(@Nonnull String... commands) {
		return addReward(new RewardExecute(commands));
	}

	@ZenCodeType.Method
	public ResearchBuilder setRewardItems(@Nonnull IItemStack... items) {
		NonNullList<ItemStack> rawItems = NonNullList.create();
		for (IItemStack item : items) {
			ItemStack stack = item.getInternal();
			if (!stack.isEmpty()) {
				rawItems.add(stack.copy());
			}
		}
		return addReward(new RewardItems(rawItems));
	}

	@ZenCodeType.Method
	public ResearchBuilder setTriggerCommands(@Nonnull String... commands) {
		return addTrigger(new RewardExecute(commands));
	}

	@ZenCodeType.Method
	public ResearchBuilder setTriggerItems(@Nonnull IItemStack... items) {
		NonNullList<ItemStack> rawItems = NonNullList.create();
		for (IItemStack item : items) {
			ItemStack stack = item.getInternal();
			if (!stack.isEmpty()) {
				rawItems.add(stack.copy());
			}
		}
		return addTrigger(new RewardItems(rawItems));
	}

	@ZenCodeType.Method
	public ResearchBuilder setTitle(@Nonnull String title) {
		this.title = title;
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setDescription(@Nonnull String description) {
		this.description = description;
		return this;
	}

	// ---- Item conditions: multiple overloads for ZenScript usage ----

	@ZenCodeType.Method
	public ResearchBuilder addCondition(@Nonnull IIngredient... ingredients) {
		for (IIngredient ingredient : ingredients) {
			pushItem(ingredient, getIngredientAmount(ingredient), null);
		}
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder addCondition(@Nonnull IIngredient ingredient) {
		return pushItem(ingredient, getIngredientAmount(ingredient), null);
	}

	@ZenCodeType.Method
	public ResearchBuilder addCondition(@Nonnull IIngredient ingredient, @Nonnull String customName) {
		return pushItem(ingredient, getIngredientAmount(ingredient), customName);
	}

	@ZenCodeType.Method
	public ResearchBuilder addItemCondition(@Nonnull IIngredient ingredient) {
		return pushItem(ingredient, getIngredientAmount(ingredient), null);
	}

	@ZenCodeType.Method
	public ResearchBuilder addItemCondition(@Nonnull IIngredient ingredient, @Nonnull String customName) {
		return pushItem(ingredient, getIngredientAmount(ingredient), customName);
	}

	private static long getIngredientAmount(IIngredient ingredient) {
		IItemStack[] items = ingredient.getItems();
		if (items == null || items.length == 0) {
			throw new IllegalArgumentException("Ingredient has no matching items: " + ingredient);
		}
		long amount = items[0].amount();
		if (amount <= 0) {
			throw new IllegalArgumentException("Ingredient amount must be positive: " + ingredient);
		}
		return amount;
	}

	private ResearchBuilder pushItem(IIngredient ingredient, long amount, String customName) {
		ConditionCrTItem cond = new ConditionCrTItem(ingredient, amount);
		if (customName != null && !customName.isEmpty()) {
			cond.setCustomName(customName);
		}
		return addConditionInternal(cond);
	}

	// ---- Fluid conditions ----

	@ZenCodeType.Method
	public ResearchBuilder addFluidCondition(@Nonnull IFluidStack fluid) {
		return addConditionInternal(new ConditionCrTLiquid(fluid, fluid.getAmount()));
	}

	@ZenCodeType.Method
	public ResearchBuilder addFluidCondition(@Nonnull IFluidStack fluid, int amount) {
		return addConditionInternal(new ConditionCrTLiquid(fluid, amount));
	}

	// ---- Energy ----

	@ZenCodeType.Method
	public ResearchBuilder addEnergyCondition(int amount) {
		return addConditionInternal(new ConditionForgeEnergy(amount));
	}

	@ZenCodeType.Method
	public ResearchBuilder addEnergyCondition(long amount) {
		return addConditionInternal(new ConditionForgeEnergy(amount));
	}

	@ZenCodeType.Method
	public ResearchBuilder setMaxCount(int count) {
		this.maxCount = Math.max(0, count);
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setNoMaxCount() {
		return setMaxCount(0);
	}

	@ZenCodeType.Method
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
}
