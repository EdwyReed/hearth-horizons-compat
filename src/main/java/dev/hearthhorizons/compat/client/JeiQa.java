package dev.hearthhorizons.compat.client;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.culinary.PreparationItems;
import dev.hearthhorizons.compat.culinary.CookingPreparations;
import dev.hearthhorizons.compat.jei.PreparationJeiPlugin;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.gui.bookmarks.BookmarkFactory;
import mezz.jei.gui.bookmarks.BookmarkList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in integration harness. It only runs in the separate development client's JeiQA world. */
@EventBusSubscriber(modid = HearthHorizonsCompat.ID, value = Dist.CLIENT)
public final class JeiQa {
    private static final BlockPos STOVE = new BlockPos(10, 79, 0);
    private static int stage, delay, age;
    private static boolean failed;
    private static String screen;
    private static CompletableFuture<Void> pending;
    private static IJeiRuntime jei;
    private static IRecipeLayoutDrawable<?> transferLayout;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("hh.jeiQa") || failed) return;
        Minecraft mc = Minecraft.getInstance();
        String nextScreen = mc.screen == null ? "world" : mc.screen.getClass().getName();
        if (!nextScreen.equals(screen)) { screen = nextScreen; System.out.println("JEI_QA_SCREEN " + screen); }
        if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding) { onboarding.onClose(); return; }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        try {
            if (++age > 12000) throw new IllegalStateException("JEI QA timed out at stage " + stage);
            if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
            if (delay-- > 0) return;
            jei = PreparationJeiPlugin.runtime();
            if (jei == null) return;
            switch (stage) {
                case 0 -> {
                    require(mc.getSingleplayerServer().getWorldData().getLevelName().equals("ShelfQA") ||
                        mc.getSingleplayerServer().getWorldData().getLevelName().equals("JeiQA"), "Not a QA world");
                    onServer(player -> {
                        player.setGameMode(GameType.SURVIVAL);
                        player.getInventory().clearContent();
                        for (var definition : PreparationItems.DEFINITIONS) {
                            player.getInventory().add(definition.stack());
                            player.getInventory().add(cooked(definition));
                        }
                        player.serverLevel().setDayTime(6000);
                    });
                    next(1, 50);
                }
                case 1 -> {
                    testIngredientsAndFocuses();
                    testExpandedStages();
                    testBookmarks(Boolean.getBoolean("hh.jeiQaReload"));
                    mc.setScreen(new InventoryScreen(mc.player));
                    jei.getIngredientFilter().setFilterText("@hearth");
                    next(2, 30);
                }
                case 2 -> {
                    capture("jei-preparations-list.png");
                    jei.getIngredientFilter().setFilterText(PreparationItems.DEFINITIONS.getFirst().stack().getHoverName().getString());
                    jei.getRecipesGui().show(focus(PreparationItems.DEFINITIONS.getFirst().stack(), RecipeIngredientRole.OUTPUT));
                    next(3, 30);
                }
                case 3 -> {
                    capture("jei-raw-recipes.png");
                    // The built-in tag-information tab can sort before cooking; select the actual stove tab for visual evidence.
                    showRecipe(layoutFor("hearth_horizons:baking/raisin_oatmeal_cookie"));
                    next(4, 30);
                }
                case 4 -> {
                    capture("jei-cooked-recipes.png");
                    mc.setScreen(null);
                    onServer(player -> {
                        player.closeContainer();
                        player.getInventory().clearContent();
                        var definition = PreparationItems.DEFINITIONS.getFirst();
                        player.getInventory().add(definition.stack().copyWithCount(4));
                        player.getInventory().add(cooked(definition).copyWithCount(7));
                        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("farm_and_charm:stove"));
                        player.teleportTo(STOVE.getX()+.5,STOVE.getY()+1,STOVE.getZ()+2.5);
                        if (player.serverLevel().getBlockEntity(STOVE) instanceof net.minecraft.world.Container old) old.clearContent();
                        player.serverLevel().setBlock(STOVE, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        player.serverLevel().setBlock(STOVE, block.defaultBlockState(), 3);
                        var be = player.serverLevel().getBlockEntity(STOVE);
                        require(be instanceof MenuProvider, "Stove has no menu provider: " + be);
                        player.openMenu((MenuProvider) be, STOVE);
                    });
                    next(5, 40);
                }
                case 5 -> {
                    require(mc.player.containerMenu.getClass().getName().endsWith("StoveGuiHandler"), "Stove menu did not open");
                    transferLayout = layoutFor("hearth_horizons:baking/raisin_oatmeal_cookie");
                    require(transfer(transferLayout, false) == null, "Native stove transfer dry run failed");
                    require(transfer(transferLayout, true) == null, "Native stove transfer failed");
                    next(6, 30);
                }
                case 6 -> {
                    capture("jei-stove-transfer.png");
                    onServer(player -> {
                        var definition = PreparationItems.DEFINITIONS.getFirst();
                        var menu = player.containerMenu;
                        require(menu.getClass().getName().endsWith("StoveGuiHandler"), "Stove menu was closed before transfer");
                        require(menu.slots.subList(1, 4).stream().anyMatch(slot -> slot.getItem().is(definition.stack().getItem())), "Stove got no raw preparation");
                        require(menu.slots.subList(1, 4).stream().noneMatch(slot -> slot.getItem().is(cooked(definition).getItem())), "Stove received cooked food");
                        require(countInventory(player, cooked(definition)) == 7, "Stove transfer consumed cooked food");
                        for (int index = 1; index <= 3; index++) menu.getSlot(index).set(ItemStack.EMPTY);
                        player.getInventory().clearContent();
                        player.getInventory().add(cooked(definition).copyWithCount(7));
                        menu.broadcastChanges();
                    });
                    next(7, 30);
                }
                case 7 -> {
                    require(transfer(transferLayout, false) != null, "Stove transfer accepts cooked food instead of raw preparation");
                    System.out.println("JEI_QA_TRANSFER stove: raw transferred; cooked alternative rejected");
                    next(8, 0);
                }
                case 8 -> {
                    System.out.println("JEI_QA_COMPLETE search/list, R/U, slots, native transfers, six raw/cooked bookmarks " + (Boolean.getBoolean("hh.jeiQaReload") ? "after restart" : "saved"));
                    showExpandedStage("skillet");
                    next(10, 30);
                }
                case 9 -> mc.stop();
                case 10 -> { capture("jei-expanded-skillet.png"); showExpandedStage("pot"); next(11,30); }
                case 11 -> { capture("jei-expanded-pot.png"); showExpandedStage("dry"); next(12,30); }
                case 12 -> { capture("jei-expanded-drying.png"); showExpandedStage("ferment"); next(13,30); }
                case 13 -> { capture("jei-expanded-fermenting.png"); next(9,10); }
                default -> throw new IllegalStateException("Unexpected QA stage " + stage);
            }
        } catch (Throwable error) {
            failed = true;
            System.out.println("JEI_QA_FAILURE stage=" + stage);
            error.printStackTrace();
            capture("jei-failure.png");
            mc.stop();
        }
    }
    private static void next(int next, int ticks) { stage = next; delay = ticks; }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void onServer(Consumer<ServerPlayer> task) {
        var mc = Minecraft.getInstance(); var uuid = mc.player.getUUID();
        pending = new CompletableFuture<>();
        var completion = pending;
        mc.getSingleplayerServer().execute(() -> {
            try { task.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(uuid)); completion.complete(null); }
            catch (Throwable error) { completion.completeExceptionally(error); }
        });
    }
    private static int countInventory(ServerPlayer player, ItemStack item) {
        return player.getInventory().items.stream().filter(stack -> stack.is(item.getItem())).mapToInt(ItemStack::getCount).sum();
    }
    private static ItemStack cooked(PreparationItems.Definition definition) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("croptopia", definition.carrier())));
    }
    private static IFocus<ItemStack> focus(ItemStack stack, RecipeIngredientRole role) {
        return jei.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
    }
    private static String recipeId(Object recipe, IRecipeCategory<?> category) {
        if (recipe instanceof RecipeHolder<?> holder) return holder.id().toString();
        if(recipe.getClass().getName().equals("umpaz.brewinandchewin.integration.jei.KegFermentingPouringRecipe")) {
            try { return recipe.getClass().getMethod("getId").invoke(recipe).toString(); }
            catch(ReflectiveOperationException error) { throw new IllegalStateException(error); }
        }
        for (var holder : Minecraft.getInstance().level.getRecipeManager().getRecipes())
            if (holder.value() == recipe) return holder.id().toString();
        return "unmapped:" + category.getRecipeType().getUid() + ":" + recipe;
    }
    private static <R> Map<String, IRecipeLayoutDrawable<?>> recipes(IRecipeCategory<R> category, IFocus<?> focus) {
        var result = new LinkedHashMap<String, IRecipeLayoutDrawable<?>>();
        jei.getRecipeManager().createRecipeLookup(category.getRecipeType()).limitFocus(List.of(focus)).get().forEach(recipe -> {
            var layout = jei.getRecipeManager().createRecipeLayoutDrawable(category, recipe, jei.getJeiHelpers().getFocusFactory().createFocusGroup(List.of(focus))).orElseThrow();
            result.put(recipeId(recipe, category), layout);
        });
        return result;
    }
    private static Map<String, IRecipeLayoutDrawable<?>> recipes(IFocus<?> focus) {
        var result = new LinkedHashMap<String, IRecipeLayoutDrawable<?>>();
        jei.getRecipeManager().createRecipeCategoryLookup().limitFocus(List.of(focus)).get().forEach(category -> result.putAll(recipes(category, focus)));
        return result;
    }
    private static IRecipeLayoutDrawable<?> layoutFor(String id) {
        for (var definition : PreparationItems.DEFINITIONS) {
            var focus = focus(cooked(definition), RecipeIngredientRole.OUTPUT);
            for (var category : jei.getRecipeManager().createRecipeCategoryLookup().limitFocus(List.of(focus)).get().toList()) {
                var expectedType = "farm_and_charm:stove";
                if (!category.getRecipeType().getUid().toString().equals(expectedType)) continue;
                var map = recipes(category, focus);
                if (map.containsKey(id)) return map.get(id);
            }
        }
        throw new IllegalStateException("JEI layout missing: " + id);
    }
    private static <R> Object transfer(IRecipeLayoutDrawable<R> layout, boolean perform) {
        var mc = Minecraft.getInstance();
        var handler = jei.getRecipeTransferManager().getRecipeTransferHandler(mc.player.containerMenu, layout.getRecipeCategory())
            .orElseThrow(() -> new IllegalStateException("Missing native transfer handler: " + mc.player.containerMenu.getClass().getName() + " / " + layout.getRecipeCategory().getRecipeType().getUid()));
        return handler.transferRecipe(mc.player.containerMenu, layout.getRecipe(), layout.getRecipeSlotsView(), mc.player, false, perform);
    }
    private static <R> void showRecipe(IRecipeLayoutDrawable<R> layout) {
        jei.getRecipesGui().showRecipes(layout.getRecipeCategory(), List.of(layout.getRecipe()), List.of());
    }
    private static void testIngredientsAndFocuses() {
        var filter = jei.getIngredientFilter(); filter.setFilterText("");
        var all = filter.getFilteredItemStacks();
        for (var definition : PreparationItems.DEFINITIONS) {
            var raw = definition.stack(); var cooked = cooked(definition);
            require(all.stream().filter(stack -> stack.is(raw.getItem())).count() == 1, "Raw JEI list entry missing or duplicated");
            require(all.stream().filter(stack -> stack.is(cooked.getItem())).count() == 1, "Cooked JEI list entry missing or duplicated");
            require(!raw.has(DataComponents.FOOD), "Raw ingredient has FOOD");
            filter.setFilterText(raw.getHoverName().getString());
            require(filter.getFilteredItemStacks().stream().anyMatch(stack -> stack.is(raw.getItem())), "Localized raw search failed: " + raw.getHoverName().getString());
            filter.setFilterText(cooked.getHoverName().getString());
            require(filter.getFilteredItemStacks().stream().anyMatch(stack -> stack.is(cooked.getItem())), "Localized cooked search failed");
            var creation = recipes(focus(raw, RecipeIngredientRole.OUTPUT));
            var uses = recipes(focus(raw, RecipeIngredientRole.INPUT));
            var readyCreation = recipes(focus(cooked, RecipeIngredientRole.OUTPUT));
            var readyUses = recipes(focus(cooked, RecipeIngredientRole.INPUT));
            var preparationId = "croptopia:" + definition.recipe();
            var migrationId = "hearth_horizons:preparation_migration/" + definition.recipe();
            var cookingId = "hearth_horizons:" + (definition.recipe().equals("raisin_oatmeal_cookie") ? "baking/" : "frying/") + definition.recipe();
            require(creation.containsKey(preparationId) && !creation.containsKey(migrationId), "Raw R has wrong stages: " + creation.keySet());
            require(uses.containsKey(cookingId) && !uses.containsKey(migrationId), "Raw U has wrong stages: " + uses.keySet());
            require(readyCreation.containsKey(cookingId) && !readyCreation.containsKey(preparationId) && !readyCreation.containsKey(migrationId), "Cooked R has wrong stages: " + readyCreation.keySet());
            require(!readyUses.containsKey(cookingId) && !readyUses.containsKey(migrationId), "Cooked U includes raw stages");
            for (var slot : uses.get(cookingId).getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT))
                slot.getItemStacks().forEach(stack -> require(!stack.has(DataComponents.FOOD), "JEI raw recipe slot has FOOD"));
            System.out.println("JEI_QA_FOCUS " + definition.recipe() + " rawR=" + creation.keySet() + " rawU=" + uses.keySet() + " cookedR=" + readyCreation.keySet() + " cookedU=" + readyUses.keySet());
        }
        filter.setFilterText("");
        System.out.println("JEI_QA_INGREDIENTS six distinct raw/cooked entries; Russian name search; raw slots have no FOOD");
    }
    private static IRecipeLayoutDrawable<?> expandedLayout(CookingPreparations.Definition definition) {
        var raw=new ItemStack(definition.item().get());
        String expected="hearth_horizons_compat:cooking/"+definition.recipe().replace(':','/');
        var uses=recipes(focus(raw,RecipeIngredientRole.INPUT));
        require(uses.containsKey(expected),"Expanded stage absent from raw U: "+expected+" / "+uses.keySet());
        return uses.get(expected);
    }
    private static void showExpandedStage(String stage) {
        var definition=CookingPreparations.ALL.stream().filter(d -> d.stage().equals(stage)).findFirst().orElseThrow();
        showRecipe(expandedLayout(definition));
    }
    private static void testExpandedStages() {
        var filter=jei.getIngredientFilter();filter.setFilterText("");
        var all=filter.getFilteredItemStacks();
        var stages=new HashSet<String>();
        for(var definition:CookingPreparations.ALL) {
            var raw=new ItemStack(definition.item().get());
            require(all.stream().filter(s -> s.is(raw.getItem())).count()==1,"Expanded JEI entry missing/duplicated: "+definition.recipe());
            require(!raw.has(DataComponents.FOOD),"Expanded preparation is edible");
            var creation=recipes(focus(raw,RecipeIngredientRole.OUTPUT));
            require(creation.containsKey(definition.recipe()),"Expanded raw R lacks source preparation: "+definition.recipe()+" / "+creation.keySet());
            var layout=expandedLayout(definition);
            for(var slot:layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT))
                slot.getItemStacks().forEach(s -> require(!s.has(DataComponents.FOOD),"Expanded raw stage input has FOOD"));
            if(stages.add(definition.stage())) {
                filter.setFilterText(raw.getHoverName().getString());
                require(filter.getFilteredItemStacks().stream().anyMatch(s -> s.is(raw.getItem())),"Expanded localized search failed: "+raw.getHoverName());
                var ready=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(definition.result())));
                var readyR=recipes(focus(ready,RecipeIngredientRole.OUTPUT));
                String cookingId="hearth_horizons_compat:cooking/"+definition.recipe().replace(':','/');
                require(readyR.containsKey(cookingId) && !readyR.containsKey(definition.recipe()),"Expanded cooked R has wrong stages: "+definition.recipe());
            }
        }
        filter.setFilterText("");
        System.out.println("JEI_QA_EXPANDED "+CookingPreparations.ALL.size()+" distinct non-food raw entries; all R/U and stage slots; localized search and cooked R across "+stages);
    }
    private static Object field(Object object, String name) throws ReflectiveOperationException {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static void testBookmarks(boolean reload) throws ReflectiveOperationException {
        var list = (BookmarkList) field(jei.getBookmarkOverlay(), "bookmarkList");
        var factory = (BookmarkFactory) field(list, "bookmarkFactory");
        if (!reload) list.setFromConfigFile(List.of());
        var expected = new ArrayList<ItemStack>();
        for (var definition : PreparationItems.DEFINITIONS) {
            expected.add(definition.stack()); expected.add(cooked(definition));
        }
        for (var stack : expected) {
            var typed = jei.getIngredientManager().createTypedIngredient(VanillaTypes.ITEM_STACK, stack, false).orElseThrow();
            var bookmark = factory.create(typed);
            if (reload) require(list.contains(bookmark), "Bookmark lost after restart: " + stack);
            else list.add(bookmark);
        }
        require(list.getElements().size() == 6, "Raw/cooked bookmarks collapsed or duplicated");
        System.out.println("JEI_QA_BOOKMARKS 6 distinct raw/cooked variants " + (reload ? "restored after process restart" : "saved through native JEI bookmark config"));
    }
    private static void capture(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> System.out.println("JEI_QA_SCREENSHOT " + message.getString()));
    }
}
