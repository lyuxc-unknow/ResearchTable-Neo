package snownee.researchtable.plugin.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;

public class ResearchTableKubeJSPlugin implements KubeJSPlugin {
	public static final EventGroup GROUP = EventGroup.of("ResearchTable");
	public static final EventHandler ADD_RESEARCH = GROUP.server("addResearch", () -> AddResearchEventJS.class);

	@Override
	public void registerEvents(EventGroupRegistry registry) {
		registry.register(GROUP);
	}
}
