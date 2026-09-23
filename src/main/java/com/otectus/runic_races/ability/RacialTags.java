package com.otectus.runic_races.ability;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class RacialTags {
    private RacialTags() {}
    private static ResourceLocation id(String name) { return new ResourceLocation("runic_races", name); }
    public static final TagKey<Biome> GROVE = TagKey.create(Registries.BIOME, id("grove_homes"));
    public static final TagKey<Biome> COLD = TagKey.create(Registries.BIOME, id("cold_environments"));
    public static final TagKey<Block> STONE = TagKey.create(Registries.BLOCK, id("stone_support"));
    public static final TagKey<Block> QUARRY = TagKey.create(Registries.BLOCK, id("quarry_eligible"));
    public static final TagKey<Block> UNDERBRUSH = TagKey.create(Registries.BLOCK, id("grove_underbrush"));
    public static final TagKey<Item> MUSHROOM_FOOD = TagKey.create(Registries.ITEM, id("mushroom_nourishment"));
    public static final TagKey<EntityType<?>> CANNOT_BLEED = TagKey.create(Registries.ENTITY_TYPE, id("cannot_bleed"));
    public static final TagKey<EntityType<?>> CONTROL_RESISTANT = TagKey.create(Registries.ENTITY_TYPE, id("racial_control_resistant"));
    public static final TagKey<DamageType> MAGIC = TagKey.create(Registries.DAMAGE_TYPE, id("racial_magic"));
    public static final TagKey<DamageType> PHYSICAL = TagKey.create(Registries.DAMAGE_TYPE, id("racial_physical"));
}
