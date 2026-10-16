package dev.hearthhorizons.compat.culinary;

import com.google.gson.*;
import dev.hearthhorizons.compat.HearthHorizonsCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Fixed, source-controlled registry manifest derived from the installed recipe
 * audit. No runtime datapack can silently add/remove registered item IDs.
 */
public final class CookingPreparations {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(HearthHorizonsCompat.ID);
    public record Definition(String recipe,String result,int count,String stage,DeferredItem<Item> item) {}
    public static final List<Definition> ALL=load();
    private static List<Definition> load() {
        var list=new ArrayList<Definition>();
        try(var stream=CookingPreparations.class.getResourceAsStream("/culinary-preparations.json")) {
            if(stream==null) return List.of();
            var array=JsonParser.parseString(new String(stream.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonArray();
            for(var entry:array) {
                var row=entry.getAsJsonObject();
                String path=row.get("raw_path").getAsString(), result=row.get("result").getAsString();
                String[] id=result.split(":",2);
                String translationKey=result.equals("minecraft:cake") ? "block.minecraft.cake" : "item."+id[0]+"."+id[1];
                var item=ITEMS.register(path,() -> new Item(new Item.Properties().component(DataComponents.ITEM_NAME,
                    Component.translatable("item.hearth_horizons_compat.uncooked",Component.translatable(translationKey)))));
                list.add(new Definition(row.get("source_recipe").getAsString(),result,row.get("count").getAsInt(),row.get("stage").getAsString(),item));
            }
        } catch(Exception ex) { throw new IllegalStateException("Invalid fixed cooking preparation manifest",ex); }
        return List.copyOf(list);
    }
    public static void register(IEventBus bus) { ITEMS.register(bus); }
    private CookingPreparations() {}
}
