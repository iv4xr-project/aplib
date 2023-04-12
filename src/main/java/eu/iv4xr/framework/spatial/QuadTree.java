package eu.iv4xr.framework.spatial;

import java.util.*;

// TODO: everything is still WIP

/**
 * WIP: A quad tree structure for quick 2D access.
 * 
 * @param <T> The type of the contained objects.
 * 
 * @author Naraenda
 */
public abstract class QuadTree<T> {
    public boolean isLeaf;
}

class QuadTreeLeaf<T> extends QuadTree<T> {
    public T item;

    public QuadTreeLeaf(T item) {
        this.isLeaf = true;
        this.item = item;
    }
}

enum QuadTreeSections {
    NW, NE, SW, SE
}

class QuadTreeBranch<T> extends QuadTree<T> {

    public Map<QuadTreeSections, QuadTree<T>> branches;

    public QuadTreeBranch() {
        this.isLeaf = false;
        this.branches = new HashMap<QuadTreeSections, QuadTree<T>>(0);
    }

    public void put(QuadTreeSections key, QuadTree<T> tree) {
        this.branches.put(key, tree);
    }

    public void remove(QuadTreeSections key) {
        this.branches.remove(key);
    }
}
