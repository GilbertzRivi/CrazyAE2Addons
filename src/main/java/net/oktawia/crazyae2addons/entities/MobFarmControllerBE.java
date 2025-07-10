package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.XpShardItem;
import net.oktawia.crazyae2addons.menus.MobFarmControllerMenu;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;
import net.oktawia.crazyae2addons.misc.MobFarmValidator;
import net.oktawia.crazyae2addons.renderer.preview.Previewable;
import net.oktawia.crazyae2addons.mobstorage.MobKey;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MobFarmControllerBE extends AENetworkBlockEntity implements Previewable, MenuProvider, IUpgradeableObject, IGridTickable, InternalInventoryHost {

    public IUpgradeInventory upgrades = UpgradeInventories.forMachine(CrazyBlockRegistrar.MOB_FARM_CONTROLLER.get(), 5, this::saveChanges);
    public MobFarmValidator validator;
    public Integer damageBlocks = 0;
    public final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 1, 1);
    public final ConfigInventory configInventory = ConfigInventory.configTypes(
            (x) -> x instanceof MobKey || x.wrapForDisplayOrFilter().getItem() instanceof SpawnEggItem,
            3,
            this::saveChanges);
    public FakePlayer fakePlayer;

    public boolean preview = false;

    private PreviewInfo previewInfo = null;

    @Override
    public PreviewInfo getPreviewInfo() {
        return previewInfo;
    }

    @Override
    public void setPreviewInfo(PreviewInfo info) {
        this.previewInfo = info;
    }

    public static final Set<MobFarmControllerBE> CLIENT_INSTANCES = new HashSet<>();

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



    public MobFarmControllerBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.MOB_FARM_CONTROLLER_BE.get(), pos, blockState);
        validator = new MobFarmValidator();
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.MOB_FARM_CONTROLLER.get().asItem())
                );
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("upgrades")) {
            this.upgrades.readFromNBT(data, "upgrades");
        }
        if (data.contains("config")) {
            this.configInventory.readFromChildTag(data, "config");
        }
        if (data.contains("inventory")) {
            this.inventory.readFromNBT(data, "inventory");
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        drops.add(inventory.getStackInSlot(0));
        for (int i = 0; i < upgrades.size(); i++) {
            drops.add(upgrades.getStackInSlot(i));
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        this.upgrades.writeToNBT(data, "upgrades");
        this.configInventory.writeToChildTag(data, "config");
        this.inventory.writeToNBT(data, "inventory");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new MobFarmControllerMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Mob Farm Controller");
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.MOB_FARM_CONTROLLER_MENU.get(), player, locator);
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }
        return super.getSubInventory(id);
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false, false);
    }

    public int getSpeed() {
        if (this.damageBlocks <= 0) {
            return 0;
        }
        int upgradeCount = this.getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD);
        int maxSpeed = switch (upgradeCount) {
            case 1 -> 28;
            case 2 -> 40;
            case 3 -> 52;
            case 4 -> 64;
            default -> 16;
        };
        double ratio = this.damageBlocks / 16.0;
        return (int) Math.round(maxSpeed * ratio);
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        this.saveChanges();
    }

    public record EntityDropResult(List<ItemStack> drops, int experience) {}

    public EntityDropResult getDropsAndExp(ServerLevel level, EntityType<?> type) {
        try {
            Entity entity = type.create(level);
            if (!(entity instanceof LivingEntity livingEntity)) {
                return new EntityDropResult(List.of(), 0);
            } else if (entity instanceof WitherBoss){
                return new EntityDropResult(List.of(Items.NETHER_STAR.getDefaultInstance()), 1500);
            }

            if (this.fakePlayer == null){
                this.fakePlayer = FakePlayerFactory.get(Objects.requireNonNull(level.getServer()).getLevel(level.dimension()),
                        new GameProfile(UUID.randomUUID(), "[CrazyAE2Addons]"));
            }

            ItemStack setDamager = this.inventory.getStackInSlot(0);
            ItemStack damager;
            if (setDamager == null || setDamager.isEmpty()) {
                damager = new ItemStack(Items.STICK);
            } else {
                damager = setDamager.copy();
            }

            int lootingLevel = getInstalledUpgrades(CrazyItemRegistrar.LOOTING_UPGRADE_CARD.get());
            Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(damager));
            int finalLootingLvl = lootingLevel + enchants.getOrDefault(Enchantments.MOB_LOOTING, 0);
            enchants.put(Enchantments.MOB_LOOTING, finalLootingLvl);
            EnchantmentHelper.setEnchantments(enchants, damager);

            int exp = livingEntity.getExperienceReward();

            ResourceLocation lootTableId = type.getDefaultLootTable();
            LootTable lootTable = level.getServer().getLootData().getLootTable(lootTableId);

            LootParams lootParams = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, livingEntity)
                    .withOptionalParameter(LootContextParams.KILLER_ENTITY, this.fakePlayer)
                    .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, this.fakePlayer)
                    .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, this.fakePlayer)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(this.fakePlayer))
                    .withParameter(LootContextParams.TOOL, damager)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(livingEntity.blockPosition()))
                    .create(LootContextParamSets.ENTITY);

            int rolls = 1 + finalLootingLvl;
            Map<Item, Integer> itemCounts = new HashMap<>();

            for (int i = 0; i < rolls; i++) {
                List<ItemStack> drops = lootTable.getRandomItems(lootParams);
                for (ItemStack stack : drops) {
                    if (!stack.isEmpty() && !stack.hasTag() && stack.getMaxStackSize() != 1) {
                        itemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                    }
                }
            }

            List<ItemStack> allDrops = new ArrayList<>();
            for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
                allDrops.add(new ItemStack(entry.getKey(), entry.getValue()));
            }

            return new EntityDropResult(allDrops, exp);

        } catch (Exception e) {
            return new EntityDropResult(List.of(), 0);
        }
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!CrazyConfig.COMMON.enablePeacefullSpawner.get() && getLevel().getDifficulty() == Difficulty.PEACEFUL) return TickRateModulation.IDLE;
        if (!validator.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this)){
            return TickRateModulation.IDLE;
        }

        this.damageBlocks = validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), CrazyBlockRegistrar.MOB_FARM_DAMAGE.get());
        MEStorage inv = getGridNode().getGrid().getStorageService().getInventory();
        IEnergyService eng = getGridNode().getGrid().getEnergyService();
        int speed = Math.max(getSpeed(), 1);
        int i;
        for (i = 0; i < speed;){
            AEKey key = this.configInventory.getKey(i % 3);
            if (key == null){
                i++;
                continue;
            }
            long extracted = StorageHelper.poweredExtraction(eng, inv, key, 1, IActionSource.ofMachine(this), Actionable.MODULATE);
            if (extracted <= 0){
                i++;
                continue;
            }
            EntityType<?> entityType = ((MobKey) key).getEntityType();
            EntityDropResult result = getDropsAndExp(level.getServer().getLevel(level.dimension()), entityType);
            for (ItemStack is : result.drops){
                StorageHelper.poweredInsert(eng, inv, AEItemKey.of(is.getItem()), is.getCount(), IActionSource.ofMachine(this), Actionable.MODULATE);
            }
            StorageHelper.poweredInsert(eng, inv, AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                    Math.max((result.experience * (getInstalledUpgrades(CrazyItemRegistrar.EXPERIENCE_UPGRADE_CARD.get()) + 1) / XpShardItem.XP_VAL), 1), IActionSource.ofMachine(this), Actionable.MODULATE);
            i++;
        }
        return TickRateModulation.IDLE;
    }
}
