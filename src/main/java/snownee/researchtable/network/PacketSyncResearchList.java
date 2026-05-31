package snownee.researchtable.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.api.ICriterion;
import snownee.researchtable.core.ConditionType;
import snownee.researchtable.core.CriterionType;
import snownee.researchtable.core.Research;
import snownee.researchtable.core.ResearchCategory;
import snownee.researchtable.core.ResearchList;

/**
 * Server → client snapshot of the entire research registry. Sent on player join and after
 * /reload via OnDatapackSyncEvent, so dedicated-server clients see CrT changes without
 * reconnecting. Rewards/triggers are server-only and intentionally excluded from the wire format.
 */
public final class PacketSyncResearchList implements CustomPacketPayload {

	public static final Type<PacketSyncResearchList> TYPE = new Type<>(NetworkChannel.id("sync_research_list"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PacketSyncResearchList> STREAM_CODEC = StreamCodec.of(
			PacketSyncResearchList::encode,
			PacketSyncResearchList::decode);

	public final List<CategorySnapshot> categories;
	public final List<ResearchSnapshot> researches;
	@Nullable
	public final String scoreFormattingText;
	public final String[] scores;

	private PacketSyncResearchList(List<CategorySnapshot> categories,
			List<ResearchSnapshot> researches,
			@Nullable String scoreFormattingText,
			String[] scores) {
		this.categories = List.copyOf(categories);
		this.researches = List.copyOf(researches);
		this.scoreFormattingText = scoreFormattingText;
		this.scores = scores.clone();
	}

	public static PacketSyncResearchList fromCurrentState() {
		List<CategorySnapshot> cats = new ArrayList<>(ResearchList.CATEGORIES.size());
		for (ResearchCategory c : ResearchList.CATEGORIES) {
			cats.add(new CategorySnapshot(c.icon(), c.nameKey()));
		}
		List<ResearchSnapshot> resz = new ArrayList<>(ResearchList.LIST.size());
		for (Research r : ResearchList.LIST.values()) {
			int idx = ResearchList.CATEGORIES.indexOf(r.getCategory());
			List<ICondition<?>> conds = new ArrayList<>(r.getConditions());
			resz.add(new ResearchSnapshot(
					r.getName(),
					idx,
					r.getTitleRaw(),
					r.getDescriptionRaw(),
					new ArrayList<>(snapshotIcons(r)),
					conds,
					new ArrayList<>(r.getCriteria())));
		}
		String[] scoresCopy = ResearchTable.scores == null ? new String[0] : ResearchTable.scores.clone();
		return new PacketSyncResearchList(cats, resz, ResearchTable.scoreFormattingText, scoresCopy);
	}

	private static List<ItemStack> snapshotIcons(Research r) {
		ItemStack first = r.getIcon();
		if (first.isEmpty()) {
			return Collections.emptyList();
		}
		return List.of(first.copy());
	}

	private static void encode(RegistryFriendlyByteBuf buf, PacketSyncResearchList packet) {
		buf.writeBoolean(packet.scoreFormattingText != null);
		if (packet.scoreFormattingText != null) {
			buf.writeUtf(packet.scoreFormattingText);
		}
		buf.writeVarInt(packet.scores.length);
		for (String s : packet.scores) {
			buf.writeUtf(s);
		}

		buf.writeVarInt(packet.categories.size());
		for (CategorySnapshot c : packet.categories) {
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, c.icon);
			buf.writeBoolean(c.nameKey != null);
			if (c.nameKey != null) {
				buf.writeUtf(c.nameKey);
			}
		}

		buf.writeVarInt(packet.researches.size());
		for (ResearchSnapshot r : packet.researches) {
			buf.writeUtf(r.name);
			buf.writeVarInt(r.categoryIdx);
			buf.writeUtf(r.title);
			buf.writeUtf(r.description);

			buf.writeVarInt(r.icons.size());
			for (ItemStack icon : r.icons) {
				ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, icon);
			}

			buf.writeVarInt(r.conditions.size());
			for (ICondition<?> c : r.conditions) {
				ConditionType.writeAny(buf, c);
			}

			buf.writeVarInt(r.criteria.size());
			for (ICriterion c : r.criteria) {
				CriterionType.writeAny(buf, c);
			}
		}
	}

	private static PacketSyncResearchList decode(RegistryFriendlyByteBuf buf) {
		String fmt = buf.readBoolean() ? buf.readUtf() : null;
		int nScores = buf.readVarInt();
		String[] scores = new String[nScores];
		for (int i = 0; i < nScores; i++) {
			scores[i] = buf.readUtf();
		}

		int nCats = buf.readVarInt();
		List<CategorySnapshot> cats = new ArrayList<>(nCats);
		for (int i = 0; i < nCats; i++) {
			ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
			String nameKey = buf.readBoolean() ? buf.readUtf() : null;
			cats.add(new CategorySnapshot(icon, nameKey));
		}

		int nRes = buf.readVarInt();
		List<ResearchSnapshot> resz = new ArrayList<>(nRes);
		for (int i = 0; i < nRes; i++) {
			String name = buf.readUtf();
			int catIdx = buf.readVarInt();
			String title = buf.readUtf();
			String desc = buf.readUtf();

			int nIcons = buf.readVarInt();
			List<ItemStack> icons = new ArrayList<>(nIcons);
			for (int j = 0; j < nIcons; j++) {
				icons.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
			}

			int nConds = buf.readVarInt();
			List<ICondition<?>> conds = new ArrayList<>(nConds);
			for (int j = 0; j < nConds; j++) {
				conds.add(ConditionType.readAny(buf));
			}

			int nCrits = buf.readVarInt();
			List<ICriterion> crits = new ArrayList<>(nCrits);
			for (int j = 0; j < nCrits; j++) {
				crits.add(CriterionType.readAny(buf));
			}

			resz.add(new ResearchSnapshot(name, catIdx, title, desc, icons, conds, crits));
		}
		return new PacketSyncResearchList(cats, resz, fmt, scores);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public record CategorySnapshot(ItemStack icon, @Nullable String nameKey) {
		public CategorySnapshot {
			icon = icon.copy();
		}

		@Override
		public ItemStack icon() {
			return icon.copy();
		}
	}

	public record ResearchSnapshot(String name, int categoryIdx, String title, String description,
	                               List<ItemStack> icons, Collection<? extends ICondition<?>> conditions,
	                               Collection<? extends ICriterion> criteria) {
		public ResearchSnapshot {
			icons = icons.stream()
					.filter(stack -> !stack.isEmpty())
					.map(ItemStack::copy)
					.toList();
			conditions = List.copyOf(conditions);
			criteria = List.copyOf(criteria);
		}
	}
}
