package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PacketHandlerTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("PacketHandler instance is initialized")
    void instance_isNotNull() {
        assertThat(PacketHandler.INSTANCE).isNotNull();
    }

    @Test
    @DisplayName("PROTOCOL_VERSION matches version 2")
    void protocolVersion_isVersion2() throws Exception {
        Field versionField = PacketHandler.class.getDeclaredField("PROTOCOL_VERSION");
        versionField.setAccessible(true);
        String version = (String) versionField.get(null);
        assertThat(version).isEqualTo("2");
    }

    @Test
    @DisplayName("PacketHandler.init registers all mod network packets")
    void init_registersAllPackets() throws Exception {
        assertThatCode(PacketHandler::init).doesNotThrowAnyException();

        Field packetIdField = PacketHandler.class.getDeclaredField("packetId");
        packetIdField.setAccessible(true);
        int currentId = packetIdField.getInt(null);
        assertThat(currentId).isGreaterThanOrEqualTo(17);
    }

    @Test
    @DisplayName("PacketHandler private id generator increments sequentially")
    void idGenerator_increments() throws Exception {
        Method idMethod = PacketHandler.class.getDeclaredMethod("id");
        idMethod.setAccessible(true);
        int first = (int) idMethod.invoke(null);
        int second = (int) idMethod.invoke(null);
        assertThat(second).isEqualTo(first + 1);
    }
}
