package snownee.researchtable.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.block.TableBlockEntity;
import snownee.researchtable.core.DataStorage;
import snownee.researchtable.core.Research;
import snownee.researchtable.core.ResearchList;

public final class NetworkChannel {
	private NetworkChannel() {
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(ResearchTable.MODID).versioned("1");
		registrar.playToServer(
				PacketResearchChanged.TYPE,
				PacketResearchChanged.STREAM_CODEC,
				NetworkChannel::handleResearchChanged);
		registrar.playToClient(
				PacketSyncClient.TYPE,
				PacketSyncClient.STREAM_CODEC,
				NetworkChannel::handleSyncClient);
		registrar.playToClient(
				PacketSyncResearchList.TYPE,
				PacketSyncResearchList.STREAM_CODEC,
				NetworkChannel::handleSyncResearchList);
	}

	private static void handleResearchChanged(PacketResearchChanged packet, IPayloadContext ctx) {
		ctx.enqueueWork(() -> {
			if (!(ctx.player() instanceof ServerPlayer player)) {
				return;
			}
			BlockPos pos = packet.pos();
			if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 100 || !player.level().isLoaded(pos)) {
				return;
			}
			BlockEntity tile = player.level().getBlockEntity(pos);
			if (!(tile instanceof TableBlockEntity table)) {
				return;
			}
			if (!table.hasPermission(player)) {
				return;
			}
			Research research = ResearchList.find(packet.researchName()).orElse(null);
			switch (packet.action()) {
				case START -> {
					if (research == null) {
						return;
					}
					if (!research.canResearch(player, table.getData())) {
						return;
					}
					if (table.getResearch() == null) {
						research.start(player.level(), pos, player);
						table.setResearch(research);
					}
				}
				case STOP -> {
					if (table.getResearch() == research && !table.canComplete()) {
						table.setResearch(null);
					}
				}
				case COMPLETE -> {
					if (table.getResearch() == research && table.canComplete()) {
						table.complete(player);
					}
				}
				case SUBMIT -> {
					if (table.getResearch() == research && !table.canComplete()) {
						table.submit(player);
					}
				}
			}
			table.hasChanged = true;
		});
	}

	private static void handleSyncClient(PacketSyncClient packet, IPayloadContext ctx) {
		ctx.enqueueWork(() -> {
			DataStorage.clientData = packet.map();
		});
	}

	private static void handleSyncResearchList(PacketSyncResearchList packet, IPayloadContext ctx) {
		// Skip on the integrated (single-player) connection: server and client share the same
		// static ResearchList in the same JVM, and the snapshot loses non-serializable state like
		// ConditionCrTItem.ingredient. Applying it would clobber the authoritative server-side data.
		if (ctx.connection().isMemoryConnection()) {
			return;
		}
		ctx.enqueueWork(() -> ResearchList.applySnapshot(packet));
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, path);
	}
}
