package snownee.researchtable.client.gui.screen;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import snownee.researchtable.ModConfig;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.api.ICriterion;
import snownee.researchtable.block.TableBlockEntity;
import snownee.researchtable.client.gui.TexturedButton;
import snownee.researchtable.client.gui.container.TableContainer;
import snownee.researchtable.client.renderer.ConditionRenderer;
import snownee.researchtable.core.ConditionTypes;
import snownee.researchtable.core.DataStorage;
import snownee.researchtable.core.Research;
import snownee.researchtable.core.ResearchCategory;
import snownee.researchtable.core.ResearchList;
import snownee.researchtable.network.PacketResearchChanged;
import snownee.researchtable.network.PacketResearchChanged.Action;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TableScreen extends AbstractContainerScreen<TableContainer> {

	private static final ResourceLocation GLOBE = ResearchTable.id("textures/gui/globe.png");
	// 3 separate PNGs at the same pixel size as the button (default 80x20).
	// Drop in PNGs at these paths to override the vanilla button look.
	private static final ResourceLocation BUTTON_NORMAL = ResearchTable.id("textures/gui/button_normal.png");
	private static final ResourceLocation BUTTON_HOVERED = ResearchTable.id("textures/gui/button_hovered.png");
	private static final ResourceLocation BUTTON_DISABLED = ResearchTable.id("textures/gui/button_disabled.png");

	private final List<Research> researches = new ArrayList<>();
	private CompoundTag data = new CompoundTag();
	private ResearchCategory currentCategory;
	private Research selected;
	private int scroll;
	private int descScroll;
	private int conditionScroll;
	private final int slotHeight = 20;
	private static final int CONDITION_ROW_HEIGHT = 24;
	private static final int MAX_VISIBLE_CONDITIONS = 3;
	private int listWidth;
	private int detailWidth;
	private List<Component> scoreText;

	// Layout state computed each frame for click/scroll hit-testing
	private int descViewportLeft;
	private int descViewportRight;
	private int descViewportTop;
	private int descViewportHeight;
	private int descContentHeight;
	private int condViewportLeft;
	private int condViewportRight;
	private int condViewportTop;
	private int condViewportHeight;
	private int condContentHeight;

	private TexturedButton submitButton;
	private TexturedButton actionButton;
	private int lastSeenResearchListVersion = -1;
	private int lastSeenClientDataVersion = -1;

	public TableScreen(TableContainer menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.listWidth = ModConfig.guiListWidth;
		if (ModConfig.guiListAutoWidth) {
			int titleWidth = ResearchList.LIST.values().stream()
					.map(Research::getTitle)
					.mapToInt(s -> Minecraft.getInstance().font.width(s))
					.max().orElse(0);
			listWidth = Math.max(listWidth, 40 + titleWidth);
		}
		TableBlockEntity t = menu.getTile();
		if (t != null) {
			data = t.getData();
		}
		if (!ResearchList.CATEGORIES.isEmpty()) {
			this.currentCategory = ResearchList.CATEGORIES.getFirst();
		}
	}

	@Override
	protected void init() {
		int tabWidth = (ResearchList.CATEGORIES.size() > 1) ? 24 : 0;
		if (ModConfig.guiFullScreen) {
			imageWidth = width;
			imageHeight = height;
			detailWidth = width - listWidth - tabWidth;
		} else {
			detailWidth = ModConfig.guiDetailWidth;
			imageWidth = tabWidth + listWidth + detailWidth + 8;
			imageHeight = ModConfig.guiHeight;
		}
		super.init();

		lastSeenResearchListVersion = ResearchList.clientVersion;
		lastSeenClientDataVersion = DataStorage.clientVersion;
		updateResearchList();

		submitButton = addRenderableWidget(new TexturedButton(-200, -200, 80, 20,
				Component.translatable(ResearchTable.MODID + ".gui.button.submit"),
				b -> onSubmit(),
				BUTTON_NORMAL, BUTTON_HOVERED, BUTTON_DISABLED));
		actionButton = addRenderableWidget(new TexturedButton(-200, -200, 80, 20,
				Component.translatable(ResearchTable.MODID + ".gui.button.research"),
				b -> onAction(),
				BUTTON_NORMAL, BUTTON_HOVERED, BUTTON_DISABLED));

		buildScoreText();
		refreshButtons();
	}

	private void buildScoreText() {
		scoreText = null;
		if (ResearchTable.scoreFormattingText == null || ResearchTable.scores == null) {
			return;
		}
		Integer[] values = new Integer[ResearchTable.scores.length];
		int i = 0;
		for (String s : ResearchTable.scores) {
			values[i] = data.contains("score." + s) ? data.getInt("score." + s) : 0;
			++i;
		}
		String string = ResearchTable.scoreFormattingText;
		if (I18n.exists(string)) {
			string = I18n.get(ResearchTable.scoreFormattingText, (Object[]) values);
		} else {
			try {
				string = String.format(ResearchTable.scoreFormattingText, (Object[]) values);
			} catch (IllegalFormatException ex) {
				string = "Format error: " + string;
			}
		}
		scoreText = new ArrayList<>();
		for (String line : string.split("\\n")) {
			scoreText.add(Component.literal(line));
		}
	}

	private void updateResearchList() {
		researches.clear();
		if (currentCategory == null) {
			return;
		}
		List<Research> available = new ArrayList<>();
		List<Research> unavailable = new ArrayList<>();
		List<Research> completed = new ArrayList<>();
		for (Research research : ResearchList.LIST.values()) {
			if (research.getCategory() != currentCategory) {
				continue;
			}
			if (research.canResearch(minecraft.player, data)) {
				available.add(research);
			} else if (DataStorage.count(minecraft.player.getGameProfile().getId(), research) > 0) {
				if (!ModConfig.hideCompletedResearch) {
					completed.add(research);
				}
			} else if (!ModConfig.hideUnavailableResearch) {
				unavailable.add(research);
			}
		}
		researches.addAll(available);
		researches.addAll(unavailable);
		researches.addAll(completed);
	}

	private void onSubmit() {
		TableBlockEntity tile = menu.getTile();
		if (tile == null || selected == null) {
			return;
		}
		if (tile.getResearch() == selected) {
			playClick();
			PacketDistributor.sendToServer(new PacketResearchChanged(tile.getBlockPos(), selected.getName(), Action.SUBMIT));
		}
	}

	private void onAction() {
		TableBlockEntity tile = menu.getTile();
		if (tile == null || selected == null) {
			return;
		}
		playClick();
		if (tile.getResearch() == null) {
			PacketDistributor.sendToServer(new PacketResearchChanged(tile.getBlockPos(), selected.getName(), Action.START));
		} else if (tile.getResearch() == selected) {
			if (tile.canComplete()) {
				PacketDistributor.sendToServer(new PacketResearchChanged(tile.getBlockPos(), selected.getName(), Action.COMPLETE));
			} else if (Screen.hasShiftDown()) {
				PacketDistributor.sendToServer(new PacketResearchChanged(tile.getBlockPos(), selected.getName(), Action.STOP));
			}
		}
	}

	private void playClick() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	private void refreshButtons() {
		TableBlockEntity tile = menu.getTile();
		if (tile == null || selected == null) {
			submitButton.visible = false;
			actionButton.visible = false;
			actionButton.setTooltip(null);
			return;
		}
		Research researching = tile.getResearch();
		if (researching == selected) {
			boolean canComplete = tile.canComplete();
			submitButton.visible = !canComplete;
			submitButton.active = !canComplete;
			actionButton.visible = true;
			actionButton.active = true;
			actionButton.setMessage(Component.translatable(ResearchTable.MODID + ".gui.button." + (canComplete ? "complete" : "cancel")));
			// In the "cancel" state the click only fires while Shift is held (see onAction);
			// the tooltip surfaces that hidden requirement.
			actionButton.setTooltip(canComplete ? null : Tooltip.create(Component.translatable(ResearchTable.MODID + ".gui.button.shift")));
		} else if (researching == null) {
			submitButton.visible = false;
			actionButton.visible = true;
			actionButton.active = selected.canResearch(minecraft.player, data);
			actionButton.setMessage(Component.translatable(ResearchTable.MODID + ".gui.button.research"));
			actionButton.setTooltip(null);
		} else {
			submitButton.visible = false;
			actionButton.visible = false;
			actionButton.setTooltip(null);
		}
	}

	@Override
	public void containerTick() {
		super.containerTick();
		// If the server pushed a new snapshot (player join / post-reload), our `selected` and
		// `currentCategory` may now reference dead objects. Drop them and rebuild the list.
		if (ResearchList.clientVersion != lastSeenResearchListVersion) {
			lastSeenResearchListVersion = ResearchList.clientVersion;
			if (selected != null && !ResearchList.LIST.containsKey(selected.getName())) {
				selected = null;
				descScroll = 0;
				conditionScroll = 0;
			}
			if (currentCategory == null || !ResearchList.CATEGORIES.contains(currentCategory)) {
				currentCategory = ResearchList.CATEGORIES.isEmpty() ? null : ResearchList.CATEGORIES.getFirst();
			}
			updateResearchList();
			buildScoreText();
			refreshButtons();
		}
		if (DataStorage.clientVersion != lastSeenClientDataVersion) {
			lastSeenClientDataVersion = DataStorage.clientVersion;
			updateResearchList();
			refreshButtons();
		}
		TableBlockEntity tile = menu.getTile();
		if (tile != null && tile.hasChanged) {
			data = tile.getData();
			updateResearchList();
			buildScoreText();
			refreshButtons();
			tile.hasChanged = false;
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		// Fill only the GUI rect (centered when not full-screen) with a soft gray that's slightly darker
		// than the left sidebar (0xEEEEEE). The rest of the screen stays dim from renderBackground.
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFD8D8D8);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// 1. Compute detail-panel layout & reposition button widgets BEFORE super.render
		positionButtons();
		// 2. Vanilla dim + renderBg + widget rendering (including our repositioned buttons)
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		// 3. Custom overlays (lists, detail panel content, tabs, globe)
		renderLeftList(graphics, mouseX, mouseY);
		renderDetail(graphics);
		renderTabs(graphics, mouseX, mouseY);
		renderScoreGlobe(graphics, mouseX, mouseY);
	}

	private void positionButtons() {
		if (selected == null) {
			submitButton.visible = false;
			actionButton.visible = false;
			return;
		}
		int leftPanel = leftPos + ((ResearchList.CATEGORIES.size() > 1) ? 24 : 0) + listWidth + 4;
		int rightPanel = leftPanel + detailWidth - 8;
		int contentWidth = rightPanel - leftPanel;

		refreshButtons();

		// Layout order: title → description → conditions → failing → buttons.
		// Same recipe is used in renderDetail; keep them in sync.
		int top = topPos + 4 + 14; // after title
		top += descriptionViewportHeight(contentWidth) + 4;
		top += Math.min(selected.getConditions().size(), MAX_VISIBLE_CONDITIONS) * CONDITION_ROW_HEIGHT + 4;
		int failingH = failingTextsHeight(contentWidth);
		top += failingH;
		if (failingH > 0) {
			top += 4;
		}

		int buttonRowTop = top;
		int gap = 4;
		int totalWidth = 0;
		if (submitButton.visible) {
			totalWidth += submitButton.getWidth();
		}
		if (actionButton.visible) {
			totalWidth += actionButton.getWidth();
		}
		if (submitButton.visible && actionButton.visible) {
			totalWidth += gap;
		}
		int bx = rightPanel - totalWidth - 4;
		if (submitButton.visible) {
			submitButton.setX(bx);
			submitButton.setY(buttonRowTop);
			bx += submitButton.getWidth() + gap;
		}
		if (actionButton.visible) {
			actionButton.setX(bx);
			actionButton.setY(buttonRowTop);
		}
	}

	/**
	 * Height reserved for the failing/limit text block at the bottom of the detail panel.
	 * Returns 0 when the section isn't shown (player is researching this or it's available).
	 */
	private int failingTextsHeight(int contentWidth) {
		if (selected == null) {
			return 0;
		}
		TableBlockEntity tile = menu.getTile();
		Research researching = tile != null ? tile.getResearch() : null;
		if (researching == selected || selected.canResearch(minecraft.player, data)) {
			return 0;
		}
		int h = 0;
		for (ICriterion criterion : selected.getCriteria()) {
			if (criterion.matches(minecraft.player, data)) continue;
			String failingText = criterion.getFailingText(minecraft.player, data);
			List<FormattedCharSequence> lines = font.split(Component.literal(failingText), contentWidth);
			h += lines.size() * font.lineHeight;
		}
		return h;
	}

	/**
	 * Description fills whatever vertical room is left after title, conditions, failing, and
	 * buttons claim their share. Always non-negative; floors to 0 if the window is too short.
	 */
	private int descriptionViewportHeight(int contentWidth) {
		if (selected == null) {
			return 0;
		}
		int titleH = 14;
		int condsH = Math.min(selected.getConditions().size(), MAX_VISIBLE_CONDITIONS) * CONDITION_ROW_HEIGHT;
		int failingH = failingTextsHeight(contentWidth);
		boolean btn = (submitButton != null && (submitButton.visible || actionButton.visible));
		int btnH = btn ? 22 : 0;
		int padding = 4 /* top */ + 4 /* after desc */ + 4 /* after conds */ + (failingH > 0 ? 4 : 0) + 4 /* bottom */;
		int reserved = titleH + condsH + failingH + btnH + padding;
		return Math.max(0, imageHeight - reserved);
	}

	private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
		if (ResearchList.CATEGORIES.size() <= 1) {
			return;
		}
		int x = leftPos + 2;
		int y = topPos + 4;
		for (ResearchCategory category : ResearchList.CATEGORIES) {
			int bg = (category == currentCategory) ? 0xFFEEEEEE : 0xFF919191;
			g.fill(x, y, x + 20, y + 20, bg);
			g.renderItem(category.icon(), x + 2, y + 2);
			if (mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20) {
				if (category.nameKey() != null) {
					g.renderTooltip(font, Component.translatable(category.nameKey()), mouseX, mouseY + 8);
				}
			}
			y += 22;
		}
	}

	private void renderLeftList(GuiGraphics g, int mouseX, int mouseY) {
		int left = leftPos + ((ResearchList.CATEGORIES.size() > 1) ? 24 : 0);
		int top = topPos;
		int right = left + listWidth;
		int bottom = topPos + imageHeight;
		g.fill(left, top, right, bottom, 0xFFEEEEEE);

		g.enableScissor(left, top, right, bottom);
		int y = top + 4 - scroll;
		for (int i = 0; i < researches.size(); ++i) {
			Research r = researches.get(i);
			int slotTop = y + i * slotHeight;
			if (slotTop + slotHeight < top || slotTop > bottom) {
				continue;
			}
			boolean hover = mouseX >= left && mouseX < right && mouseY >= slotTop && mouseY < slotTop + slotHeight;
			boolean isSelected = r == selected;
			int color = isSelected ? 0xFFCCCCFF : hover ? 0xFFDDDDDD : 0xFFEEEEEE;
			g.fill(left, slotTop, right, slotTop + slotHeight - 1, color);
			g.renderItem(r.getIcon(), left + 2, slotTop + 2);
			String title = r.getTitle();
			boolean dim = !r.canResearch(minecraft.player, data);
			int textColor = dim ? 0x808080 : 0x000000;
			g.drawString(font, title, left + 22, slotTop + 6, textColor, false);
		}
		g.disableScissor();
	}

	private void renderDetail(GuiGraphics g) {
		int leftPanel = leftPos + ((ResearchList.CATEGORIES.size() > 1) ? 24 : 0) + listWidth + 4;
		int rightPanel = leftPanel + detailWidth - 8;
		int contentWidth = rightPanel - leftPanel;
		int top = topPos + 6;
		if (selected == null) {
			descViewportHeight = 0;
			condViewportHeight = 0;
			return;
		}

		// Title
		g.drawString(font, Component.literal(selected.getTitle()), leftPanel, top, 0x202020, false);
		top += 14;

		// Description (scrollable) — sized to absorb remaining vertical space.
		int descHeight = descriptionViewportHeight(contentWidth);
		renderScrollableDescription(g, leftPanel, top, rightPanel, descHeight);
		top += descHeight + 4;

		// Conditions (research requirements) — clipped to MAX_VISIBLE_CONDITIONS rows, extras scroll.
		TableBlockEntity tile = menu.getTile();
		Research researching = tile != null ? tile.getResearch() : null;
		List<ICondition<?>> conditions = selected.getConditions();
		renderScrollableConditions(g, leftPanel, top, rightPanel, conditions, researching == selected);
		int visibleConditions = Math.min(conditions.size(), MAX_VISIBLE_CONDITIONS);
		top += visibleConditions * CONDITION_ROW_HEIGHT;
		top += 4;

		// Limit / failing texts — only when the player is not researching this one and the criteria block them.
		if (researching != selected && !selected.canResearch(minecraft.player, data)) {
			for (ICriterion criterion : selected.getCriteria()) {
				if (criterion.matches(minecraft.player, data)) {
					continue;
				}
				String failingText = criterion.getFailingText(minecraft.player, data);
				List<FormattedCharSequence> lines = font.split(Component.literal(failingText), contentWidth);
				for (FormattedCharSequence line : lines) {
					g.drawString(font, line, leftPanel, top, 0xCC0000, false);
					top += font.lineHeight;
				}
			}
		}
	}

	private void renderScrollableConditions(GuiGraphics g, int leftPanel, int top, int rightPanel,
			List<ICondition<?>> conditions, boolean isResearching) {
		int visibleRows = Math.min(conditions.size(), MAX_VISIBLE_CONDITIONS);
		condViewportLeft = leftPanel;
		condViewportRight = rightPanel;
		condViewportTop = top;
		condViewportHeight = visibleRows * CONDITION_ROW_HEIGHT;
		condContentHeight = conditions.size() * CONDITION_ROW_HEIGHT;

		boolean needsScrollbar = conditions.size() > MAX_VISIBLE_CONDITIONS;
		int maxScroll = Math.max(0, condContentHeight - condViewportHeight);
		if (conditionScroll < 0) {
			conditionScroll = 0;
		}
		if (conditionScroll > maxScroll) {
			conditionScroll = maxScroll;
		}

		if (condViewportHeight <= 0) {
			return;
		}

		int scrollbarWidth = 4;
		int conditionRight = needsScrollbar ? condViewportRight - scrollbarWidth - 4 : condViewportRight - 4;

		g.enableScissor(condViewportLeft, condViewportTop, condViewportRight, condViewportTop + condViewportHeight);
		int y = condViewportTop - conditionScroll;
		for (int i = 0; i < conditions.size(); i++) {
			if (y + CONDITION_ROW_HEIGHT >= condViewportTop && y <= condViewportTop + condViewportHeight) {
				renderCondition(g, condViewportLeft, y + 1, conditionRight, conditions.get(i), i, isResearching);
			}
			y += CONDITION_ROW_HEIGHT;
		}
		g.disableScissor();

		if (needsScrollbar) {
			int sbX = condViewportRight - scrollbarWidth;
			g.fill(sbX, condViewportTop, sbX + scrollbarWidth, condViewportTop + condViewportHeight, 0xFFB0B0B0);
			int thumbHeight = Math.max(20, condViewportHeight * condViewportHeight / condContentHeight);
			int thumbY = condViewportTop + (maxScroll == 0 ? 0 : (condViewportHeight - thumbHeight) * conditionScroll / maxScroll);
			g.fill(sbX, thumbY, sbX + scrollbarWidth, thumbY + thumbHeight, 0xFF707070);
		}
	}

	private void renderScrollableDescription(GuiGraphics g, int leftPanel, int top, int rightPanel, int viewportHeight) {
		descViewportLeft = leftPanel;
		descViewportRight = rightPanel;
		descViewportTop = top;
		descViewportHeight = Math.max(0, viewportHeight);
		if (descViewportHeight == 0) {
			return;
		}

		String desc = selected.getDescription();
		if (desc == null || desc.isEmpty()) {
			descContentHeight = 0;
			return;
		}

		int scrollbarWidth = 4;
		int textRight = rightPanel - scrollbarWidth - 4;
		int textWidth = textRight - leftPanel;
		List<FormattedCharSequence> lines = font.split(Component.literal(desc), textWidth);
		descContentHeight = lines.size() * font.lineHeight;

		int maxScroll = Math.max(0, descContentHeight - descViewportHeight);
		if (descScroll < 0) {
			descScroll = 0;
		}
		if (descScroll > maxScroll) {
			descScroll = maxScroll;
		}

		g.enableScissor(descViewportLeft, descViewportTop, descViewportRight, descViewportTop + descViewportHeight);
		int y = descViewportTop - descScroll;
		for (FormattedCharSequence line : lines) {
			if (y + font.lineHeight >= descViewportTop && y <= descViewportTop + descViewportHeight) {
				g.drawString(font, line, descViewportLeft, y, 0x404040, false);
			}
			y += font.lineHeight;
		}
		g.disableScissor();

		if (maxScroll > 0) {
			int sbX = descViewportRight - scrollbarWidth;
			g.fill(sbX, descViewportTop, sbX + scrollbarWidth, descViewportTop + descViewportHeight, 0xFFB0B0B0);
			int thumbHeight = Math.max(20, descViewportHeight * descViewportHeight / descContentHeight);
			int thumbY = descViewportTop + (descViewportHeight - thumbHeight) * descScroll / maxScroll;
			g.fill(sbX, thumbY, sbX + scrollbarWidth, thumbY + thumbHeight, 0xFF707070);
		}
	}

	private void renderCondition(GuiGraphics g, int left, int top, int barRight, ICondition<?> condition, int idx, boolean isResearching) {
		TableBlockEntity tile = menu.getTile();
		long target = condition.getGoal();
		long current = isResearching && tile != null ? tile.getProgress(idx) : 0;
		// Track (slightly darker than panel) + outline
		g.fill(left, top, barRight, top + 22, 0xFFC4C4C4);
		g.fill(left, top, barRight, top + 1, 0xFFA8A8A8);
		g.fill(left, top + 21, barRight, top + 22, 0xFFA8A8A8);
		g.fill(left, top, left + 1, top + 22, 0xFFA8A8A8);
		g.fill(barRight - 1, top, barRight, top + 22, 0xFFA8A8A8);
		if (isResearching && target > 0) {
			double progress = (double) current / (double) target;
			progress = Math.clamp(progress, 0, 1);
			int innerLeft = left + 1;
			int innerRight = barRight - 1;
			int fillRight = innerLeft + (int) Math.round((innerRight - innerLeft) * progress);
			if (fillRight > innerLeft) {
				g.fill(innerLeft, top + 1, fillRight, top + 21, 0xFF7BC97B);
			}
			String pct = String.format("%d%%", (int) (progress * 100));
			g.drawString(font, pct, barRight - font.width(pct) - 4, top + 7, 0x202020, false);
		}

		ConditionRenderer<?> renderer = ConditionRenderer.get(condition);
		if (renderer != null) {
			renderer.draw(g, minecraft, left + 3, top + 3);
			String text = renderer.name();
			if (isResearching) {
				text += " (" + renderer.format(current) + "/" + renderer.format(target) + ")";
			} else {
				text += " (" + renderer.format(target) + ")";
			}
			g.drawString(font, text, left + 24, top + 7, 0x303030, false);
		} else {
			String name = describeCondition(condition);
			g.drawString(font, name, left + 4, top + 7, 0x303030, false);
		}
	}

	private String describeCondition(ICondition<?> condition) {
		String type;
		if (Objects.equals(condition.getMatchType(), ConditionTypes.ITEM)) {
			type = "Item";
		} else if (Objects.equals(condition.getMatchType(), ConditionTypes.FLUID)) {
			type = "Fluid";
		} else if (Objects.equals(condition.getMatchType(), ConditionTypes.ENERGY)) {
			type = I18n.get(ResearchTable.MODID + ".gui.fe");
		} else {
			type = condition.getClass().getSimpleName();
		}
		return type + " x " + condition.getGoal();
	}

	private void renderScoreGlobe(GuiGraphics g, int mouseX, int mouseY) {
		if (scoreText == null || scoreText.isEmpty()) {
			return;
		}
		int listLeft = leftPos + ((ResearchList.CATEGORIES.size() > 1) ? 24 : 0);
		int x = listLeft + listWidth - 18;
		int y = topPos + imageHeight - 18;
		RenderSystem.setShaderColor(1, 1, 1, 1);
		g.blit(GLOBE, x, y, 0, 0, 11, 10, 11, 10);
		if (mouseX >= x && mouseX <= x + 11 && mouseY >= y && mouseY <= y + 10) {
			g.renderTooltip(font, scoreText, java.util.Optional.empty(), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			// Tab click
			if (ResearchList.CATEGORIES.size() > 1) {
				int x = leftPos + 2, y = topPos + 4;
				for (ResearchCategory cat : ResearchList.CATEGORIES) {
					if (mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20) {
						currentCategory = cat;
						updateResearchList();
						playClick();
						return true;
					}
					y += 22;
				}
			}
			// List click
			int left = leftPos + ((ResearchList.CATEGORIES.size() > 1) ? 24 : 0);
			int right = left + listWidth;
			int listTop = topPos;
			int listBottom = topPos + imageHeight;
			if (mouseX >= left && mouseX < right && mouseY >= listTop && mouseY < listBottom) {
				int y = listTop + 4 - scroll;
				for (int i = 0; i < researches.size(); ++i) {
					int slotTop = y + i * slotHeight;
					if (mouseY >= slotTop && mouseY < slotTop + slotHeight) {
						if (selected != researches.get(i)) {
							selected = researches.get(i);
							descScroll = 0;
							conditionScroll = 0;
						}
						refreshButtons();
						playClick();
						return true;
					}
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int left = leftPos + ((ResearchList.CATEGORIES.size() > 1) ? 24 : 0);
		int right = left + listWidth;
		int listTop = topPos;
		int listBottom = topPos + imageHeight;
		if (mouseX >= left && mouseX < right && mouseY >= listTop && mouseY < listBottom) {
			scroll -= (int) (scrollY * 12);
			int maxScroll = Math.max(0, researches.size() * slotHeight + 8 - imageHeight);
			scroll = Math.clamp(scroll, 0, maxScroll);
			return true;
		}
		// Condition list scroll
		if (condViewportHeight > 0
				&& mouseX >= condViewportLeft && mouseX < condViewportRight
				&& mouseY >= condViewportTop && mouseY < condViewportTop + condViewportHeight) {
			conditionScroll -= (int) (scrollY * 12);
			// Clamp happens in renderScrollableConditions each frame
			return true;
		}
		// Description scroll
		if (descViewportHeight > 0
				&& mouseX >= descViewportLeft && mouseX < descViewportRight
				&& mouseY >= descViewportTop && mouseY < descViewportTop + descViewportHeight) {
			descScroll -= (int) (scrollY * 12);
			// Clamp happens in renderScrollableDescription each frame
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		// Disable default labels rendering.
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		data = new CompoundTag();
		super.removed();
	}
}
