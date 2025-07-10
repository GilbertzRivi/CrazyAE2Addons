package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.blocks.EntropyCradleCapacitor;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.EntropyCradleControllerMenu;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;
import net.oktawia.crazyae2addons.misc.CradleRecipes;
import net.oktawia.crazyae2addons.misc.EntropyCradleValidator;
import net.oktawia.crazyae2addons.renderer.preview.Previewable;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntropyCradleControllerBE extends AENetworkInvBlockEntity implements Previewable, IGridTickable, MenuProvider {

    public EntropyCradleValidator validator;
    public int MAX_ENERGY = 600_000_000;
    public IEnergyStorage storedEnergy;

    private PreviewInfo previewInfo = null;

    @Override
    public PreviewInfo getPreviewInfo() {
        return previewInfo;
    }

    @Override
    public void setPreviewInfo(PreviewInfo info) {
        this.previewInfo = info;
    }

    public boolean preview = false;

    public static final Set<EntropyCradleControllerBE> CLIENT_INSTANCES = new java.util.HashSet<>();

    public EntropyCradleControllerBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.ENTROPY_CRADLE_CONTROLLER_BE.get(), pos, blockState);
        validator = new EntropyCradleValidator();
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.ENTROPY_CRADLE_CONTROLLER.get().asItem())
                );
        this.storedEnergy = new EnergyStorage(MAX_ENERGY, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            CLIENT_INSTANCES.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_INSTANCES.remove(this);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("storedeng")){
            this.storedEnergy.receiveEnergy(data.getInt("storedeng"), false);
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putInt("storedeng", storedEnergy.getEnergyStored());
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Entropy Cradle");
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 5, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!validator.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this))
            return TickRateModulation.IDLE;

        int currentFE = this.storedEnergy.getEnergyStored();

        double fillRatio = currentFE / (double) MAX_ENERGY;

        int maxLevels = 6;
        int litLevels = (int) Math.round(fillRatio * maxLevels);

        for (int level = 0; level < maxLevels; level++) {
            boolean shouldBeLit = level < litLevels;
            validator.markCaps(getLevel(), getBlockPos(), getBlockState(), EntropyCradleCapacitor.POWER, shouldBeLit, level, this.storedEnergy.getEnergyStored() == 600_000_000);
        }

        if (currentFE >= MAX_ENERGY) {
            validator.markCaps(getLevel(), getBlockPos(), getBlockState(), EntropyCradleCapacitor.POWER, true, 0, this.storedEnergy.getEnergyStored() == 600_000_000);
            return TickRateModulation.IDLE;
        }

        int remainingFE = MAX_ENERGY - currentFE;
        int maxAEToExtract = remainingFE / 2;
        if (maxAEToExtract > 25_000_000){
            maxAEToExtract = 25_000_000;
        }

        var extractedAE = getGridNode().getGrid().getEnergyService().extractAEPower(
                maxAEToExtract, Actionable.MODULATE, PowerMultiplier.CONFIG);

        int toInsertFE = (int) (extractedAE * 2);
        this.storedEnergy.receiveEnergy(toInsertFE, false);
        this.setChanged();

        return TickRateModulation.IDLE;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return InternalInventory.empty();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {}

    public void onRedstonePulse() {
        var extracted = this.storedEnergy.extractEnergy(MAX_ENERGY, false);
        for (int level = 0; level < 6; level++) {
            validator.markCaps(getLevel(), getBlockPos(), getBlockState(), EntropyCradleCapacitor.POWER, false, level, false);
        }
        if (extracted < MAX_ENERGY) return;
        var toCenter = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        var origin = this.getBlockPos().relative(toCenter.getAxis(), 5).above(3);
        for (var recipe : CradleRecipes.RECIPES.entrySet()){
            if (validateStructure(getLevel(), origin, recipe.getKey())){
                fillStructureWithAir(getLevel(), origin);
                getLevel().setBlock(origin, recipe.getValue().defaultBlockState(), 3);
                if (!level.isClientSide()) {
                    ((ServerLevel) level).sendParticles(ParticleTypes.EXPLOSION,
                            origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5,
                            30, 0.5, 0.5, 0.5, 0.01);

                    level.playSound(null, origin, SoundEvents.GENERIC_EXPLODE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }


    public static boolean validateStructure(Level level, BlockPos center, String patternJsonStr) {
        JsonObject patternJson = JsonParser.parseString(patternJsonStr).getAsJsonObject();

        Map<String, List<Block>> symbolMap = new HashMap<>();
        JsonObject symbolsJson = patternJson.getAsJsonObject("symbols");
        for (Map.Entry<String, JsonElement> entry : symbolsJson.entrySet()) {
            JsonArray arr = entry.getValue().getAsJsonArray();
            List<Block> blocks = new ArrayList<>();
            for (JsonElement el : arr) {
                ResourceLocation id = new ResourceLocation(el.getAsString());
                Block block = ForgeRegistries.BLOCKS.getValue(id);
                if (block != null) {
                    blocks.add(block);
                }
            }
            symbolMap.put(entry.getKey(), blocks);
        }

        JsonArray layersArray = patternJson.getAsJsonArray("layers");
        if (layersArray.size() != 5) return false;

        final int XZ_OFFSET = -2;
        final int Y_OFFSET = -2;

        for (int y = 0; y < 5; y++) {
            JsonArray layer = layersArray.get(y).getAsJsonArray();
            if (layer.size() != 5) return false;
            for (int z = 0; z < 5; z++) {
                String[] row = layer.get(z).getAsString().split(" ");
                if (row.length != 5) return false;
                for (int x = 0; x < 5; x++) {
                    String symbol = row[x];
                    if (symbol.equals(".")) continue;

                    BlockPos checkPos = center.offset(x + XZ_OFFSET, y + Y_OFFSET, z + XZ_OFFSET);
                    BlockState state = level.getBlockState(checkPos);
                    Block block = state.getBlock();

                    List<Block> allowed = symbolMap.get(symbol);
                    if (allowed == null || !allowed.contains(block)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static void fillStructureWithAir(Level level, BlockPos center) {
        final int XZ_OFFSET = -2;
        final int Y_OFFSET = -2;

        for (int y = 0; y < 5; y++) {
            for (int z = 0; z < 5; z++) {
                for (int x = 0; x < 5; x++) {
                    BlockPos target = center.offset(x + XZ_OFFSET, y + Y_OFFSET, z + XZ_OFFSET);
                    if (x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4){
                        ((ServerLevel) level).sendParticles(ParticleTypes.FIREWORK,
                                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                                5, 0.5, 0.5, 0.5, 0.01);
                    }
                    level.setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new EntropyCradleControllerMenu(i, inventory, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.ENTROPY_CRADLE_CONTROLLER_MENU.get(), player, locator);
    }
}
