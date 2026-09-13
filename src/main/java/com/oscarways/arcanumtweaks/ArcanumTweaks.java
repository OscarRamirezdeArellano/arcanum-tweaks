package com.oscarways.arcanumtweaks;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.oscarways.arcanumtweaks.data.ArcanumData;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Unofficial Arcanum addon. Changes Arcanum's item values from outside its jar, using
 * config/arcanum_tweaks-startup.toml. Defaults are the original values, so it changes nothing
 * unless configured. The JEI pages live in the jei package and only load when JEI is present.
 */
@Mod(ArcanumTweaks.MODID)
public class ArcanumTweaks {
    public static final String MODID = "arcanum_tweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcanumTweaks(IEventBus modBus, ModContainer container) {
        // STARTUP configs load right here, before item components are built.
        container.registerConfig(ModConfig.Type.STARTUP, Config.SPEC);
        // LOWEST: apply after any other mod that touches these items.
        modBus.addListener(EventPriority.LOWEST, ModifyDefaultComponentsEvent.class, ArcanumTweaks::adjust);
        modBus.addListener(FMLCommonSetupEvent.class, e -> ArcanumData.logSummary());
    }

    private static void adjust(ModifyDefaultComponentsEvent event) {
        int scheduled = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!"arcanum".equals(id.getNamespace())) continue;

            String path = id.getPath().equals("sapphirebeatingstick") ? "greensapphirebeatingstick" : id.getPath();
            Map.Entry<String, Config.Tier> entry = Config.TIERS.entrySet().stream()
                    .filter(e -> path.startsWith(e.getKey())).findFirst().orElse(null);
            if (entry == null) continue;
            Config.Tier tier = entry.getValue();
            String type = path.substring(entry.getKey().length());

            Double damage = switch (type) {
                case "sword" -> tier.sword().get();
                case "axe" -> tier.axe().get();
                case "pickaxe" -> tier.pickaxe().get();
                case "shovel" -> tier.shovel().get();
                case "hoe" -> tier.hoe().get();
                case "beatingstick" -> tier.beatingStick().get();
                default -> null;
            };
            boolean isTool = List.of("pickaxe", "axe", "shovel", "hoe").contains(type);
            boolean isArmor = List.of("helmet", "chestplate", "leggings", "boots").contains(type);
            // Beating Sticks are unbreakable in Arcanum; their durability is left alone.
            boolean hasDurability = !isArmor && !type.equals("beatingstick")
                    && (damage != null || isTool || type.equals("shears") || type.equals("wand"));
            if (damage == null && !isTool && !isArmor && !hasDurability) continue;

            double speed = tier.miningSpeed().get();
            int durability = tier.durability().get();
            double toughness = tier.armorToughness().get();
            event.modify(item, (c, context, it) -> apply(id, c, damage, isTool ? speed : null,
                    hasDurability ? durability : null, isArmor ? toughness : null));
            scheduled++;
        }
        LOGGER.info("[Arcanum Tweaks] {} Arcanum items checked against config/arcanum_tweaks-startup.toml", scheduled);
    }

    private static void apply(Identifier id, DataComponentMap.Builder c, Double damage, Double speed,
            Integer durability, Double toughness) {
        StringBuilder changes = new StringBuilder();

        ItemAttributeModifiers attributes = c.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attributes != null && (damage != null || toughness != null)) {
            ItemAttributeModifiers.Builder updated = ItemAttributeModifiers.builder();
            for (ItemAttributeModifiers.Entry e : attributes.modifiers()) {
                AttributeModifier m = e.modifier();
                if (damage != null && e.matches(Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID)) {
                    m = change(changes, "damage", m, damage - 1, 1);
                } else if (toughness != null && e.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
                    m = change(changes, "toughness", m, toughness, 0);
                }
                updated.add(e.attribute(), m, e.slot(), e.display());
            }
            c.set(DataComponents.ATTRIBUTE_MODIFIERS, updated.build());
        }

        if (speed != null) {
            Tool tool = c.get(DataComponents.TOOL);
            if (tool != null) {
                // Only the main rule (mines and drops) carries the material speed.
                List<Tool.Rule> rules = tool.rules().stream().map(r -> {
                    if (r.speed().isEmpty() || !r.correctForDrops().orElse(false)) return r;
                    if (r.speed().get() != speed.floatValue()) {
                        changes.append(" mining_speed ").append(r.speed().get()).append("->").append(speed);
                    }
                    return new Tool.Rule(r.blocks(), Optional.of(speed.floatValue()), r.correctForDrops());
                }).toList();
                c.set(DataComponents.TOOL, new Tool(rules, tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
            }
        }

        if (durability != null) {
            boolean wasUnbreakable = c.get(DataComponents.UNBREAKABLE) != null || c.get(DataComponents.MAX_DAMAGE) == null;
            Integer currentMax = c.get(DataComponents.MAX_DAMAGE);
            if (durability == 0) {
                if (!wasUnbreakable) changes.append(" durability ").append(currentMax).append("->unbreakable");
                c.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            } else if (wasUnbreakable || !durability.equals(currentMax)) {
                changes.append(" durability ").append(wasUnbreakable ? "unbreakable" : currentMax).append("->").append(durability);
                c.set(DataComponents.UNBREAKABLE, null);
                c.set(DataComponents.MAX_DAMAGE, durability);
                if (c.get(DataComponents.DAMAGE) == null) c.set(DataComponents.DAMAGE, 0);
            }
        }

        if (!changes.isEmpty()) {
            LOGGER.info("[Arcanum Tweaks] {}:{}", id, changes);
        }
    }

    private static AttributeModifier change(StringBuilder changes, String name, AttributeModifier m, double value, double shownOffset) {
        if (Math.abs(m.amount() - value) > 1e-6) {
            changes.append(' ').append(name).append(' ').append(m.amount() + shownOffset).append("->").append(value + shownOffset);
        }
        return new AttributeModifier(m.id(), value, m.operation());
    }
}
