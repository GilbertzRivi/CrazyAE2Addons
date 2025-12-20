package net.oktawia.crazyae2addons.logic;

import appeng.api.config.*;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.oktawia.crazyae2addons.interfaces.IAdvPatternProviderCpu;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderCpu;
import net.oktawia.crazyae2addons.misc.PatternDetailsSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ImpulsedPatternProviderLogic extends PatternProviderLogic implements IPatternProviderCpu {

    public final PatternProviderLogicHost host;
    private final IActionSource actionSource;
    @Nullable
    private IPatternDetails patternDetails;
    @Nullable
    private IPatternDetails lastPattern = null;
    private boolean ignoreNBT = false;
    @Nullable
    private CraftingCPUCluster cpuCluster = null;
    @Nullable
    private BlockPos cpuClusterPos = null;
    @Nullable
    private ServerLevel cpuClusterLvl = null;
    private boolean bypassCraftingLock = false;

    public ImpulsedPatternProviderLogic(IManagedGridNode mainNode, PatternProviderLogicHost host, int invSize) {
        super(mainNode, host, invSize);
        this.host = host;
        this.actionSource = new MachineSource(mainNode::getNode);

        this.getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);

        this.getConfigManager().putSetting(Settings.BLOCKING_MODE, YesNo.NO);
        this.getConfigManager().putSetting(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.LOCK_UNTIL_RESULT);
    }

    // ====== API dla mixina ======
    public boolean bypassLock() {
        return this.bypassCraftingLock;
    }

    public boolean ignoreNbtUnlock() {
        return this.ignoreNBT;
    }

    public void onUnlockCleared() {
        this.lastPattern = null;
        this.cpuCluster = null;
    }

    // ====== IPatternProviderCpu ======
    @Override
    public void setCpuCluster(CraftingCPUCluster cpu) {
        this.cpuCluster = cpu;
    }

    @Override
    public CraftingCPUCluster getCpuCluster() {
        return this.cpuCluster;
    }

    @Override
    public void setPatternDetails(IPatternDetails details) {
        this.patternDetails = details;
    }

    @Override
    public IPatternDetails getPatternDetails() {
        return this.patternDetails;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        boolean pushed = super.pushPattern(patternDetails, inputHolder);

        if (pushed) {
            this.lastPattern = PatternDetailsSerializer.deserialize(PatternDetailsSerializer.serialize(patternDetails));

            var defTag = patternDetails.getDefinition().getTag();
            this.ignoreNBT = defTag != null && defTag.contains("ignorenbt") && defTag.getBoolean("ignorenbt");
        }

        return pushed;
    }

    // ====== NBT ======
    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);

        if (this.lastPattern != null) {
            tag.put("lastpattern", PatternDetailsSerializer.serialize(this.lastPattern));
        } else {
            tag.remove("lastpattern");
        }

        if (this.cpuCluster != null) {
            CompoundTag clusterTag = new CompoundTag();
            clusterTag.putLong("pos", this.cpuCluster.getBoundsMin().asLong());
            clusterTag.putString("level", this.cpuCluster.getLevel().dimension().location().toString());
            tag.put("cpuCluster", clusterTag);
        } else {
            tag.remove("cpuCluster");
        }

        if (this.getPatternDetails() != null) {
            tag.put("pdetails", PatternDetailsSerializer.serialize(this.getPatternDetails()));
        } else {
            tag.remove("pdetails");
        }

        if (this instanceof IAdvPatternProviderCpu advCpu) {
            advCpu.advSaveNbt(tag);
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);

        if (tag.contains("lastpattern")) {
            this.lastPattern = PatternDetailsSerializer.deserialize(tag.getCompound("lastpattern"));
        } else {
            this.lastPattern = null;
        }

        if (tag.contains("pdetails")) {
            this.setPatternDetails(PatternDetailsSerializer.deserialize(tag.getCompound("pdetails")));
        } else {
            this.setPatternDetails(null);
        }

        if (tag.contains("cpuCluster")) {
            try {
                var clusterTag = tag.getCompound("cpuCluster");
                var lvl = ServerLifecycleHooks.getCurrentServer().getLevel(
                        ResourceKey.create(Registries.DIMENSION, new ResourceLocation(clusterTag.getString("level")))
                );
                if (lvl != null) {
                    this.cpuClusterLvl = lvl;
                    this.cpuClusterPos = BlockPos.of(clusterTag.getLong("pos"));
                }
            } catch (Exception e) {
                LogUtils.getLogger().info(e.toString());
                this.cpuClusterLvl = null;
                this.cpuClusterPos = null;
            }
        } else {
            this.cpuClusterLvl = null;
            this.cpuClusterPos = null;
        }

        if (this instanceof IAdvPatternProviderCpu advCpu) {
            advCpu.advReadNbt(tag);
        }
    }

    // ====== repeat() ======
    public void repeat() {
        if (this.cpuClusterPos != null && this.cpuClusterLvl != null) {
            var cpuEntity = this.cpuClusterLvl.getBlockEntity(this.cpuClusterPos);
            if (cpuEntity instanceof CraftingBlockEntity entity && entity.getCluster() != null) {
                this.cpuCluster = entity.getCluster();
            }
            this.cpuClusterLvl = null;
            this.cpuClusterPos = null;
        }

        if (this instanceof IAdvPatternProviderCpu advCpu) {
            advCpu.loadTag();
        }

        if (this.lastPattern == null) {
            return;
        }

        IGrid grid = this.getGrid();
        if (grid == null) {
            return;
        }

        var inv = grid.getStorageService().getInventory();

        for (var input : this.lastPattern.getInputs()) {
            boolean canSatisfy = false;
            for (var item : input.getPossibleInputs()) {
                long extracted = inv.extract(item.what(), item.amount(), Actionable.SIMULATE, this.actionSource);
                if (extracted >= item.amount()) {
                    canSatisfy = true;
                    break;
                }
            }
            if (!canSatisfy) {
                failCrafting();
                return;
            }
        }

        List<KeyCounter> holders = new ArrayList<>();
        for (var input : this.lastPattern.getInputs()) {
            KeyCounter holder = new KeyCounter();
            for (var item : input.getPossibleInputs()) {
                long canExtract = inv.extract(item.what(), item.amount(), Actionable.SIMULATE, this.actionSource);
                if (canExtract >= item.amount()) {
                    holder.add(item.what(), item.amount());
                    break;
                }
            }
            holders.add(holder);
        }

        KeyCounter[] inputHolderArray = holders.toArray(new KeyCounter[0]);

        var live = resolveLivePattern(this.lastPattern);
        if (live == null) {
            return;
        }

        this.bypassCraftingLock = true;
        try {
            boolean pushed = super.pushPattern(live, inputHolderArray);

            if (pushed) {
                for (KeyCounter holder : holders) {
                    holder.forEach(e ->
                            inv.extract(e.getKey(), e.getLongValue(), Actionable.MODULATE, this.actionSource)
                    );
                }
            }
        } finally {
            this.bypassCraftingLock = false;
        }
    }

    public void failCrafting() {
        if (this instanceof IAdvPatternProviderCpu adv) {
            adv.failAdvCrafting();
        }

        if (this.cpuCluster != null) {
            this.cpuCluster.cancelJob();
            this.cpuCluster = null;
        }

        this.resetCraftingLock();
        this.lastPattern = null;
        this.ignoreNBT = false;
    }

    @Nullable
    private IPatternDetails resolveLivePattern(IPatternDetails snapshot) {
        ItemStack snapDef = snapshot.getDefinition().toStack();
        for (var p : this.getAvailablePatterns()) {
            if (ItemStack.isSameItemSameTags(p.getDefinition().toStack(), snapDef)) {
                return p;
            }
        }
        return null;
    }
}
