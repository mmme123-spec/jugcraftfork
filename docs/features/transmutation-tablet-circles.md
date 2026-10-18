# Transmutation tablet on the circle art

Status: implemented on the contributor's fork, pending review. **Open to change:** the contributor asked for this look and expects the owner may rework it, so the layout is kept in one screen class and one slot table.

Proposal issue: none. The contributor asked for the tablet to look like the classic transmutation table (two magic circles, items round the right one) instead of the dark panel layout from [PR #415](https://github.com/jimbozoomer-byte/jugcraft/pull/415).

Owner: jimbozoomer-byte
Target milestone and tier: Concordance alchemy (the existing tablet; no tier change)
Primary specialty and supported player role: alchemists using the Transmutation Tablet, Transmutation Stone, alchemy stone or Mercurial Eye

## Player experience

The tablet now uses the owner's supplied `textures/gui/alchemy/transmutation.png` (copied unchanged from `art/owner-library/originals/Magic/textures/gui/transmute.png`; its provenance is already recorded in `tools/concordance_alchemical_gear.py`) at its own size, on a light grey sheet cut from the same art's border:

- **Left circle:** the item to dissolve sits in its centre. A violet ring lights round it while Dissolve can act.
- **Right circle:** up to **16 learned items a page**, twelve round the rim and four inside (24 a page before). The centre is the output. Until something is withdrawn, the selected item shows faintly there. A violet ring lights round it while Withdraw can act. The selected pattern is outlined in violet.
- **Column on the right:** title, essence, wallet and stars, the search box, Dissolve / Withdraw, 1 / 16 / 64 / Max, a star button for favourite and the All patterns / Favorites filter, page arrows, a dark read-out with the selected item's name and price, then Guide and Gem armor.
- **Read-out under the inventory:** what the input is worth or why it can't be dissolved, whether Withdraw is ready, and the total price.
- **Mercurial Eye:** the eye's icon sits in the left circle, the chosen building block in the right, and Use pattern takes the whole row.

The art's two small "unlearn" and "burn" tiles are covered, because this tablet has neither action. The buttons are new light keys in the art's greys that light violet like its stars. They keep keyboard focus, narration and tooltips. The favourite button draws a star instead of its label, but its label is still narrated and is its tooltip.

No other screen changes. The rest of the alchemy suite keeps the dark equipment style.

## Connections

- **Input producer:** unchanged (items to dissolve, from anywhere).
- **Output consumer:** unchanged (learned items withdrawn at their exact price).
- **Costs, unlocks and failure behaviour:** unchanged. The server still decides every dissolve, withdrawal, favourite, filter and page. It only moved the slots.

## Multiplayer and persistence

- `AlchemyPatternMenu` now has **16 pattern slots instead of 24**, at new positions. Slot numbers 0–37 (input, output, inventory) and the first pattern slot (38) are unchanged. Nothing is saved in the pattern slots, so worlds and learned patterns are unaffected.
- No new registrations, items, data or text strings.

## Verification

**Local checks** (no Gradle or game here): `python scripts/check_repository.py` passed. A preview image was drawn from the real art with stand-in lettering and shown to the contributor; it is not a game screenshot.

**New server game test for CI:** `TransmutationTabletLayoutGameTests.learnedPatternsRingTheRightCircle` checks that:

- the menu has sixteen pattern slots;
- a player who knows more than sixteen items sees a full first page and the rest on the next;
- no two slots' 18-pixel areas overlap;
- the input and output are at the circles' centres;
- every pattern lies on the right circle.

**Existing client tests that exercise this screen:** `AlchemicalGearClientGameTests`, `ConnectedAlchemyClientChecks` and `CompleteAlchemyClientGameTests`. Between them they:

- click Dissolve, Confirm, Favorite, Withdraw, the batch buttons, Use pattern and Area;
- search for "gold" and check the filtered patterns;
- keep the search through a resize;
- keep every slot inside 360 × 238;
- reach a control with Tab;
- screenshot the tablet at GUI scales 1 and 2.

**Results:** not yet run; the fork's Build workflow results will be recorded here.

**Not run:** a hands-on playtest.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
