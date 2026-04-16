package com.otectus.runic_races.presentation;

import com.otectus.runic_races.network.NetworkHandler;
import com.otectus.runic_races.network.S2CScreenCuePacket;
import com.otectus.runic_races.presentation.SignatureEntry.SfxSpec;
import com.otectus.runic_races.presentation.SignatureEntry.VfxSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Single entry point for "signature moment" presentation. Callers pick a
 * {@link SignatureKey} and this helper plays the matching sounds, particles,
 * actionbar banner and optional screen cue in one shot.
 *
 * Server-side only. Banners/screen cues are delivered per-player; sfx/vfx are
 * broadcast at the player's position so nearby players see and hear them too.
 */
public final class RunicPresentation {

    private RunicPresentation() {}

    /** Fires the full presentation bundle at the given player. */
    public static void fire(ServerPlayer player, SignatureKey key, Object... bannerArgs) {
        SignatureEntry entry = SignatureRegistry.get(key);
        if (entry == null || !(player.level() instanceof ServerLevel level)) return;

        playSignatureSfx(level, player.position(), entry);
        spawnSignatureVfx(level, player.position(), entry);
        showRunicBanner(player, entry, bannerArgs);
        if (entry.screenCue() != null) {
            showScreenCue(player, entry.screenCue(), entry.screenCueDurationTicks());
        }
    }

    public static void playSignatureSfx(ServerLevel level, Vec3 pos, SignatureEntry entry) {
        for (SfxSpec spec : entry.sounds()) {
            level.playSound(null, pos.x, pos.y, pos.z, spec.sound(), SoundSource.PLAYERS, spec.volume(), spec.pitch());
        }
    }

    public static void spawnSignatureVfx(ServerLevel level, Vec3 pos, SignatureEntry entry) {
        double x = pos.x;
        double y = pos.y + 0.3;
        double z = pos.z;
        for (VfxSpec spec : entry.particles()) {
            level.sendParticles(spec.particle(), x, y, z, spec.count(),
                    spec.spreadX(), spec.spreadY(), spec.spreadZ(), spec.speed());
        }
    }

    public static void showRunicBanner(ServerPlayer player, SignatureEntry entry, Object... args) {
        String text = args.length == 0 ? entry.bannerText() : String.format(entry.bannerText(), args);
        MutableComponent component = Component.literal(text).withStyle(entry.bannerColor());
        if (entry.bannerBold()) {
            component = component.withStyle(ChatFormatting.BOLD);
        }
        player.displayClientMessage(component, true);
    }

    public static void showScreenCue(ServerPlayer player, CueType cue, int durationTicks) {
        NetworkHandler.sendToPlayer(player, new S2CScreenCuePacket(cue, durationTicks));
    }
}
