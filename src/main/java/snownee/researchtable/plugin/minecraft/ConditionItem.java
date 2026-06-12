package snownee.researchtable.plugin.minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.core.ConditionDisplays;
import snownee.researchtable.core.ConditionType;
import snownee.researchtable.core.ConditionTypes;
import snownee.researchtable.core.ItemDisplayCondition;

public class ConditionItem implements ICondition<ItemStack>, ItemDisplayCondition {
	@Nullable
	private final Ingredient ingredient;
	private final long count;
	private final List<ItemStack> displayItems;
	@Nullable
	private String customName;

	public ConditionItem(Ingredient ingredient, long count) {
		this.ingredient = ingredient;
		this.count = Math.max(0, count);
		this.displayItems = ConditionDisplays.copyItems(List.of(ingredient.getItems()));
	}

	private ConditionItem(List<ItemStack> displayItems, long count, @Nullable String customName) {
		this.ingredient = null;
		this.count = Math.max(0, count);
		this.displayItems = ConditionDisplays.copyItems(displayItems);
		this.customName = customName;
	}

	@Override
	public long matches(ItemStack e) {
		if (e.isEmpty() || ingredient == null) {
			return 0;
		}
		ItemStack probe = e.copy();
		probe.setCount(Integer.MAX_VALUE);
		return ingredient.test(probe) ? e.getCount() : 0;
	}

	@Override
	public long getGoal() {
		return count;
	}

	@Override
	public Supplier<Class<ItemStack>> getMatchType() {
		return ConditionTypes.ITEM;
	}

	@Override
	public List<ItemStack> getDisplayItems() {
		return ConditionDisplays.copyItems(displayItems);
	}

	@Override
	@Nullable
	public String getCustomName() {
		return customName;
	}

	public void setCustomName(@Nullable String name) {
		customName = name;
	}

	public static final ConditionType<ConditionItem> TYPE = ConditionType.register(
			ResearchTable.id("item"),
			(buf, c) -> {
				ByteBufCodecs.collection(ArrayList::new, ItemStack.OPTIONAL_STREAM_CODEC)
						.encode(buf, new ArrayList<>(c.displayItems));
				buf.writeVarLong(c.count);
				buf.writeBoolean(c.customName != null);
				if (c.customName != null) {
					buf.writeUtf(c.customName);
				}
			},
			buf -> {
				ArrayList<ItemStack> items = ByteBufCodecs.collection(ArrayList::new, ItemStack.OPTIONAL_STREAM_CODEC).decode(buf);
				long count = buf.readVarLong();
				String customName = buf.readBoolean() ? buf.readUtf() : null;
				return new ConditionItem(items, count, customName);
			});

	@Override
	public ConditionType<ConditionItem> getType() {
		return TYPE;
	}
}
