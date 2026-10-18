package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalLine;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalSection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * The simple Concordance Journal (roadmap step 26): the journal the server sent, one section at a time, in plain
 * vanilla widgets, so it works without GuiLib, with every control on the keyboard and read out by the narrator. The
 * section buttons (or Left and Right) change section; Tab moves between buttons; Up, Down, Page Up, Page Down and the
 * mouse wheel scroll; R asks the server again; X shows or hides the exact figures; Compose opens the Spell Composer. Light text on a dark panel; nothing
 * here relies on colour alone (the lines say what they mean). It only shows what the server sent. It wears the GuiLib
 * journal's arcane look (docs/features/themed-screens.md): a gold-framed star-chart page with a faint sigil and gilt
 * corner stars (textures/gui/concordance/, drawn by tools/ui_kit.py) and {@link ArcaneButton}s.
 */
public class JournalScreen extends Screen {
	private static final int MAX_WIDTH = 420;
	private static final int HEADER = 34;
	private static final int FOOTER = 48;
	private static final int PAD = 8;
	private static final int SCROLL_STEP = 10;
	private static final Identifier PAGE = Jugcraft.id("textures/gui/concordance/star_page.png");
	private static final Identifier SIGIL = Jugcraft.id("textures/gui/concordance/sigil.png");
	private static final Identifier STAR = Jugcraft.id("textures/gui/concordance/corner_star.png");
	private static final int OUTLINE = 0xFF0A0614;
	private static final int GOLD = 0xFFCEA652;
	private static final int RULE = 0xFF7A5A22;
	private static final int WELL = 0x990C0818;
	private static final int EDGE = 0xFF8F74D8;
	private static final int TEXT = 0xFFF2ECFF;
	private static final int TITLE = 0xFFFFD98A;
	private static final int DIM = 0xFFB9AEE0;

	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private int scroll;
	private String selected = "";
	private Button previous;
	private Button next;
	private Button exact;
	/** The wrapped lines of the open section, made again when the journal, the section, the width or the switch changes. */
	private List<FormattedCharSequence> wrapped = List.of();
	private List<JournalSection> wrappedFrom;
	private String wrappedSection = "";
	private int wrappedWidth;
	private boolean wrappedExact;

	public JournalScreen() {
		super(Component.translatable("screen.jugcraft.journal.title"));
	}

	@Override
	protected void init() {
		panelWidth = Math.min(MAX_WIDTH, width - 20);
		panelHeight = height - 24;
		left = (width - panelWidth) / 2;
		top = 12;
		previous = addRenderableWidget(new ArcaneButton(left + PAD, top + 14, 20, 16, Component.literal("<"), button -> turn(-1)));
		next = addRenderableWidget(new ArcaneButton(left + panelWidth - PAD - 20, top + 14, 20, 16, Component.literal(">"), button -> turn(1)));
		int buttonsY = top + panelHeight - 24;
		int quarter = (panelWidth - 2 * PAD - 12) / 4;
		addRenderableWidget(new ArcaneButton(left + PAD, buttonsY, quarter, 20,
				Component.translatable("screen.jugcraft.journal.refresh"), button -> JournalClient.request()));
		exact = addRenderableWidget(new ArcaneButton(left + PAD + quarter + 4, buttonsY, quarter, 20, exactLabel(), button -> toggleExact()));
		// The Spell Composer, which comes back here.
		addRenderableWidget(new ArcaneButton(left + PAD + 2 * (quarter + 4), buttonsY, quarter, 20,
				Component.translatable("screen.jugcraft.journal.compose"), button -> ComposerClient.open(minecraft, this)));
		addRenderableWidget(new ArcaneButton(left + PAD + 3 * (quarter + 4), buttonsY, quarter, 20,
				Component.translatable("gui.done"), button -> onClose()));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static Component exactLabel() {
		return Component.translatable(ConcordanceClientOptions.exactValues() ? "screen.jugcraft.journal.exact_on" : "screen.jugcraft.journal.exact_off");
	}

	private void toggleExact() {
		ConcordanceClientOptions.setExactValues(!ConcordanceClientOptions.exactValues());
		exact.setMessage(exactLabel());
	}

	/** The section shown: the one chosen, or the first when it is gone. */
	private int index(List<JournalSection> sections) {
		for (int i = 0; i < sections.size(); i++) {
			if (sections.get(i).id().equals(selected)) {
				return i;
			}
		}
		return 0;
	}

	private void turn(int step) {
		List<JournalSection> sections = JournalClient.sections();
		if (sections.isEmpty()) {
			return;
		}
		selected = sections.get(Math.floorMod(index(sections) + step, sections.size())).id();
		scroll = 0;
	}

	private int bodyTop() {
		return top + HEADER;
	}

	private int bodyBottom() {
		return top + panelHeight - FOOTER;
	}

	private int textWidth() {
		return panelWidth - 2 * PAD - 6;
	}

	private void scrollBy(int step) {
		int visible = bodyBottom() - bodyTop();
		scroll = Math.max(0, Math.min(scroll + step, Math.max(0, wrapped.size() * font.lineHeight - visible)));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scrollBy((int) Math.round(-scrollY * SCROLL_STEP));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.input();
		int page = bodyBottom() - bodyTop() - SCROLL_STEP;
		if (key == InputConstants.KEY_LEFT) {
			turn(-1);
		} else if (key == InputConstants.KEY_RIGHT) {
			turn(1);
		} else if (key == InputConstants.KEY_UP) {
			scrollBy(-SCROLL_STEP);
		} else if (key == InputConstants.KEY_DOWN) {
			scrollBy(SCROLL_STEP);
		} else if (key == InputConstants.KEY_PAGEUP) {
			scrollBy(-page);
		} else if (key == InputConstants.KEY_PAGEDOWN) {
			scrollBy(page);
		} else if (key == InputConstants.KEY_R) {
			JournalClient.request();
		} else if (key == InputConstants.KEY_X) {
			toggleExact();
		} else {
			return super.keyPressed(event);
		}
		return true;
	}

	/** Wraps the open section's lines (each line's exact figures after it, in grey, when shown). */
	private void wrap(List<JournalSection> sections) {
		boolean showExact = ConcordanceClientOptions.exactValues();
		JournalSection section = sections.isEmpty() ? null : sections.get(index(sections));
		String id = section == null ? "" : section.id();
		if (sections == wrappedFrom && id.equals(wrappedSection) && textWidth() == wrappedWidth && showExact == wrappedExact) {
			return;
		}
		List<FormattedCharSequence> lines = new ArrayList<>();
		if (section != null) {
			for (JournalLine line : section.lines()) {
				Component text = showExact && line.exact().isPresent()
						? line.text().copy().append(" ").append(line.exact().get().copy().withStyle(ChatFormatting.GRAY)) : line.text();
				lines.addAll(font.split(text, textWidth()));
			}
		}
		wrapped = lines;
		wrappedFrom = sections;
		wrappedSection = id;
		wrappedWidth = textWidth();
		wrappedExact = showExact;
		scrollBy(0);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		graphics.fill(left - 3, top - 3, right + 3, bottom + 3, OUTLINE);
		graphics.fill(left - 2, top - 2, right + 2, bottom + 2, GOLD);
		// The star-chart page, tiled, with the sigil low on the right under the text.
		graphics.enableScissor(left, top, right, bottom);
		for (int x = left; x < right; x += 128) {
			for (int y = top; y < bottom; y += 128) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, PAGE, x, y, 0, 0, 128, 128, 128, 128, 128, 128);
			}
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, SIGIL, right - PAD - 132, bodyBottom() - 132, 0, 0, 128, 128, 128, 128, 128, 128);
		graphics.disableScissor();
		for (int x : new int[] {left - 4, right - 5}) {
			for (int y : new int[] {top - 4, bottom - 5}) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, STAR, x, y, 0, 0, 9, 9, 9, 9, 9, 9);
			}
		}
		// A gold rule under the heading, and a dark well behind the lines.
		graphics.fill(left + PAD, top + 32, right - PAD, top + 33, RULE);
		graphics.fill(left + PAD - 2, bodyTop() - 2, left + PAD + textWidth() + 8, bodyBottom() + 1, WELL);
		graphics.text(font, title, left + (panelWidth - font.width(title)) / 2, top + 3, TITLE, true);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		List<JournalSection> sections = JournalClient.sections();
		previous.visible = sections.size() > 1;
		next.visible = sections.size() > 1;
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int x = left + PAD;
		if (!JournalClient.received()) {
			graphics.text(font, Component.translatable("screen.jugcraft.journal.loading"), x, bodyTop(), DIM, false);
			return;
		}
		if (sections.isEmpty()) {
			graphics.text(font, Component.translatable("screen.jugcraft.journal.empty"), x, bodyTop(), DIM, false);
			return;
		}
		int index = index(sections);
		Component heading = Component.translatable("screen.jugcraft.journal.section", index + 1, sections.size(), sections.get(index).title());
		graphics.text(font, heading, left + (panelWidth - font.width(heading)) / 2, top + 18, TITLE, true);
		wrap(sections);
		graphics.enableScissor(x, bodyTop(), x + textWidth() + 6, bodyBottom());
		int y = bodyTop() - scroll;
		for (FormattedCharSequence line : wrapped) {
			if (y + font.lineHeight > bodyTop() && y < bodyBottom()) {
				graphics.text(font, line, x, y, TEXT, false);
			}
			y += font.lineHeight;
		}
		graphics.disableScissor();
		int total = wrapped.size() * font.lineHeight;
		int visible = bodyBottom() - bodyTop();
		if (total > visible) {
			int barHeight = Math.max(8, visible * visible / total);
			int barY = bodyTop() + (visible - barHeight) * scroll / Math.max(1, total - visible);
			graphics.fill(x + textWidth() + 3, bodyTop(), x + textWidth() + 5, bodyBottom(), 0xFF1E1438);
			graphics.fill(x + textWidth() + 3, barY, x + textWidth() + 5, barY + barHeight, EDGE);
		}
		int helpY = bodyBottom() + 4;
		for (FormattedCharSequence help : font.split(Component.translatable("screen.jugcraft.journal.help"), panelWidth - 2 * PAD)) {
			if (helpY + font.lineHeight <= top + panelHeight - 24) {
				graphics.text(font, help, x, helpY, DIM, false);
			}
			helpY += font.lineHeight;
		}
	}
}
