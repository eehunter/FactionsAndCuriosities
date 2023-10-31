package de.joh.fnc.utils;

import de.joh.fnc.FactionsAndCuriosities;
import de.joh.fnc.spelladjustment.util.SpellAdjustment;
import de.joh.fnc.wildmagic.util.WildMagic;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

/**
 * Initialization of the registers of this mod
 * @author Joh0210
 */
@Mod.EventBusSubscriber(modid = FactionsAndCuriosities.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Registries {
    /**
     * Register for all Wild Magic Effects
     */
    public static Supplier<IForgeRegistry<WildMagic>> WILD_MAGIC;

    /**
     * Register for all Random Spell Adjustments
     */
    public static Supplier<IForgeRegistry<SpellAdjustment>> SPELL_ADJUSTMENTS;

    public Registries() {
    }

    @SubscribeEvent
    public static void RegisterRegistries(NewRegistryEvent event) {
        RegistryBuilder<SpellAdjustment> spellAdjustment = new RegistryBuilder<>();
        spellAdjustment.setName(RLoc.create("spelladjustment")).setType(SpellAdjustment.class).set(key -> SpellAdjustment.INSTANCE).set((key, isNetwork) -> SpellAdjustment.INSTANCE).disableSaving().allowModification();
        SPELL_ADJUSTMENTS = event.create(spellAdjustment);

        RegistryBuilder<WildMagic> wildMagic = new RegistryBuilder<>();
        wildMagic.setName(RLoc.create("wildmagic")).setType(WildMagic.class).set(key -> WildMagic.INSTANCE).set((key, isNetwork) -> WildMagic.INSTANCE).disableSaving().allowModification();
        WILD_MAGIC = event.create(wildMagic);
    }
}
