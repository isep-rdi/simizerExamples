/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.isep.simizer.example.policy.utils;

import java.util.Arrays;
import java.util.BitSet;

/**
 *
 * @author Sylvain Lefebvre
 * Dynamic bloom filter implementation 
 */
public class BlockBloomFilter<T> {
    static private int SEED = 0x1345;
    public static double log2(double num) {
        return (Math.log(num)/Math.log(2));
    }
    
   // public static int MAX_K = 9;
   // public static int BLOCK_SIZE = 256;
    
    private Block[] blocks;
    private long  filterCapacity;
    private int k,blkSz;
    double wantedRate, actualRate;
    private final T data;
    
    
    public BlockBloomFilter(int capacity, double wantedRate, T data) {
          this.filterCapacity = capacity; // n
        this.wantedRate = wantedRate;
        this.data = data;
        //log2(1/fpRate)
        this.k = (int) Math.floor(log2(1.0/wantedRate));
        // capacity/(-log(1/2))
        this.blkSz = (int) Math.floor(capacity / -Math.log(0.5)) ;
        //System.out.println("tmpk " + tmpK);
        this.filterCapacity = this.k * blkSz;
        this.actualRate = wantedRate;
        this.blocks = new Block[k];
        //System.out.println("Capacity :" + capacity + " Size " + blkSz);
        for(int i=0; i < k; i++) {
            this.blocks[i] = new Block(blkSz, this);
        }
    }
    public T getData() {
        return data;
    }
    public int getBlockSz() {
        return blkSz;
    }
    public int getK() {
        return k;
    }
    public boolean lookup(int[] data) {
        return lookup(data,0);
    }
    
    public boolean lookup(int[] data, int stIndex) {
         boolean result = true;
        int i = stIndex;
        do {
            result = blocks[i].get(data[i]);
            i++;
        } while(result && i< blocks.length );
        
        return result;
    }
    public boolean lookup(int data, int index) {
        return blocks[index].get(data);
    }
    
    public void insert(int[] data) {
        for(int i=0;i<blocks.length;i++) {
            //System.out.println("test:" + i + "," + data[i]);
             blocks[i].set(data[i]);
        }
           
        
    }
    public double getFalsePositiveRate() {
        return actualRate;
    }
    
    public static int[] fastHash(String entry, int k) {
        int[] result = new int[k];
       
        for(int i=0; i<k;i++) {
            String dt = entry.concat(":" + i); 
            result[i] =  (int) Math.abs( 
                     MurmurHash3.murmurhash3_x86_32(dt, 0, dt.length(), SEED)
                    );
        }
        //System.out.println(Arrays.toString(result));
        return result;
    }
    
    public static class Block {
        private BlockBloomFilter filter;
        private BitSet buckets;
        private int capacity;


        public Block(int capacity, BlockBloomFilter filter) {
            this.filter = filter;
            this.capacity = capacity;
            this.buckets  = new BitSet(capacity);
            
        }

        public void set(int pos) {
            buckets.set(pos % capacity);
            
        }

        public boolean get(int pos) {
            return buckets.get(pos % capacity);
        }
        public BlockBloomFilter getFilter() {
            return filter;
        }
        }
    
    
}

