package net.oktawia.crazyae2addons.logic.viewcell;

import appeng.api.config.IncludeExclude;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.util.prioritylist.IPartitionList;
import net.oktawia.crazyae2addons.util.TagMatcher;

import java.util.Collections;

public class TagPriorityList implements IPartitionList {

    private final String criteria;

    public TagPriorityList(String criteria) {
        this.criteria = criteria == null ? "" : criteria.trim();
    }

    @Override
    public boolean isListed(AEKey key) {
        return matchesFilter(key, IncludeExclude.WHITELIST);
    }

    @Override
    public boolean isEmpty() {
        return this.criteria.isBlank();
    }

    @Override
    public Iterable<AEKey> getItems() {
        return Collections.emptyList();
    }

    @Override
    public boolean matchesFilter(AEKey key, IncludeExclude mode) {
        if (key instanceof AEItemKey itemKey) {
            return TagMatcher.doesItemMatch(itemKey, this.criteria);
        }

        return false;
    }
}