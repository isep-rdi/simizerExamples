package org.isep.simizer.example.policy;

import java.util.*;
import simizer.nodes.Node;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.BlockBloomFilter;
import org.isep.simizer.example.policy.utils.Fiboheap;
import org.isep.simizer.example.policy.utils.Fiboheap.FNode;
import simizer.nodes.VM;

public class BlockBasedWaca extends Policy.Callback {

  static long blockhits, bloomhits;
  private static int LOAD_FACTOR = 1;

  public int[] hash(String params, int k) {
    return BlockBloomFilter.fastHash(params, k);
  }

  protected int kMax = 0, mMax = 0;
  protected static double FP_RATE = 0.001;
  protected Map<Integer, FNode<BlockBloomFilter<VM>>> nodeTab
      = new HashMap<>();

  protected LinkedList[] firstBlocks;
  protected Fiboheap<BlockBloomFilter<VM>> heap = new Fiboheap<>();

  @Override
  public void initialize(List<VM> nodeList) {
    synchronized (this) {
      //Création des filtres
      //Cherche le nombre maximum de hash
      for (VM vm : nodeList) {
        BlockBloomFilter bbf = new BlockBloomFilter<>(
            (int) Math.floor(vm.getMaximumActiveRequestsCount()), FP_RATE, vm);

        if (kMax < bbf.getK()) {
          kMax = bbf.getK();
        }
        if (mMax < bbf.getBlockSz()) {
          mMax = bbf.getBlockSz();
        }

        nodeTab.put(vm.getId(), heap.insert(bbf, 0.0));
      }

      firstBlocks = new LinkedList[mMax];
    }
  }

  @Override
  public Node loadBalance(Request request) {
    BlockBloomFilter<VM> targetBf = null;
    VM target = null;

    // 1. Hash request
    int[] hash = hash(request.getQuery(), kMax);
    int firstB = hash[0] % mMax;

    // 2. Check if the first Block is indexed
    if (firstBlocks[firstB] == null) {
      firstBlocks[firstB] = new LinkedList<>();
    }

    int groupSz = firstBlocks[firstB].size();

    if (groupSz > 0) {
      //2.1 If the block is indexed lookup the filter
      blockhits++;
      Iterator<BlockBloomFilter<VM>> it = firstBlocks[firstB].iterator();

      while (it.hasNext()) {
        BlockBloomFilter<VM> bf = it.next();

        //2.1.1 filter is ok, check load and send
        boolean lookup = bf.lookup(hash, 1);
        boolean loadChk = checkLoad(bf.getData());

        if (lookup && loadChk) {
          target = bf.getData();
          updateHeap(target, target.getRequestCount() + 1);
          bloomhits++;
          return target;

        } else if (loadChk) {
          target = getLeastLoaded(target, bf.getData());
        }
      }
    }
    
    // Si on a pas fait de hit on prend le moins chargé de la liste
    // et on met son filtre à jour
    if (target != null) {
      nodeTab.get(target.getId()).getData().insert(hash);
      updateHeap(target, target.getRequestCount() + 1.0);
      return target;
    }

    //2.3 send to least loaded, and insert in the right position of the first block
    FNode<BlockBloomFilter<VM>> tgtFnode = heap.min();
    targetBf = tgtFnode.getData();
    if (!firstBlocks[firstB].contains(targetBf)) {
      firstBlocks[firstB].addFirst(targetBf);
    }

    targetBf.insert(hash);
    target = targetBf.getData();
    updateHeap(target, target.getRequestCount() + 1.0);

    return target;
  }

  @Override
  public void receivedRequest(VM vm, Request request) {
    FNode<BlockBloomFilter<VM>> fiboNode = nodeTab.get(vm.getId());
    heap.decreaseKey(fiboNode, fiboNode.getKey() - 1.0);
  }

  private boolean checkLoad(VM data) {
    return data.getRequestCount() < data.getProcessingUnit().getNbCores() * LOAD_FACTOR;
  }

  private void updateHeap(VM vm, double key) {
    FNode<BlockBloomFilter<VM>> fn = nodeTab.get(vm.getId());

    if (key < 0) {
      heap.decreaseKey(fn, fn.getKey() + key);
    } else if (key > 0) { // increase key
      heap.delete(fn);
      nodeTab.put(vm.getId(), heap.insert(fn.getData(), vm.getRequestCount() + key));
    }
  }

  private VM getLeastLoaded(VM target, VM data) {
    if (target == null) {
      return data;
    } else if (data == null) {
      return target;
    }
    return target.getRequestCount() < data.getRequestCount() ? target : data;
  }

  @Override
  public void addNode(VM vm) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void removeNode(VM vm) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void printAdditionnalStats() {
    System.out.println("Number of block hits: " + blockhits);
    System.out.println("Number of bloom hits: " + bloomhits);

    for (int i = 0; i < firstBlocks.length; i++) {
      if (firstBlocks[i] != null) {
        System.out.println(i + ";" + firstBlocks[i].size());
      }
    }
  }

}
