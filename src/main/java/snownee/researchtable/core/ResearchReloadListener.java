package snownee.researchtable.core;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.network.PacketSyncResearchList;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@EventBusSubscriber(modid = ResearchTable.MODID)
public final class ResearchReloadListener {
	private ResearchReloadListener() {
	}

	@SubscribeEvent
	public static void onAddReloadListener(AddReloadListenerEvent event) {
		// Bumps the reload epoch in `prepare` (off-thread). All listeners' `prepare` complete before
		// any `apply` runs, so the epoch is always bumped before CrT re-runs scripts in its `apply`,
		// regardless of which listener was registered first.
		event.addListener(new SimplePreparableReloadListener<Void>() {
			@Override
			protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
				ResearchList.onReloadStarting();
				return null;
			}

			@Override
			protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profiler) {
			}
		});
	}

	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		// Fired both on player login (player != null) and after /reload (player == null, broadcast).
		// Sending here covers dedicated-server clients that have no local scripts and would otherwise
		// see an empty research list.
		PacketSyncResearchList packet = PacketSyncResearchList.fromCurrentState();
		ServerPlayer player = event.getPlayer();
		if (player != null) {
			PacketDistributor.sendToPlayer(player, packet);
		} else {
			for (ServerPlayer p : event.getPlayerList().getPlayers()) {
				PacketDistributor.sendToPlayer(p, packet);
			}
		}
	}
}
