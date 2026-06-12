package snownee.researchtable.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.api.IReward;
import snownee.researchtable.network.PacketSyncResearchList;

public final class ResearchList {
	public static final List<ResearchCategory> CATEGORIES = Lists.newArrayList();
	public static final Map<String, Research> LIST = Maps.newLinkedHashMap();

	private static State pending;

	// Bumped each time the snapshot is mutated wholesale (clear / applySnapshot). The open GUI
	// polls this in containerTick to know when to drop a now-stale `selected` / `currentCategory`.
	public static volatile int clientVersion = 0;

	private ResearchList() {
	}

	public static synchronized void beginReload(ResearchDataLoader.Result result) {
		State state = new State();
		for (Research research : result.researches()) {
			add(state, research);
		}
		state.scoreFormattingText = result.scoreFormattingText();
		state.scores = result.scores().length == 0 ? null : result.scores().clone();
		pending = state;
	}

	public static synchronized boolean hasPendingReload() {
		return pending != null;
	}

	public static synchronized void finishReload() {
		if (pending == null) {
			return;
		}
		applyState(pending);
		pending = null;
	}

	private static void clearState() {
		LIST.clear();
		CATEGORIES.clear();
		ResearchTable.scores = null;
		ResearchTable.scoreFormattingText = null;
	}

	public static synchronized void clear() {
		if (pending != null) {
			pending.clear();
		} else {
			clearState();
			clientVersion++;
		}
	}

	public static synchronized void ensureReloadApplied() {
		// Kept for binary/source compatibility with integration entrypoints.
		// Reload clearing is now centralized through the pending state created in prepare().
	}

	public static synchronized boolean add(Research research) {
		if (pending != null) {
			return add(pending, research);
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

	public static synchronized boolean remove(String name) {
		if (pending != null) {
			return pending.remove(name);
		}
		Research removed = LIST.remove(name);
		if (removed == null) {
			return false;
		}
		CATEGORIES.removeIf(category -> LIST.values().stream().noneMatch(research -> research.getCategory() == category));
		clientVersion++;
		return true;
	}

	public static synchronized Optional<Research> find(String name) {
		if (pending != null) {
			return Optional.ofNullable(pending.list.get(name));
		}
		return Optional.ofNullable(ResearchList.LIST.get(name));
	}

	public static synchronized void setScoreIndicator(String formattingText, String... scores) {
		if (pending != null) {
			pending.scoreFormattingText = formattingText;
			pending.scores = scores.clone();
		} else {
			ResearchTable.scoreFormattingText = formattingText;
			ResearchTable.scores = scores.clone();
			clientVersion++;
		}
	}

	/**
	 * Replaces the entire research registry from a server-sent snapshot. Used on the client side
	 * after receiving {@link PacketSyncResearchList}. Server-only fields (rewards/triggers) are
	 * left empty since the client never executes them.
	 */
	public static synchronized void applySnapshot(PacketSyncResearchList packet) {
		pending = null;
		clearState();

		ResearchTable.scoreFormattingText = packet.scoreFormattingText;
		ResearchTable.scores = packet.scores == null || packet.scores.length == 0 ? null : packet.scores.clone();

		List<ResearchCategory> rebuilt = new ArrayList<>(packet.categories.size());
		for (PacketSyncResearchList.CategorySnapshot c : packet.categories) {
			ResearchCategory cat = new ResearchCategory(c.icon(), c.nameKey());
			rebuilt.add(cat);
		}
		CATEGORIES.addAll(rebuilt);

		List<IReward> noRewards = Collections.emptyList();
		for (PacketSyncResearchList.ResearchSnapshot s : packet.researches) {
			ResearchCategory cat = (s.categoryIdx() >= 0 && s.categoryIdx() < rebuilt.size())
					? rebuilt.get(s.categoryIdx())
					: null;
			if (cat == null) {
				continue;
			}
			List<ICondition<?>> conditions = new ArrayList<>(s.conditions());
			Research research = new Research(
					s.name(),
					cat,
					s.title(),
					s.description(),
					new ArrayList<>(s.criteria()),
					noRewards,
					noRewards,
					conditions,
					new ArrayList<>(s.icons()));
			LIST.put(research.getName(), research);
		}

		clientVersion++;
	}

	private static boolean add(State state, Research research) {
		if (state.list.containsKey(research.getName())) {
			return false;
		}
		if (!state.categories.contains(research.getCategory())) {
			state.categories.add(research.getCategory());
		}
		state.list.put(research.getName(), research);
		return true;
	}

	private static void applyState(State state) {
		clearState();
		CATEGORIES.addAll(state.categories);
		LIST.putAll(state.list);
		ResearchTable.scoreFormattingText = state.scoreFormattingText;
		ResearchTable.scores = state.scores == null || state.scores.length == 0 ? null : state.scores.clone();
		clientVersion++;
	}

	private static final class State {
		private final List<ResearchCategory> categories = new ArrayList<>();
		private final Map<String, Research> list = new LinkedHashMap<>();
		private String scoreFormattingText;
		private String[] scores;

		private void clear() {
			categories.clear();
			list.clear();
			scoreFormattingText = null;
			scores = null;
		}

		private boolean remove(String name) {
			Research removed = list.remove(name);
			if (removed == null) {
				return false;
			}
			categories.removeIf(category -> list.values().stream().noneMatch(research -> research.getCategory() == category));
			return true;
		}
	}
}
