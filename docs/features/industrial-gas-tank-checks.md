# Gas tank checks: exact contents through breaking, pickup, placement and pipes

Status: implemented on the contributor's fork (server game tests, no gameplay change), pending CI and review.

Proposal issue: none. TODO item: "Verify filled gas tanks retain exact content through break, pickup and placement and reconnect through shared pipes; cover joined-tank partitioning, simultaneous transfers and multiplayer pickup."

**From the owner's plan:** the starter gas factory ([industrial-starter-gas-and-acid-factory.md](industrial-starter-gas-and-acid-factory.md)) asks for "shared portable tanks and pipes, retaining exact fluid identity, amount and stored components across breaking, pickup, placement and restart".

**This change's choices:** which cases the tests cover, listed below.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the starter gas factory
Primary specialty and supported player role: none (a test of shared storage)

## Player experience

Nothing changes in play. The tests check what players already rely on: a tank of hydrogen keeps every drop of it, however it is moved.

The tests use the tinplate tank, the shared tank every gas line uses. The [existing tank test](../../src/gametest/java/io/github/jimbozoomer/jugcraft/test/JugcraftGameTests.java) already covers water in the tinplate tank and lava in the steel tank.

## Connections

- **Input producer / output consumer:** the Electrolytic Separator's hydrogen outlet, shared steel pipes and joined tinplate tanks.
- **Technology connection:** the shared fluid storage (`FluidTankBlockEntity`, `TankGroup`, `StoredFluid`) and pipe networks (`FluidNetworks`).
- **Magic connection:** none.

## What the tests check

`GasTankGameTests`, run on the headless server during `./gradlew build`.

The amount used is 1,234 mB and 27 droplets, which no bucket or pipe step rounds to.

| Test | What it checks |
| --- | --- |
| `hydrogenSurvivesBreakingPickupPlacementAndRestart` | A survival player breaks a hydrogen tank: one tank item drops carrying exactly that amount. Picked up and placed again by the player, the tank holds exactly that. Saved and loaded, as on a restart, it still does. |
| `aPlacedTankRejoinsAndFillsThroughAPipe` | A hydrogen tank placed by a player on another joins it into one group holding both. A Separator piped to the group fills it, and the group gains exactly what the Separator's tank lost. |
| `aBrokenJoinedTankTakesOnlyItsShare` | Three joined tanks hold 40 buckets, 16, 16 and 8. Breaking the middle one drops it with its 16 and leaves the others theirs. A methane tank placed against the full hydrogen tank keeps its methane exactly, and hydrogen offered to the pair goes into neither. |
| `twoMachinesFillOneGroupAtOnce` | Two Separators piped to the ends of one joined row of ten tanks push in the same ticks. The row ends with exactly the 5,500 mB both lost. |
| `twoPlayersCannotBothPickUpOneTank` | Two players reach a dropped hydrogen tank in the same tick. One gets it, once, with all its hydrogen. |

**A finding, not a change:** right-clicking a tank while holding a tank shows the tank's reading instead of placing. Like a chest, a tank is stacked on another by sneaking. The tests place tanks the way a player does.

## Multiplayer and persistence

- **Persistence.** The restart case saves and reloads the block entity.
- **Two players.** The pickup case uses two server players in one level.
- **Not done:** a two-client, dedicated-server playtest.

## Dependencies and assets

None.

## Verification

**Local checks** (no Gradle or game): `python scripts/check_repository.py`: passed.

### Results

Not yet run in CI.

## Rollout and open questions

- A two-client playtest of moving filled tanks between players remains.
- The steel tank and gas holder with gases could get the same cases. The existing test covers the steel tank with lava.
