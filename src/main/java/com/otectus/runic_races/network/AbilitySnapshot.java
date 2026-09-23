package com.otectus.runic_races.network;

import com.otectus.runic_races.ability.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Small owner-only state packet; never synchronizes the full Apoli container on cooldown ticks. */
public record AbilitySnapshot(String race, int cooldown, int maximum, int activeTicks, int charges,
                              float guard, String state, int targetId, boolean dry, boolean cold, boolean sunlight) {
    public static AbilitySnapshot of(ServerPlayer p, AbilityService.Session s) {
        AbilityTuning t = s.tuning;
        if (t == null) return new AbilitySnapshot("", 0, 0, 0, 0, 0, "", -1, false, false, false);
        boolean active = s.live(AbilityService.now(p));
        WardLedger ward = AbilityService.existingWards(p);
        float guard = ward == null ? 0 : java.util.Arrays.stream(WardLedger.Kind.values()).map(ward::remaining).max(Float::compare).orElse(0f);
        String state = !active ? (guard > 0 ? "ward" : "") : s.prepared ? "prepared" : s.anchor != null ? "anchor"
                : s.field != null ? "respite" : switch(t.kind()) {
                    case CHELON -> "shell"; case NIGHTBORN -> "hunt"; case RETURNED -> "pursuit";
                    case WAILER -> "windup"; case MOUNTAIN_ONE -> "quarry"; case CRYSTAL_ONE -> "counter";
                    case TIDE_ELF, BOVINE, ZEPHYR, WYVERNKIN -> "moving"; default -> guard > 0 ? "ward" : "";
                };
        var target = AbilityService.entity(p, s.target);
        return new AbilitySnapshot(t.kind().race(), s.cooldowns.getOrDefault(t.kind(), 0), t.cooldownTicks(),
                (int)Math.max(0, s.until - AbilityService.now(p)), s.charges, guard, state,
                target == null ? -1 : target.getId(), s.dry, s.cold, s.inSun);
    }
    public static void encode(AbilitySnapshot p, FriendlyByteBuf b) {
        b.writeUtf(p.race, 32); b.writeVarInt(p.cooldown); b.writeVarInt(p.maximum); b.writeVarInt(p.activeTicks);
        b.writeVarInt(p.charges); b.writeFloat(p.guard); b.writeUtf(p.state, 24); b.writeInt(p.targetId);
        b.writeBoolean(p.dry); b.writeBoolean(p.cold); b.writeBoolean(p.sunlight);
    }
    public static AbilitySnapshot decode(FriendlyByteBuf b) {
        return new AbilitySnapshot(b.readUtf(32), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readUtf(24), b.readInt(), b.readBoolean(), b.readBoolean(), b.readBoolean());
    }
    public static void handle(AbilitySnapshot p, Supplier<NetworkEvent.Context> c) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.receive(p)); c.get().setPacketHandled(true);
    }
    private static final class Client {
        static void receive(AbilitySnapshot p) { com.otectus.runic_races.client.ExpansionClient.receive(p); }
    }
}
