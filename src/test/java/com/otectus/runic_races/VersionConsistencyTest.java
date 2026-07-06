package com.otectus.runic_races;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Player-facing docs must name the version that actually builds: README and the
 * CurseForge description sat on "1.2.0" through two releases because nothing
 * failed when they drifted. The top CHANGELOG section must match too.
 */
class VersionConsistencyTest {

    @Test
    void docsNameTheBuiltVersion() throws IOException {
        Properties props = new Properties();
        props.load(Files.newBufferedReader(Path.of("gradle.properties")));
        String version = props.getProperty("mod_version");
        assertNotNull(version, "mod_version missing from gradle.properties");

        for (String doc : List.of("README.md", "CURSEFORGE_DESCRIPTION.md")) {
            String text = Files.readString(Path.of(doc));
            assertTrue(text.contains(version),
                    doc + " never mentions the built version " + version + " — stale docs");
        }

        String changelog = Files.readString(Path.of("CHANGELOG.md"));
        int topSection = changelog.indexOf("## [");
        assertTrue(topSection >= 0, "CHANGELOG.md has no version sections");
        String topHeading = changelog.substring(topSection, changelog.indexOf(']', topSection));
        assertTrue(topHeading.contains(version) || topHeading.contains("Unreleased"),
                "CHANGELOG.md top section is '" + topHeading + "]' — expected [" + version + "] or [Unreleased]");
    }
}
