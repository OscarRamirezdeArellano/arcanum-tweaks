package com.oscarways.arcanumtweaks.data;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oscarways.arcanumtweaks.ArcanumTweaks;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * Arcanum data shown in JEI.
 *
 * Upgrade and fermenting recipes are read at runtime from the installed Arcanum jar, so they
 * follow Arcanum updates and nothing from Arcanum is bundled here.
 *
 * Firebolt transmutations and binding rituals are not data files in Arcanum (they are behavior of
 * its Spellbook), so they are described below as a fixed table.
 */
public final class ArcanumData {
    private ArcanumData() {}

    /** Upgrade recipe. In the grid, key 'Z' is the previous-tier item, which keeps its enchantments. */
    public record Upgrade(String result, String[] pattern, Map<Character, String> key) {}

    public record Fermenting(String base, String ingredient1, String ingredient2, String ingredient3, String result) {}

    /** block = true: cast Firebolt at a placed block. false: at the item lying on the ground. */
    public record Transmutation(String source, String target, boolean block) {}

    /** Hold the book, look at the binding on the ground, cast the binding. dangerous = night + mob waves. */
    public record Ritual(String book, String binding, String result, int seconds, int mana, boolean dangerous, String originKey) {}

    private static List<Upgrade> upgrades;
    private static List<Fermenting> fermenting;

    public static List<Upgrade> upgrades() {
        load();
        return upgrades;
    }

    public static List<Fermenting> fermenting() {
        load();
        return fermenting;
    }

    public static final List<Transmutation> TRANSMUTATIONS = buildTransmutations();

    public static final List<Ritual> RITUALS = List.of(
            new Ritual("arcanum:spellbook", "arcanum:codex_binding", "arcanum:arcane_codex", 5, 50, false,
                    "arcanum_tweaks.jei.ritual.origin.codex"),
            new Ritual("arcanum:arcane_codex", "arcanum:grimoire_binding", "arcanum:forbidden_grimoire", 60, 50, true,
                    "arcanum_tweaks.jei.ritual.origin.grimoire"));

    public static void logSummary() {
        ArcanumTweaks.LOGGER.info("[Arcanum Tweaks] Read from Arcanum: {} upgrade recipes, {} fermenting recipes",
                upgrades().size(), fermenting().size());
    }

    private static List<Transmutation> buildTransmutations() {
        List<Transmutation> list = new ArrayList<>();
        String[][] blocks = {{"voiddiamondblock", "infernaldiamond"}, {"voiddiamondfurnace", "infernalfurnace"},
                {"voiddiamondgenerator", "infernalgenerator"}};
        for (String[] b : blocks) list.add(new Transmutation("arcanum:" + b[0], "arcanum:" + b[1], true));
        // The same blocks also transmute as items on the ground.
        for (String[] b : blocks) list.add(new Transmutation("arcanum:" + b[0], "arcanum:" + b[1], false));
        String[] gear = {"pickaxe", "axe", "hoe", "shovel", "shears", "sword", "helmet", "chestplate", "leggings", "boots"};
        for (String g : gear) list.add(new Transmutation("arcanum:voiddiamond" + g, "arcanum:infernaldiamond" + g, false));
        for (String g : gear) list.add(new Transmutation("arcanum:infernaldiamond" + g, "arcanum:infernal" + g, false));
        list.add(new Transmutation("arcanum:infernalbeatingstick", "arcanum:infernalwand", false));
        return List.copyOf(list);
    }

    private static synchronized void load() {
        if (upgrades != null) return;
        List<Upgrade> ups = new ArrayList<>();
        List<Fermenting> ferm = new ArrayList<>();
        IModFileInfo arcanum = ModList.get().getModFileById("arcanum");
        if (arcanum == null) {
            upgrades = List.of();
            fermenting = List.of();
            return;
        }
        arcanum.getFile().getContents().visitContent("data/arcanum/recipe", (path, resource) -> {
            if (!path.endsWith(".json")) return;
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String type = json.has("type") ? json.get("type").getAsString() : "";
                switch (type) {
                    case "arcanum:upgrade_copy" -> ups.add(parseUpgrade(json));
                    case "arcanum:fermenting" -> ferm.add(new Fermenting(ingredient(json.get("base")),
                            ingredient(json.get("ingredient1")), ingredient(json.get("ingredient2")),
                            ingredient(json.get("ingredient3")), result(json)));
                    default -> { }
                }
            } catch (Exception e) {
                ArcanumTweaks.LOGGER.warn("[Arcanum Tweaks] Could not read Arcanum recipe {}: {}", path, e.toString());
            }
        });
        upgrades = List.copyOf(ups);
        fermenting = List.copyOf(ferm);
    }

    private static Upgrade parseUpgrade(JsonObject json) {
        var patternJson = json.getAsJsonArray("pattern");
        String[] pattern = new String[patternJson.size()];
        for (int i = 0; i < pattern.length; i++) pattern[i] = patternJson.get(i).getAsString();
        Map<Character, String> key = new HashMap<>();
        for (var e : json.getAsJsonObject("key").entrySet()) key.put(e.getKey().charAt(0), ingredient(e.getValue()));
        return new Upgrade(result(json), pattern, Map.copyOf(key));
    }

    /** Item id, or "#namespace:tag" for tags. Lists use their first entry. */
    private static String ingredient(JsonElement e) {
        if (e.isJsonArray()) return ingredient(e.getAsJsonArray().get(0));
        if (e.isJsonPrimitive()) return e.getAsString();
        JsonObject o = e.getAsJsonObject();
        if (o.has("item")) return o.get("item").getAsString();
        if (o.has("id")) return o.get("id").getAsString();
        return "#" + o.get("tag").getAsString();
    }

    private static String result(JsonObject json) {
        JsonElement r = json.get("result");
        return r.isJsonPrimitive() ? r.getAsString() : r.getAsJsonObject().get("id").getAsString();
    }
}
