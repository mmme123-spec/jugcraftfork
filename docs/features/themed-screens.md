# Themed screens: the Concordance's arcane look

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. The contributor asked for the plain screens to "look cool", with reference pictures of a hex quest book, a spellbook interface, a quest tree and a relay screen. They looked at mock-ups first and kept the Peepo and Cooking Pot screens as they are. The references set the mood only; nothing in them is traced or copied.

Owner: jimbozoomer-byte
Target milestone and tier: Concordance interface (no tier change)
Primary specialty and supported player role: players using the Concordance Journal

## Player experience

The **Concordance Journal** now has the Concordance's arcane look:

- Minecraft's own pixel font instead of the smooth app font.
- A deep indigo star-chart page in a gold frame, with gilt stars on the corners.
- A faint drawn sigil low on the journal's right, under the text.
- Gold section tabs, and violet buttons edged in gold that glow when hovered.

This covers both versions of the journal: the GuiLib workspace players see in the Complete pack, and the plain screen used without GuiLib. The plain screen's buttons are the new `ArcaneButton`, a vanilla button underneath, so Tab, narration and clicks are unchanged.

The Spell Composer is not changed: it already has its own star-chart screen ("Weave a spell among the stars") from [PR #415](https://github.com/jimbozoomer-byte/jugcraft/pull/415). Its old GuiLib stylesheet, `assets/jugcraft/ui/composer.css`, is no longer opened by anything, so it is left as it is.

Nothing the screens show or do changes. They still show only what the server sends.

## How it is made

- `tools/ui_kit.py` draws the parts in code. `python tools/ui_kit.py` writes the three textures used here to `assets/jugcraft/textures/gui/concordance/`:
  - `star_page.png`, a 128 × 128 tile;
  - `sigil.png`, 128 × 128 with transparency;
  - `corner_star.png`, 9 × 9.
- `assets/jugcraft/ui/journal.css` (GuiLib, already an approved optional client library) uses them as background layers. It sets GuiLib's theme colours and Minecraft's font.
- `JournalScreen` tiles the same textures and uses `ArcaneButton`.

No other mod's art is used. The kit also holds an oak-and-parchment "homestead" look for farm and kitchen screens, which nothing uses yet.

## Connections

- **Input producer / output consumer:** unchanged (the journal reads what the server sends).
- **Costs, unlocks and failure behaviour:** unchanged.

## Multiplayer and persistence

Client-side looks only. No registrations, saved data or text strings change.

## Verification

**Local checks** (no Gradle or game here): `python scripts/check_repository.py` passed.

**Existing client test that photographs these screens:** `ConcordanceJournalClientGameTests` takes `jugcraft_concordance_journal_simple` (the plain screen) and `jugcraft_concordance_journal_workspace` (GuiLib). It now clears the advancement pop-ups first, which had covered the journal's top right corner.

**Results:** not yet run; the fork's Build workflow results will be recorded here.

**Not run:** a hands-on look in a running game.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
