package org.isep.simizer.example.policy;

import java.util.*;
import simizer.LBNode;
import simizer.Node;
import simizer.ServerNode;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.ClusteringRequests;
import org.isep.simizer.example.policy.utils.LpSolver;
import simizer.utils.Vector;

public class CostAware implements Policy, Policy.Callback {

  public static int REQUEST_THRESOLD = 200;
  public static boolean CLUSTERED = false;
  public static Random random = new Random(System.nanoTime());
  // This structure maps each class center to its optimal node
  Map<Vector, Node> protoToNode = new TreeMap<>();
  // Utility map for cost calculation total processing time per node
  Map<Integer, Long> nodeProcTime = new TreeMap<>();
  private int[] nodeCount;
  // List of recorded requests
  List<Request> requests = new LinkedList<>();
  //static Random ran = new Random(System.currentTimeMillis());
  List<Node> nodeList;
  private int counter = 0;
  private double lastInterval = 0.0D;

  @Override
  public void initialize(List<ServerNode> availableNodes, LBNode f) {
    f.registerAfter(this);
    this.nodeList = new ArrayList(availableNodes);
    this.nodeCount = new int[availableNodes.size()];
  }

  @Override
  public void addNode(Node n) {
    synchronized (this) {
      if (!nodeList.contains(n)) {
        nodeList.add(n);
      }
    }
  }

  @Override
  public void removeNode(Node n) {
    synchronized (this) {
      if (nodeList.contains(n)) {
        nodeList.remove(n);
      }
    }
  }

  @Override
  public Node loadBalance(Request r) {
    Node targetNode = clusterNode(r);

    if (counter % REQUEST_THRESOLD == 0) {
      Arrays.fill(nodeCount, 0);
    }

    nodeCount[targetNode.getId()]++;
    return targetNode;
  }

  @Override
  public void printAdditionnalStats() {

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

    if (!nodeProcTime.containsKey(n.getId())) {
      nodeProcTime.put(n.getId(), 0L);
    }

    long stime = r.getFtime() - r.getArTime() - r.getDelay() + nodeProcTime.get(n.getId());
    nodeProcTime.put(n.getId(), stime);

    requests.add(r);
    synchronized (this) {
      if (requests.size() >= REQUEST_THRESOLD) {
        launchClustering();
        requests.clear();
        nodeProcTime.clear();
        matrixAvailable();

      }
    }
  }

  /**
   * Calculates each requests costs relative to its processing node.
   */
  private void processCosts() {
    //Request first = requests.get(0);
    Request last = requests.get(requests.size() - 1);
    double[] avgCost = new double[nodeList.size()];
    int[] counts = new int[nodeList.size()];
        // calculate interval 

    double interval = (double) last.getFtime() - lastInterval;
    lastInterval = last.getFtime();
    //double interval = (double) (last.getFtime() - first.getArTime());
    System.out.println("interval:" + interval);
    double total = 0, tmpc;
    for (Request r : requests) {
      double duration = (double) r.getFtime() - r.getArTime();
      Node n = nodeList.get(r.getNodeId());
      tmpc = (duration / 1000) * (((ServerNode) n).getCost() / 3600);
      //tmpc = getCost(interval, duration, ((ServerNode)n).getCost(), nodeProcTime.get(n.getId()));
      total += tmpc;
      r.setCost(tmpc);

      avgCost[n.getId()] = ((avgCost[n.getId()] * counts[n.getId()]) + tmpc)
              / (++counts[n.getId()]);

    }
    System.out.println("Observed cost: " + total + " " + Arrays.toString(avgCost));

  }
  /*
   * Calculate the cost of the given interval according to the hourly cost and the total observed period.
   */

  private static double getCost(double interval, double duration, double hCost, long tProcTime) {

    double intervalCost = ((interval / 1000) * hCost) / 3600;
    //System.out.println("tProc: " + tProcTime);
    return (duration / tProcTime) * intervalCost;
  }

  private void printNodeCount() {
    for (int i : nodeCount) {
      System.out.print(i + ",");
    }
    System.out.print("\n");
  }

  /*
   * This method runs three algorithm steps:
   * - requests cost calculations,
   * - requests Clustering
   * - optimization of the requests routes.
   *
   * @TODO perform clustering in a separate (background thread)
   *
   */
  ClusteringRequests myClust;

  private void launchClustering() {

    processCosts();

    int nbParams = requests.get(0).getParameters().split("&|=").length / 2;
    synchronized (this) {
      if (myClust == null) {
        myClust = new ClusteringRequests(
                requests.toArray(new Request[requests.size()]),
                nbParams,
                nodeList
        );
      } else {
        myClust.setRequests(requests.toArray(new Request[requests.size()]));
        myClust.setNbParams(nbParams);
        myClust.setNodeList(nodeList);
      }
            //myClust.computeClustersWekaMeans();
      //myClust.computeClustersEKmeans();
      //myClust.computeClustersHMeans();
      myClust.computeClustersIKmeans();
      double[][] costMatrix = myClust.getCosts();

      LpSolver lp = new LpSolver(nodeList, costMatrix);
      lp.calculateOptimalExec();
      //lp.displayOptimalExec();
      TreeMap<Vector, Node> tmpVMap = new TreeMap<>();
      for (Node n : nodeList) {
        Vector v = new Vector(myClust.getCluster(lp.getOptimalExec().get(n.getId()) - 1));
        tmpVMap.put(v, n);
        System.out.println("Node;" + n.getId() + " => " + v.toString());

      }
      this.protoToNode = tmpVMap;

    }

  }

  private void matrixAvailable() {
    CLUSTERED = true;
  }

  private Node clusterNode(Request r) {
    Node targetNode;
    counter++;
    double dist = Double.MAX_VALUE;
    if (!CLUSTERED) {
      targetNode = nodeList.get(random.nextInt(nodeList.size()));

      //targetNode = nodeList.get(counter % nodeList.size());
    } else {
      Vector req = r.requestToVectorH();
      Vector k = null;

      for (Vector v : protoToNode.keySet()) {

        double tmpDist = v.distanceTo(req);
        if (tmpDist < dist) {
          k = v;
          dist = tmpDist;
        }
      }
      targetNode = protoToNode.get(k);
    }
    //System.out.println(r.toString() + "==>" + targetNode.getId() + "=" + dist);
    return targetNode;
  }

}
