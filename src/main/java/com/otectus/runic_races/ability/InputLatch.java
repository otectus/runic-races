package com.otectus.runic_races.ability;

/** A release edge is necessary between activations; duplicate/out-of-order packets do nothing. */
public final class InputLatch {
    private long sequence = -1;
    private long lastPress = Long.MIN_VALUE;
    private boolean down;
    public boolean accept(long incomingSequence, boolean pressed, long tick) {
        if (incomingSequence <= sequence || incomingSequence < 0) return false;
        sequence = incomingSequence;
        boolean edge = pressed && !down;
        down = pressed;
        if (!edge || (lastPress != Long.MIN_VALUE && tick - lastPress < 4)) return false;
        lastPress = tick;
        return true;
    }
}
