package snownee.researchtable.plugin.crafttweaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.ConditionType;
import snownee.researchtable.core.ConditionTypes;
import snownee.researchtable.core.ICondition;

public class ConditionCrTItem implements ICondition<ItemStack> {

	// Wire-only constructor (client) leaves ingredient null; matches() must never be called there.
	@Nullable
	final IIngredient ingredient;
	final long count;
	final List<ItemStack> displayItems;
	@Nullable
	String customName;

	public ConditionCrTItem(IIngredient ingredient) {
		this(ingredient, 1);
	}

	public ConditionCrTItem(IIngredient ingredient, long count) {
		this.count = count;
		this.ingredient = ingredient;
		this.displayItems = extractDisplayItems(ingredient);
	}

	private ConditionCrTItem(List<ItemStack> displayItems, long count, @Nullable String customName) {
		this.ingredient = null;
		this.count = count;
		this.displayItems = displayItems;
		this.customName = customName;
	}

	private static List<ItemStack> extractDisplayItems(IIngredient ingredient) {
		IItemStack[] items = ingredient.getItems();
		if (items == null || items.length == 0) {
			return Collections.emptyList();
		}
		List<ItemStack> out = new ArrayList<>(items.length);
		for (IItemStack item : items) {
			if (item == null) continue;
			ItemStack vanilla = item.getInternal();
			if (!vanilla.isEmpty()) {
				out.add(vanilla.copy());
			}
		}
		return out;
	}

	@Override
	public long matches(ItemStack e) {
		if (e.isEmpty() || ingredient == null) {
			return 0;
		}
		// Probe with an inflated count so IIngredient.matches checks only item identity, not amount —
		// otherwise a goal of `item * 5` rejects any single stack with fewer than 5, preventing partial submissions.
		ItemStack probe = e.copy();
		probe.setCount(Integer.MAX_VALUE);
		IItemStack wrapped = IItemStack.of(probe);
		if (ingredient.matches(wrapped)) {
			return e.getCount();
		}
		return 0;
	}

	@Override
	public long getGoal() {
		return count;
	}

	@Override
	public Supplier<Class<ItemStack>> getMatchType() {
		return ConditionTypes.ITEM;
	}

	public List<ItemStack> getDisplayItems() {
		return displayItems;
	}

	@Nullable
	public String getCustomName() {
		return customName;
	}

	public ConditionCrTItem setCustomName(@Nullable String name) {
		customName = name;
		return this;
	}

	public static final ConditionType<ConditionCrTItem> TYPE = ConditionType.register(
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, "crt_item"),
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
				return new ConditionCrTItem(items, count, customName);
			});

	@Override
	public ConditionType<ConditionCrTItem> getType() {
		return TYPE;
	}
}
