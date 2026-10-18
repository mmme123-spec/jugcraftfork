package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;

/** The tablet's slots sit on the transmutation art's circles: sixteen learned patterns a page, none overlapping. */
public class TransmutationTabletLayoutGameTests {
    private static final Item[] COMMON={Items.COBBLESTONE,Items.DIRT,Items.SAND,Items.GRAVEL,Items.OAK_PLANKS,Items.STICK,Items.IRON_INGOT,
            Items.GOLD_INGOT,Items.COPPER_INGOT,Items.COAL,Items.REDSTONE,Items.LAPIS_LAZULI,Items.QUARTZ,Items.CLAY_BALL,Items.WHEAT,
            Items.SUGAR_CANE,Items.PAPER,Items.STRING,Items.GLASS,Items.STONE,Items.OAK_LOG,Items.FLINT};
    @GameTest(maxTicks=20) public void learnedPatternsRingTheRightCircle(GameTestHelper h){
        var p=ConnectedAlchemyGameTests.player(h);
        int known=0;
        for(Item item:COMMON){
            var stack=new ItemStack(item);AlchemyPatterns.learn(p,stack);
            var material=AlchemicalWorkshopBlockEntity.material(stack);if(material!=null&&material.formable())known++;
        }
        h.assertTrue(known>AlchemyPatternMenu.PAGE_SIZE,"The fixture knows more patterns than one page holds ("+known+")");
        var m=new AlchemyPatternMenu(1,p.getInventory(),()->true,ItemStack.EMPTY);m.broadcastChanges();
        h.assertTrue(AlchemyPatternMenu.PAGE_SIZE==16&&m.slots.size()==AlchemyPatternMenu.GHOST_START+16,"Sixteen pattern slots after the two real slots and the inventory");
        int shown=0;
        for(int i=0;i<16;i++)if(m.getSlot(AlchemyPatternMenu.GHOST_START+i).hasItem())shown++;
        h.assertTrue(shown==16&&m.pages()==(known+15)/16,"A full first page of sixteen, and the rest on the next ("+shown+" shown, "+m.pages()+" pages)");
        for(int a=0;a<m.slots.size();a++)for(int b=a+1;b<m.slots.size();b++){
            var s=m.slots.get(a);var t=m.slots.get(b);
            h.assertTrue(Math.abs(s.x-t.x)>=18||Math.abs(s.y-t.y)>=18,"Slots "+a+" and "+b+" do not overlap");
        }
        // The input is the left circle's centre and the output the right's, with every pattern round the output.
        var input=m.getSlot(0);var output=m.getSlot(1);
        h.assertTrue(input.x==42&&input.y==49&&output.x==157&&output.y==49,"Input and output sit at the circles' centres");
        for(int i=0;i<16;i++){
            var s=m.getSlot(AlchemyPatternMenu.GHOST_START+i);int dx=s.x-output.x,dy=s.y-output.y;
            h.assertTrue(dx*dx+dy*dy<=45*45,"Pattern "+i+" lies on the right circle");
        }
        m.removed(p);h.succeed();
    }
}
