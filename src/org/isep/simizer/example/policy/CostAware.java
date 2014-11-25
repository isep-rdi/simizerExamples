package org.isep.simizer.example.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.isep.simizer.example.policy.utils.ClusteringRequests;
import org.isep.simizer.example.policy.utils.LpSolver;
import simizer.Node;
import simizer.VM;
import simizer.requests.Request;
import simizer.utils.Vector;

public class CostAware extends Policy.Callback {

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

  List<Node> nodeList;
  private int counter = 0;
  private double lastInterval = 0.0D;

  @Override
  public void initialize(List<VM> availableNodes) {
    this.nodeList = new ArrayList<Node>(availableNodes);
    this.nodeCount = new int[availableNodes.size()];
  }

  @Override
  public void addNode(VM vm) {
    synchronized (this) {
      if (!nodeList.contains(vm)) {
        nodeList.add(vm);
      }
    }
  }

  @Override
  public void removeNode(VM vm) {
    synchronized (this) {
      if (nodeList.contains(vm)) {
        nodeList.remove(vm);
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
    double interval = (double) last.getServerFinishTimestamp() - lastInterval;
    lastInterval = last.getServerFinishTimestamp();
    System.out.println("interval:" + interval);
    double total = 0, tmpc;
    for (Request r : requests) {
      double duration = (double) r.getServerFinishTimestamp() - r.getClientStartTimestamp();
      Node n = nodeList.get(r.getNodeId());
      tmpc = (duration / 1000) * (((VM) n).getCost() / 3600);
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
    return (duration / tProcTime) * intervalCost;
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
      myClust.computeClustersIKmeans();
      double[][] costMatrix = myClust.getCosts();

      LpSolver lp = new LpSolver(nodeList, costMatrix);
      lp.calculateOptimalExec();

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
    return targetNode;
  }

}
