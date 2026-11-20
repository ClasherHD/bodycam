package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.bodycam;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"null", "unchecked"})
@Mixin(SelectItemModel.UnbakedSwitch.class)
public abstract class SelectItemModelUnbakedSwitchMixin<P extends SelectItemModelProperty<T>, T> {
    @Unique
    private static final ResourceKey<TrimMaterial> BODYCAM_OBSERVATION_CRYSTAL =
            ResourceKey.create(Registries.TRIM_MATERIAL, bodycam.id("observation_crystal"));

    @Unique
    private static final Set<String> BODYCAM_SUPPORTED_ARMORS = Set.of(
            "turtle_helmet",
            "chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots",
            "copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots",
            "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots",
            "golden_helmet", "golden_chestplate", "golden_leggings", "golden_boots",
            "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots",
            "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots",
            "netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots"
    );

    @Shadow
    @Final
    private P property;

    @Shadow
    @Final
    private List<SelectItemModel.SwitchCase<T>> cases;

    @Redirect(
            method = {"resolveDependencies", "bake"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/item/SelectItemModel$UnbakedSwitch;cases:Ljava/util/List;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private List<SelectItemModel.SwitchCase<T>> bodycam$injectObservationCrystalCase(
            SelectItemModel.UnbakedSwitch<P, T> instance
    ) {
        if (!(this.property instanceof TrimMaterialProperty)) {
            return this.cases;
        }
        BlockModelWrapper.Unbaked template = null;
        for (SelectItemModel.SwitchCase<T> entry : this.cases) {
            for (T value : entry.values()) {
                if (BODYCAM_OBSERVATION_CRYSTAL.equals(value)) {
                    return this.cases;
                }
                if (TrimMaterials.AMETHYST.equals(value)
                        && entry.model() instanceof BlockModelWrapper.Unbaked wrapped) {
                    template = wrapped;
                }
            }
        }
        if (template == null) {
            return this.cases;
        }
        Identifier templateModel = template.model();
        String path = templateModel.getPath();
        if (!"minecraft".equals(templateModel.getNamespace())
                || !path.startsWith("item/")
                || !path.endsWith("_amethyst_trim")) {
            return this.cases;
        }
        String armorName = path.substring("item/".length(), path.length() - "_amethyst_trim".length());
        if (!BODYCAM_SUPPORTED_ARMORS.contains(armorName)) {
            return this.cases;
        }
        Identifier observationModel = bodycam.id("item/trims/" + armorName + "_observation_crystal_trim");
        BlockModelWrapper.Unbaked wrappedObservation = new BlockModelWrapper.Unbaked(observationModel, template.tints());
        List<SelectItemModel.SwitchCase<T>> extended = new ArrayList<>(this.cases.size() + 1);
        extended.addAll(this.cases);
        extended.add(new SelectItemModel.SwitchCase<>((List<T>) List.of(BODYCAM_OBSERVATION_CRYSTAL), wrappedObservation));
        return extended;
    }
}
