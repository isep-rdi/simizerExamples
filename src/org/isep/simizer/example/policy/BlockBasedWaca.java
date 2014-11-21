/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.isep.simizer.example.policy;

import java.util.*;
import simizer.LBNode;
import simizer.Node;
import simizer.ServerNode;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.BlockBloomFilter;
import org.isep.simizer.example.policy.utils.Fiboheap;
import org.isep.simizer.example.policy.utils.Fiboheap.FNode;

/**
 *
 * @author isep
 */
public class BlockBasedWaca implements Policy, PolicyAfterCallback{
    static long blockhits, bloomhits;
    private static int LOAD_FACTOR = 1;
    public int[] hash(String params, int k) {
        return BlockBloomFilter.fastHash(params, k);
    }

    private int counter=0;
    protected int kMax=0,mMax=0;
    protected static double FP_RATE= 0.001;
    protected Map<Integer,FNode<BlockBloomFilter<ServerNode>>> nodeTab 
            = new HashMap<Integer, FNode<BlockBloomFilter<ServerNode>>>();
            
    protected LinkedList[] firstBlocks;// = new LinkedList[];
    protected Fiboheap<BlockBloomFilter<ServerNode>> heap = new Fiboheap<BlockBloomFilter<ServerNode>>();
    private long currentTime;
    private boolean initialized = false;
    
   
    @Override
     public void initialize(List<ServerNode> nodeList, LBNode lbn) {
        lbn.registerAfter(this);
        synchronized(this) {
        
        //Création des filtres
        //Cherche le nombre maximum de hash
            
        for(ServerNode n: nodeList) {
            
            BlockBloomFilter bbf = new BlockBloomFilter<Node>((int) Math.floor(n.getCapacity()),FP_RATE,n);
            
            if(kMax < bbf.getK()) kMax = bbf.getK();
            if(mMax < bbf.getBlockSz()) mMax = bbf.getBlockSz();
            
            nodeTab.put(n.getId(), 
                    heap.insert(bbf
                    , 0.0));
         
        }
        
        firstBlocks = new LinkedList[mMax];
        
        initialized =true;
       }
    }

    @Override
    public Node loadBalance(Request r) {
        
       
        currentTime = r.getArTime();
        BlockBloomFilter<ServerNode> targetBf= null;
        ServerNode target = null;
       //FNode<BlockBloomFilter<Node>> targetFiboNode = null;
        
        //1. Hash request 
        int[] hash = hash(r.getParameters(),kMax);
        int firstB = hash[0] % mMax;
        
        //2. Check if the first Block is indexed
        if(firstBlocks[firstB] == null) 
            firstBlocks[firstB] = new LinkedList<BlockBloomFilter<ServerNode>>();
        
        int groupSz = firstBlocks[firstB].size();
        
        if(groupSz > 0) {
        //2.1 If the block is indexed lookup the filter
            blockhits++;
           Iterator<BlockBloomFilter<ServerNode>> it = firstBlocks[firstB].iterator();

           while(it.hasNext()) {
                
               BlockBloomFilter<ServerNode> bf = it.next();

               //2.1.1 filter is ok, check load and send
               boolean lookup = bf.lookup(hash,1)
                       , loadChk = checkLoad(bf.getData());           
               
               if(lookup && loadChk) {
                   //it.remove();
                   target = bf.getData();
                   updateHeap(target,target.getRequestCount() +1);
                   bloomhits++;
                   return target;
                   
               } else if(loadChk) {
                   
                   target = getLeastLoaded(target, bf.getData());
               }
            }
        }
        // Si on a pas fait de hit on prend le moins chargé de la liste
        // et on met son filtre à jour
        if(target!=null) { 
            nodeTab.get(target.getId()).getData().insert(hash);
            updateHeap(target,target.getRequestCount() +1.0);
            return target;
        }
               
        //2.3 send to least loaded, and insert in the right position of the first block
         
        FNode<BlockBloomFilter<ServerNode>> tgtFnode = heap.min();      
        targetBf =tgtFnode.getData();
        if(!firstBlocks[firstB].contains(targetBf))  
            firstBlocks[firstB].addFirst(targetBf);
        
        targetBf.insert(hash);
        target = targetBf.getData();
        updateHeap(target, target.getRequestCount()+1.0);
        
        

        return target;
    }

    @Override
    public void receivedRequest(Node n, Request r) {
            currentTime = r.getFtime();
            FNode<BlockBloomFilter<ServerNode>> fiboNode = nodeTab.get(n.getId());
            heap.decreaseKey(fiboNode, fiboNode.getKey() - 1.0);
     }

    private boolean checkLoad(ServerNode data) {
        return data.getRequestCount() < data.getNbCores()*LOAD_FACTOR;      
    }
    private void updateHeap(ServerNode n, double key) {
        FNode<BlockBloomFilter<ServerNode>> fn = nodeTab.get(n.getId());
        
        if(key < 0) {
            heap.decreaseKey(fn, fn.getKey()+key);
         } else if(key>0) { // increase key
            heap.delete(fn);
            nodeTab.put(n.getId(), heap.insert(fn.getData(), n.getRequestCount() + key));
        }
    }

    private ServerNode getLeastLoaded(ServerNode target, ServerNode data) {
        if(target==null) {
            return data;
        } else if(data==null) {
            return target;
        }
       return target.getRequestCount() < data.getRequestCount() ? target : data;
    }

    private synchronized FNode updateCounter(FNode<BlockBloomFilter<ServerNode>> fiboNode, ServerNode data) {
        
       BlockBloomFilter<ServerNode> bf = fiboNode.getData();
      
       double tmpKey = fiboNode.getKey(); 
       heap.delete(fiboNode);
       fiboNode = heap.insert(bf, tmpKey + 1.0);
       nodeTab.put(data.getId(), fiboNode);
        
        return(fiboNode);
    }

    @Override
    public void printAdditionnalStats() {
       System.out.println("Number of block hits: "+blockhits );
       System.out.println("Number of bloom hits: "+bloomhits );
       
       for(int i=0;i<firstBlocks.length;i++) {
           if(firstBlocks[i]!=null) {
            System.out.println(i + ";" +firstBlocks[i].size() );
           }
       }
    }

    @Override
    public void addNode(Node n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeNode(Node n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


     
}
