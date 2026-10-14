package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.MachineStatus;
import io.github.jimbozoomer.jugcraft.machine.form.FormCell;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormPort;
import io.github.jimbozoomer.jugcraft.machine.form.FormSide;
import io.github.jimbozoomer.jugcraft.machine.form.IndustrialForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The Polymer Molding Press (docs/features/industrial-polymer-molding-press.md): it stands whole in every orientation
 * with conveyors, pipes and cables reaching it only at its four ports; with no mold it waits on its tool socket; a
 * sheet mold makes two plastic sheets from a pellet and three from PVC resin, a gasket mold four gaskets from a pellet
 * and a piece of rubber, each shot using 50 mB of cooling water and the mold never used up; every part lights while it
 * works; and it is built from a Metal Press and its bill.
 */
public class PolymerMoldingPressGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PARTS = 18;
	/** The chemical reactor family's 96 JE/t, the entry profile's full draw. */
	private static final long DRAW = 96;

	/**
	 * In every orientation it fills its 18 blocks, and each block's faces answer conveyors, pipes and cables only where
	 * the form declares a port. The hopper takes pellets and rubber, not cobblestone; the coolant inlet takes water.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.POLYMER_MOLDING_PRESS_FORM;
		helper.assertTrue(form.positions() == PARTS && form.footprint().size() == PARTS
				&& IndustrialForms.POLYMER_MOLDING_PRESS.partProperty().getPossibleValues().size() == PARTS, "18 blocks in a 2x3x3 envelope");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		BlockPos[] controllers = {new BlockPos(5, 1, 1), new BlockPos(14, 1, 5), new BlockPos(1, 1, 14), new BlockPos(9, 1, 9)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.POLYMER_MOLDING_PRESS) && IndustrialForms.POLYMER_MOLDING_PRESS.part(state) == part
						&& state.getValue(MachineBlock.FACING) == facing, facing + ": part " + part + " is " + state);
				for (Direction side : Direction.values()) {
					FormPort port = form.port(part, FormSide.of(side, facing));
					boolean fluid = FluidStorage.SIDED.find(level, at, side) != null;
					boolean items = ItemStorage.SIDED.find(level, at, side) != null;
					boolean power = EnergyStorage.SIDED.find(level, at, side) != null;
					helper.assertTrue(fluid == (port != null && port.kind().fluid()) && items == (port != null && port.kind().item())
							&& power == (port != null && port.kind() == FormPort.Kind.ENERGY_IN),
							facing + ": part " + part + " " + side + " answers fluid " + fluid + ", items " + items + ", power " + power);
				}
			}
			try (Transaction transaction = Transaction.openOuter()) {
				Storage<ItemVariant> hopper = itemPort(helper, machine, "feed_in");
				helper.assertTrue(hopper.insert(ItemVariant.of(net.minecraft.world.item.Items.COBBLESTONE), 1, transaction) == 0,
						facing + ": the hopper refuses cobblestone");
				helper.assertTrue(hopper.insert(ItemVariant.of(item("plastic_pellets")), 4, transaction) == 4
						&& hopper.insert(ItemVariant.of(item("rubber")), 4, transaction) == 4, facing + ": the hopper takes pellets and rubber");
				helper.assertTrue(port(helper, machine, "coolant_in").insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the coolant inlet takes water");
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/**
	 * With no mold a pellet and water wait on the tool socket, spending nothing. With a sheet mold: 40 paid ticks at
	 * 96 JE, 3,840 JE, two plastic sheets, 50 mB of water used and the mold kept.
	 */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void aSheetMoldMakesTwoSheetsFromAPellet(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		int socket = machine.form().firstSocketSlot();
		feed(helper, machine, "plastic_pellets", 1);
		insert(helper, port(helper, machine, "coolant_in"), Fluids.WATER, 1000);
		helper.runAfterDelay(10, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.reason() == MachineStatus.Reason.TOOL_REQUIRED && machine.lane(0) == null && used(machine, supplied) == 0,
					"Without a mold it waits on the socket: " + status);
			machine.container().setItem(socket, stack("sheet_mold"));
		});
		helper.runAfterDelay(120, () -> {
			helper.assertTrue(machine.lane(0) == null, "Still molding");
			assertMade(helper, machine, "plastic_sheet", 2);
			helper.assertTrue(machine.tanks().input(0).millibuckets() == 950, "Used " + (1000 - machine.tanks().input(0).millibuckets()) + " mB of water");
			helper.assertTrue(used(machine, supplied) == 40 * DRAW, "Paid " + used(machine, supplied) + " JE, not " + 40 * DRAW);
			helper.assertTrue(machine.container().getItem(socket).is(item("sheet_mold")), "The mold was used up");
			helper.succeed();
		});
	}

	/** A sheet mold makes three plastic sheets from PVC resin. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void pvcResinMoldsIntoThreeSheets(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		supply(helper, machine);
		machine.container().setItem(machine.form().firstSocketSlot(), stack("sheet_mold"));
		feed(helper, machine, "pvc_resin", 1);
		insert(helper, port(helper, machine, "coolant_in"), Fluids.WATER, 1000);
		helper.succeedWhen(() -> assertMade(helper, machine, "plastic_sheet", 3));
	}

	/** A gasket mold makes four gaskets from a pellet and a piece of rubber, 60 paid ticks, 5,760 JE. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void aGasketMoldMakesFourGaskets(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		machine.container().setItem(machine.form().firstSocketSlot(), stack("gasket_mold"));
		feed(helper, machine, "plastic_pellets", 1);
		feed(helper, machine, "rubber", 1);
		insert(helper, port(helper, machine, "coolant_in"), Fluids.WATER, 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null, "Still molding");
			assertMade(helper, machine, "gasket", 4);
			helper.assertTrue(used(machine, supplied) == 60 * DRAW, "Paid " + used(machine, supplied) + " JE, not " + 60 * DRAW);
		});
	}

	/** While it works every part shows lit, but only the controller gives off light; paused, it goes dark. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void everyPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		supply(helper, machine);
		machine.container().setItem(machine.form().firstSocketSlot(), stack("sheet_mold"));
		machine.container().setItem(0, new ItemStack(item("plastic_pellets"), 16));
		machine.tanks().input(0).fill(Fluids.WATER, 2000);
		helper.runAfterDelay(10, () -> {
			assertLit(helper, machine, true);
			machine.togglePause();
		});
		helper.runAfterDelay(15 + FormMachineBlockEntity.LIT_HOLD, () -> {
			assertLit(helper, machine, false);
			helper.succeed();
		});
	}

	/**
	 * It runs exactly its three molding recipes, is built from a Metal Press, an electric motor, two steel gears, steel
	 * plates and a basic circuit, and its molds are crafted from steel plates and ingots.
	 */
	@GameTest(structure = ARENA)
	public void itsRecipesAndBills(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		List<?> own = machine.recipes(level.getServer()).stream().map(FluidRecipes.Entry::id).sorted().toList();
		helper.assertTrue(own.equals(List.of(Jugcraft.id("chemical_reaction/molding_gasket"), Jugcraft.id("chemical_reaction/molding_plastic_sheet"),
				Jugcraft.id("chemical_reaction/molding_pvc_sheet"))), "Its recipes: " + own);
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack plate = stack("steel_plate");
		ItemStack gear = stack("steel_gear");
		CraftingInput bill = CraftingInput.of(3, 3, List.of(plate, stack("electric_motor"), plate, gear, stack("metal_press"), gear,
				plate, stack("basic_circuit"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(bill, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(bill).is(IndustrialForms.POLYMER_MOLDING_PRESS.asItem()),
				"Its bill makes the Polymer Molding Press");
		ItemStack ingot = stack("steel_ingot");
		CraftingInput sheet = CraftingInput.of(3, 1, List.of(plate, ingot, plate));
		CraftingInput gasket = CraftingInput.of(3, 1, List.of(ingot, plate, ingot));
		helper.assertTrue(crafting.getRecipeFor(sheet, level).map(r -> r.value().assemble(sheet).is(item("sheet_mold"))).orElse(false)
				&& crafting.getRecipeFor(gasket, level).map(r -> r.value().assemble(gasket).is(item("gasket_mold"))).orElse(false),
				"The molds are crafted from steel plates and ingots");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.POLYMER_MOLDING_PRESS.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.POLYMER_MOLDING_PRESS.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
				ItemStack.EMPTY);
		return helper.getBlockEntity(relative, FormMachineBlockEntity.class);
	}

	private static void assertLit(GameTestHelper helper, FormMachineBlockEntity machine, boolean lit) {
		MachineForm form = machine.form();
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		for (int part = 0; part < form.footprint().size(); part++) {
			BlockState state = helper.getLevel().getBlockState(form.footprint().partPos(machine.getBlockPos(), facing, part));
			int light = lit && part == 0 ? 13 : 0;
			helper.assertTrue(state.getValue(MachineBlock.LIT) == lit && state.getLightEmission() == light,
					"Part " + part + ": lit " + state.getValue(MachineBlock.LIT) + ", light " + state.getLightEmission() + ", not " + lit + ", " + light);
		}
	}

	private static Storage<ItemVariant> itemPort(GameTestHelper helper, FormMachineBlockEntity machine, String name) {
		MachineForm form = machine.form();
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		FormPort port = form.ports().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
		BlockPos at = form.cellPos(machine.getBlockPos(), facing, form.cellIndex(port.column(), port.row(), port.layer()));
		Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), at, port.side().world(facing));
		helper.assertTrue(storage != null, "No item storage at port " + name);
		return storage;
	}

	private static void feed(GameTestHelper helper, FormMachineBlockEntity machine, String item, int count) {
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = itemPort(helper, machine, "feed_in").insert(ItemVariant.of(item(item)), count, transaction);
			helper.assertTrue(moved == count, "The hopper took " + moved + " " + item + ", not " + count);
			transaction.commit();
		}
	}

	private static void assertMade(GameTestHelper helper, FormMachineBlockEntity machine, String item, int count) {
		ItemStack made = machine.container().getItem(machine.form().firstOutputSlot());
		helper.assertTrue(made.is(item(item)) && made.getCount() == count, "Made " + made + ", not " + count + " " + item);
	}

	private static Storage<FluidVariant> port(GameTestHelper helper, FormMachineBlockEntity machine, String name) {
		MachineForm form = machine.form();
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		FormPort port = form.ports().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
		BlockPos at = form.cellPos(machine.getBlockPos(), facing, form.cellIndex(port.column(), port.row(), port.layer()));
		Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), at, port.side().world(facing));
		helper.assertTrue(storage != null, "No storage at port " + name);
		return storage;
	}

	private static long[] supply(GameTestHelper helper, FormMachineBlockEntity machine) {
		SimpleEnergyStorage store = (SimpleEnergyStorage) machine.energy();
		long[] supplied = {0};
		store.setAmount(store.getCapacity());
		helper.onEachTick(() -> {
			supplied[0] += store.getCapacity() - store.getAmount();
			store.setAmount(store.getCapacity());
		});
		return supplied;
	}

	private static long used(FormMachineBlockEntity machine, long[] supplied) {
		return supplied[0] + machine.energy().getCapacity() - machine.energy().getAmount();
	}

	private static void insert(GameTestHelper helper, Storage<FluidVariant> storage, Fluid fluid, int mb) {
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = storage.insert(FluidVariant.of(fluid), mb * FluidConstants.BUCKET / 1000, transaction);
			helper.assertTrue(moved == mb * FluidConstants.BUCKET / 1000, "Only " + moved + " droplets of " + mb + " mB went in");
			transaction.commit();
		}
	}

	private static int extract(GameTestHelper helper, Storage<FluidVariant> storage, Fluid fluid) {
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = storage.extract(FluidVariant.of(fluid), 16 * FluidConstants.BUCKET, transaction);
			transaction.commit();
			return (int) (moved * 1000 / FluidConstants.BUCKET);
		}
	}

	private static int amount(FluidTank tank, Fluid fluid) {
		return !tank.variant.isBlank() && tank.variant.isOf(fluid) ? tank.millibuckets() : 0;
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static ItemStack stack(String path) {
		return new ItemStack(item(path));
	}
}
