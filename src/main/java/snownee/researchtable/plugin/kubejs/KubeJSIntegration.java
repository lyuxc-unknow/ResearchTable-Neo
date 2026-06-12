package snownee.researchtable.plugin.kubejs;

import dev.latvian.mods.kubejs.script.ScriptType;

public final class KubeJSIntegration {
	private KubeJSIntegration() {
	}

	public static void postAddResearchEvent() {
		ResearchTableKubeJSPlugin.ADD_RESEARCH.post(ScriptType.SERVER, new AddResearchEventJS());
	}
}
