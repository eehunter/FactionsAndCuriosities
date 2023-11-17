package de.joh.fnc.common.event;

import com.mna.api.events.ComponentApplyingEvent;
import com.mna.api.events.SpellCastEvent;
import com.mna.api.spells.attributes.Attribute;
import com.mna.api.spells.base.IModifiedSpellPart;
import com.mna.api.spells.parts.Shape;
import de.joh.fnc.FactionsAndCuriosities;
import de.joh.fnc.common.init.EffectInit;
import de.joh.fnc.common.effect.neutral.WildMagicCooldownMobEffect;
import de.joh.fnc.api.event.PerformSpellAdjustmentEvent;
import de.joh.fnc.api.event.PerformWildMagicEvent;
import de.joh.fnc.api.event.ShouldCauseWildMagicEvent;
import de.joh.fnc.common.init.ItemInit;
import de.joh.fnc.common.item.DebugOrbSpellAdjustmentItem;
import de.joh.fnc.common.item.FourLeafCloverRingItem;
import de.joh.fnc.common.item.MischiefArmorItem;
import de.joh.fnc.api.spelladjustment.SpellAdjustmentHelper;
import de.joh.fnc.common.utils.CommonConfig;
import de.joh.fnc.api.util.Quality;
import de.joh.fnc.api.wildmagic.WildMagic;
import de.joh.fnc.api.wildmagic.WildMagicHelper;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Handler for Forge-Events that revolve around magic
 * @author Joh0210
 */
@Mod.EventBusSubscriber(modid = FactionsAndCuriosities.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MagicEventHandler {
    /**
     * Handels {@link EffectInit#MAXIMIZED}, {@link EffectInit#MINIMIZED}, {@link EffectInit#EMPOWERED} and {@link EffectInit#RANDOM_SPELL_ADJUSTMENT}
     * also:
     * @see DebugOrbSpellAdjustmentItem
     * @see MischiefArmorItem
     */
    @SubscribeEvent
    public static void onSpellCast(SpellCastEvent event){
        Player caster = event.getCaster();

        if(caster.getItemBySlot(EquipmentSlot.OFFHAND).getItem() instanceof DebugOrbSpellAdjustmentItem debugOrbSpellAdjustmentItem){
            debugOrbSpellAdjustmentItem.useSpellAdjustment(caster.getLevel(), caster, caster.getItemBySlot(EquipmentSlot.OFFHAND), event);
        }

        //todo: outsource as Event
        if(caster.hasEffect(EffectInit.RANDOM_SPELL_ADJUSTMENT.get())){
            SpellAdjustmentHelper.performRandomSpellAdjustment(event, (rs, c, s) -> true);
            caster.removeEffect(EffectInit.RANDOM_SPELL_ADJUSTMENT.get());
            caster.addEffect(new MobEffectInstance(EffectInit.WILD_MAGIC_COOLDOWN.get(), CommonConfig.getWildMagicCooldown(), 0));
        } else if(caster.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof MischiefArmorItem mischiefArmorItem
                && mischiefArmorItem.isSetEquipped(caster)
                && new Random().nextBoolean()
        ){
            SpellAdjustmentHelper.performRandomSpellAdjustment(event, (rs, c, s) -> rs.getQuality(s.getComponent(0).getPart().getUseTag()).ordinal() >= Quality.NEUTRAL.ordinal());
        }

        IModifiedSpellPart<Shape> shape = event.getSpell().getShape();

        int maximizedLevel = 0;
        if(caster.hasEffect(EffectInit.MAXIMIZED.get())){
            maximizedLevel += caster.getEffect(EffectInit.MAXIMIZED.get()).getAmplifier() + 1;
        }
        if(caster.hasEffect(EffectInit.MINIMIZED.get())){
            maximizedLevel -= caster.getEffect(EffectInit.MINIMIZED.get()).getAmplifier() + 1;
        }
        if(maximizedLevel > 0 && shape != null){
            event.getSpell().getComponents().forEach(modifiedSpellPart -> modifiedSpellPart.getContainedAttributes().stream()
                    .filter(attribute -> attribute != Attribute.DELAY)
                    .forEach(attribute -> modifiedSpellPart.setValue(attribute, modifiedSpellPart.getMaximumValue(attribute))));
        } else if(maximizedLevel < 0 && shape != null){
            event.getSpell().getComponents().forEach(modifiedSpellPart -> modifiedSpellPart.getContainedAttributes().stream()
                    .filter(attribute -> attribute != Attribute.DELAY)
                    .forEach(attribute -> modifiedSpellPart.setValue(attribute, modifiedSpellPart.getMinimumValue(attribute))));
        }

        if(caster.hasEffect(EffectInit.EMPOWERED.get()) && shape != null){
            int level = caster.getEffect(EffectInit.EMPOWERED.get()).getAmplifier() + 1;
            event.getSpell().getComponents().forEach(modifiedSpellPart -> modifiedSpellPart.getContainedAttributes().stream()
                    .filter(attribute -> attribute != Attribute.DELAY && attribute != Attribute.PRECISION)
                    .forEach(attribute -> modifiedSpellPart.setValue(attribute, modifiedSpellPart.getValue(attribute) + level * modifiedSpellPart.getStep(attribute))));

            event.getSpell().getComponents().forEach(modifiedSpellPart -> modifiedSpellPart.getContainedAttributes().stream()
                    .filter(attribute -> attribute == Attribute.PRECISION)
                    .forEach(attribute -> modifiedSpellPart.setValue(attribute, Math.min(modifiedSpellPart.getValue(attribute) + level * modifiedSpellPart.getStep(attribute), modifiedSpellPart.getMaximumValue(attribute)))));
        }
    }

    /**
     * Causes the Wild Magic, when a spell takes place.
     * <br> a small cooldown will be added.
     * <br>Determination of (dis-)advantage the Wild Magic Roll: {@link WildMagicHelper#getWildMagicLuck(LivingEntity) WildMagicHelper.getWildMagicLuck()}
     * @see WildMagicCooldownMobEffect
     * @see WildMagic
     */
    @SubscribeEvent
    public static void onComponentApplying(ComponentApplyingEvent event){
        LivingEntity source = event.getSource().getPlayer();

        ShouldCauseWildMagicEvent shouldCauseWildMagicEvent = new ShouldCauseWildMagicEvent(source);
        MinecraftForge.EVENT_BUS.post(shouldCauseWildMagicEvent);
        if(source != null && shouldCauseWildMagicEvent.shouldCauseWildMagic()) {
            WildMagicHelper.performRandomWildMagic(source, event.getTarget(), event.getComponent().getUseTag(), (wm, s, t, ct) -> true);
            source.addEffect(new MobEffectInstance(EffectInit.WILD_MAGIC_COOLDOWN.get(), CommonConfig.getWildMagicCooldown(), 0));
        }
    }

    /**
     * Processing of {@link FourLeafCloverRingItem} protection
     */
    @SubscribeEvent
    public static void onPerformWildMagic(PerformWildMagicEvent event){
        LivingEntity source = event.getSource();
        if(event.isCancelable() && !event.isCanceled()
                && (event.getQuality() == Quality.VERY_BAD)
                && ((FourLeafCloverRingItem) ItemInit.FOUR_LEAF_CLOVER_RING.get()).isEquippedAndHasMana(source, 20.0F, true)
        ){
            event.setCanceled(true);
            if(source instanceof Player){
                ((Player) source).displayClientMessage(new TranslatableComponent("fnc.feedback.wildmagic.accident_protection"), true);
            }
            source.level.playSound(null, source.getX(), source.getY(), source.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.9F + (float)Math.random() * 0.2F);
        }
    }

    /**
     * Processing of {@link FourLeafCloverRingItem} protection
     */
    @SubscribeEvent
    public static void onPerformSpellAdjustment(PerformSpellAdjustmentEvent event){
        Player source = event.getSource();
        if(event.isCancelable() && !event.isCanceled()
                && (event.getQuality() == Quality.VERY_BAD)
                && ((FourLeafCloverRingItem) ItemInit.FOUR_LEAF_CLOVER_RING.get()).isEquippedAndHasMana(source, 20.0F, true)
        ){
            event.setCanceled(true);
            source.displayClientMessage(new TranslatableComponent("fnc.feedback.wildmagic.accident_protection"), true);
            source.level.playSound(null, source.getX(), source.getY(), source.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.9F + (float)Math.random() * 0.2F);
        }
    }
}
