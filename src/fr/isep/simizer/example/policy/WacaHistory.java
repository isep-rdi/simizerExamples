//Aruth Perum Jothi, Aruth Perum Jothi, Thani Perum Karunai, Aruth Perum Jothi..

package fr.isep.simizer.example.policy;

import fr.isep.simizer.example.policy.utils.CountingFilter;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.requests.Request;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WacaHistory extends Policy {

  static Map<Integer, CountingFilter<String>> nodeBloomMap = null;
  static Map<Integer, Integer> histories = null;

  static double fp_rate = 1.0 / 10.0;
  static long bloomhits;
  static List<VM> nodeList = new LinkedList<>();
  /*
   * Associates unique CountingFilter for each IP address
   * @param nodeBloomMap map each IP to unique BF
   */

  @Override
  public void initialize(List<VM> availableNodes) {
    synchronized (this) {
      nodeBloomMap = new HashMap<>();
      histories = new HashMap<>();

      for (VM vm : availableNodes) {
        nodeBloomMap.put(vm.getId(),
            new CountingFilter<String>(vm.getMaximumActiveRequestsCount(),
                fp_rate));
        histories.put(vm.getId(), 0);
        this.nodeList.add(vm);
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
    
    if (bloomNode != null) {
      target = bloomNode;
      bloomhits++;
    } else {
      target = leastLoaded;
      int tgtId = target.getId();
      CountingFilter<String> bf = nodeBloomMap.get(tgtId);
      bf.add(query);
      histories.put(tgtId, histories.get(tgtId) + 1);
    }

    return target;
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
    System.out.println("Number of bloom hits: " + bloomhits);
  }

}
