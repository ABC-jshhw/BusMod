package com.example.busmod.block;

import com.example.busmod.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Binding data stored on a platform item stack.
 *
 * <p>A platform item only becomes placeable after it was bound to a bus stop
 * by right-clicking the stop while holding the item. The binding is stored as
 * a data component so it survives being dropped, stored in a chest or moved
 * between dimensions.</p>
 */
public final class PlatformBinding {
    /** Suffix used when the item is not bound yet. */
    public static final String UNBOUND = "";

    private PlatformBinding() {
    }

    /** Returns {@code true} when the stack carries a valid binding. */
    public static boolean isBound(ItemStack stack) {
        Binding binding = stack.get(ModDataComponents.PLATFORM_BINDING.get());
        return binding != null && !binding.suffix().isEmpty();
    }

    /** Position of the bound stop, or {@code null} when unbound. */
    public static BlockPos getStopPos(ItemStack stack) {
        Binding binding = stack.get(ModDataComponents.PLATFORM_BINDING.get());
        return binding == null ? null : binding.stopPos();
    }

    /** Suffix of the bound platform, or an empty string when unbound. */
    public static String getSuffix(ItemStack stack) {
        Binding binding = stack.get(ModDataComponents.PLATFORM_BINDING.get());
        return binding == null ? UNBOUND : binding.suffix();
    }

    /** Writes a binding onto the stack. */
    public static void bind(ItemStack stack, BlockPos stopPos, String suffix) {
        stack.set(ModDataComponents.PLATFORM_BINDING.get(), new Binding(stopPos.immutable(), suffix));
    }

    /** Removes the binding from the stack. */
    public static void unbind(ItemStack stack) {
        stack.remove(ModDataComponents.PLATFORM_BINDING.get());
    }

    /** Human readable id of the bound platform, e.g. {@code 001-A}. */
    public static Component describe(ItemStack stack) {
        BlockPos pos = getStopPos(stack);
        String suffix = getSuffix(stack);
        if (pos == null || suffix.isEmpty()) {
            return Component.translatable("tooltip.busmod.platform.unbound");
        }
        return Component.literal(suffix + " @ " + pos.toShortString());
    }

    /**
     * Immutable binding payload.
     *
     * @param stopPos position of the bus stop block
     * @param suffix  single upper case letter that makes the platform unique
     */
    public record Binding(BlockPos stopPos, String suffix) {
    }
}
