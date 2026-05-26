package snownee.researchtable.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@MethodsReturnNonnullByDefault
public record PacketResearchChanged(BlockPos pos, String researchName, Action action) implements CustomPacketPayload {

	public static final Type<PacketResearchChanged> TYPE = new Type<>(NetworkChannel.id("research_changed"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PacketResearchChanged> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, PacketResearchChanged::pos,
			ByteBufCodecs.STRING_UTF8, PacketResearchChanged::researchName,
			ByteBufCodecs.idMapper(i -> Action.values()[i % Action.values().length], Action::ordinal), PacketResearchChanged::action,
			PacketResearchChanged::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public enum Action {
		START, STOP, COMPLETE, SUBMIT
	}
}
