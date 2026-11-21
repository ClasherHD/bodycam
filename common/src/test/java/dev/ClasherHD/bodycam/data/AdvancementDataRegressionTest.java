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
    void packMcmeta_usesMc262PackFormats() throws Exception {
        Path resourcesRoot = resolveResourcesRoot();
        JsonObject root = readJson(resourcesRoot.resolve("pack.mcmeta"));
        assertThat(root.has("pack")).isTrue();
        JsonObject pack = root.getAsJsonObject("pack");
        assertThat(pack.get("pack_format").getAsInt()).isEqualTo(88);
        assertThat(pack.get("min_format").getAsInt()).isEqualTo(88);
        JsonArray maxFormat = pack.getAsJsonArray("max_format");
        assertThat(maxFormat.size()).isEqualTo(2);
        assertThat(maxFormat.get(0).getAsInt()).isEqualTo(107);
        assertThat(maxFormat.get(1).getAsInt()).isEqualTo(1);
    }

    @Test
    void villagerTradeDatapackEntriesAndTags_matchMc262Schema() throws Exception {
        Path resourcesRoot = resolveResourcesRoot();

        JsonObject cartographerTrade = readJson(resourcesRoot.resolve(
                "data/bodycam/villager_trade/cartographer/3/cherry_house_explorer_map.json"));
        assertThat(cartographerTrade.getAsJsonObject("wants").get("id").getAsString()).isEqualTo("minecraft:emerald");
        assertThat(cartographerTrade.getAsJsonObject("wants").get("count").getAsDouble()).isEqualTo(14.0);
        assertThat(cartographerTrade.getAsJsonObject("additional_wants").get("id").getAsString()).isEqualTo("minecraft:compass");
        assertThat(cartographerTrade.getAsJsonObject("gives").get("id").getAsString()).isEqualTo("bodycam:cherry_house_explorer_map");
        assertThat(cartographerTrade.get("max_uses").getAsDouble()).isEqualTo(12.0);
        assertThat(cartographerTrade.get("xp").getAsDouble()).isEqualTo(10.0);
        assertThat(cartographerTrade.get("reputation_discount").getAsDouble()).isEqualTo(0.2);

        JsonObject cartographerTag = readJson(resourcesRoot.resolve(
                "data/minecraft/tags/villager_trade/cartographer/level_3.json"));
        assertThat(cartographerTag.get("replace").getAsBoolean()).isFalse();
        assertThat(cartographerTag.getAsJsonArray("values")).hasSize(1);
        assertThat(cartographerTag.getAsJsonArray("values").get(0).getAsString())
                .isEqualTo("bodycam:cartographer/3/cherry_house_explorer_map");

        JsonObject wanderingTrade = readJson(resourcesRoot.resolve(
                "data/bodycam/villager_trade/wandering_trader/cherry_house_explorer_map.json"));
        assertThat(wanderingTrade.getAsJsonObject("wants").get("id").getAsString()).isEqualTo("minecraft:emerald");
        assertThat(wanderingTrade.getAsJsonObject("wants").get("count").getAsDouble()).isEqualTo(16.0);
        assertThat(wanderingTrade.getAsJsonObject("additional_wants").get("id").getAsString()).isEqualTo("minecraft:compass");
        assertThat(wanderingTrade.getAsJsonObject("gives").get("id").getAsString()).isEqualTo("bodycam:cherry_house_explorer_map");
        assertThat(wanderingTrade.get("max_uses").getAsDouble()).isEqualTo(2.0);
        assertThat(wanderingTrade.get("xp").getAsDouble()).isEqualTo(10.0);
        assertThat(wanderingTrade.get("reputation_discount").getAsDouble()).isEqualTo(0.05);

        JsonObject wanderingTag = readJson(resourcesRoot.resolve(
                "data/minecraft/tags/villager_trade/wandering_trader/uncommon.json"));
        assertThat(wanderingTag.get("replace").getAsBoolean()).isFalse();
        assertThat(wanderingTag.getAsJsonArray("values")).hasSize(1);
        assertThat(wanderingTag.getAsJsonArray("values").get(0).getAsString())
                .isEqualTo("bodycam:wandering_trader/cherry_house_explorer_map");
    }

    @Test
    void translucentBlockModelsAndCustomRecipe_useMc262Syntax() throws Exception {
        Path resourcesRoot = resolveResourcesRoot();

        for (String modelPath : List.of(
                "assets/bodycam/models/block/hologram_block.json",
                "assets/bodycam/models/block/watch_point.json"
        )) {
            JsonObject model = readJson(resourcesRoot.resolve(modelPath));
            assertThat(model.has("render_type")).isFalse();
            JsonObject textures = model.getAsJsonObject("textures");
            assertThat(textures.size()).isGreaterThan(0);
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                JsonObject slot = entry.getValue().getAsJsonObject();
                assertThat(slot.get("force_translucent").getAsBoolean()).isTrue();
                assertThat(slot.get("sprite").getAsString()).startsWith("bodycam:block/");
            }
        }

        JsonObject loggerRefill = readJson(resourcesRoot.resolve("data/bodycam/recipe/logger_refill.json"));
        assertThat(loggerRefill.get("type").getAsString()).isEqualTo("bodycam:logger_refill");
        assertThat(loggerRefill.has("category")).isFalse();
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
