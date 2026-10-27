# Jug Nouveau items describe themselves; Source shows as Ley Charge

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. The contributor asked for the worst problems in the pack to be fixed (26 October 2026): unhelpful text (item 15 of the audit) and the confusing words "Essence" and "Source" (item 16).

Owner: jimbozoomer-byte
Target milestone and tier: Jug Nouveau, all tiers (text and display)
Primary specialty and supported player role: magic; anyone reading an item or the codex

## Player experience

**Item descriptions.** Every Jug Nouveau item had a line "Jug Nouveau · X. See its encyclopedia entry for use and recipes." in the language file, and nothing showed it. Each item now has its own description, shown in grey under its name:
- from the supplied text where it has one: the glyph, thread, ritual and familiar descriptions and the first sentence of the item's guide page (177 items);
- 54 lines written from the items' recipes, drops and code, saying where each comes from or what it does (for example "Dropped by Wilden Hunters. Throw one near a wild Drygmy to befriend it." or "Sorcerer mage armor: +5 Focus capacity a piece, and the most thread sockets.");
- spawn eggs name their creature ("Spawns a Wilden Hunter."), and music discs say what they are.

The Dominion Wand and the charms keep the lines they already had.

**The codex.** The Jug Nouveau category had 573 entries; 163 items had two, a supplied reference and a "how to make it" entry. Each item now has one entry: what it does first, then how to make it. The category has 410 entries. Its subtitles are now the item's first sentence or where it is made ("Made at the Enchanting Apparatus"), instead of "Acquisition and workshop inputs", "Supplied field reference and Jugcraft integration notes" or "Glyph and augment reference". Recipe pages no longer repeat "Craft this with the shown ingredients." under the grid, and workshop pages say "Costs 0 Ley Charge" only when there is a cost. The "Jugcraft controls" page that opened 114 entries is replaced, where the supplied text mentions Source, by one closing line saying Source is Ley Charge here.

**Source.** The supplied text calls the magic resource Source; Jugcraft stores it as Ley Charge, 100 Source to 1. Looking at a Source Jar, relay or Sourcelink with Jade now shows "Ley Charge: 340 / 1000", and a jar adds "The codex calls this Source: 100 Source is 1 Ley Charge". The Source Gem's description says the same.

**Essence.** The alchemy balance is called essence by the owner's choice (alchemical-gear.md, 16 October) and is left as it is; the Jug Nouveau essence items are unchanged apart from their descriptions.

## How

- `tools/nouveau_guide.py`: `item_tooltip` picks each item's description (supplied text, then the written-in `HAND` lines, then a spawn egg's creature); `codex()` folds a reference into the same item's acquisition entry, keeping the acquisition entry's ID, which the progression check and links use.
- `JugNouveauClient.describe`: shows `tooltip.jugcraft.<id>` for `nouveau_` items, wrapped at about 40 characters.
- `JugcraftJadeClient.NouveauChargeTooltip`: reads the jar's Ley Charge from the block entity the client already receives; no new network data.

## Connections

Text and display only. No recipe, cost, ID or behavior changes. The Jade line is optional: without Jade, nothing changes.

## Multiplayer and persistence

No IDs, saved data or network messages change. 163 codex entry files are removed, merged into the entry for the same item; a codex bookmark on a removed entry opens the category instead.

## Verification

- `JugNouveauClientGameTests`: the Fell glyph's tooltip lines include its description.
- Local: `python tools/generate_material_data.py` wrote the descriptions and the codex (no "See its encyclopedia entry" line is left; 410 entries); `python tools/check_mod_data.py` passed (7,797 material IDs, including the progression check that every made item's codex entry shows how to make it); a tree-sitter parse of the changed Java found no syntax errors; `python scripts/check_repository.py` passed.
- CI: pending.

Not run: a human reading tooltips, the codex or the Jade line in game.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
