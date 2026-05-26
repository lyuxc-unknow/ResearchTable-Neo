package snownee.researchtable.plugin.crafttweaker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

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
	public List<ICriterion> criteria = new LinkedList<>();
	public List<IReward> triggers = new LinkedList<>();
	public List<IReward> rewards = new LinkedList<>();
	public List<ICondition> conditions = new ArrayList<>(4);
	public List<ItemStack> icons;
	private String title;
	private String description;
	private int maxCount = 1;

	public ResearchBuilder(@Nonnull String name, @Nonnull ResearchCategoryWrapper category) {
		this.name = name;
		this.category = category.category;
	}

	@ZenCodeType.Method
	public ResearchBuilder setIcons(@Nonnull IIngredient... items) {
		NonNullList<ItemStack> actualItems = NonNullList.create();
		for (IIngredient item : items) {
			for (IItemStack stack : item.getItems()) {
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
		criteria.add(new CriterionResearches(set, set.size()));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setOptionalResearches(int amount, @Nonnull String... researches) {
		Set<String> set = ImmutableSet.copyOf(researches);
		criteria.add(new CriterionResearches(set, amount));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setRequiredScore(String score, String failingText, int min, @ZenCodeType.OptionalInt int max) {
		if (max < min)
			max = min;
		criteria.add(new CriterionScore(score, min, max, failingText));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setRewardCommands(@Nonnull String... commands) {
		rewards.add(new RewardExecute(commands));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setRewardItems(@Nonnull IItemStack... items) {
		NonNullList<ItemStack> rawItems = NonNullList.create();
		for (IItemStack item : items) {
			rawItems.add(item.getInternal().copy());
		}
		rewards.add(new RewardItems(rawItems));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setTriggerCommands(@Nonnull String... commands) {
		triggers.add(new RewardExecute(commands));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setTriggerItems(@Nonnull IItemStack... items) {
		NonNullList<ItemStack> rawItems = NonNullList.create();
		for (IItemStack item : items) {
			rawItems.add(item.getInternal().copy());
		}
		triggers.add(new RewardItems(rawItems));
		return this;
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
			pushItem(ingredient, ingredient.getItems()[0].amount(), null);
		}
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder addCondition(@Nonnull IIngredient ingredient) {
		return pushItem(ingredient, ingredient.getItems()[0].amount(), null);
	}

	@ZenCodeType.Method
	public ResearchBuilder addCondition(@Nonnull IIngredient ingredient, @Nonnull String customName) {
		return pushItem(ingredient, ingredient.getItems()[0].amount(), customName);
	}

	@ZenCodeType.Method
	public ResearchBuilder addItemCondition(@Nonnull IIngredient ingredient) {
		return pushItem(ingredient, ingredient.getItems()[0].amount(), null);
	}

	@ZenCodeType.Method
	public ResearchBuilder addItemCondition(@Nonnull IIngredient ingredient, @Nonnull String customName) {
		return pushItem(ingredient, ingredient.getItems()[0].amount(), customName);
	}

	private ResearchBuilder pushItem(IIngredient ingredient, long amount, String customName) {
		ResearchTable.logger.debug("pushItem: ingredient={} amount={} customName={}", ingredient, amount, customName);
		ConditionCrTItem cond = new ConditionCrTItem(ingredient, amount);
		if (customName != null && !customName.isEmpty()) {
			cond.setCustomName(customName);
		}
		conditions.add(cond);
		return this;
	}

	// ---- Fluid conditions ----

	@ZenCodeType.Method
	public ResearchBuilder addFluidCondition(@Nonnull IFluidStack fluid) {
		conditions.add(new ConditionCrTLiquid(fluid, fluid.getAmount()));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder addFluidCondition(@Nonnull IFluidStack fluid, int amount) {
		conditions.add(new ConditionCrTLiquid(fluid, amount));
		return this;
	}

	// ---- Energy ----

	@ZenCodeType.Method
	public ResearchBuilder addEnergyCondition(int amount) {
		conditions.add(new ConditionForgeEnergy(amount));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder addEnergyCondition(long amount) {
		conditions.add(new ConditionForgeEnergy(amount));
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setMaxCount(int count) {
		this.maxCount = count;
		return this;
	}

	@ZenCodeType.Method
	public ResearchBuilder setNoMaxCount() {
		return setMaxCount(0);
	}

	@ZenCodeType.Method
	public boolean build() {
		if (title == null) {
			title = KEY_NO_TITLE;
		}
		if (description == null) {
			description = KEY_NO_DESCRIPTION;
		}
		if (maxCount > 0) {
			criteria.add(new CriterionResearchCount(name, maxCount));
		}
		Research research = new Research(name, category, title, description, criteria, triggers, rewards, conditions, icons);
		return ResearchList.add(research);
	}
}
