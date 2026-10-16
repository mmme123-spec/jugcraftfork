# Glucose syrup

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Connect HCl-assisted corn-starch hydrolysis to finished glucose syrup; …" This change delivers the corn → starch → glucose syrup route and its first use.

**This change's choices:** the quantities and the syrup's first use (boiling to sugar), listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: early electric, after the Pulverizer, the Chemical Reactor and hydrochloric acid
Primary specialty and supported player role: agriculture and chemistry; a corn farmer who wants sugar without sugar cane

## Player experience

- **Corn Starch.** The Pulverizer grinds one corn into one corn starch (100 ticks).
- **Glucose Syrup.** In the Chemical Reactor, two corn starch with **500 mB of water** (first tank) and **50 mB of hydrochloric acid** (second tank) give **three glucose syrup** (120 ticks). The acid is the catalyst that breaks the starch down; only a splash is used.
- **Use:** glucose syrup boils down to one **sugar** in a furnace or smoker (100 ticks).

## Connections

- **Input producers:**
  - corn (farmed, or found as wild corn);
  - water;
  - hydrochloric acid (the Chemical Infuser's hydrogen chloride absorbed in water).
- **Output consumers:** sugar (cakes, pumpkin pie, jams, fermented spider eyes, potions, the Cooking Pot's recipes). Later candy, drinks and fermentation can take the syrup itself.
- **Machines:** the existing Pulverizer and Chemical Reactor, so no new machine.
- **Optional:** yes. Sugar cane stays the ordinary sugar source.

## Balance

- Two corn, 500 mB of water, 50 mB of hydrochloric acid and the machines' power make three sugar: 1.5 sugar a corn, against one sugar a sugar cane.
- It suits corn farms in biomes without water edges for sugar cane, at the cost of an acid supply and two machine steps.
- **No loops:** sugar never turns back into corn or acid.

## Multiplayer and persistence

- **New registrations,** stable from now on:
  - the items `jugcraft:corn_starch` and `jugcraft:glucose_syrup`;
  - the recipes `pulverizing/corn`, `chemical_reaction/glucose_syrup`, `sugar_from_glucose_syrup_smelting` and `sugar_from_glucose_syrup_smoking`.
- The recipes load only with both the agriculture and machines features on.

## Dependencies and assets

There are no new dependencies. The icons are drawn by `tools/petro_textures.py`:

- the starch: a small heap of creamy-white powder;
- the syrup: a squat glass jar of amber syrup with a cork lid.

No texture is copied.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`. The icons were inspected at 10x.

**Server game tests for CI** (`GlucoseSyrupGameTests`):

| Test | What it checks |
| --- | --- |
| `cornGrindsToStarch` | The Pulverizer grinds corn into one corn starch. |
| `starchBreaksDownIntoSyrup` | The reactor recipe takes two starch, 500 mB of water and 50 mB of hydrochloric acid, and gives three syrup. |
| `syrupBoilsDownToSugar` | A furnace and a smoker each boil the syrup into one sugar. |

### Results

Not yet run in CI.

## Rollout and open questions

- The quantities and the sugar use await the owner's review.
- Candy, drinks and fermenting the syrup into bioethanol remain open.
