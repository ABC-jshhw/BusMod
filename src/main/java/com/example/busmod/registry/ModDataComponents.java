package com.example.busmod.registry;

import com.example.busmod.BusMod;
import com.example.busmod.block.PlatformBinding;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components added by the mod. The platform binding is stored on the
 * platform item so a bound platform keeps its stop and suffix when it is
 * picked up and placed again.
 */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BusMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PlatformBinding.Binding>> PLATFORM_BINDING =
            DATA_COMPONENTS.register("platform_binding", () -> DataComponentType.<PlatformBinding.Binding>builder()
                    .persistent(RecordCodecBuilder.create(instance -> instance.group(
                            BlockPos.CODEC.fieldOf("stop").forGetter(PlatformBinding.Binding::stopPos),
                            Codec.STRING.fieldOf("suffix").forGetter(PlatformBinding.Binding::suffix))
                            .apply(instance, PlatformBinding.Binding::new)))
                    .networkSynchronized(net.minecraft.network.codec.StreamCodec.composite(
                            BlockPos.STREAM_CODEC, PlatformBinding.Binding::stopPos,
                            ByteBufCodecs.STRING_UTF8, PlatformBinding.Binding::suffix,
                            PlatformBinding.Binding::new))
                    .build());

    private ModDataComponents() {
    }
}
