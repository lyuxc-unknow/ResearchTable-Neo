package snownee.researchtable;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = ResearchTable.MODID)
public final class ModConfig {
	public static final ModConfigSpec SPEC;
	private static final ModConfigSpec.BooleanValue GUI_FULL_SCREEN;
	private static final ModConfigSpec.IntValue GUI_HEIGHT;
	private static final ModConfigSpec.IntValue GUI_LIST_WIDTH;
	private static final ModConfigSpec.BooleanValue GUI_LIST_AUTO_WIDTH;
	private static final ModConfigSpec.IntValue GUI_DETAIL_WIDTH;
	private static final ModConfigSpec.BooleanValue HIDE_UNAVAILABLE_RESEARCH;
	private static final ModConfigSpec.BooleanValue HIDE_COMPLETED_RESEARCH;
	private static final ModConfigSpec.BooleanValue NON_PRIVILEGED_MODE;

	public static boolean guiFullScreen = true;
	public static int guiHeight = 180;
	public static int guiListWidth = 120;
	public static boolean guiListAutoWidth = true;
	public static int guiDetailWidth = 200;
	public static boolean hideUnavailableResearch = false;
	public static boolean hideCompletedResearch = false;
	public static boolean nonPrivilegedMode = false;

	static {
		ModConfigSpec.Builder b = new ModConfigSpec.Builder();
		GUI_FULL_SCREEN = b.define("guiFullScreen", true);
		GUI_HEIGHT = b.defineInRange("guiHeight", 180, 1, Integer.MAX_VALUE);
		GUI_LIST_WIDTH = b.defineInRange("guiListWidth", 120, 1, Integer.MAX_VALUE);
		GUI_LIST_AUTO_WIDTH = b.define("guiListAutoWidth", true);
		GUI_DETAIL_WIDTH = b.defineInRange("guiDetailWidth", 200, 1, Integer.MAX_VALUE);
		HIDE_UNAVAILABLE_RESEARCH = b.define("hideUnavailableResearch", false);
		HIDE_COMPLETED_RESEARCH = b.define("hideCompletedResearch", false);
		NON_PRIVILEGED_MODE = b.define("nonPrivilegedCommandReward", false);
		SPEC = b.build();
	}

	private ModConfig() {
	}

	public static void register(ModContainer container) {
		container.registerConfig(Type.COMMON, SPEC);
	}

	@SubscribeEvent
	public static void onConfigLoad(ModConfigEvent.Loading event) {
		if (event.getConfig().getSpec() == SPEC) {
			sync();
		}
	}

	@SubscribeEvent
	public static void onConfigReload(ModConfigEvent.Reloading event) {
		if (event.getConfig().getSpec() == SPEC) {
			sync();
		}
	}

	private static void sync() {
		guiFullScreen = GUI_FULL_SCREEN.get();
		guiHeight = GUI_HEIGHT.get();
		guiListWidth = GUI_LIST_WIDTH.get();
		guiListAutoWidth = GUI_LIST_AUTO_WIDTH.get();
		guiDetailWidth = GUI_DETAIL_WIDTH.get();
		hideUnavailableResearch = HIDE_UNAVAILABLE_RESEARCH.get();
		hideCompletedResearch = HIDE_COMPLETED_RESEARCH.get();
		nonPrivilegedMode = NON_PRIVILEGED_MODE.get();
	}
}
