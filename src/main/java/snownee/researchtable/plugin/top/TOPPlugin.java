package snownee.researchtable.plugin.top;

import java.util.function.Function;

import mcjty.theoneprobe.api.ITheOneProbe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import snownee.researchtable.ResearchTable;

/**
 * TheOneProbe integration. Wired through NeoForge IMC during {@link InterModEnqueueEvent} —
 * the old 1.12 {@code FMLInterModComms.sendFunctionMessage} pipeline no longer exists.
 * Listener lives on the MOD bus; IMC to a missing mod is a no-op, so no extra gating is needed
 * at runtime, but we still check {@link ModList} so the embedded Function class isn't loaded
 * (and verified) when TOP isn't on the classpath.
 */
@EventBusSubscriber(modid = ResearchTable.MODID)
public final class TOPPlugin {

	private TOPPlugin() {
	}

	@SubscribeEvent
	public static void onIMCEnqueue(InterModEnqueueEvent event) {
		if (!ModList.get().isLoaded("theoneprobe")) {
			return;
		}
		InterModComms.sendTo("theoneprobe", "getTheOneProbe", GetTheOneProbe::new);
	}

	public static final class GetTheOneProbe implements Function<ITheOneProbe, Void> {
		@Override
		public Void apply(ITheOneProbe probe) {
			probe.registerProvider(new TableInfoProvider());
			return null;
		}
	}
}
