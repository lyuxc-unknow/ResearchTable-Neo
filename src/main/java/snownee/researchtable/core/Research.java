package snownee.researchtable.core;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class Research {
	private static final ItemStack DEFAULT_ICON = new ItemStack(Blocks.GRASS_BLOCK);

	private final String name;
	private final ResearchCategory category;
	private final String title;
	private final String description;
	@Nullable
	private final List<ItemStack> icons;
	private final List<ICondition> conditions;
	private final Collection<ICriterion> criteria;
	private final Collection<IReward> triggers;
	private final Collection<IReward> rewards;

	public Research(String name, ResearchCategory category, String title, String description, Collection<ICriterion> criteria, Collection<IReward> triggers, Collection<IReward> rewards, List<ICondition> conditions, @Nullable List<ItemStack> icons) {
		this.name = name;
		this.category = category;
		this.title = title;
		this.description = description;
		this.criteria = criteria;
		this.triggers = triggers;
		this.rewards = rewards;
		this.conditions = conditions;
		this.icons = icons;
	}

	public String getName() {
		return name;
	}

	public ResearchCategory getCategory() {
		return category;
	}

	public String getTitleRaw() {
		return title;
	}

	public String getTitle() {
		return I18n.exists(title) ? I18n.get(title) : title;
	}

	public String getDescriptionRaw() {
		return description;
	}

	public String getDescription() {
		return I18n.exists(description) ? I18n.get(description) : description;
	}

	public ItemStack getIcon() {
		if (icons == null || icons.isEmpty()) {
			return DEFAULT_ICON;
		} else {
			return icons.getFirst();
		}
	}

	public List<ICondition> getConditions() {
		return Collections.unmodifiableList(conditions);
	}

	public boolean canResearch(Player player, CompoundTag data) {
		return criteria.stream().allMatch(c -> c.matches(player, data));
	}

	public Collection<ICriterion> getCriteria() {
		return Collections.unmodifiableCollection(criteria);
	}

	public Collection<IReward> getTriggers() {
		return Collections.unmodifiableCollection(triggers);
	}

	public void complete(Level world, BlockPos pos, Player player) {
		rewards.forEach(e -> e.earn(world, pos, player));
	}

	@Override
	public String toString() {
		return "Research@" + getName();
	}

	public void start(Level world, BlockPos pos, Player player) {
		triggers.forEach(r -> r.earn(world, pos, player));
	}
}
