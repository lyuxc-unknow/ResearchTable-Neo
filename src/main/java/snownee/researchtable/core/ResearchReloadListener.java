package snownee.researchtable.core;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.network.PacketSyncResearchList;
import snownee.researchtable.plugin.kubejs.KubeJSIntegration;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@EventBusSubscriber(modid = ResearchTable.MODID)
public final class ResearchReloadListener {
	private ResearchReloadListener() {
	}

	@SubscribeEvent
	public static void onAddReloadListener(AddReloadListenerEvent event) {
		// Build the pending datapack state in prepare. All script integrations append to that same
		// pending state during apply, then we publish once before sync/server start.
		event.addListener(new SimplePreparableReloadListener<Void>() {
			@Override
			protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
				ResearchList.beginReload(ResearchDataLoader.load(resourceManager));
				return null;
			}

			@Override
			protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profiler) {
			}
		});
	}

	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent event) {
		applyPendingScriptResearches();
	}

	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		applyPendingScriptResearches();
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

	private static void applyPendingScriptResearches() {
		if (!ResearchList.hasPendingReload()) {
			return;
		}
		if (ModList.get().isLoaded("kubejs")) {
			KubeJSIntegration.postAddResearchEvent();
		}
		ResearchList.finishReload();
	}
}
