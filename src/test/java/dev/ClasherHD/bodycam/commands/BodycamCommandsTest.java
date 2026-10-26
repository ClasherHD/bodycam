package dev.ClasherHD.bodycam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BodycamCommandsTest {

    @Test
    @DisplayName("BodycamCommands.register should correctly build the entire /bodycam command tree")
    void register_buildsCompleteCommandTree() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        BodycamCommands.register(dispatcher);

        CommandNode<CommandSourceStack> root = dispatcher.getRoot();
        CommandNode<CommandSourceStack> bodycamNode = root.getChild("bodycam");

        assertThat(bodycamNode).isNotNull();
        assertThat(bodycamNode).isInstanceOf(LiteralCommandNode.class);

        assertThat(bodycamNode.getChild("config")).isNotNull();
        assertThat(bodycamNode.getChild("reload")).isNotNull();
        assertThat(bodycamNode.getChild("reset")).isNotNull();
        assertThat(bodycamNode.getChild("status")).isNotNull();
        assertThat(bodycamNode.getChild("watchpoint")).isNotNull();
        assertThat(bodycamNode.getChild("give")).isNotNull();
        assertThat(bodycamNode.getChild("tp")).isNotNull();
        assertThat(bodycamNode.getChild("remove")).isNotNull();

        CommandNode<CommandSourceStack> watchpoint = bodycamNode.getChild("watchpoint");
        assertThat(watchpoint.getChild("list")).isNotNull();
        assertThat(watchpoint.getChild("list").getChild("player")).isNotNull();
        assertThat(watchpoint.getChild("remove")).isNotNull();
        assertThat(watchpoint.getChild("remove").getChild("all")).isNotNull();
        assertThat(watchpoint.getChild("remove").getChild("pos")).isNotNull();
        assertThat(watchpoint.getChild("remove").getChild("id")).isNotNull();
        assertThat(watchpoint.getChild("tp")).isNotNull();
        assertThat(watchpoint.getChild("tp").getChild("id")).isNotNull();

        CommandNode<CommandSourceStack> give = bodycamNode.getChild("give");
        CommandNode<CommandSourceStack> item = give.getChild("item");
        assertThat(item).isNotNull();
        assertThat(item.getChild("players")).isNotNull();

        CommandNode<CommandSourceStack> tp = bodycamNode.getChild("tp");
        CommandNode<CommandSourceStack> tpDummy = tp.getChild("dummy");
        assertThat(tpDummy).isNotNull();
        CommandNode<CommandSourceStack> dummyPlayer = tpDummy.getChild("dummyPlayer");
        assertThat(dummyPlayer).isNotNull();
        assertThat(dummyPlayer.getChild("pos")).isNotNull();
        assertThat(dummyPlayer.getChild("destinationPlayer")).isNotNull();

        CommandNode<CommandSourceStack> tpCam = tp.getChild("cam");
        assertThat(tpCam).isNotNull();
        assertThat(tpCam.getChild("target")).isNotNull();

        CommandNode<CommandSourceStack> remove = bodycamNode.getChild("remove");
        CommandNode<CommandSourceStack> removeDummy = remove.getChild("dummy");
        assertThat(removeDummy).isNotNull();
        assertThat(removeDummy.getChild("dummyPlayer")).isNotNull();

        CommandNode<CommandSourceStack> removeAll = remove.getChild("all");
        assertThat(removeAll).isNotNull();
        assertThat(removeAll.getChild("dummies")).isNotNull();
    }
}
