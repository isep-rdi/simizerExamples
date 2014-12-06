/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.isep.simizer.example.policy.utils;

import java.util.BitSet;

/**
 *
 * @author isep
 */
public class SBlockBloomFilter<T> {
     static private int SEED = 0x2845;
    public static double log2(double num) {
        return (Math.log(num)/Math.log(2));
    }
    
   // public static int MAX_K = 9;
   // public static int BLOCK_SIZE = 256;
    
    private SBlockBloomFilter.Block[] blocks;
    private long  filterCapacity;
    private int k,blkSz;
    double wantedRate, actualRate;
    private final T data;
    
    
    public SBlockBloomFilter(int capacity, double wantedRate, T data) {
          this.filterCapacity = capacity; // n
        this.wantedRate = wantedRate;
        this.data = data;
        //log2(1/fpRate)
        this.k = (int) Math.floor(1.0/log2(wantedRate));
        // capacity/(-log(1/2))
        this.blkSz = (int) Math.floor(capacity / -Math.log(0.5)) ;
        //System.out.println("tmpk " + tmpK);
        this.filterCapacity = this.k * blkSz;
        this.actualRate = wantedRate;
        this.blocks = new SBlockBloomFilter.Block[k];
        
        for(int i=0; i < k; i++) {
            this.blocks[i] = new SBlockBloomFilter.Block(blkSz, this);
        }
    }
    public T getData() {
        return data;
    }
    public int getBlockSz() {
        return blkSz;
    }
    public boolean lookup(int[] data) {
        boolean result = true;
        int i = 0;
        do {
            result = blocks[i].get(data[i]);
            i++;
        } while(result && i< blocks.length );
        
        return result;
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
    
    public static int[] fastHash(String data, int k, int blkSz) {
        int[] result = new int[k];
        
        for(int i=0; i<k;i++) {
           
            result[i] =  (int) Math.abs( 
                    MurmurHash3.murmurhash3_x86_32(data.concat(":"+i), 0, data.length(), SEED)
                    % blkSz);
        }
        return result;
    }
    
    public static class Block {
        private SBlockBloomFilter filter;
        private BitSet buckets;
        private int capacity;


        public Block(int capacity, SBlockBloomFilter filter) {
            this.filter = filter;
            this.capacity = capacity;
            this.buckets  = new BitSet(capacity);
            
        }

        public void set(int pos) {
            buckets.set(pos);
            
        }

        public boolean get(int pos) {
            return buckets.get(pos);
        }
        public SBlockBloomFilter getFilter() {
            return filter;
        }
        }
    
    
}
