# Polymer Molding Press: plastic sheets and gaskets with reusable molds

Status: implemented on the contributor's fork, pending CI and review.
Stacked on the [Reagent Purification Station](industrial-reagent-purification.md) and the branches below it.

Proposal issue: none. TODO item: "Build the selected shared **Polymer Molding Press with reusable molds** for housings, fittings, insulation, gaskets, flexible hoses and vehicle panels; use earlier supplies for the first station/tooling."

**From the owner's plan:**
- the selected shared molding form with reusable molds;
- the art brief's [Polymer Molding Press](industrial-machine-models-and-textures.md#polymer-molding-press), 2×3×3: a mold chamber between platens on four tie bars, a hopper feeding a heated ringed barrel, a guarded product tray and a tool-change handle;
- reusable molds that decide the product, with ordinary stock keeping its recipe requirements.

**This change's choices:** the first two molds and their products, the yields, the cooling water and the bills, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, polymers, after the refinery's plastic pellets
Primary specialty and supported player role: plastics manufacture

## Player experience

The **Polymer Molding Press** stands on an olive base:
- a horizontal mold chamber between a thick fixed front platen and a sliding middle platen, carried with the rear platen on four bright steel tie bars;
- behind them, the heated ringed barrel with its amber heater band, fed by the hopper on top at the back;
- at the front, the guarded product tray, the tool-change handle and the recessed control box (the controller);
- the side drive on the right.

**A mold goes in its tool socket** and is never used up. The first two molds are crafted from steel:
- the **Sheet Mold** (a steel plate either side of a steel ingot) molds:
  - **two plastic sheets** from a plastic pellet, where the Metal Press makes one;
  - **three plastic sheets** from PVC resin, where the Metal Press makes two;
- the **Gasket Mold** (a steel ingot either side of a steel plate) molds **four gaskets** from a pellet and a piece of rubber. Crafting four gaskets needs two rubber and a steel plate.

**Each shot uses 50 mB of cooling water**, piped into the inlet low on its left side. Stock goes in at the hopper on top at the back; products leave at the tray at the front; power comes in at the junction box at the back.

Without a mold, the stock waits and the screen names the tool socket.

**Guides.** Its Engineer's Handbook page describes it and its molds, and JEI lists its three recipes.

## Connections

- **Existing input producers:**
  - plastic pellets from the refinery gas;
  - PVC resin from the chlor-alkali line;
  - rubber from butadiene;
  - water.
- **Existing output consumers:** plastic sheets go to the 37 recipes that use them (machines, electronics, the diesel engine and more), and gaskets to steel fluid pipe, the turbocharger and other fittings.
- **Technology connection:** the shared form foundation, in the chemical reactor family (96 JE/t), entry profile, with a tool socket like the Infuser's catalyst bed. One new capability, `jugcraft:polymer_molding`.
- **Magic connection:** none.
- **Reachable entry path:** its bill contains a Metal Press, and its molds need only steel. Nothing it makes is needed to build it.
- **Required vs optional:** optional. The Metal Press and crafting still make sheets and gaskets.

## Balance and automation

- **Energy:**
  - sheets: 40 ticks, 3,840 JE a shot;
  - gaskets: 60 ticks, 5,760 JE a shot.
- **Saving:**
  - sheets: twice the sheets from a pellet, and half again from PVC resin;
  - gaskets: half the rubber and no steel plate.
- **No loops:** nothing it makes turns back into its inputs.
- **Upgrade cards** fit its terminal.

## Choices made here for review

- **The first molds:** sheets and gaskets, the plan's products that already have consumers.
  - The plan's housings, fittings, insulation, flexible hoses and vehicle panels need their own items and consumers first. They can join as further molds on the same press.
  - Hoses and insulation may belong with the later Polymer Extruder.
- **Yields:**
  - pellet → 2 sheets;
  - PVC resin → 3 sheets;
  - pellet and rubber → 4 gaskets.
- **Cooling water:** 50 mB a shot.
- **Bills:**
  - the press: a Metal Press, an electric motor, two steel gears, four steel plates and a basic circuit (`PMP / GXG / PCP`);
  - the Sheet Mold: `PIP`;
  - the Gasket Mold: `IPI`.
- **Not modelled:** the brief's moving platens and output piece are drawn standing still.

## Multiplayer and persistence

- **Server authority.** The server decides everything. The tool socket takes molds only from a player at the screen.
- **Ports.** The hopper and coolant inlet take only what its recipes use.
- **New registrations,** stable from now on:
  - the block and item `jugcraft:polymer_molding_press`;
  - the items `jugcraft:sheet_mold` and `jugcraft:gasket_mold`;
  - the item tag `jugcraft:molds`;
  - the capability `jugcraft:polymer_molding`;
  - the recipes `chemical_reaction/molding_plastic_sheet`, `chemical_reaction/molding_pvc_sheet`, `chemical_reaction/molding_gasket`, `sheet_mold`, `gasket_mold` and `polymer_molding_press`.
- **Not done:** two-client and dedicated-server play.

## Dependencies and assets

There are no new dependencies.

- **The model** is `polymer_molding_press()` in `tools/industrial_forms.py`, made of existing textures.
- **The molds' icons** are drawn in `tools/petro_textures.py` (`_mold`), 16×16 in the item icon manner: a steel mold half lit from the top left, outlined in its own dark, with brass dowel pins and its cavity, a broad flat one for the Sheet Mold and a ring for the Gasket Mold.

## Verification

**Local checks** (no Gradle or game):
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the part models and the icons at 8× were inspected.

**Server game tests for CI** (`PolymerMoldingPressGameTests`):

| Test | What it checks |
| --- | --- |
| `itStandsWholeAndOnlyItsPortsAnswer` | It places as 18 blocks in every orientation, only its four ports answer, and the hopper takes pellets and rubber, not cobblestone. |
| `aSheetMoldMakesTwoSheetsFromAPellet` | Without a mold it waits on the socket, spending nothing. With one: two sheets, 50 mB of water and exactly 3,840 JE, the mold kept. |
| `pvcResinMoldsIntoThreeSheets` | PVC resin molds into three sheets. |
| `aGasketMoldMakesFourGaskets` | A pellet and a piece of rubber mold into four gaskets for exactly 5,760 JE. |
| `everyPartLightsWhileItWorks` | Every part lights while it works. |
| `itsRecipesAndBills` | Exactly its three recipes, and the bills build the press and both molds. |

**Client game test** (`PolymerMoldingPressClientGameTests`): three views, a night view, its screen while working and its screen when a full product slot blocks it.

### Results

Not yet run in CI.

**Not yet done:**
- a survival playtest;
- a two-client test;
- a performance measurement.

## Rollout and open questions

- The yields, the water, the bills and the choice of first molds await the owner's review.
- Further molds (housings, fittings, vehicle panels) once those products and their consumers exist.
