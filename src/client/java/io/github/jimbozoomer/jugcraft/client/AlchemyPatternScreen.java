package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The tablet laid out like the classic transmutation table, on the owner's two-circle art: what you dissolve sits in the
 * left circle, sixteen learned patterns ring the output in the right one, and the controls stand in a column beside
 * them. Only the layout lives here (the circle slots are in the menu); the art, the column and the read-outs are drawn
 * separately so any of them can be reworked. Transactions are server-owned.
 */
public final class AlchemyPatternScreen extends AbstractContainerScreen<AlchemyPatternMenu> {
    private static final Identifier ART=Jugcraft.id("textures/gui/alchemy/transmutation.png");
    /** The art's sheet is 229 x 197 with a four-pixel bevelled border; the column starts right of it. */
    private static final int ART_W=229, ART_H=197, EDGE=4, COLUMN=232, COLUMN_W=122;
    private static final int FACE=0xFFC6C6C6, TEXT=0xFF404040, MUTED=0xFF5E5E5E, VIOLET=0xFF6B2FA3, GLOW=0xFFB45CF0,
            READOUT=0xFF1B1424, BRIGHT=0xFFE9DDF8, DIM=0xFFA898BC;
    private EditBox search;
    private String sent="";
    private int debounce;
    private SheetButton learn,make,favorite,filter,previous,next,area;
    private final SheetButton[] batches=new SheetButton[4];
    public AlchemyPatternScreen(AlchemyPatternMenu menu,Inventory inv,Component title){super(menu,inv,title,360,238);}
    private static Component ui(String s,Object... args){return Component.translatable("screen.jugcraft.pattern."+s,args);}
    private void press(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private SheetButton button(int x,int y,int w,Component title,Button.OnPress action){return addRenderableWidget(new SheetButton(leftPos+x,topPos+y,w,title,action));}
    private SheetButton button(int x,int y,int w,String key,int id){return button(x,y,w,ui(key),b->press(id));}
    @Override protected void init(){
        String saved=search==null?sent:search.getValue();super.init();
        search=addRenderableWidget(new EditBox(font,leftPos+COLUMN,topPos+40,COLUMN_W,16,ui("search")));search.setMaxLength(48);search.setHint(ui("search"));search.setValue(saved);
        search.setResponder(s->debounce=5);
        learn=button(COLUMN,60,60,"learn",0);learn.setTooltip(Tooltip.create(ui("learn_help")));make=button(COLUMN+62,60,60,"make",1);
        for(int i=0;i<4;i++)batches[i]=button(COLUMN+i*31,83,29,"batch_"+i,6+i);
        area=button(COLUMN,83,COLUMN_W,"area",10);
        favorite=button(COLUMN,106,20,"favorite",4);favorite.star=true;filter=button(COLUMN+22,106,100,"all",5);
        previous=button(COLUMN,129,20,"previous",2);next=button(COLUMN+102,129,20,"next",3);
        button(COLUMN,212,60,ui("guide"),b->{if(minecraft.player!=null)minecraft.player.closeContainer();minecraft.gui.setScreen(new AlchemyStorageManualScreen());});
        button(COLUMN+62,212,60,Component.translatable("screen.jugcraft.alchemy.gem_controls"),b->{if(minecraft.player!=null)minecraft.player.closeContainer();minecraft.gui.setScreen(new GemAbilitiesScreen());});
        refresh();
    }
    private void refresh(){
        boolean eye=menu.eye();
        area.visible=eye;area.setMessage(ui("area",menu.area(),menu.area()));
        learn.confirming=menu.confirming();learn.active=menu.canDissolve();learn.setMessage(ui(menu.confirming()?"confirm":"learn"));
        learn.visible=!eye;favorite.active=!menu.selection().isEmpty();previous.active=menu.page()>0;next.active=menu.page()+1<menu.pages();
        // The eye has nothing to dissolve, so its one action takes the whole row.
        make.setX(leftPos+COLUMN+(eye?0:62));make.setWidth(eye?COLUMN_W:60);
        make.active=menu.canMake();make.setMessage(ui(eye?"choose":"make"));
        favorite.setMessage(ui(menu.isFavorite()?"unfavorite":"favorite"));favorite.setTooltip(Tooltip.create(favorite.getMessage()));
        filter.setMessage(ui(menu.favorites()?"favorites":"all"));filter.selected=menu.favorites();favorite.selected=menu.isFavorite();
        make.setTooltip(Tooltip.create(ui(eye?"build_help":"make_help")));
        for(int i=0;i<4;i++){batches[i].visible=!eye;batches[i].selected=menu.batch()==new int[]{1,16,64,0}[i];batches[i].active=!batches[i].selected;}
    }
    @Override protected void containerTick(){
        super.containerTick();refresh();
        if(debounce>0&&--debounce==0&&!sent.equals(search.getValue())){sent=search.getValue();ClientPlayNetworking.send(new AlchemyPatterns.Search(menu.containerId,sent));}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){extractBackground(g,mx,my,delta);super.extractRenderState(g,mx,my,delta);extractTooltip(g,mx,my);}
    /** Draws a piece of the art at its own size, or stretched when the source is a one-pixel strip. */
    private static void art(GuiGraphicsExtractor g,int x,int y,int u,int v,int w,int h,int uw,int vh){g.blit(RenderPipelines.GUI_TEXTURED,ART,x,y,u,v,w,h,uw,vh,256,256);}
    /** A sheet of any size cut from the art's own border: its corners as drawn, its edges stretched, its plain face. */
    private static void sheet(GuiGraphicsExtractor g,int x,int y,int w,int h){
        int r=ART_W-EDGE,b=ART_H-EDGE;
        art(g,x,y,0,0,EDGE,EDGE,EDGE,EDGE);art(g,x+w-EDGE,y,r,0,EDGE,EDGE,EDGE,EDGE);
        art(g,x,y+h-EDGE,0,b,EDGE,EDGE,EDGE,EDGE);art(g,x+w-EDGE,y+h-EDGE,r,b,EDGE,EDGE,EDGE,EDGE);
        art(g,x+EDGE,y,10,0,w-2*EDGE,EDGE,1,EDGE);art(g,x+EDGE,y+h-EDGE,10,b,w-2*EDGE,EDGE,1,EDGE);
        art(g,x,y+EDGE,0,10,EDGE,h-2*EDGE,EDGE,1);art(g,x+w-EDGE,y+EDGE,r,10,EDGE,h-2*EDGE,EDGE,1);
        g.fill(x+EDGE,y+EDGE,x+w-EDGE,y+h-EDGE,FACE);
    }
    /** A recessed vanilla slot well round an item position. */
    private static void well(GuiGraphicsExtractor g,int x,int y){
        g.fill(x-1,y-1,x+17,y+17,0xFF373737);g.fill(x,y,x+17,y+17,0xFFFFFFFF);g.fill(x,y,x+16,y+16,0xFF8B8B8B);
    }
    /** A sunken dark read-out panel for text that changes. */
    private static void readout(GuiGraphicsExtractor g,int x,int y,int w,int h){
        g.fill(x,y,x+w,y+h,0xFF373737);g.fill(x+1,y+1,x+w,y+h,0xFFFFFFFF);g.fill(x+1,y+1,x+w-1,y+h-1,READOUT);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;
        boolean eye=menu.eye();
        g.fill(0,0,width,height,0x90101014);
        sheet(g,x,y,imageWidth,imageHeight);
        art(g,x+EDGE,y+EDGE,EDGE,EDGE,ART_W-2*EDGE,ART_H-2*EDGE,ART_W-2*EDGE,ART_H-2*EDGE);
        // The art's unlearn and burn tiles belong to another mod's table; this tablet has neither.
        g.fill(x+88,y+96,x+124,y+114,FACE);
        // An engraved rule between the circles and the column.
        g.fill(x+ART_W,y+6,x+ART_W+1,y+imageHeight-6,0xFF8B8B8B);g.fill(x+ART_W+1,y+6,x+ART_W+2,y+imageHeight-6,0xFFFFFFFF);
        readout(g,x+6,y+197,ART_W-10,37);
        readout(g,x+COLUMN,y+153,COLUMN_W,55);
        var input=menu.getSlot(0);var output=menu.getSlot(1);
        int ix=x+input.x,iy=y+input.y,ox=x+output.x,oy=y+output.y;
        // A violet ring lights round each circle's centre while it can act; nothing moves.
        if(!eye&&menu.canDissolve())BookArt.circle(g,ix+8,iy+8,13,GLOW);
        if(menu.canMake())BookArt.circle(g,ox+8,oy+8,13,GLOW);
        well(g,ix,iy);well(g,ox,oy);
        var selection=menu.selection();
        if(!selection.isEmpty())g.item(selection,x+COLUMN+4,y+168);
        if(eye){
            BookArt.icon(g,"jugcraft:mercurial_eye",ix,iy);
            if(!selection.isEmpty())g.item(selection,ox,oy);
        }else if(!selection.isEmpty()&&output.getItem().isEmpty()){
            // What Withdraw will make, shown faintly until it is made.
            g.item(selection,ox,oy);g.fill(ox,oy,ox+16,oy+16,0xA08B8B8B);
        }
        for(var s:menu.slots){
            if(s.index<AlchemyPatternMenu.GHOST_START||s.getItem().isEmpty()||!ItemStack.isSameItemSameComponents(s.getItem(),selection))continue;
            int sx=x+s.x,sy=y+s.y;
            g.fill(sx-1,sy-1,sx+17,sy+17,0x60B45CF0);g.outline(sx-2,sy-2,20,20,VIOLET);
        }
    }
    private void line(GuiGraphicsExtractor g,Component text,int x,int y,int width,int color){g.text(font,BookArt.fit(font,text.getString(),width),x,y,color,false);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        boolean eye=menu.eye();
        line(g,ui(eye?"build":"title"),COLUMN,7,COLUMN_W,TEXT);
        line(g,ui("balance",String.format("%,d",menu.balance())),COLUMN,19,COLUMN_W,VIOLET);
        line(g,ui("sources",menu.balance()-menu.stars(),menu.stars()),COLUMN,29,COLUMN_W,MUTED);
        Component page=ui("page",menu.page()+1,menu.pages());
        g.text(font,page,COLUMN+(COLUMN_W-font.width(page))/2,135,TEXT,false);
        // The selected pattern: what it is and what one costs.
        line(g,ui("target"),COLUMN+4,157,COLUMN_W-8,DIM);
        var selection=menu.selection();
        if(selection.isEmpty())g.text(font,"-",COLUMN+4,172,BRIGHT,false);
        else{
            line(g,selection.getHoverName(),COLUMN+24,172,COLUMN_W-28,BRIGHT);
            line(g,ui("cost",eye?menu.price()+8:menu.price()),COLUMN+4,192,COLUMN_W-8,DIM);
        }
        // What the tablet is waiting for, under the inventory.
        int w=ART_W-18;
        if(eye){
            line(g,ui("cost",menu.price()+8),10,201,w,BRIGHT);
            line(g,ui("area_help"),10,212,w,DIM);
        }else{
            line(g,ui("input_"+menu.inputStatus(),menu.gain()),10,201,w,menu.inputStatus()==0?BRIGHT:DIM);
            line(g,ui("form_"+menu.formStatus()),10,212,w,menu.formStatus()==0?BRIGHT:DIM);
            line(g,ui("total",menu.batchCost()),10,223,w,DIM);
        }
    }

    /** A pressed-paper key in the art's own greys that lights violet like its stars. Native focus, narration and clicks. */
    static final class SheetButton extends Button {
        /** A five-pointed star, drawn for the favourite toggle in place of its (still narrated) label. */
        private static final String[] STAR={"....#....","....#....","...###...","#########",".#######.","..#####..","..##.##..",".##...##.",".#.....#."};
        boolean confirming, selected, star;
        SheetButton(int x,int y,int w,Component title,OnPress action){super(x,y,w,20,title,action,DEFAULT_NARRATION);}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            boolean lit=active&&(isHovered()||isFocused());
            int face=confirming?0xFFE9CF86:selected?0xFFC9A8EE:!active?0xFFA9A9A9:lit?0xFFEDE4F8:0xFFD8D8D8;
            g.fill(x,y,x+w,y+h,0xFF1E1E1E);
            g.fill(x+1,y+1,x+w-1,y+h-1,face);
            g.fill(x+1,y+1,x+w-1,y+2,0xFFFFFFFF);g.fill(x+1,y+2,x+2,y+h-2,0xFFFFFFFF);
            g.fill(x+1,y+h-2,x+w-1,y+h-1,0xFF7C7C7C);g.fill(x+w-2,y+2,x+w-1,y+h-2,0xFF7C7C7C);
            if(isFocused())g.outline(x,y,w,h,0xFF7A2FC0);
            if(star){
                int sx=x+(w-9)/2,sy=y+(h-9)/2;
                for(int row=0;row<9;row++)for(int col=0;col<9;col++)if(STAR[row].charAt(col)=='#'){
                    g.fill(sx+col+1,sy+row+1,sx+col+2,sy+row+2,0xFF3A3A3A);
                }
                for(int row=0;row<9;row++)for(int col=0;col<9;col++)if(STAR[row].charAt(col)=='#'){
                    g.fill(sx+col,sy+row,sx+col+1,sy+row+1,selected?0xFFE3A21A:active?0xFFF7F7F7:0xFFC4C4C4);
                }
                return;
            }
            var font=Minecraft.getInstance().font;
            String label=BookArt.fit(font,getMessage().getString(),w-6);
            g.text(font,label,x+(w-font.width(label))/2,y+(h-8)/2,active||selected||confirming?0xFF2A2A2A:0xFF6E6E6E,false);
        }
    }
}
