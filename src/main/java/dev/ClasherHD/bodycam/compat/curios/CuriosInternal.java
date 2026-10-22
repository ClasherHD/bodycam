package dev.ClasherHD.bodycam.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("null")
final class CuriosInternal {

    private CuriosInternal() {
    }

    static boolean hasItem(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .resolve()
                .flatMap(handler -> handler.findFirstCurio(item))
                .isPresent();
    }

    static List<ItemStack> getEquippedItems(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .resolve()
                .map(handler -> handler.findCurios(item).stream()
                        .map(SlotResult::stack)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
