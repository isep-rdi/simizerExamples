/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is GraphMaker. The Initial Developer of the Original
 * Software is Nathan L. Fiedler. Portions created by Nathan L. Fiedler
 * are Copyright (C) 1999-2008. All Rights Reserved.
 *
 * Contributor(s): Nathan L. Fiedler.
 *
 * $Id$
 */
package org.isep.simizer.example.policy.utils;

/**
 * This class implements a Fibonacci heap data structure. Much of the
 * code in this class is based on the algorithms in Chapter 21 of the
 * "Introduction to Algorithms" by Cormen, Leiserson, Rivest, and Stein.
 * The amortized running time of most of these methods is O(1), making
 * it a very fast data structure. Several have an actual running time
 * of O(1). removeMin() and delete() have O(log n) amortized running
 * times because they do the heap consolidation.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a set concurrently, and at least one of the
 * threads modifies the set, it <em>must</em> be synchronized externally.
 * This is typically accomplished by synchronizing on some object that
 * naturally encapsulates the set.</p>
 *
 * @author  Nathan Fiedler
 * 
 * 
 * 
 */
public class Fiboheap<T> {
    
    
    public void printForest (StringBuffer sb) {
        if (min != null)
            min.printSubtree(sb);
    }
    /** Points to the minimum FNode in the heap. */
    private FNode<T> min;
    /** Number of FNodes in the heap. If the type is ever widened,
     * (e.g. changed to long) then recalculate the maximum degree
     * value used in the consolidate() method. */
    private int n;

    /**
     * Removes all elements from this heap.
     *
     * <p><em>Running time: O(1)</em></p>
     */
    public void clear() {
        min = null;
        n = 0;
    }

    /**
     * Consolidates the trees in the heap by joining trees of equal
     * degree until there are no more trees of equal degree in the
     * root list.
     *
     * <p><em>Running time: O(log n) amortized</em></p>
     */
    private void consolidate() {
        //System.out.println("Consolidate");
        // The magic 45 comes from log base phi of Integer.MAX_VALUE,
        // which is the most elements we will ever hold, and log base
        // phi represents the largest degree of any root list FNode.
        FNode<T>[] A = new FNode[45];

        // For each root list FNode look for others of the same degree.
        FNode<T> start = min;
        FNode<T> w = min;
        do {
            FNode<T> x = w;
            // Because x might be moved, save its sibling now.
            FNode<T> nextW = w.right;
            int d = x.degree;
            while (A[d] != null) {
                // Make one of the FNodes a child of the other.
                FNode<T> y = A[d];
                if (x.key > y.key) {
                    FNode temp = y;
                    y = x;
                    x = temp;
                }
                if (y == start) {
                    // Because removeMin() arbitrarily assigned the min
                    // reference, we have to ensure we do not miss the
                    // end of the root FNode list.
                    start = start.right;
                }
                if (y == nextW) {
                    // If we wrapped around we need to check for this case.
                    nextW = nextW.right;
                }
                // FNode y disappears from root list.
                y.link(x);
                // We've handled this degree, go to next one.
                A[d] = null;
                d++;
            }
            // Save this FNode for later when we might encounter another
            // of the same degree.
            A[d] = x;
            // Move forward through list.
            w = nextW;
        } while (w != start);

        // The FNode considered to be min may have been changed above.
        min = start;
        // Find the minimum key again.
        for (FNode a : A) {
            if (a != null && a.key < min.key) {
                min = a;
            }
        }
    }

    /**
     * Decreases the key value for a heap FNode, given the new value
     * to take on. The structure of the heap may be changed, but will
     * not be consolidated.
     *
     * <p><em>Running time: O(1) amortized</em></p>
     *
     * @param  x  FNode to decrease the key of
     * @param  k  new key value for FNode x
     * @exception  IllegalArgumentException
     *             if k is larger than x.key value.
     */
    public void decreaseKey(FNode<T> x, double k) {
        decreaseKey(x, k, false);
    }

    /**
     * Decrease the key value of a FNode, or simply bubble it up to the
     * top of the heap in preparation for a delete operation.
     *
     * @param  x       FNode to decrease the key of.
     * @param  k       new key value for FNode x.
     * @param  delete  true if deleting FNode (in which case, k is ignored).
     */
    private void decreaseKey(FNode<T> x, double k, boolean delete) {
        if (!delete && k > x.key) {
           throw new IllegalArgumentException("cannot increase key value");
           
        }
        if (!delete && k < 0.0) { 
            //throw new IllegalArgumentException("0.0 is absolute minimum");
             x.key = 0.0;
        }
        x.key = k;
        FNode y = x.parent;
        if (y != null && (delete || k < y.key)) {
            
            y.cut(x, min);
            y.cascadingCut(min);
        }
        if (delete || k < min.key) {
            min = x;
        }
    }

    /**
     * Deletes a FNode from the heap given the reference to the FNode.
     * The trees in the heap will be consolidated, if necessary.
     *
     * <p><em>Running time: O(log n) amortized</em></p>
     *
     * @param  x  FNode to remove from heap.
     */
    public void delete(FNode<T> x) {
        // make x as small as possible
       // System.out.println("removing : " + x.key);
        decreaseKey(x, 0.0, true);
        // remove the smallest, which decreases n also
        removeMin();
    }

    /**
     * Tests if the Fibonacci heap is empty or not. Returns true if
     * the heap is empty, false otherwise.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @return  true if the heap is empty, false otherwise.
     */
    public boolean isEmpty() {
        return min == null;
    }

    /**
     * Inserts a new data element into the heap. No heap consolidation
     * is performed at this time, the new FNode is simply inserted into
     * the root list of this heap.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @param  x    data object to insert into heap.
     * @param  key  key value associated with data object.
     * @return newly created heap FNode.
     */
    public FNode<T> insert(T x, double key) {
        
        FNode FNode = new FNode(x, key);
        // concatenate FNode into min list
        if (min != null) {
            FNode.right = min;
            FNode.left = min.left;
            min.left = FNode;
            FNode.left.right = FNode;
            if (key < min.key) {
                min = FNode;
            }
        } else {
            min = FNode;
        }
        n++;
        return FNode;
    }

    /**
     * Returns the smallest element in the heap. This smallest element
     * is the one with the minimum key value.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @return  heap FNode with the smallest key, or null if empty.
     */
    public FNode min() {
        return min;
    }

    /**
     * Removes the smallest element from the heap. This will cause
     * the trees in the heap to be consolidated, if necessary.
     *
     * <p><em>Running time: O(log n) amortized</em></p>
     *
     * @return  data object with the smallest key.
     */
    public T removeMin() {
        //System.out.println("remove min" + min.key);
        FNode<T> z = min;
        if (z == null) {
            return null;
        }
        if (z.child != null) {
            z.child.parent = null;
            // for each child of z do...
            //System.out.append("z has :" + z.degree);
            
            for (FNode x = z.child.right; x != z.child ; x = x.right) {
                //set parent[x] to null:
          //     System.out.println("lol");
                x.parent = null;
            
            }
            // merge the children into root list
            FNode minleft = min.left;
            FNode zchildleft = z.child.left;
            min.left = zchildleft;
            zchildleft.right = min;
            z.child.left = minleft;
            minleft.right = z.child;
        }
        // remove z from root list of heap
        z.left.right = z.right;
        z.right.left = z.left;
        if (z == z.right) {
            min = null;
        } else {
            min = z.right;
            consolidate();
        }
        // decrement size of heap
        n--;
        return z.data;
    }

    /**
     * Returns the size of the heap which is measured in the
     * number of elements contained in the heap.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @return  number of elements in the heap.
     */
    public int size() {
        return n;
    }

    /**
     * Joins two Fibonacci heaps into a new one. No heap consolidation is
     * performed at this time. The two root lists are simply joined together.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @param  H1  first heap
     * @param  H2  second heap
     * @return  new heap containing H1 and H2
     */
    public static Fiboheap union(Fiboheap H1, Fiboheap H2) {
        Fiboheap H = new Fiboheap();
        if (H1 != null && H2 != null) {
            H.min = H1.min;
            if (H.min != null) {
                if (H2.min != null) {
                    H.min.right.left = H2.min.left;
                    H2.min.left.right = H.min.right;
                    H.min.right = H2.min;
                    H2.min.left = H.min;
                    if (H2.min.key < H1.min.key) {
                        H.min = H2.min;
                    }
                }
            } else {
                H.min = H2.min;
            }
            H.n = H1.n + H2.n;
        }
        return H;
    }

    /**
     * Implements a FNode of the Fibonacci heap. It holds the information
     * necessary for maintaining the structure of the heap. It acts as
     * an opaque handle for the data element, and serves as the key to
     * retrieving the data from the heap.
     *
     * @author  Nathan Fiedler Modified for T by S.LEFEBVRE
     */
    public static class FNode<T> {
        /** Data object for this FNode, holds the key value. */
        private T data;
        /** Key value for this FNode. */
        private double key;
        /** Parent FNode. */
        private FNode parent;
        /** First child FNode. */
        private FNode child;
        /** Right sibling FNode. */
        private FNode right;
        /** Left sibling FNode. */
        private FNode left;
        /** Number of children of this FNode. */
        private int degree;
        /** True if this FNode has had a child removed since this FNode was
         * added to its parent. */
        private boolean mark;

        /**
         * Two-arg constructor which sets the data and key fields to the
         * passed arguments. It also initializes the right and left pointers,
         * making this a circular doubly-linked list.
         *
         * @param  data  data object to associate with this FNode
         * @param  key   key value for this data object
         */
        public FNode(T data, double key) {
            this.data = data;
            this.key = key;
            right = this;
            left = this;
        }
        
        public T getData() {
            return data;
        }
        /**
         * Performs a cascading cut operation. Cuts this from its parent
         * and then does the same for its parent, and so on up the tree.
         *
         * <p><em>Running time: O(log n)</em></p>
         *
         * @param  min  the minimum heap FNode, to which FNodes will be added.
         */
        public void cascadingCut(FNode min) {
            FNode z = parent;
            // if there's a parent...
            if (z != null) {
                if (mark) {
                    // it's marked, cut it from parent
                    z.cut(this, min);
                    // cut its parent as well
                    z.cascadingCut(min);
                } else {
                    // if y is unmarked, set it marked
                    mark = true;
                }
            }
        }

        /**
         * The reverse of the link operation: removes x from the child
         * list of this FNode.
         *
         * <p><em>Running time: O(1)</em></p>
         *
         * @param  x    child to be removed from this FNode's child list
         * @param  min  the minimum heap FNode, to which x is added.
         */
        public void cut(FNode x, FNode min) {
            // remove x from childlist and decrement degree
            x.left.right = x.right;
            x.right.left = x.left;
            degree--;
            // reset child if necessary
            if (degree == 0) {
                child = null;
            } else if (child == x) {
                child = x.right;
            }
            // add x to root list of heap
            x.right = min;
            x.left = min.left;
            min.left = x;
            x.left.right = x;
            // set parent[x] to nil
            x.parent = null;
            // set mark[x] to false
            x.mark = false;
        }

        /**
         * Make this FNode a child of the given parent FNode. All linkages
         * are updated, the degree of the parent is incremented, and
         * mark is set to false.
         *
         * @param  parent  the new parent FNode.
         */
        public void link(FNode parent) {
            // Note: putting this code here in FNode makes it 7x faster
            // because it doesn't have to use generated accessor methods,
            // which add a lot of time when called millions of times.
            // remove this from its circular list
            left.right = right;
            right.left = left;
            // make this a child of x
            this.parent = parent;
            if (parent.child == null) {
                parent.child = this;
                right = this;
                left = this;
            } else {
                left = parent.child;
                right = parent.child.right;
                parent.child.right = this;
                right.left = this;
            }
            // increase degree[x]
            parent.degree++;
            // set mark false
            mark = false;
        }

        public double getKey() {
            return this.key;
        }
        public void printSubtree (StringBuffer sb) {
            FNode w = this;
            do {
            System.out.print(w.key);
            if (w.parent != null)
                System.out.print(" (" +w.parent.key + ")");
            
            System.out.print("\n");

            if (w.child != null)
                w.child.printSubtree(sb);
            
            w = w.right;
            } while (w != this);
        }
    }
}

