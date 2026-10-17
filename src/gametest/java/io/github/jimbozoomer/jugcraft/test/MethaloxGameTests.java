package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Methane/LOX (docs/features/methalox.md): the cryogenic liquefier condenses 1,000 mB of methane into 250 mB of liquid
 * methane; the Chemical Reactor fills a rocket casing with 1,000 mB of it into a liquid methane tank; and the rocket
 * workshop pairs a methane tank and a liquid oxygen tank with two nozzles into four rocket motors. Hydrogen/LOX
 * (docs/features/hydrolox.md) follows: 1,000 mB of hydrogen condenses to 125 mB of liquid hydrogen, a casing fills with
 * 1,000 mB of it, and a hydrogen tank, a liquid oxygen tank and two nozzles load five rocket motors.
 */
public class MethaloxGameTests {
	@GameTest
	public void liquefierCondensesMethane(GameTestHelper helper) {
		FluidRecipe recipe = recipe(helper, MachineKind.CRYOGENIC_LIQUEFIER, "liquefaction/liquid_methane");
		helper.assertTrue(recipe.fluids().size() == 1 && recipe.fluids().getFirst().fluid() == PetroFluids.METHANE.fluid()
				&& recipe.fluids().getFirst().amount() == 1000, "The liquefier takes " + recipe.fluids());
		helper.assertTrue(recipe.fluidResults().size() == 1
				&& recipe.fluidResults().getFirst().fluid() == PetroFluids.LIQUID_METHANE.source()
				&& recipe.fluidResults().getFirst().amount() == 250, "The liquefier gives " + recipe.fluidResults());
		helper.succeed();
	}

	@GameTest
	public void reactorFillsMethaneTank(GameTestHelper helper) {
		FluidRecipe recipe = recipe(helper, MachineKind.CHEMICAL_REACTOR, "chemical_reaction/methane_tank");
		helper.assertTrue(recipe.itemsMatch(List.of(new ItemStack(item("rocket_casing")))), "The tank does not take a casing");
		helper.assertTrue(recipe.fluids().size() == 1 && recipe.fluids().getFirst().fluid() == PetroFluids.LIQUID_METHANE.source()
				&& recipe.fluids().getFirst().amount() == 1000, "The tank takes " + recipe.fluids());
		ItemStack tank = recipe.results().getFirst().create();
		helper.assertTrue(tank.is(item("methane_tank")) && tank.getCount() == 1, "The reactor makes " + tank);
		helper.succeed();
	}

	@GameTest
	public void workshopLoadsMethaloxMotors(GameTestHelper helper) {
		List<ItemStack> inputs = List.of(new ItemStack(item("methane_tank")), new ItemStack(item("lox_tank")),
				new ItemStack(item("rocket_nozzle"), 2));
		ItemStack motors = MachineRecipes.findMulti(helper.getLevel(), MachineKind.ROCKET_WORKSHOP, inputs)
				.orElseThrow(() -> helper.assertionException("The workshop does not pair methane and oxygen tanks"))
				.recipe().output().create();
		helper.assertTrue(motors.is(item("rocket_motor")) && motors.getCount() == 4, "The workshop makes " + motors);
		helper.succeed();
	}

	@GameTest
	public void hydrogenLiquefiesAndLoadsFiveMotors(GameTestHelper helper) {
		FluidRecipe liquefy = recipe(helper, MachineKind.CRYOGENIC_LIQUEFIER, "liquefaction/liquid_hydrogen");
		helper.assertTrue(liquefy.fluids().getFirst().fluid() == PetroFluids.HYDROGEN.fluid()
				&& liquefy.fluids().getFirst().amount() == 1000
				&& liquefy.fluidResults().getFirst().fluid() == PetroFluids.LIQUID_HYDROGEN.source()
				&& liquefy.fluidResults().getFirst().amount() == 125, "Hydrogen liquefies as " + liquefy.fluidResults());
		FluidRecipe fill = recipe(helper, MachineKind.CHEMICAL_REACTOR, "chemical_reaction/hydrogen_tank");
		helper.assertTrue(fill.itemsMatch(List.of(new ItemStack(item("rocket_casing"))))
				&& fill.fluids().getFirst().fluid() == PetroFluids.LIQUID_HYDROGEN.source()
				&& fill.fluids().getFirst().amount() == 1000, "The hydrogen tank takes " + fill.fluids());
		ItemStack tank = fill.results().getFirst().create();
		helper.assertTrue(tank.is(item("hydrogen_tank")), "The reactor makes " + tank);
		List<ItemStack> inputs = List.of(new ItemStack(item("hydrogen_tank")), new ItemStack(item("lox_tank")),
				new ItemStack(item("rocket_nozzle"), 2));
		ItemStack motors = MachineRecipes.findMulti(helper.getLevel(), MachineKind.ROCKET_WORKSHOP, inputs)
				.orElseThrow(() -> helper.assertionException("The workshop does not pair hydrogen and oxygen tanks"))
				.recipe().output().create();
		helper.assertTrue(motors.is(item("rocket_motor")) && motors.getCount() == 5, "The workshop makes " + motors);
		helper.succeed();
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static FluidRecipe recipe(GameTestHelper helper, MachineKind kind, String path) {
		return FluidRecipes.entries(helper.getLevel().getServer(), kind).stream()
				.filter(e -> e.id().equals(Jugcraft.id(path))).findFirst()
				.orElseThrow(() -> helper.assertionException("No recipe " + path)).recipe();
	}
}
