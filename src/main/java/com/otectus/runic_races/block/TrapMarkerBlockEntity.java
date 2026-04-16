package com.otectus.runic_races.block;

import com.otectus.runic_races.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Persistent state for a placed Kobold trap: the owner UUID (trap ignores its
 * own caster) and an absolute {@code expiresAt} game-time after which the trap
 * auto-removes itself — prevents abandoned traps from accumulating forever.
 *
 * Ticked once per second via {@code TrapMarkerBlock#getTicker}.
 */
public class TrapMarkerBlockEntity extends BlockEntity {

    private static final String OWNER_KEY = "owner";
    private static final String EXPIRES_KEY = "expiresAt";
    public static final int DEFAULT_DURATION_TICKS = 6000; // 5 minutes

    @Nullable
    private UUID ownerUuid;
    private long expiresAt;

    public TrapMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAP_MARKER.get(), pos, state);
    }

    public void setOwner(UUID owner, long expiresAt) {
        this.ownerUuid = owner;
        this.expiresAt = expiresAt;
        setChanged();
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void serverTick(ServerLevel level, BlockPos pos) {
        if (expiresAt <= 0) return;
        if ((level.getGameTime() & 19L) != 0) return; // check ~once per second
        if (level.getGameTime() >= expiresAt) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerUuid != null) tag.putUUID(OWNER_KEY, ownerUuid);
        tag.putLong(EXPIRES_KEY, expiresAt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID(OWNER_KEY)) ownerUuid = tag.getUUID(OWNER_KEY);
        expiresAt = tag.getLong(EXPIRES_KEY);
    }
}
