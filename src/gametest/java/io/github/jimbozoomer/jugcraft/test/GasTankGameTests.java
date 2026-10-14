package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.StoredFluid;
import io.github.jimbozoomer.jugcraft.fluid.TankGroup;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormPort;
import io.github.jimbozoomer.jugcraft.machine.form.IndustrialForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Filled gas tanks (docs/features/industrial-gas-tank-checks.md, the starter gas factory's "shared portable tanks and
 * pipes, retaining exact fluid identity, amount and stored components across breaking, pickup, placement and restart"):
 * hydrogen to the droplet through a player's breaking, picking up, placing and a reload; a placed tank rejoining its
 * neighbours and filling again through a shared pipe; a broken tank of a joined stack taking only its own share; two
 * machines filling one joined group in the same ticks; and two players reaching the same dropped tank.
 */
public class GasTankGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	/** 1,234 mB and 27 droplets: an amount no bucket or pipe step would round to. */
	private static final long ODD = 1_234 * FluidNetworks.DROPLETS_PER_MB + 27;

	/**
	 * A tinplate tank holding 1,234 mB and 27 droplets of hydrogen, broken by a survival player, drops one tank item
	 * carrying exactly that; picked up and placed again by the player, the tank holds exactly that; and saved and
	 * loaded (a restart), it still does.
	 */
	@GameTest(structure = ARENA, maxTicks = 60)
	public void hydrogenSurvivesBreakingPickupPlacementAndRestart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos tank = new BlockPos(3, 1, 3);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		fill(helper.getBlockEntity(tank, FluidTankBlockEntity.class), PetroFluids.HYDROGEN.fluid(), ODD);
		ServerPlayer player = player(helper, tank.east(2));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(tank)), "The player could not break the tank");
		List<ItemEntity> dropped = drops(helper, tank);
		helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1,
				"Breaking it dropped " + dropped.stream().map(ItemEntity::getItem).toList());
		check(helper, dropped.get(0).getItem(), PetroFluids.HYDROGEN.fluid(), ODD, "The dropped tank");

		ItemEntity drop = dropped.get(0);
		drop.setNoPickUpDelay();
		drop.playerTouch(player);
		helper.assertTrue(drop.isRemoved(), "The player did not pick the tank up");
		ItemStack carried = take(player);
		check(helper, carried, PetroFluids.HYDROGEN.fluid(), ODD, "The carried tank");

		BlockPos placed = new BlockPos(5, 1, 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, carried);
		placeOn(helper, player, placed);
		FluidTankBlockEntity entity = helper.getBlockEntity(placed, FluidTankBlockEntity.class);
		helper.assertTrue(entity.storage.variant.isOf(PetroFluids.HYDROGEN.fluid()) && entity.storage.amount == ODD,
				"The placed tank holds " + entity.storage.amount + " droplets of " + entity.storage.variant + ", not " + ODD + " of hydrogen");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "Placing it did not use the tank item up");

		CompoundTag saved = entity.saveWithoutMetadata(level.registryAccess());
		FluidTankBlockEntity reloaded = new FluidTankBlockEntity(entity.getBlockPos(), entity.getBlockState());
		reloaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(reloaded.storage.variant.isOf(PetroFluids.HYDROGEN.fluid()) && reloaded.storage.amount == ODD,
				"After a reload it holds " + reloaded.storage.amount + " droplets of " + reloaded.storage.variant);
		helper.succeed();
	}

	/**
	 * A tank of hydrogen placed by a player on another hydrogen tank joins it into one group holding both; an
	 * Electrolytic Separator's hydrogen outlet, piped to the group, fills it, and the group gains exactly what the
	 * Separator's tank lost.
	 */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void aPlacedTankRejoinsAndFillsThroughAPipe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity separator = separator(helper, new BlockPos(4, 1, 8));
		BlockPos lower = hydrogenLine(helper, separator);
		helper.setBlock(lower, JugcraftFluids.FLUID_TANK);
		fill(helper.getBlockEntity(lower, FluidTankBlockEntity.class), PetroFluids.HYDROGEN.fluid(), 2 * FluidConstants.BUCKET);

		ServerPlayer player = player(helper, lower.east(2).below(lower.getY() - 1));
		player.setItemInHand(InteractionHand.MAIN_HAND, tankItem(PetroFluids.HYDROGEN.fluid(), ODD));
		placeOn(helper, player, lower.above());
		helper.assertTrue(helper.getBlockEntity(lower.above(), FluidTankBlockEntity.class).storage.amount == ODD, "The placed tank lost its hydrogen");
		TankGroup group = TankGroup.at(level, helper.absolutePos(lower));
		long before = 2 * FluidConstants.BUCKET + ODD;
		helper.assertTrue(group.fluid().isOf(PetroFluids.HYDROGEN.fluid()) && group.amount() == before
				&& group.capacity() == 2 * FluidTankBlockEntity.CAPACITY, "The two tanks did not join: " + group.amount() + " of " + group.capacity());

		separator.tanks().output(1).fill(PetroFluids.HYDROGEN.fluid(), 1_500);
		long made = 1_500 * FluidNetworks.DROPLETS_PER_MB;
		helper.succeedWhen(() -> {
			helper.assertTrue(separator.tanks().output(1).amount == 0, "The Separator still holds hydrogen");
			long now = TankGroup.at(level, helper.absolutePos(lower)).amount();
			helper.assertTrue(now == before + made, "The group holds " + now + " droplets, not " + (before + made));
		});
	}

	/**
	 * Three stacked tanks joined as one hold 40 buckets of hydrogen, 16, 16 and 8 from the bottom. Breaking the middle
	 * one drops it with its own 16 buckets and leaves the others theirs: nothing is lost or made. A methane tank placed
	 * against the full bottom tank keeps its methane exactly: hydrogen offered to the pair goes into neither.
	 */
	@GameTest(structure = ARENA, maxTicks = 60)
	public void aBrokenJoinedTankTakesOnlyItsShare(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos bottom = new BlockPos(3, 1, 3);
		for (int up = 0; up < 3; up++) {
			helper.setBlock(bottom.above(up), JugcraftFluids.FLUID_TANK);
		}
		try (Transaction transaction = Transaction.openOuter()) {
			long in = TankGroup.at(level, helper.absolutePos(bottom)).insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), 40 * FluidConstants.BUCKET, transaction);
			helper.assertTrue(in == 40 * FluidConstants.BUCKET, "The stack took " + in + " droplets");
			transaction.commit();
		}
		ServerPlayer player = player(helper, bottom.east(2));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
		player.gameMode.destroyBlock(helper.absolutePos(bottom.above()));
		List<ItemEntity> dropped = drops(helper, bottom.above());
		helper.assertTrue(dropped.size() == 1, "Breaking the middle tank dropped " + dropped.size() + " stacks");
		check(helper, dropped.get(0).getItem(), PetroFluids.HYDROGEN.fluid(), FluidTankBlockEntity.CAPACITY, "The middle tank");
		helper.assertTrue(TankGroup.at(level, helper.absolutePos(bottom)).amount() == FluidTankBlockEntity.CAPACITY
				&& TankGroup.at(level, helper.absolutePos(bottom.above(2))).amount() == 8 * FluidConstants.BUCKET,
				"The bottom and top tanks no longer hold their 16 and 8 buckets");

		BlockPos methane = bottom.north();
		helper.setBlock(methane, JugcraftFluids.FLUID_TANK);
		long gas = 3 * FluidConstants.BUCKET + 5;
		fill(helper.getBlockEntity(methane, FluidTankBlockEntity.class), PetroFluids.METHANE.fluid(), gas);
		try (Transaction transaction = Transaction.openOuter()) {
			long in = TankGroup.at(level, helper.absolutePos(bottom)).insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction);
			helper.assertTrue(in == 0, "The full hydrogen tank and the methane tank took " + in + " droplets of hydrogen");
			transaction.commit();
		}
		FluidTankBlockEntity other = helper.getBlockEntity(methane, FluidTankBlockEntity.class);
		FluidTankBlockEntity hydrogen = helper.getBlockEntity(bottom, FluidTankBlockEntity.class);
		helper.assertTrue(other.storage.variant.isOf(PetroFluids.METHANE.fluid()) && other.storage.amount == gas
				&& hydrogen.storage.variant.isOf(PetroFluids.HYDROGEN.fluid()) && hydrogen.storage.amount == FluidTankBlockEntity.CAPACITY,
				"The methane tank holds " + other.storage.amount + " droplets of " + other.storage.variant + " and the hydrogen tank "
						+ hydrogen.storage.amount + " of " + hydrogen.storage.variant);
		helper.succeed();
	}

	/**
	 * Two Separators, each piped to an end of one joined row of ten tanks, push their hydrogen in the same ticks: the
	 * row ends up with exactly what both lost, 3,000 and 2,500 mB.
	 */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void twoMachinesFillOneGroupAtOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity first = separator(helper, new BlockPos(3, 1, 8));
		FormMachineBlockEntity second = separator(helper, new BlockPos(12, 1, 8));
		BlockPos a = hydrogenLine(helper, first);
		BlockPos b = hydrogenLine(helper, second);
		helper.assertTrue(a.getY() == b.getY() && a.getZ() == b.getZ(), "The two outlets do not line up: " + a + " and " + b);
		for (int x = Math.min(a.getX(), b.getX()); x <= Math.max(a.getX(), b.getX()); x++) {
			helper.setBlock(new BlockPos(x, a.getY(), a.getZ()), JugcraftFluids.FLUID_TANK);
		}
		helper.assertTrue(TankGroup.tanks(level, helper.absolutePos(a)).size() == Math.abs(a.getX() - b.getX()) + 1, "The row did not join");
		first.tanks().output(1).fill(PetroFluids.HYDROGEN.fluid(), 3_000);
		second.tanks().output(1).fill(PetroFluids.HYDROGEN.fluid(), 2_500);
		long total = 5_500 * FluidNetworks.DROPLETS_PER_MB;
		helper.succeedWhen(() -> {
			helper.assertTrue(first.tanks().output(1).amount == 0 && second.tanks().output(1).amount == 0, "A Separator still holds hydrogen");
			TankGroup group = TankGroup.at(level, helper.absolutePos(a));
			helper.assertTrue(group.fluid().isOf(PetroFluids.HYDROGEN.fluid()) && group.amount() == total,
					"The row holds " + group.amount() + " droplets, not " + total);
		});
	}

	/** Two players reach a dropped hydrogen tank in the same tick: one of them gets it, once, with all its hydrogen. */
	@GameTest(structure = ARENA, maxTicks = 60)
	public void twoPlayersCannotBothPickUpOneTank(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(4, 1, 4);
		Vec3 spot = Vec3.atBottomCenterOf(helper.absolutePos(at));
		ItemEntity drop = new ItemEntity(level, spot.x, spot.y, spot.z, tankItem(PetroFluids.HYDROGEN.fluid(), ODD));
		drop.setNoPickUpDelay();
		level.addFreshEntity(drop);
		ServerPlayer one = player(helper, at);
		ServerPlayer two = player(helper, at);
		drop.playerTouch(one);
		drop.playerTouch(two);
		ItemStack first = take(one);
		ItemStack second = take(two);
		helper.assertTrue(drop.isRemoved() && first.isEmpty() != second.isEmpty(),
				"Picked up by " + (first.isEmpty() ? 0 : 1) + (second.isEmpty() ? 0 : 1) + " players, removed " + drop.isRemoved());
		check(helper, first.isEmpty() ? second : first, PetroFluids.HYDROGEN.fluid(), ODD, "The picked-up tank");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	/** A survival player standing at {@code at}. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(at);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	/**
	 * The player places what they hold at {@code at}, clicking the top of the block beneath while sneaking, as a player
	 * does to stack a tank on a tank (clicking a tank otherwise reads it).
	 */
	private static void placeOn(GameTestHelper helper, ServerPlayer player, BlockPos at) {
		BlockPos below = helper.absolutePos(at.below());
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).relative(Direction.UP, 0.5), Direction.UP, below, false);
		player.setShiftKeyDown(true);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		player.setShiftKeyDown(false);
		helper.assertTrue(helper.getBlockState(at).is(JugcraftFluids.FLUID_TANK), "The player did not place the tank at " + at);
	}

	/** The tank item the player carries, taken out of their inventory (or empty). */
	private static ItemStack take(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(JugcraftFluids.FLUID_TANK.asItem())) {
				return player.getInventory().removeItemNoUpdate(slot);
			}
		}
		return ItemStack.EMPTY;
	}

	private static List<ItemEntity> drops(GameTestHelper helper, BlockPos at) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(1.5),
				item -> item.getItem().is(JugcraftFluids.FLUID_TANK.asItem()));
	}

	private static void check(GameTestHelper helper, ItemStack stack, Fluid fluid, long amount, String what) {
		StoredFluid stored = stack.get(JugcraftFluids.STORED_FLUID);
		helper.assertTrue(stack.is(JugcraftFluids.FLUID_TANK.asItem()) && stored != null && stored.variant().isOf(fluid) && stored.amount() == amount,
				what + " carries " + stored + ", not " + amount + " droplets of " + fluid);
	}

	private static ItemStack tankItem(Fluid fluid, long amount) {
		ItemStack stack = new ItemStack(JugcraftFluids.FLUID_TANK);
		stack.set(JugcraftFluids.STORED_FLUID, new StoredFluid(FluidVariant.of(fluid), amount));
		return stack;
	}

	private static void fill(FluidTankBlockEntity tank, Fluid fluid, long amount) {
		try (Transaction transaction = Transaction.openOuter()) {
			long in = tank.storage.insert(FluidVariant.of(fluid), amount, transaction);
			if (in != amount) {
				throw new AssertionError("The tank took " + in + " of " + amount + " droplets");
			}
			transaction.commit();
		}
	}

	/** An Electrolytic Separator facing north, its controller at {@code relative}. */
	private static FormMachineBlockEntity separator(GameTestHelper helper, BlockPos relative) {
		helper.setBlock(relative, IndustrialForms.ELECTROLYTIC_SEPARATOR.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		IndustrialForms.ELECTROLYTIC_SEPARATOR.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
				ItemStack.EMPTY);
		return helper.getBlockEntity(relative, FormMachineBlockEntity.class);
	}

	/** A steel pipe on the Separator's hydrogen outlet; returns the relative position just past it, for a tank. */
	private static BlockPos hydrogenLine(GameTestHelper helper, FormMachineBlockEntity machine) {
		MachineForm form = machine.form();
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		FormPort port = form.ports().stream().filter(p -> p.name().equals("hydrogen_out")).findFirst().orElseThrow();
		BlockPos cell = form.cellPos(machine.getBlockPos(), facing, form.cellIndex(port.column(), port.row(), port.layer()));
		Direction side = port.side().world(facing);
		BlockPos pipe = helper.relativePos(cell.relative(side));
		helper.setBlock(pipe, JugcraftFluids.STEEL_FLUID_PIPE);
		return pipe.relative(side);
	}
}
