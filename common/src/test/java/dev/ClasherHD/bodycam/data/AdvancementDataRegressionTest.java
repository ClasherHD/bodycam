package dev.ClasherHD.bodycam.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancementDataRegressionTest {

    private Path resolveResourcesRoot() throws URISyntaxException {
        URL packUrl = getClass().getClassLoader().getResource("pack.mcmeta");
        if (packUrl != null && "file".equals(packUrl.getProtocol())) {
            return Path.of(packUrl.toURI()).getParent();
        }
        Path direct = Path.of("src/main/resources");
        if (Files.exists(direct.resolve("pack.mcmeta"))) {
            return direct;
        }
        return Path.of("common/src/main/resources");
    }

    private JsonObject readJson(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        JsonElement parsed = JsonParser.parseString(content);
        assertThat(parsed.isJsonObject()).as("Expected JSON object in %s", path.getFileName()).isTrue();
        return parsed.getAsJsonObject();
    }

    private void assertNoLegacy1201Keys(JsonElement element, String fileName) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            assertThat(obj.has("nbt"))
                    .as("Advancement %s must not contain removed 1.20.1 'nbt' key", fileName)
                    .isFalse();
            assertThat(obj.has("structure"))
                    .as("Advancement %s must not contain singular 'structure' key", fileName)
                    .isFalse();
            if (obj.has("predicates")) {
                JsonElement predicatesElem = obj.get("predicates");
                assertThat(predicatesElem.isJsonObject())
                        .as("Advancement %s 'predicates' must be a JSON object", fileName)
                        .isTrue();
                assertThat(predicatesElem.getAsJsonObject().size())
                        .as("Advancement %s 'predicates' must not be empty", fileName)
                        .isGreaterThan(0);
            }
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                assertNoLegacy1201Keys(entry.getValue(), fileName);
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (JsonElement item : arr) {
                assertNoLegacy1201Keys(item, fileName);
            }
        }
    }

    @Test
    void packMcmeta_usesPackFormat48() throws Exception {
        Path resourcesRoot = resolveResourcesRoot();
        JsonObject root = readJson(resourcesRoot.resolve("pack.mcmeta"));
        assertThat(root.has("pack")).isTrue();
        JsonObject pack = root.getAsJsonObject("pack");
        assertThat(pack.get("pack_format").getAsInt()).isEqualTo(48);
    }

    @Test
    void allAdvancementJsons_haveNoRemoved1201NbtOrEmptyPredicatesOrSingularStructure() throws Exception {
        Path advDir = resolveResourcesRoot().resolve("data/bodycam/advancement");
        assertThat(Files.isDirectory(advDir)).isTrue();

        List<Path> jsonFiles;
        try (Stream<Path> stream = Files.list(advDir)) {
            jsonFiles = stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        assertThat(jsonFiles).isNotEmpty();

        for (Path file : jsonFiles) {
            JsonObject root = readJson(file);
            assertThat(root.has("criteria")).as("Missing criteria in %s", file.getFileName()).isTrue();
            assertNoLegacy1201Keys(root, file.getFileName().toString());
        }
    }

    @Test
    void findCherryHouse_usesPluralStructuresField() throws Exception {
        Path file = resolveResourcesRoot().resolve("data/bodycam/advancement/find_cherry_house.json");
        JsonObject root = readJson(file);
        JsonObject predicate = root.getAsJsonObject("criteria")
                .getAsJsonObject("in_cherry_house")
                .getAsJsonObject("conditions")
                .getAsJsonArray("player")
                .get(0).getAsJsonObject()
                .getAsJsonObject("predicate");

        assertThat(predicate.has("structures")).isTrue();
        assertThat(predicate.get("structures").getAsString()).isEqualTo("bodycam:cherry_house");
        assertThat(predicate.has("structure")).isFalse();
    }

    @Test
    void trueAnonymity_uses121EnchantmentPredicates() throws Exception {
        Path file = resolveResourcesRoot().resolve("data/bodycam/advancement/true_anonymity.json");
        JsonObject root = readJson(file);
        JsonObject criteria = root.getAsJsonObject("criteria");

        JsonObject bookPredicates = criteria.getAsJsonObject("has_book")
                .getAsJsonObject("conditions")
                .getAsJsonArray("items")
                .get(0).getAsJsonObject()
                .getAsJsonObject("predicates");
        assertThat(bookPredicates.has("minecraft:stored_enchantments")).isTrue();
        JsonObject storedEnch = bookPredicates.getAsJsonArray("minecraft:stored_enchantments").get(0).getAsJsonObject();
        assertThat(storedEnch.get("enchantments").getAsString()).isEqualTo("bodycam:anonymity");
        assertThat(storedEnch.get("levels").getAsInt()).isEqualTo(2);

        JsonObject itemPredicates = criteria.getAsJsonObject("has_item")
                .getAsJsonObject("conditions")
                .getAsJsonArray("items")
                .get(0).getAsJsonObject()
                .getAsJsonObject("predicates");
        assertThat(itemPredicates.has("minecraft:enchantments")).isTrue();
        JsonObject wornEnch = itemPredicates.getAsJsonArray("minecraft:enchantments").get(0).getAsJsonObject();
        assertThat(wornEnch.get("enchantments").getAsString()).isEqualTo("bodycam:anonymity");
        assertThat(wornEnch.get("levels").getAsInt()).isEqualTo(2);
    }
}
