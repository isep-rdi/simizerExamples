package fr.isep.simizer.example.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import fr.isep.simizer.example.policy.utils.Clustering;
import fr.isep.simizer.example.policy.utils.LpSolving;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.requests.Request;
import fr.isep.simizer.utils.Vector;

/**
 * This class implements the Cost AWare Algorithm for cost based request
 * routing. The algorithm records a fixed (REQUEST_THRESOLD) number of requests.
 * When the thresold is reached, the algorithmclassifies the requests based on
 * their cost and parameters in a number of classes equal to the number of
 * nodes. This step is performed by a call to @link simizer.utils.Clustering
 * class.
 *
 * @author R. Chiky
 */
public class CawaDyn extends Policy.Callback {

  public static int REQUEST_THRESOLD = 100;
  public static boolean CLUSTERED = false;

  // This structure maps each class center to its optimal node
  Map<Vector, Node> protoToNode = new TreeMap<>();
  // Utility map for cost calculation total processing time per node
  Map<Integer, Long> nodeProcTime = new TreeMap<>();
  private int[] nodeCount;
  // List of recorded requests
  List<Request> requests = new LinkedList<>();
  List<VM> nodeList;
  private int counter = 0;

  @Override
  public void initialize(List<VM> availableNodes) {
    this.nodeList = new ArrayList<>(availableNodes);
    this.nodeCount = new int[availableNodes.size()];
  }

  @Override
  public void addNode(VM vm) {
    // Not implemnted yet
    // Wil require a new classification
  }

  @Override
  public void removeNode(VM vm) {
    //Not implemented 
    //will require new classification
  }

  /**
   * Performs a Round Robin request distribution until the algorithm status is
   * CLUSTERED. Once clustering as been performed, each node is associated to a
   * request prototype (which is the cluster center of each class) When a
   * request arrives it is compared to each prototype and routed to the closest
   * one (Euclidean distance of the parameter string). Please look at @link
   * Vector and @link Request files.
   *
   * @param r Request
   * @return target node
   */
  @Override
  public Node loadBalance(Request r) {
    Node tgt;
    counter++;
    if (!CLUSTERED) {

      tgt = nodeList.get(counter % nodeList.size());

    } else {
      Vector req = r.requestToVectorH();
      Vector k = null;
      double dist = Double.MAX_VALUE;

      //gets the closest vector 
      // maybe use SF-Curves for reducing search time.
      for (Vector v : protoToNode.keySet()) {
        double tmpDist = v.distanceTo(req);
        if (tmpDist < dist) {
          k = v;
          dist = tmpDist;
        }
      }
      tgt = protoToNode.get(k);

    }
    if (counter % REQUEST_THRESOLD == 0) {
      printNodeCount();
      Arrays.fill(nodeCount, 0);
    }

    nodeCount[tgt.getId()]++;
    return tgt;

  }

  /**
   * Method automatically called when a request is over and returned to the
   * client. Each request execution time and parameter is recorded by this
   * method. When the number of recorded request reaches REQUEST_THRESOLD, the
   * clustering algorithm is launched.
   *
   * @param vm
   * @param request
   */
  @Override
  public void receivedRequest(VM vm, Request request) {

    //total du temps d'exéution des requêtes sur un noeud donné
    // permettra de calculer les coûts unitaires des requêtes.
    if (!nodeProcTime.containsKey(vm.getId())) {
      nodeProcTime.put(vm.getId(), 0L);
    }

    long stime = request.getServerFinishTimestamp() - request.getClientStartTimestamp() - request.getNetworkDelay() + nodeProcTime.get(vm.getId());
    nodeProcTime.put(vm.getId(), stime);
    requests.add(request);
    synchronized (this) {
      if (requests.size() >= REQUEST_THRESOLD) {

        launchClustering();
        requests.clear();
        matrixAvailable();
      }
    }
  }

  /**
   * Calculates each requests costs relative to its processing node.
   */
  private void processCosts() {
    Request first = requests.get(0);
    Request last = requests.get(requests.size() - 1);
    // calculate interval
    double interval = (double) (last.getServerFinishTimestamp() - first.getClientStartTimestamp());
    double total = 0, tmpc = 0;
    for (Request r : requests) {
      double duration = (double) r.getServerFinishTimestamp() - r.getClientStartTimestamp();
      VM vm = nodeList.get(r.getNodeId());
      // because the cost is per hour
      tmpc = getCost(interval, duration, vm.getCost(), nodeProcTime.get(vm.getId()));

      total += tmpc;
      r.set("cost", tmpc);
    }
    System.out.println("Total cost = " + total);

  }

  /**
   * Calculate the cost of the given interval according to the hourly cost and
   * the total observed period.
   *
   * @param interval
   * @param duration
   * @param hCost
   * @param tProcTime
   * @return cost
   */
  private static double getCost(double interval, double duration, double hCost, double tProcTime) {
    double intervalCost = ((interval / 1000) * hCost) / 3600;
    double cost = (duration / tProcTime) * intervalCost;
    return cost;
  }

  private void printNodeCount() {
    for (int i : nodeCount) {
      System.out.print(i + ",");
    }
    System.out.print("\n");
  }

  /**
   * This method runs threee algorithm steps: - requests cost calculations, -
   * requests Clustering - optimization of the requests routes.
   *
   * @TODO perform clustering in a separate (background thread)
   *
   */
  private void launchClustering() {

    processCosts();

    // Max: Changed to split into groups of parameters so that we don't need to
    // divide by two.
    int nbParams = requests.get(0).getQuery().split("&").length;

    synchronized (this) {
      System.out.println(" Launching clustering");
      Clustering myClust = new Clustering(
              requests.toArray(new Request[requests.size()]),
              nbParams,
              nodeList
      );

      myClust.computeClusters();

      double[][] costMatrix = myClust.getCosts();

      for (int i = 0; i < costMatrix.length; i++) {
        for (int j = 0; j < nodeList.size(); j++) {
          System.out.print(costMatrix[i][j] + ";");
        }
        System.out.print("\n");
      }

      LpSolving lp = new LpSolving(nodeList, costMatrix);
      lp.calculateOptimalExec();

      TreeMap<Vector, Node> tmpVMap = new TreeMap<Vector, Node>();
      for (Node n : nodeList) {
        tmpVMap.put(
                new Vector(myClust.getCluster(lp.getOptimalExec().get(n.getId()) - 1)),
                n);
      }

      this.protoToNode = tmpVMap;
    }

  }

  private void matrixAvailable() {
    CLUSTERED = true;
  }
}
