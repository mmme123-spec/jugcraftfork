package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * A jar of preserves: jam, fruit butter, jelly, pickles or relish, cooked into a Mason Jar in the Cooking Pot. It holds
 * {@value #SERVINGS} servings, eaten one at a time like food; the last leaves the empty jar.
 * <ul>
 * <li>Fresh from the pot it is unsealed, and keeps {@value #SPOIL_TICKS} ticks (three days) from when it was cooked
 * ({@link JarContents}). After that it has spoiled: a serving is a mouthful of bad food that turns the stomach.</li>
 * <li>Processed in a boiling Canning Kettle it is sealed ({@code jugcraft:sealed}): it keeps for ever, stacks with other
 * sealed jars of the same kind, and may go on a Pantry Shelf like any jar.</li>
 * <li>Opening a sealed jar (eating its first serving) starts its three days.</li>
 * <li>A preservative (item tag {@code jugcraft:preservatives}: citric acid) clicked onto an unspoiled jar is stirred in
 * ({@code jugcraft:preserved}, one a jar): the jar then keeps {@value #PRESERVED_FACTOR} times as long, unsealed or
 * once opened.</li>
 * </ul>
 */
public class PreserveJarItem extends Item {
	public static final int SERVINGS = 4;
	public static final int SPOIL_TICKS = 72000;
	public static final int PRESERVED_FACTOR = 2;
	public static final TagKey<Item> PRESERVATIVES = TagKey.create(Registries.ITEM, Jugcraft.id("preservatives"));
	private static final int EAT_TICKS = 32;
	private static final String TOOLTIP = "tooltip.jugcraft.preserves.";

	public final int nutrition;
	public final float saturation;
	public final @Nullable Holder<MobEffect> effect;
	public final int seconds;
	/** The colour of what is in the jar (for the renderers of the kettle and the shelf). */
	public final int color;

	public PreserveJarItem(Properties properties, int nutrition, float saturation, @Nullable Holder<MobEffect> effect, int seconds, int color) {
		super(properties);
		this.nutrition = nutrition;
		this.saturation = saturation;
		this.effect = effect;
		this.seconds = seconds;
		this.color = color;
	}

	public static boolean sealed(ItemStack stack) {
		return Boolean.TRUE.equals(stack.get(JugcraftAgriculture.SEALED));
	}

	/** The servings left: all of them in a sealed jar or one never opened. */
	public static int servings(ItemStack stack) {
		JarContents contents = stack.get(JugcraftAgriculture.JAR_CONTENTS);
		return sealed(stack) || contents == null ? SERVINGS : contents.servings();
	}

	public static boolean preserved(ItemStack stack) {
		return Boolean.TRUE.equals(stack.get(JugcraftAgriculture.PRESERVED));
	}

	/** How long an unsealed or opened jar keeps: three days, or twice that with a preservative stirred in. */
	public static long keeps(ItemStack stack) {
		return (long) SPOIL_TICKS * (preserved(stack) ? PRESERVED_FACTOR : 1);
	}

	/** Whether the jar has spoiled at game time {@code now}: unsealed, and made longer ago than it {@link #keeps}. */
	public static boolean spoiled(ItemStack stack, long now) {
		JarContents contents = stack.get(JugcraftAgriculture.JAR_CONTENTS);
		return !sealed(stack) && contents != null && now - contents.made() > keeps(stack);
	}

	/**
	 * Stirs a preservative into every jar of {@code jars} at game time {@code now}, one preservative a jar, taken from
	 * {@code preservative}; returns whether it did. A spoiled jar or one already preserved is left, and so is a stack
	 * the preservative can't cover.
	 */
	public static boolean preserve(ItemStack jars, ItemStack preservative, long now) {
		if (!(jars.getItem() instanceof PreserveJarItem) || !preservative.is(PRESERVATIVES) || preserved(jars) || spoiled(jars, now)
				|| preservative.getCount() < jars.getCount()) {
			return false;
		}
		jars.set(JugcraftAgriculture.PRESERVED, true);
		preservative.shrink(jars.getCount());
		return true;
	}

	/** Clicking a preservative onto a jar in an inventory stirs it in. */
	@Override
	public boolean overrideOtherStackedOnMe(ItemStack jars, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
		if (other.isEmpty() || !other.is(PRESERVATIVES) || !slot.allowModification(player)) {
			return false;
		}
		if (!preserve(jars, other, player.level().getGameTime())) {
			return false;
		}
		player.playSound(SoundEvents.BOTTLE_FILL, 0.6F, 1.2F);
		return true;
	}

	/** The game time an unsealed jar's three days started (now, for a sealed jar being opened or one never stamped). */
	private static long made(ItemStack stack, long now) {
		JarContents contents = stack.get(JugcraftAgriculture.JAR_CONTENTS);
		return sealed(stack) || contents == null ? now : contents.made();
	}

	/** Marks a jar just cooked at {@code now}: full and unsealed. */
	public static void cooked(ItemStack stack, long now) {
		stack.remove(JugcraftAgriculture.SEALED);
		stack.set(JugcraftAgriculture.JAR_CONTENTS, new JarContents(SERVINGS, now));
	}

	/** Seals a full jar: it keeps for ever. */
	public static void seal(ItemStack stack) {
		stack.remove(JugcraftAgriculture.JAR_CONTENTS);
		stack.set(JugcraftAgriculture.SEALED, true);
	}

	/**
	 * Whether a jar can be sealed at game time {@code now}: full, not yet sealed and not spoiled (an opened jar can't be
	 * put up again, and sealing a spoiled one would make it keep for ever).
	 */
	public static boolean sealable(ItemStack stack, long now) {
		return stack.getItem() instanceof PreserveJarItem && !sealed(stack) && servings(stack) == SERVINGS && !spoiled(stack, now);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.canEat(false)) {
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.EAT;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return EAT_TICKS;
	}

	/** One serving: food (and the preserve's effect), or, spoiled, a little food and a bad stomach. */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof Player player) || level.isClientSide()) {
			return stack;
		}
		long now = level.getGameTime();
		if (spoiled(stack, now)) {
			player.getFoodData().eat(1, 0.1F);
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600));
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200));
			player.sendOverlayMessage(Component.translatable(TOOLTIP + "spoiled_eaten"));
		} else {
			player.getFoodData().eat(nutrition, saturation);
			if (effect != null) {
				player.addEffect(new MobEffectInstance(effect, seconds * 20));
			}
		}
		level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.8F, 0.9F);
		int left = servings(stack) - 1;
		long made = made(stack, now);
		ItemStack rest;
		if (left > 0) {
			rest = stack.copyWithCount(1);
			rest.remove(JugcraftAgriculture.SEALED);
			rest.set(JugcraftAgriculture.JAR_CONTENTS, new JarContents(left, made));
		} else {
			rest = new ItemStack(JugcraftAgriculture.item("mason_jar"));
		}
		if (player.getAbilities().instabuild) {
			return stack;
		}
		stack.shrink(1);
		if (stack.isEmpty()) {
			return rest;
		}
		if (!player.getInventory().add(rest)) {
			Block.popResource(level, player.blockPosition(), rest);
		}
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		if (sealed(stack)) {
			tooltip.accept(Component.translatable(TOOLTIP + "sealed").withStyle(ChatFormatting.GREEN));
		} else if (stack.get(JugcraftAgriculture.JAR_CONTENTS) != null && servings(stack) < SERVINGS) {
			tooltip.accept(Component.translatable(TOOLTIP + "opened").withStyle(ChatFormatting.GOLD));
		} else {
			tooltip.accept(Component.translatable(TOOLTIP + "unsealed").withStyle(ChatFormatting.GOLD));
		}
		if (preserved(stack)) {
			tooltip.accept(Component.translatable(TOOLTIP + "preserved").withStyle(ChatFormatting.YELLOW));
		}
		tooltip.accept(Component.translatable(TOOLTIP + "servings", servings(stack), SERVINGS).withStyle(ChatFormatting.GRAY));
	}
}
