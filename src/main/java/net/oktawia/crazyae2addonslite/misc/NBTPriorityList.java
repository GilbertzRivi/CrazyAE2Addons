package net.oktawia.crazyae2addonslite.misc;

import appeng.api.config.IncludeExclude;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.util.prioritylist.IPartitionList;

import java.util.Collections;

public class NBTPriorityList implements IPartitionList {

    private final String criteria;

    public NBTPriorityList(String criteria) {
        this.criteria = criteria == null ? "" : criteria;
    }

    @Override
    public boolean isListed(AEKey key) {
        return matchesFilter(key, IncludeExclude.WHITELIST);
    }

    @Override
    public boolean isEmpty() {
        return criteria.isEmpty();
    }

    @Override
    public Iterable<AEKey> getItems() {
        return Collections.emptyList();
    }

    @Override
    public boolean matchesFilter(AEKey key, IncludeExclude mode) {
        if (key instanceof AEItemKey ik) {
            return NBTMatcher.doesItemMatch(ik, criteria);
        }
        return false;
    }
}
