package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipe;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.material.Fluids;

/**
 * Glucose syrup (docs/features/glucose-syrup.md): the Pulverizer grinds one corn into one corn starch; the Chemical Reactor
 * breaks two starch with 500 mB of water and 50 mB of hydrochloric acid into three glucose syrup; and the syrup boils down
 * to sugar in a furnace or smoker.
 */
public class GlucoseSyrupGameTests {
	@GameTest
	public void cornGrindsToStarch(GameTestHelper helper) {
		MachineRecipe grinding = MachineRecipes.find(helper.getLevel(), MachineKind.PULVERIZER,
				new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("corn"))))
				.orElseThrow(() -> helper.assertionException("The pulverizer does not grind corn"));
		ItemStack starch = grinding.output().create();
		helper.assertTrue(starch.is(PetroItems.CORN_STARCH) && starch.getCount() == 1, "Corn grinds into " + starch);
		helper.succeed();
	}

	@GameTest
	public void starchBreaksDownIntoSyrup(GameTestHelper helper) {
		FluidRecipe syrup = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(e -> e.id().equals(Jugcraft.id("chemical_reaction/glucose_syrup"))).findFirst()
				.orElseThrow(() -> helper.assertionException("No glucose syrup reaction")).recipe();
		helper.assertTrue(syrup.itemsMatch(List.of(new ItemStack(PetroItems.CORN_STARCH, 2))),
				"The reaction does not take two corn starch");
		helper.assertTrue(syrup.fluids().size() == 2 && syrup.fluids().get(0).fluid() == Fluids.WATER
				&& syrup.fluids().get(0).amount() == 500 && syrup.fluids().get(1).fluid() == PetroFluids.HYDROCHLORIC_ACID.source()
				&& syrup.fluids().get(1).amount() == 50, "The reaction takes " + syrup.fluids());
		ItemStack result = syrup.results().getFirst().create();
		helper.assertTrue(result.is(PetroItems.GLUCOSE_SYRUP) && result.getCount() == 3, "The reaction makes " + result);
		helper.succeed();
	}

	@GameTest
	public void syrupBoilsDownToSugar(GameTestHelper helper) {
		for (ItemStack sugar : List.of(boil(helper, RecipeType.SMELTING), boil(helper, RecipeType.SMOKING))) {
			helper.assertTrue(sugar.is(Items.SUGAR) && sugar.getCount() == 1, "Glucose syrup boils into " + sugar);
		}
		helper.succeed();
	}

	private static <T extends Recipe<SingleRecipeInput>> ItemStack boil(GameTestHelper helper, RecipeType<T> type) {
		ServerLevel level = helper.getLevel();
		SingleRecipeInput syrup = new SingleRecipeInput(new ItemStack(PetroItems.GLUCOSE_SYRUP));
		return level.getServer().getRecipeManager().getRecipeFor(type, syrup, level)
				.orElseThrow(() -> helper.assertionException(type + " does not boil glucose syrup")).value().assemble(syrup);
	}
}
