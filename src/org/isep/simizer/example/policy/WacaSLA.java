//Aruth Perum Jothi, Aruth Perum Jothi, Thani Perum Karunai, Aruth Perum Jothi..

package org.isep.simizer.example.policy;

import java.util.*;
import simizer.LBNode;
import simizer.Node;
import simizer.ServerNode;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.CountingFilter;

public class WacaSLA implements Policy, Policy.Callback {

  private List<ServerNode> nodeList = null;
  private Map<Integer, CountingFilter<String>> nodeBloomMap = null;
  private Map<Integer, Integer> histories = null;
  private Double T_MAX = new Double(250.0);
  private Map<Integer, Double> avgResponses = new TreeMap<Integer, Double>();
  private double fp_rate = 1.0 / 10.0;
  private int[] nbRequests;
  private long bloomhits = 0;

  @Override
  public void initialize(List<ServerNode> nodeList, LBNode f) {

    this.nodeList = new LinkedList<ServerNode>(nodeList);

    synchronized (this) {
      nodeBloomMap = new HashMap<Integer, CountingFilter<String>>();
      histories = new TreeMap<Integer, Integer>();
      nbRequests = new int[nodeList.size()];
      Arrays.fill(nbRequests, 0);

      for (ServerNode n : nodeList) {
        Integer nodeId = new Integer(n.getId());
        nodeBloomMap.put(nodeId, new CountingFilter<String>(n.getCapacity(), fp_rate));
        histories.put(nodeId, new Integer(0));
        avgResponses.put(nodeId, new Double(0.0));

      }
    }
  }

  @Override
  public Node loadBalance(Request r) {
    long time = System.nanoTime();
    String result = "";
    ServerNode leastLoaded = null;
    ServerNode bloomNode = null;
    //int flag=0;
    String query = r.getParameters();

    Node target = null;

    for (ServerNode n : nodeList) {
      CountingFilter<String> bf = nodeBloomMap.get(new Integer(n.getId()));

      if (bf.contains(query)) {
        if (bloomNode == null) {
          bloomNode = n;
        } else if (n.getRequestCount() < bloomNode.getRequestCount());
        bloomNode = n;
      } else {
        if (leastLoaded == null) {
          leastLoaded = n;
        }

        if (histories.get(new Integer(n.getId()))
                < histories.get(new Integer(leastLoaded.getId()))) {
          leastLoaded = n;
        } else if (n.getRequestCount()
                < leastLoaded.getRequestCount()) {
          leastLoaded = n;
        }

      }

    }

    if (bloomNode != null
            && avgResponses.get(new Integer(bloomNode.getId())) < T_MAX) {
      target = bloomNode;
      bloomhits++;
    } else {
      target = leastLoaded;
      Integer tgtId = new Integer(target.getId());

      CountingFilter<String> bf = nodeBloomMap.get(tgtId);
      bf.add(query);
      histories.put(tgtId, histories.get(tgtId) + 1);

    }

    nbRequests[target.getId()]++;

    return target;

  }

  @Override
  public void receivedRequest(Node n, Request r) {
    Integer nodeId = new Integer(n.getId());
    int coef = nbRequests[nodeId.intValue()];
    long exetime = r.getFtime() - r.getArTime();

    double avg = (avgResponses.get(nodeId) * coef + exetime) / (coef + 1);
    //System.out.println(avg);
    avgResponses.put(nodeId, new Double(avg));

  }

  @Override
  public void printAdditionnalStats() {
    System.out.println("Number of bloom hits: " + bloomhits);
  }

  @Override
  public void addNode(Node n) {
    synchronized (this) {
      nodeList.add((ServerNode) n);
    }
  }

  @Override
  public void removeNode(Node n) {
    synchronized (this) {
      nodeList.remove(n);
    }
  }

}
