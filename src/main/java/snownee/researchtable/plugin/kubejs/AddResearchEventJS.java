package snownee.researchtable.plugin.kubejs;

import javax.annotation.Nullable;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.rhino.Context;
import net.minecraft.world.item.ItemStack;
import snownee.researchtable.core.ResearchCategory;
import snownee.researchtable.core.ResearchList;

public class AddResearchEventJS implements KubeEvent {
	public ResearchBuilderJS builder(String name, ResearchCategoryJS category) {
		return new ResearchBuilderJS(name, category);
	}

	public ResearchCategoryJS addCategory(Context cx, Object stack) {
		return addCategory(cx, stack, null);
	}

	public ResearchCategoryJS addCategory(Context cx, Object stack, @Nullable String name) {
		ItemStack icon = ItemWrapper.wrap(cx, stack).copy();
		return new ResearchCategoryJS(new ResearchCategory(icon, name));
	}

	public boolean remove(String name) {
		return ResearchList.remove(name);
	}

	public void removeAll() {
		ResearchList.clear();
	}

	public void scoreIndicator(String formattingText) {
		scoreIndicator(formattingText, new Object[0]);
	}

	public void scoreIndicator(String formattingText, Object[] scores) {
		ResearchList.setScoreIndicator(formattingText, ResearchBuilderJS.toStringArray(scores));
	}
}
