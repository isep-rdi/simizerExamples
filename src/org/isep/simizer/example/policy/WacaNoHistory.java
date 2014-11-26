//Aruth Perum Jothi, Aruth Perum Jothi, Thani Perum Karunai, Aruth Perum Jothi..

package org.isep.simizer.example.policy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import simizer.nodes.Node;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.CountingFilter;
import simizer.nodes.VM;

/**
 *
 * @author sathya
 */
public class WacaNoHistory extends Policy {

  static Map<Integer, CountingFilter<String>> nodeBloomMap = null;
  static double fp_rate = 1.0 / 10.0;
  static long bloomhits = 0;
  static List<VM> nodeList = new LinkedList<>();
  /* Associates unique CountingFilter for each IP address
   * @param nodeBloomMap map each IP to unique BF
   */

  @Override
  public void initialize(List<VM> nodeList) {
    synchronized (this) {
      nodeBloomMap = new HashMap<>();

      for (VM vm : nodeList) {
        nodeBloomMap.put(vm.getId(),
            new CountingFilter<String>(vm.getMaximumActiveRequestsCount(),
                fp_rate));
        this.nodeList.add(vm);
      }
    }
  }

  @Override
  public Node loadBalance(Request r) {
    VM leastLoaded = null;
    VM bloomNode = null;
    String query = r.getParameters();


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
      CountingFilter<String> bf = nodeBloomMap.get(target.getId());
      bf.add(query);
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
