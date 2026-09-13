package com.oscarways.arcanumtweaks;

import java.util.LinkedHashMap;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Startup config: config/arcanum_tweaks-startup.toml
 *
 * It is a STARTUP config because the values are baked into the items while the game boots,
 * before any world loads. Because of that it is NOT synced from the server: the server and every
 * client must use the same file.
 *
 * Every default is the original Arcanum 10.2.1 value, so an untouched file changes nothing.
 */
public final class Config {
    private Config() {}

    /** Values for one material tier. durability = 0 means unbreakable. */
    public record Tier(
            ModConfigSpec.DoubleValue sword,
            ModConfigSpec.DoubleValue axe,
            ModConfigSpec.DoubleValue pickaxe,
            ModConfigSpec.DoubleValue shovel,
            ModConfigSpec.DoubleValue hoe,
            ModConfigSpec.DoubleValue beatingStick,
            ModConfigSpec.DoubleValue miningSpeed,
            ModConfigSpec.IntValue durability,
            ModConfigSpec.DoubleValue armorToughness) {}

    /** Arcanum item id prefix -> values. Order matters: "infernaldiamond" must come before "infernal". */
    public static final Map<String, Tier> TIERS = new LinkedHashMap<>();
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Arcanum Tweaks - weapon, tool and armor values for Arcanum.",
                "Damage is the value shown in-game (it includes the player's base 1 damage).",
                "All defaults are the original Arcanum values: leave them as-is and nothing changes.",
                "IMPORTANT: this file is not synced. The server and every client must have the same file.");

        tier(b, "green_sapphire", "greensapphire", 12, 17, 12, 12.5, 9, 12, 10, 1500, 20);
        tier(b, "blood_diamond", "blooddiamond", 52, 57, 52, 52.5, 49, 52, 20, 3000, 40);
        tier(b, "void_diamond", "voiddiamond", 77, 82, 77, 77.5, 74, 77, 50, 6000, 60);
        tier(b, "infernal_diamond", "infernaldiamond", 102, 107, 102, 102.5, 99, 102, 100, 8000, 80);
        tier(b, "infernal", "infernal", 100002, 100007, 100002, 100002.5, 99999, 100002, 500, 0, 9999);

        SPEC = b.build();
    }

    private static void tier(ModConfigSpec.Builder b, String section, String prefix,
            double sword, double axe, double pickaxe, double shovel, double hoe, double beatingStick,
            double miningSpeed, int durability, double armorToughness) {
        double max = 1_000_000;
        b.push(section);
        Tier t = new Tier(
                b.defineInRange("sword_damage", sword, 1, max),
                b.defineInRange("axe_damage", axe, 1, max),
                b.defineInRange("pickaxe_damage", pickaxe, 1, max),
                b.defineInRange("shovel_damage", shovel, 1, max),
                b.defineInRange("hoe_damage", hoe, 1, max),
                b.comment("Beating Stick of this tier (Green Sapphire uses the Sapphire Beating Stick). Always unbreakable.")
                        .defineInRange("beating_stick_damage", beatingStick, 1, max),
                b.defineInRange("mining_speed", miningSpeed, 0.1, max),
                b.comment("0 = unbreakable. Applies to tools, swords and shears.")
                        .defineInRange("durability", durability, 0, Integer.MAX_VALUE),
                b.defineInRange("armor_toughness", armorToughness, 0, max));
        b.pop();
        TIERS.put(prefix, t);
    }
}
