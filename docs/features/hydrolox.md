# Hydrogen/LOX rocket fuel

Status: implemented on the contributor's fork, pending review. Built on [methane/LOX](methalox.md); merge that first.

Proposal issue: none. TODO item: "Connect existing RP-1/LOX and requested methane/LOX, LH2/LOX and hydrazine/MMH/N2O4 families … oxygen cooling -> methane -> more advanced hydrogen capability …" This change delivers the LH2/LOX step. Hydrazine stays open.

**This change's choices:** the quantities and the five-motor yield, listed below for the owner to review. The owner's "upgraded Cryogenic Liquefier" role is not built. Hydrogen is instead the liquefier's slowest, most wasteful job, and the upgrade is left as an open question.

Owner: jimbozoomer-byte
Target milestone and tier: rocketry, after methane/LOX
Primary specialty and supported player role: rocketry; a player with plentiful hydrogen (brine or water electrolysis) who wants the most motors per pair of tanks

## Player experience

- **Liquid Hydrogen** (a liquid, with a bucket). The existing Cryogenic Liquefier condenses **1,000 mB of hydrogen** into **125 mB of liquid hydrogen** (160 ticks): eight parts to one, and twice as long as oxygen or methane.
- **Liquid Hydrogen Tank.** The Chemical Reactor fills one **rocket casing** with **1,000 mB of liquid hydrogen** (60 ticks).
- **Rocket motors.** The rocket workshop pairs **one liquid hydrogen tank, one liquid oxygen tank and two nozzles** into **five rocket motors** (200 ticks). Methane makes four and kerosene three.

## Connections

- **Input producers:**
  - hydrogen (brine electrolysis, water electrolysis, or cleaned syngas);
  - oxygen for the liquid oxygen;
  - rocket casings and nozzles.
- **Output consumers:** rocket motors, which go into every rocket.
- **Machines:** the existing Cryogenic Liquefier, Chemical Reactor and rocket workshop.
- **Optional:** yes. Kerosene, methane and solid motors stay.

## Balance

- One hydrogen tank needs 8,000 mB of hydrogen and 1,280 liquefier ticks. A methane tank needs 4,000 mB and 320 ticks.
- The extra motor (five against four) pays for twice the gas and four times the liquefier time.
- **No loops:** motors never turn back into gas. Liquid out (125 mB) is an eighth of gas in.

## Multiplayer and persistence

- **New registrations,** stable from now on:
  - the fluid `jugcraft:liquid_hydrogen` and its bucket;
  - the item `jugcraft:hydrogen_tank`;
  - the recipes `liquefaction/liquid_hydrogen`, `chemical_reaction/hydrogen_tank` and `rocket_assembly/hydrolox_rocket_motor`.
- The fluid is registered after liquid methane, so earlier fluids' textures are unchanged.

## Dependencies and assets

There are no new dependencies.

- The liquid's textures and bucket are drawn by `tools/petro_textures.py`.
- The tank icon is drawn at 16×16 by `tools/rocketry.py`: the methane tank's casing with an orange insulating-foam band. Both icons now share one drawing helper. The methane icon's pixels are unchanged.

No texture is copied.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py` passed. The icon was inspected at 10x.

**Server game test for CI:** `MethaloxGameTests.hydrogenLiquefiesAndLoadsFiveMotors` checks that 1,000 mB of hydrogen liquefies to 125 mB, that a casing fills with 1,000 mB into a hydrogen tank, and that a hydrogen tank, a liquid oxygen tank and two nozzles make five motors.

### Results

Not yet run in CI.

## Rollout and open questions

- The quantities and the five-motor yield await the owner's review.
- Should liquid hydrogen need an upgraded liquefier, as the to-do item suggests? The liquefier has no upgrade slot today, so that needs a design.
- Hydrazine/MMH/N2O4, the Rotary Condensator role and insulated shared tanks remain open.
