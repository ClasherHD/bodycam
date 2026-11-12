package dev.ClasherHD.bodycam.network.creepingdeath;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class CreepingDeathPacketRoundtripTest {

    @Test
    @DisplayName("CreepingDeathActionC2SPacket should encode and decode mode, offHand, UUID, and name")
    void creepingDeathActionC2SPacket_roundtrip_preservesFields() {
        UUID chosenId = UUID.randomUUID();
        CreepingDeathActionC2SPacket originalWithId = new CreepingDeathActionC2SPacket(
                1,
                true,
                chosenId,
                "TargetPlayer"
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CreepingDeathActionC2SPacket.encode(originalWithId, buf);
        CreepingDeathActionC2SPacket decodedWithId = CreepingDeathActionC2SPacket.decode(buf);

        assertThat(decodedWithId.mode).isEqualTo(1);
        assertThat(decodedWithId.offHand).isTrue();
        assertThat(decodedWithId.chosenUUID).isEqualTo(chosenId);
        assertThat(decodedWithId.chosenName).isEqualTo("TargetPlayer");

        CreepingDeathActionC2SPacket originalNullId = new CreepingDeathActionC2SPacket(
                0,
                false,
                null,
                ""
        );
        FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
        CreepingDeathActionC2SPacket.encode(originalNullId, buf2);
        CreepingDeathActionC2SPacket decodedNullId = CreepingDeathActionC2SPacket.decode(buf2);

        assertThat(decodedNullId.mode).isEqualTo(0);
        assertThat(decodedNullId.offHand).isFalse();
        assertThat(decodedNullId.chosenUUID).isNull();
        assertThat(decodedNullId.chosenName).isEmpty();
    }

    @Test
    @DisplayName("OpenCreepingDeathScreenS2CPacket should encode and decode entries and selectedSkinUUID")
    void openCreepingDeathScreenS2CPacket_roundtrip_preservesFields() {
        List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> entries = new ArrayList<>();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        entries.add(new OpenCreepingDeathScreenS2CPacket.PlayerEntryData(p1, "PlayerOne", false));
        entries.add(new OpenCreepingDeathScreenS2CPacket.PlayerEntryData(p2, "PlayerTwo", true));

        UUID skinId = UUID.randomUUID();
        OpenCreepingDeathScreenS2CPacket original = new OpenCreepingDeathScreenS2CPacket(
                OpenCreepingDeathScreenS2CPacket.MODE_SKIN,
                true,
                entries,
                skinId
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        OpenCreepingDeathScreenS2CPacket.encode(original, buf);
        OpenCreepingDeathScreenS2CPacket decoded = OpenCreepingDeathScreenS2CPacket.decode(buf);

        assertThat(decoded.mode).isEqualTo(OpenCreepingDeathScreenS2CPacket.MODE_SKIN);
        assertThat(decoded.offHand).isTrue();
        assertThat(decoded.selectedSkinUUID).isEqualTo(skinId);
        assertThat(decoded.entries).hasSize(2);
        assertThat(decoded.entries.get(0).uuid()).isEqualTo(p1);
        assertThat(decoded.entries.get(0).name()).isEqualTo("PlayerOne");
        assertThat(decoded.entries.get(0).disabled()).isFalse();
        assertThat(decoded.entries.get(1).uuid()).isEqualTo(p2);
        assertThat(decoded.entries.get(1).name()).isEqualTo("PlayerTwo");
        assertThat(decoded.entries.get(1).disabled()).isTrue();
    }
}
