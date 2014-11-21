package org.isep.simizer.example.policy;

import java.util.*;
import simizer.LBNode;
import simizer.Node;
import simizer.ServerNode;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.Clustering;
import org.isep.simizer.example.policy.utils.LpSolving;
import simizer.utils.Vector;

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
public class CawaDyn implements Policy, Policy.Callback {

  public static int REQUEST_THRESOLD = 100;
  public static boolean CLUSTERED = false;

  // This structure maps each class center to its optimal node
  Map<Vector, Node> protoToNode = new TreeMap<Vector, Node>();
  // Utility map for cost calculation total processing time per node
  Map<Integer, Long> nodeProcTime = new TreeMap<Integer, Long>();
  private int[] nodeCount;
  // List of recorded requests
  List<Request> requests = new LinkedList<Request>();
  //static Random ran = new Random(System.currentTimeMillis());
  List<ServerNode> nodeList;
  private int counter = 0;

  @Override
  public void initialize(List<ServerNode> availableNodes, LBNode f) {
    f.registerAfter(this);
    this.nodeList = new ArrayList(availableNodes);
    this.nodeCount = new int[availableNodes.size()];

  }

  @Override
  public void addNode(Node n) {
       // Not implemnted yet
    // Wil require a new classification
  }

  @Override
  public void removeNode(Node n) {
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
            //nodeCount[tgt.getId()]++;
      //return tgt;

    }
    if (counter % REQUEST_THRESOLD == 0) {
      printNodeCount();
      Arrays.fill(nodeCount, 0);
    }

    nodeCount[tgt.getId()]++;
    return tgt;

  }

  @Override
  public void printAdditionnalStats() {
    //throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Method automatically called when a request is over and returned to the
   * client. Each request execution time and parameter is recorded by this
   * method. When the number of recorded request reaches REQUEST_THRESOLD, the
   * clustering algorithm is launched.
   *
   * @param n
   * @param r
   */
  @Override
  public void receivedRequest(Node n, Request r) {

        //total du temps d'exéution des requêtes sur un noeud donné
    // permettra de calculer les coûts unitaires des requêtes.
    if (!nodeProcTime.containsKey(n.getId())) {
      nodeProcTime.put(n.getId(), 0L);
    }

    long stime = r.getFtime() - r.getArTime() - r.getDelay() + nodeProcTime.get(n.getId());
    nodeProcTime.put(n.getId(), stime);
        //double cost = ( stime /1000) * ( n.getCost() /3600);
    //System.out.println("cost " + cost);
    //r.setCost(cost);
    requests.add(r);
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
    double interval = (double) (last.getFtime() - first.getArTime());
    double total = 0, tmpc = 0;
    for (Request r : requests) {
      double duration = (double) r.getFtime() - r.getArTime();
      Node n = nodeList.get(r.getNodeId());
      // because the cost is per hour
      tmpc = getCost(interval, duration, ((ServerNode) n).getCost(), nodeProcTime.get(n.getId()));

      total += tmpc;
      r.setCost(tmpc);
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

    int nbParams = requests.get(0).getParameters().split("&|=").length / 2;
    //System.out.println("nb params = " + nbParams);
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
      //lp.displayOptimalExec();
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
