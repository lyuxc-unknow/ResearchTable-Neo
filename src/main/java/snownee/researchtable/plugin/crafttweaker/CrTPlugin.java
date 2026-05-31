package snownee.researchtable.plugin.crafttweaker;

import javax.annotation.Nonnull;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;

import org.openzen.zencode.java.ZenCodeType;

import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.ResearchCategory;
import snownee.researchtable.core.ResearchList;

@ZenRegister
@ZenCodeType.Name("mods.researchtable.ResearchTable")
public class CrTPlugin {

	@ZenCodeType.Method
	public static ResearchBuilder builder(@Nonnull String name, @Nonnull ResearchCategoryWrapper category) {
		return new ResearchBuilder(name, category);
	}

	@ZenCodeType.Method
	public static ResearchCategoryWrapper addCategory(@Nonnull IItemStack stack, @ZenCodeType.Optional String name) {
		return new ResearchCategoryWrapper(new ResearchCategory(stack.getInternal().copy(), name));
	}

	@ZenCodeType.Method
	public static boolean remove(@Nonnull String name) {
		return ResearchList.remove(name);
	}

	@ZenCodeType.Method
	public static void removeAll() {
		ResearchList.clear();
	}

	@ZenCodeType.Method
	public static void scoreIndicator(String formattingText, String... scores) {
		ResearchList.ensureReloadApplied();
		ResearchTable.scoreFormattingText = formattingText;
		ResearchTable.scores = scores.clone();
	}
}
