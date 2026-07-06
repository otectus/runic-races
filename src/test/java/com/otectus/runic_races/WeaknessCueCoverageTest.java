package com.otectus.runic_races;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the weakness-cue contract: every SignatureKey and RaceStateFlags constant
 * referenced by {@code WeaknessCueRegistry} must exist (a typo'd reference is a
 * silent no-op at runtime), and every weakness-cue entry in the signature registry
 * must be bannerless — the notification system owns the words for flag transitions,
 * so a banner here would double-speak.
 *
 * Source-scanned, matching SignatureCoverageTest's style.
 */
class WeaknessCueCoverageTest {

    private static final Path CUE_SRC =
            Path.of("src/main/java/com/otectus/runic_races/presentation/WeaknessCueRegistry.java");
    private static final Path KEY_SRC =
            Path.of("src/main/java/com/otectus/runic_races/presentation/SignatureKey.java");
    private static final Path FLAGS_SRC =
            Path.of("src/main/java/com/otectus/runic_races/common/state/RaceStateFlags.java");
    private static final Path REGISTRY_SRC =
            Path.of("src/main/java/com/otectus/runic_races/presentation/SignatureRegistry.java");

    // Constants may end with ',', ';', open a ctor-arg paren, or (the last one) close the enum bare.
    private static final Pattern ENUM_CONSTANT = Pattern.compile("^\\s{4}([A-Z][A-Z_]*)(?:[,;(]|\\s*$)", Pattern.MULTILINE);
    private static final Pattern KEY_REF = Pattern.compile("SignatureKey\\.([A-Z_]+)");
    private static final Pattern FLAG_REF = Pattern.compile("RaceStateFlags\\.([A-Z_]+)");
    private static final Pattern ENTRY_START = Pattern.compile("ENTRIES\\.put\\(SignatureKey\\.([A-Z_]+)");

    @Test
    void everyReferencedKeyAndFlagExists() throws IOException {
        String cueSource = Files.readString(CUE_SRC);
        Set<String> keys = constants(KEY_SRC);
        Set<String> flags = constants(FLAGS_SRC);

        List<String> problems = new ArrayList<>();
        Matcher keyRefs = KEY_REF.matcher(cueSource);
        while (keyRefs.find()) {
            if (!keys.contains(keyRefs.group(1))) {
                problems.add("WeaknessCueRegistry references nonexistent SignatureKey." + keyRefs.group(1));
            }
        }
        Matcher flagRefs = FLAG_REF.matcher(cueSource);
        while (flagRefs.find()) {
            if (!flags.contains(flagRefs.group(1))) {
                problems.add("WeaknessCueRegistry references nonexistent RaceStateFlags." + flagRefs.group(1));
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void weaknessCueEntriesAreBannerless() throws IOException {
        String registry = Files.readString(REGISTRY_SRC);
        List<String> problems = new ArrayList<>();

        Matcher starts = ENTRY_START.matcher(registry);
        List<int[]> spans = new ArrayList<>();
        List<String> names = new ArrayList<>();
        while (starts.find()) {
            spans.add(new int[]{starts.end()});
            names.add(starts.group(1));
        }
        for (int i = 0; i < names.size(); i++) {
            if (!names.get(i).startsWith("WEAKNESS_")) continue;
            int end = i + 1 < spans.size() ? spans.get(i + 1)[0] : registry.length();
            String block = registry.substring(spans.get(i)[0], end);
            // The banner key is the first ctor argument: bannerless entries open with "null,".
            if (!block.replaceAll("\\s+", " ").contains("new SignatureEntry( null,")) {
                problems.add(names.get(i) + " must be bannerless (bannerKey == null) — "
                        + "the notification system owns flag-transition copy");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static Set<String> constants(Path source) throws IOException {
        Set<String> result = new TreeSet<>();
        Matcher m = ENUM_CONSTANT.matcher(Files.readString(source));
        while (m.find()) {
            result.add(m.group(1));
        }
        assertTrue(!result.isEmpty(), "Expected enum constants in " + source);
        return result;
    }
}
