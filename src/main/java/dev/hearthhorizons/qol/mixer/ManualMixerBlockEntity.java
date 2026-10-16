package dev.hearthhorizons.qol.mixer;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.*;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Reuses Create's transactional basin recipe application, including fluid/item remainders. */
public final class ManualMixerBlockEntity extends BasinBlockEntity {
    public static final int RPM=32, KEEP_ALIVE_TICKS=10;
    private long activeUntil, lastTurn=Long.MIN_VALUE;
    private boolean backwards;
    private float angle, previousAngle;
    private ResourceLocation recipeId;
    private int progress;
    private RecipeHolder<MixingRecipe> cached;
    private Object recipeManagerIdentity;
    private int retry;
    public ManualMixerBlockEntity(BlockPos pos,BlockState state){super(ManualMixer.TYPE.get(),pos,state);}
    public IItemHandlerModifiable items(){return itemCapability;}
    public IFluidHandler fluids(){return fluidCapability;}
    public int progress(){return progress;}
    public boolean rotating(){return level!=null&&level.getGameTime()<activeUntil;}
    public float angle(float partial){return previousAngle+(angle-previousAngle)*partial;}
    public float direction(){return backwards?-1:1;}
    public static boolean allowed(MixingRecipe recipe){return recipe.getRequiredHeat()==HeatCondition.NONE;}
    public static int duration(MixingRecipe recipe){return Math.max(20,recipe.getProcessingDuration()>0?recipe.getProcessingDuration():100);}
    public boolean turn(Player player){
        if(level==null||level.isClientSide||player.isSpectator()||!player.mayBuild())return false;
        long now=level.getGameTime();
        if(lastTurn!=Long.MIN_VALUE&&now-lastTurn<4)return false;
        lastTurn=now;activeUntil=now+KEEP_ALIVE_TICKS;backwards=player.isShiftKeyDown();
        // Exactly the installed Create hand crank: speed * server-configured food exhaustion.
        player.causeFoodExhaustion(RPM*AllConfigs.server().kinetics.crankHungerMultiplier.getF());
        AllSoundEvents.CRANKING.playOnServer(level,worldPosition,.4f,backwards?.9f:1f);
        notifyUpdate();return true;
    }
    @Override public boolean canContinueProcessing(){return false;} // never driven by a separate powered mixer above
    @Override public void notifyChangeOfContents(){super.notifyChangeOfContents();retry=0;}
    private RecipeHolder<MixingRecipe> recipe(){
        var manager=level.getRecipeManager();
        if(recipeManagerIdentity!=manager){cached=null;recipeManagerIdentity=manager;retry=0;}
        if(cached!=null&&manager.byKey(cached.id()).map(h->h.value()==cached.value()).orElse(false)
            &&allowed(cached.value())&&BasinRecipe.match(this,cached.value()))return cached;
        cached=null;
        if(retry-->0)return null;
        retry=20;
        // No namespace allow-list: all loaded create:mixing recipes, including generated ones.
        var recipes=manager.<net.minecraft.world.item.crafting.RecipeInput,MixingRecipe>getAllRecipesFor(AllRecipeTypes.MIXING.getType());
        for(var holder:recipes){if(allowed(holder.value())&&BasinRecipe.match(this,holder.value())){cached=holder;return holder;}}
        return null;
    }
    @Override public void tick(){
        super.tick();
        previousAngle=angle;
        if(rotating())angle+=direction()*RPM*.3f;
        if(level==null||level.isClientSide||!rotating())return;
        var holder=recipe();
        if(holder==null){if(progress!=0||recipeId!=null){progress=0;recipeId=null;setChanged();}return;}
        if(!holder.id().equals(recipeId)){recipeId=holder.id();progress=0;}
        progress++;
        if(progress>=duration(holder.value())){
            // Apply rechecks current inputs and full result capacity before consuming anything.
            if(allowed(holder.value())&&BasinRecipe.apply(this,holder.value())){
                progress=0;recipeId=null;cached=null;retry=0;notifyUpdate();
            }
        }
        setChanged();
    }
    @Override public void write(CompoundTag tag,HolderLookup.Provider registries,boolean client){
        super.write(tag,registries,client);
        tag.putInt("MixerProgress",progress);if(recipeId!=null)tag.putString("MixerRecipe",recipeId.toString());
        if(client){tag.putLong("MixerActiveUntil",activeUntil);tag.putBoolean("MixerBackwards",backwards);tag.putFloat("MixerAngle",angle);}
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean client){
        super.read(tag,registries,client);progress=Math.max(0,tag.getInt("MixerProgress"));recipeId=dev.hearthhorizons.qol.migration.LegacyIds.migrate(ResourceLocation.tryParse(tag.getString("MixerRecipe")));
        if(client){activeUntil=tag.getLong("MixerActiveUntil");backwards=tag.getBoolean("MixerBackwards");angle=previousAngle=tag.getFloat("MixerAngle");}
        else {activeUntil=0;cached=null;recipeManagerIdentity=null;retry=0;}
    }
}
