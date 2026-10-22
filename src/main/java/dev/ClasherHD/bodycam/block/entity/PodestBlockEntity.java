package dev.ClasherHD.bodycam.block.entity;

import dev.ClasherHD.bodycam.block.PodestBlock;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class PodestBlockEntity extends BlockEntity {

    public static final int MAX_CHARGES = 10;
    public static final double BLOCKS_PER_CHARGE = 10.0D;

    public static final Set<PodestBlockEntity> LOADED_PODESTS = ConcurrentHashMap.newKeySet();

    private ItemStack heldItem = ItemStack.EMPTY;
    private int charges = 0;

    public PodestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PODEST.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            LOADED_PODESTS.add(this);
        }
    }

    @Override
    public void setRemoved() {
        LOADED_PODESTS.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        LOADED_PODESTS.remove(this);
        super.onChunkUnloaded();
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    public void setHeldItem(ItemStack stack) {
        this.heldItem = stack == null ? ItemStack.EMPTY : stack;
        if (this.heldItem.getItem() instanceof JammerItem) {
            int currentMode = this.heldItem.hasTag() ? this.heldItem.getTag().getInt("JammerMode") : 0;
            if (currentMode == 0) {
                this.heldItem.getOrCreateTag().putInt("JammerMode", 1);
            }
        }
        setChanged();
        syncToClient();
    }

    public ItemStack removeHeldItem() {
        ItemStack stack = this.heldItem;
        this.heldItem = ItemStack.EMPTY;
        cleanPlacedItemTags(stack);
        setChanged();
        syncToClient();
        return stack;
    }

    private static void cleanPlacedItemTags(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (stack.getItem() instanceof JammerItem) {
            if (tag.getInt("JammerMode") != 2) {
                tag.putInt("JammerMode", 0);
            }
            tag.remove("active_id");
        } else if (stack.getItem() instanceof AnonymizerItem) {
            tag.putBoolean("AnonymizerActive", false);
            tag.remove("active_id");
        }
    }

    public int getCharges() {
        return charges;
    }

    public double getSphericalRadius() {
        return charges * BLOCKS_PER_CHARGE;
    }

    public void addCharge(Level level, BlockPos pos, BlockState state) {
        if (this.charges < MAX_CHARGES) {
            this.charges++;
            boolean powered = level.hasNeighborSignal(pos);
            level.setBlock(pos, state.setValue(PodestBlock.CHARGES, this.charges).setValue(PodestBlock.POWERED, powered), 3);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
            setChanged();
            syncToClient();
        }
    }

    public int getEffectiveJammerMode() {
        if (!(this.heldItem.getItem() instanceof JammerItem)) {
            return 0;
        }
        if (this.heldItem.hasTag() && this.heldItem.getTag().getInt("JammerMode") == 2) {
            return 2;
        }
        return 1;
    }

    public boolean isPodestActive() {
        if (this.level == null || this.isRemoved()) {
            return false;
        }
        if (!ModServerConfig.ENABLE_PODEST.get()) {
            return false;
        }
        BlockState state = this.getBlockState();
        if (!state.is(dev.ClasherHD.bodycam.registry.ModBlocks.PODEST.get())) {
            return false;
        }
        if (!state.getValue(PodestBlock.POWERED) || this.charges <= 0) {
            return false;
        }
        if (this.heldItem.isEmpty() || ItemDurabilityHelper.isEmpty(this.heldItem)) {
            return false;
        }
        if (this.heldItem.getItem() instanceof JammerItem) {
            return ModServerConfig.ENABLE_JAMMER.get();
        }
        if (this.heldItem.getItem() instanceof AnonymizerItem) {
            return ModServerConfig.ENABLE_ANONYMIZER.get();
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PodestBlockEntity podest) {
        LOADED_PODESTS.add(podest);

        boolean hasSignal = level.hasNeighborSignal(pos);
        if (state.getValue(PodestBlock.CHARGES) != podest.charges || state.getValue(PodestBlock.POWERED) != hasSignal) {
            state = state.setValue(PodestBlock.CHARGES, podest.charges).setValue(PodestBlock.POWERED, hasSignal);
            level.setBlock(pos, state, 3);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }

        if (!podest.isPodestActive()) {
            return;
        }

        long gameTime = level.getGameTime();
        ItemStack stack = podest.heldItem;
        if (stack.getItem() instanceof JammerItem) {
            int mode = podest.getEffectiveJammerMode();
            int interval = mode == 2 ? 40 : 20;
            if (gameTime % interval == 0) {
                boolean wasEmpty = ItemDurabilityHelper.isEmpty(stack);
                ItemDurabilityHelper.damagePerTime(stack, null, 1);
                podest.setChanged();
                if (!wasEmpty && ItemDurabilityHelper.isEmpty(stack)) {
                    podest.syncToClient();
                }
            }
        } else if (stack.getItem() instanceof AnonymizerItem) {
            if (gameTime % 20 == 0) {
                boolean wasEmpty = ItemDurabilityHelper.isEmpty(stack);
                ItemDurabilityHelper.damagePerTime(stack, null, 1);
                podest.setChanged();
                if (!wasEmpty && ItemDurabilityHelper.isEmpty(stack)) {
                    podest.syncToClient();
                }
            }
        }
    }

    private static Vec3 getEffectivePlayerPos(ServerPlayer player) {
        if (player.getPersistentData().getBoolean("bodycam_active")) {
            Vec3 dummyVec = BodycamDummyEntity.DUMMY_POS.get(player.getUUID());
            if (dummyVec != null) {
                return dummyVec;
            }
            Vec3 origVec = BodycamSetCameraPacket.ORIGINAL_POS.get(player.getUUID());
            if (origVec != null) {
                return origVec;
            }
            if (player.getPersistentData().contains("bodycam_orig_x")) {
                return new Vec3(
                        player.getPersistentData().getDouble("bodycam_orig_x"),
                        player.getPersistentData().getDouble("bodycam_orig_y"),
                        player.getPersistentData().getDouble("bodycam_orig_z")
                );
            }
        }
        return player.position();
    }

    private static String getEffectivePlayerDim(ServerPlayer player) {
        if (player.getPersistentData().getBoolean("bodycam_active")) {
            String origDim = BodycamSetCameraPacket.ORIGINAL_DIM.get(player.getUUID());
            if (origDim != null && !origDim.isEmpty()) {
                return origDim;
            }
            if (player.getPersistentData().contains("bodycam_orig_dim")) {
                return player.getPersistentData().getString("bodycam_orig_dim");
            }
        }
        return player.level().dimension().location().toString();
    }

    private boolean isWithinSphere(String targetDim, Vec3 targetPos) {
        if (!isPodestActive() || this.level == null) {
            return false;
        }
        String podestDim = this.level.dimension().location().toString();
        if (!podestDim.equals(targetDim)) {
            return false;
        }
        double radius = getSphericalRadius();
        if (radius <= 0.0D) {
            return false;
        }
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        return center.distanceToSqr(targetPos) <= (radius * radius);
    }

    public static int combineJammerModes(int modeA, int modeB) {
        if (modeA == 1 || modeB == 1) {
            return 1;
        }
        if (modeA == 2 || modeB == 2) {
            return 2;
        }
        return 0;
    }

    public static int getPodestJammerMode(ServerPlayer player) {
        LOADED_PODESTS.removeIf(p -> p == null || p.isRemoved() || p.getLevel() == null);
        if (LOADED_PODESTS.isEmpty()) {
            return 0;
        }
        String dim = getEffectivePlayerDim(player);
        Vec3 pos = getEffectivePlayerPos(player);
        int result = 0;
        for (PodestBlockEntity podest : LOADED_PODESTS) {
            if (podest.heldItem.getItem() instanceof JammerItem && podest.isWithinSphere(dim, pos)) {
                result = combineJammerModes(result, podest.getEffectiveJammerMode());
                if (result == 1) {
                    return 1;
                }
            }
        }
        return result;
    }

    public static boolean hasPodestAnonymizer(ServerPlayer player) {
        LOADED_PODESTS.removeIf(p -> p == null || p.isRemoved() || p.getLevel() == null);
        if (LOADED_PODESTS.isEmpty()) {
            return false;
        }
        String dim = getEffectivePlayerDim(player);
        Vec3 pos = getEffectivePlayerPos(player);
        for (PodestBlockEntity podest : LOADED_PODESTS) {
            if (podest.heldItem.getItem() instanceof AnonymizerItem && podest.isWithinSphere(dim, pos)) {
                return true;
            }
        }
        return false;
    }

    public static int getPodestAnonymityLevel(ServerPlayer player) {
        LOADED_PODESTS.removeIf(p -> p == null || p.isRemoved() || p.getLevel() == null);
        if (LOADED_PODESTS.isEmpty()) {
            return 0;
        }
        String dim = getEffectivePlayerDim(player);
        Vec3 pos = getEffectivePlayerPos(player);
        int highest = 0;
        for (PodestBlockEntity podest : LOADED_PODESTS) {
            if (podest.heldItem.getItem() instanceof AnonymizerItem && podest.isWithinSphere(dim, pos)) {
                highest = Math.max(highest, EnchantmentLevels.anonymity(podest.heldItem));
            }
        }
        return highest;
    }

    public void dropContents(Level level, BlockPos pos) {
        if (!this.heldItem.isEmpty()) {
            ItemStack toDrop = this.heldItem.copy();
            cleanPlacedItemTags(toDrop);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), toDrop);
            this.heldItem = ItemStack.EMPTY;
        }
        if (this.charges > 0) {
            ItemStack crystals = new ItemStack(ModItems.OBSERVATION_CRYSTAL.get(), this.charges);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), crystals);
            this.charges = 0;
        }
    }

    public void syncToClient() {
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Charges", this.charges);
        if (!this.heldItem.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            this.heldItem.save(itemTag);
            tag.put("HeldItem", itemTag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.charges = Math.max(0, Math.min(MAX_CHARGES, tag.getInt("Charges")));
        if (tag.contains("HeldItem")) {
            this.heldItem = ItemStack.of(tag.getCompound("HeldItem"));
        } else {
            this.heldItem = ItemStack.EMPTY;
        }
    }
}
