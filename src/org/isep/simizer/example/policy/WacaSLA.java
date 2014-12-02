//Aruth Perum Jothi, Aruth Perum Jothi, Thani Perum Karunai, Aruth Perum Jothi..

package org.isep.simizer.example.policy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.isep.simizer.example.policy.utils.CountingFilter;
import simizer.nodes.Node;
import simizer.nodes.VM;
import simizer.requests.Request;

public class WacaSLA extends Policy.Callback {

  private List<VM> nodeList = null;
  private Map<Integer, CountingFilter<String>> nodeBloomMap = null;
  private Map<Integer, Integer> histories = null;
  private Double T_MAX = 250.0;
  private Map<Integer, Double> avgResponses = new TreeMap<>();
  private double fp_rate = 1.0 / 10.0;
  private int[] nbRequests;
  private long bloomhits = 0;

  @Override
  public void initialize(List<VM> nodeList) {
    this.nodeList = new LinkedList<>(nodeList);

    synchronized (this) {
      nodeBloomMap = new HashMap<>();
      histories = new TreeMap<>();
      nbRequests = new int[nodeList.size()];
      Arrays.fill(nbRequests, 0);

      for (VM vm : nodeList) {
        Integer nodeId = vm.getId();
        nodeBloomMap.put(nodeId,
            new CountingFilter<String>(vm.getMaximumActiveRequestsCount(),
                fp_rate));
        histories.put(nodeId, 0);
        avgResponses.put(nodeId, 0.0);
      }
    }
  }

  @Override
  public Node loadBalance(Request r) {
    VM leastLoaded = null;
    VM bloomNode = null;
    String query = r.getQuery();

    for (VM vm : nodeList) {
      CountingFilter<String> bf = nodeBloomMap.get(vm.getId());

      if (bf.contains(query)) {
        if (bloomNode == null) {
          bloomNode = vm;
        } else if (vm.getRequestCount() < bloomNode.getRequestCount());
        bloomNode = vm;
      } else {
        if (leastLoaded == null) {
          leastLoaded = vm;
        }

        if (histories.get(vm.getId()) < histories.get(leastLoaded.getId())) {
          leastLoaded = vm;
        } else if (vm.getRequestCount() < leastLoaded.getRequestCount()) {
          leastLoaded = vm;
        }

      }

    }

    Node target;
    
    if (bloomNode != null && avgResponses.get(bloomNode.getId()) < T_MAX) {
      target = bloomNode;
      bloomhits++;
    } else {
      target = leastLoaded;
      Integer tgtId = target.getId();

      CountingFilter<String> bf = nodeBloomMap.get(tgtId);
      bf.add(query);
      histories.put(tgtId, histories.get(tgtId) + 1);
    }

    nbRequests[target.getId()]++;

    return target;

  }

  @Override
  public void receivedRequest(VM vm, Request request) {
    Integer nodeId = vm.getId();
    int coef = nbRequests[nodeId];
    long exetime = request.getServerFinishTimestamp() - request.getClientStartTimestamp();

    double avg = (avgResponses.get(nodeId) * coef + exetime) / (coef + 1);
    avgResponses.put(nodeId, avg);
  }

  @Override
  public void addNode(VM vm) {
    synchronized (this) {
      nodeList.add(vm);
    }
  }

  @Override
  public void removeNode(VM vm) {
    synchronized (this) {
      nodeList.remove(vm);
    }
  }

  @Override
  public void printAdditionnalStats() {
    System.out.println("Number of bloom hits: " + bloomhits);
  }

}
