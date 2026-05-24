package snownee.researchtable.plugin.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;

import org.openzen.zencode.java.ZenCodeType;

import snownee.researchtable.core.ResearchCategory;

@ZenRegister
@ZenCodeType.Name("mods.researchtable.ResearchCategory")
public class ResearchCategoryWrapper {
	protected final ResearchCategory category;

	public ResearchCategoryWrapper(ResearchCategory category) {
		this.category = category;
	}
}
