package snownee.researchtable.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import snownee.researchtable.ResearchTable;
import snownee.researchtable.network.PacketSyncResearchList;

public final class ResearchList {
	public static final List<ResearchCategory> CATEGORIES = Lists.newArrayList();
	public static final Map<String, Research> LIST = Maps.newLinkedHashMap();

	// Bumped from the reload listener's `prepare` (off-thread, before any `apply`).
	// `add()` compares against `lastAppliedEpoch` and clears state on the first add of a new reload,
	// so the bump is guaranteed to be visible before CrT re-runs scripts regardless of listener registration order.
	private static volatile int reloadEpoch = 0;
	private static int lastAppliedEpoch = -1;

	// Bumped each time the snapshot is mutated wholesale (clear / applySnapshot). The open GUI
	// polls this in containerTick to know when to drop a now-stale `selected` / `currentCategory`.
	public static volatile int clientVersion = 0;

	private ResearchList() {
	}

	public static void onReloadStarting() {
		reloadEpoch++;
	}

	public static synchronized void clear() {
		LIST.clear();
		CATEGORIES.clear();
		ResearchTable.scores = null;
		ResearchTable.scoreFormattingText = null;
		clientVersion++;
	}

	public static synchronized boolean add(Research research) {
		if (reloadEpoch != lastAppliedEpoch) {
			clear();
			lastAppliedEpoch = reloadEpoch;
		}
		if (LIST.containsKey(research.getName())) {
			return false;
		}
		if (!CATEGORIES.contains(research.getCategory())) {
			CATEGORIES.add(research.getCategory());
		}
		LIST.put(research.getName(), research);
		return true;
	}

	public static Optional<Research> find(String name) {
		return Optional.ofNullable(ResearchList.LIST.get(name));
	}

	/**
	 * Replaces the entire research registry from a server-sent snapshot. Used on the client side
	 * after receiving {@link PacketSyncResearchList}. Server-only fields (rewards/triggers) are
	 * left empty since the client never executes them.
	 */
	public static synchronized void applySnapshot(PacketSyncResearchList packet) {
		clear();

		ResearchTable.scoreFormattingText = packet.scoreFormattingText;
		ResearchTable.scores = packet.scores == null || packet.scores.length == 0 ? null : packet.scores;

		List<ResearchCategory> rebuilt = new ArrayList<>(packet.categories.size());
		for (PacketSyncResearchList.CategorySnapshot c : packet.categories) {
			ResearchCategory cat = new ResearchCategory(c.icon, c.nameKey);
			rebuilt.add(cat);
		}
		CATEGORIES.addAll(rebuilt);

		List<IReward> noRewards = Collections.emptyList();
		for (PacketSyncResearchList.ResearchSnapshot s : packet.researches) {
			ResearchCategory cat = (s.categoryIdx >= 0 && s.categoryIdx < rebuilt.size())
					? rebuilt.get(s.categoryIdx)
					: null;
			if (cat == null) {
				continue;
			}
			@SuppressWarnings({"rawtypes", "unchecked"})
			List<ICondition> conditions = new ArrayList<>((List) s.conditions);
			Research research = new Research(
					s.name,
					cat,
					s.title,
					s.description,
					new ArrayList<>(s.criteria),
					noRewards,
					noRewards,
					conditions,
					new ArrayList<>(s.icons));
			LIST.put(research.getName(), research);
		}

		// Keep the epoch in sync so a stray server-side add() (e.g. in single-player after sync)
		// doesn't think it's a new reload and wipe the snapshot we just applied.
		lastAppliedEpoch = reloadEpoch;
		clientVersion++;
	}
}
