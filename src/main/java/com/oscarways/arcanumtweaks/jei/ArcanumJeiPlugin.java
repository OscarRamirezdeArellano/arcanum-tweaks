package com.oscarways.arcanumtweaks.jei;

import java.util.List;
import java.util.Optional;

import com.oscarways.arcanumtweaks.ArcanumTweaks;
import com.oscarways.arcanumtweaks.data.ArcanumData;
import com.oscarways.arcanumtweaks.data.ArcanumData.Fermenting;
import com.oscarways.arcanumtweaks.data.ArcanumData.Ritual;
import com.oscarways.arcanumtweaks.data.ArcanumData.Transmutation;
import com.oscarways.arcanumtweaks.data.ArcanumData.Upgrade;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** JEI pages for what Arcanum does not register with JEI. Loaded only when JEI is installed. */
@JeiPlugin
public class ArcanumJeiPlugin implements IModPlugin {
    private static final int SLOT = 18;
    private static final int GREY = 0xFF606060;

    static final IRecipeType<Upgrade> UPGRADES = IRecipeType.create(ArcanumTweaks.MODID, "upgrades", Upgrade.class);
    static final IRecipeType<Fermenting> FERMENTER = IRecipeType.create(ArcanumTweaks.MODID, "fermenter", Fermenting.class);
    static final IRecipeType<Transmutation> FIREBOLT = IRecipeType.create(ArcanumTweaks.MODID, "firebolt", Transmutation.class);
    static final IRecipeType<Ritual> RITUALS = IRecipeType.create(ArcanumTweaks.MODID, "rituals", Ritual.class);

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(ArcanumTweaks.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new UpgradesCategory(gui), new FermenterCategory(gui),
                new FireboltCategory(gui), new RitualsCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Only recipes whose items exist: if Arcanum renames something, that entry is skipped.
        registration.addRecipes(UPGRADES, ArcanumData.upgrades().stream()
                .filter(u -> exists(u.result()) && u.key().values().stream().allMatch(ArcanumJeiPlugin::exists)).toList());
        registration.addRecipes(FERMENTER, ArcanumData.fermenting().stream()
                .filter(f -> List.of(f.base(), f.ingredient1(), f.ingredient2(), f.ingredient3(), f.result()).stream()
                        .allMatch(ArcanumJeiPlugin::exists)).toList());
        registration.addRecipes(FIREBOLT, ArcanumData.TRANSMUTATIONS.stream()
                .filter(t -> exists(t.source()) && exists(t.target())).toList());
        registration.addRecipes(RITUALS, ArcanumData.RITUALS.stream()
                .filter(r -> exists(r.book()) && exists(r.binding()) && exists(r.result())).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(UPGRADES, Items.CRAFTING_TABLE);
        item("arcanum:fermenter").ifPresent(i -> registration.addCraftingStation(FERMENTER, i));
        item("arcanum:spellbook").ifPresent(i -> {
            registration.addCraftingStation(FIREBOLT, i);
            registration.addCraftingStation(RITUALS, i);
        });
    }

    // ---------- helpers ----------

    private static Optional<Item> item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id));
    }

    private static boolean exists(String id) {
        return id.startsWith("#") || item(id).isPresent();
    }

    private static Ingredient ingredient(String id) {
        if (id.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1)));
            return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(tag));
        }
        return Ingredient.of(item(id).orElseThrow());
    }

    private static ItemStack stack(String id) {
        return new ItemStack(item(id).orElseThrow());
    }

    private static IDrawable icon(IGuiHelper gui, String id, Item fallback) {
        return gui.createDrawableItemLike(item(id).orElse(fallback));
    }

    private static Component t(String key, Object... args) {
        return Component.translatable("arcanum_tweaks.jei." + key, args);
    }

    // ---------- categories ----------

    /** 3x3 grid -> arrow -> result. */
    private static final class UpgradesCategory extends AbstractRecipeCategory<Upgrade> {
        UpgradesCategory(IGuiHelper gui) {
            super(UPGRADES, t("upgrades"), icon(gui, "arcanum:blooddiamondpickaxe", Items.DIAMOND_PICKAXE), 116, 64);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Upgrade recipe, IFocusGroup focuses) {
            String[] pattern = recipe.pattern();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    var slot = builder.addInputSlot(col * SLOT, row * SLOT).setStandardSlotBackground();
                    if (row >= pattern.length || col >= pattern[row].length()) continue;
                    char c = pattern[row].charAt(col);
                    String id = recipe.key().get(c);
                    if (id == null) continue;
                    slot.add(ingredient(id));
                    if (c == 'Z') {
                        slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(t("upgrades.previous_item")));
                    }
                }
            }
            builder.addOutputSlot(94, 18).setOutputSlotBackground().add(stack(recipe.result()));
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder extras, Upgrade recipe, IFocusGroup focuses) {
            extras.addRecipeArrowWidget().setPosition(62, 19);
            extras.addText(t("upgrades.keeps_enchantments"), 116, 10).setPosition(0, 56).setColor(GREY);
        }
    }

    /** Base + 3 ingredients -> result. */
    private static final class FermenterCategory extends AbstractRecipeCategory<Fermenting> {
        FermenterCategory(IGuiHelper gui) {
            super(FERMENTER, t("fermenter"), icon(gui, "arcanum:fermenter", Items.BARREL), 120, 40);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Fermenting f, IFocusGroup focuses) {
            builder.addInputSlot(0, 11).setStandardSlotBackground().add(stack(f.base()))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(t("fermenter.base")));
            builder.addInputSlot(24, 0).setStandardSlotBackground().add(stack(f.ingredient1()));
            builder.addInputSlot(42, 0).setStandardSlotBackground().add(stack(f.ingredient2()));
            builder.addInputSlot(60, 0).setStandardSlotBackground().add(stack(f.ingredient3()));
            builder.addOutputSlot(102, 11).setOutputSlotBackground().add(stack(f.result()));
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder extras, Fermenting f, IFocusGroup focuses) {
            extras.addRecipeArrowWidget().setPosition(78, 12);
        }
    }

    /** Source -> Firebolt -> target. */
    private static final class FireboltCategory extends AbstractRecipeCategory<Transmutation> {
        FireboltCategory(IGuiHelper gui) {
            super(FIREBOLT, t("firebolt"), icon(gui, "arcanum:spellbook", Items.BOOK), 140, 36);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Transmutation tr, IFocusGroup focuses) {
            builder.addInputSlot(0, 0).setStandardSlotBackground().add(stack(tr.source()));
            builder.addOutputSlot(60, 0).setOutputSlotBackground().add(stack(tr.target()));
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder extras, Transmutation tr, IFocusGroup focuses) {
            extras.addRecipeArrowWidget().setPosition(26, 1);
            extras.addText(t(tr.block() ? "firebolt.placed_block" : "firebolt.dropped_item"), 140, 10).setPosition(0, 24).setColor(GREY);
        }
    }

    /** Book in hand + binding on the ground -> higher book, with time, mana and warnings. */
    private static final class RitualsCategory extends AbstractRecipeCategory<Ritual> {
        private static final int WIDTH = 160;

        RitualsCategory(IGuiHelper gui) {
            super(RITUALS, t("rituals"), icon(gui, "arcanum:arcane_codex", Items.BOOK), WIDTH, 78);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Ritual r, IFocusGroup focuses) {
            builder.addInputSlot(0, 0).setStandardSlotBackground().add(stack(r.book()))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(t("ritual.in_hand")));
            builder.addInputSlot(36, 0).setStandardSlotBackground().add(stack(r.binding()))
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(t("ritual.on_ground"));
                        tooltip.add(t("ritual.found_in", Component.translatable(r.originKey())));
                    });
            builder.addOutputSlot(90, 0).setOutputSlotBackground().add(stack(r.result()));
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder extras, Ritual r, IFocusGroup focuses) {
            extras.addRecipePlusSignWidget().setPosition(21, 3);
            extras.addRecipeArrowWidget().setPosition(60, 1);
            extras.addText(t("ritual.steps", r.seconds(), r.mana()), WIDTH, 30).setPosition(0, 22).setColor(GREY);
            if (r.dangerous()) {
                extras.addText(t("ritual.danger"), WIDTH, 26).setPosition(0, 52).setColor(0xFFAA2222);
            }
        }
    }
}
