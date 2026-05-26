package snownee.researchtable.network;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import snownee.researchtable.core.DataStorage;

@MethodsReturnNonnullByDefault
public record PacketSyncClient(Object2IntMap<String> map) implements CustomPacketPayload {

	public static final Type<PacketSyncClient> TYPE = new Type<>(NetworkChannel.id("sync_client"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PacketSyncClient> STREAM_CODEC = StreamCodec.of(
			(buf, payload) -> ByteBufCodecs.COMPOUND_TAG.encode(buf, DataStorage.writePlayerData(payload.map())),
			buf -> {
				CompoundTag tag = ByteBufCodecs.COMPOUND_TAG.decode(buf);
				return new PacketSyncClient(DataStorage.readPlayerData(tag));
			});

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
