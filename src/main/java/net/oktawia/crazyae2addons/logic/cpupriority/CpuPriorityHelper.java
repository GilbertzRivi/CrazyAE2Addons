package net.oktawia.crazyae2addons.logic.cpupriority;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.menu.me.crafting.CraftingStatusMenu.CraftingCpuListEntry;
import lombok.experimental.UtilityClass;
import net.oktawia.crazyae2addons.logic.interfaces.ICpuPrio;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@UtilityClass
public class CpuPriorityHelper {

    public int getCpuPriority(ICraftingCPU cpu) {
        if (cpu instanceof ICpuPrio prio) {
            return prio.getPrio();
        }
        return 0;
    }

    public int getClusterPriority(CraftingCPUCluster cluster) {
        if ((Object) cluster instanceof ICpuPrio prio) {
            return prio.getPrio();
        }
        return 0;
    }

    public int getEntryPriority(CraftingCpuListEntry entry) {
        if ((Object) entry instanceof ICpuPrio prio) {
            return prio.getPrio();
        }
        return 0;
    }

    public Comparator<ICraftingCPU> cpuComparator() {
        return Comparator
                .comparingInt(CpuPriorityHelper::getCpuPriority)
                .reversed()
                .thenComparingInt(System::identityHashCode);
    }

    public Comparator<CraftingCPUCluster> clusterComparator() {
        return Comparator
                .comparingInt(CpuPriorityHelper::getClusterPriority)
                .reversed()
                .thenComparingInt(System::identityHashCode);
    }

    public Comparator<CraftingCPUCluster> clusterComparatorAscending() {
        return Comparator
                .comparingInt(CpuPriorityHelper::getClusterPriority)
                .thenComparingInt(System::identityHashCode);
    }

    public Comparator<CraftingCpuListEntry> entryComparator() {
        return Comparator
                .comparingInt(CpuPriorityHelper::getEntryPriority)
                .reversed()
                .thenComparing(entry -> {
                    var name = entry.name();
                    return name == null ? "CPU" : name.getString();
                }, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(CraftingCpuListEntry::serial);
    }

    public Comparator<CraftingCPUCluster> extendFastFirstComparator(Comparator<CraftingCPUCluster> base) {
        return Comparator
                .comparingInt(CpuPriorityHelper::getClusterPriority)
                .reversed()
                .thenComparing(base)
                .thenComparingInt(System::identityHashCode);
    }

    public Comparator<CraftingCPUCluster> extendFastLastComparator(Comparator<CraftingCPUCluster> base) {
        return Comparator
                .comparingInt(CpuPriorityHelper::getClusterPriority)
                .thenComparing(base)
                .thenComparingInt(System::identityHashCode);
    }

    public List<ICraftingCPU> sortCpus(Collection<? extends ICraftingCPU> cpus) {
        return cpus.stream()
                .sorted(cpuComparator())
                .map(cpu -> (ICraftingCPU) cpu)
                .toList();
    }

    public List<CraftingCPUCluster> sortClusters(Collection<CraftingCPUCluster> clusters) {
        return clusters.stream()
                .sorted(clusterComparator())
                .toList();
    }

    public Iterator<CraftingCPUCluster> sortedClusterIterator(Collection<CraftingCPUCluster> clusters) {
        return sortClusters(clusters).iterator();
    }

    public List<CraftingCpuListEntry> sortEntries(List<CraftingCpuListEntry> entries) {
        return entries.stream()
                .sorted(entryComparator())
                .toList();
    }
}