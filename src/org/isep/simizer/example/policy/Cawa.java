package org.isep.simizer.example.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.requests.Request;
import org.isep.simizer.example.policy.utils.Clustering;
import org.isep.simizer.example.policy.utils.LpSolving;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.utils.SimizerUtils;

/**
 * run ./5nodes.json ./reqDescription_v1.csv ./workload_gene_v1.csv ./test lbsim.policies.CawaDyn
 * @author rdi
 */
public class Cawa extends Policy {

  static Map<Integer, Request> nodeRequestMap = null;
  public String fileName = "request.csv";

  /* Associates a prototype request to each node
   * @param filename contains queries for clustering
   */
  @Override
  public void initialize(List<VM> nodeList) {
    synchronized (this) {
      nodeRequestMap = new HashMap<>();
      String tmp = SimizerUtils.readFile(fileName);
      String[] req = tmp.split("\\n");
      Request[] queries = new Request[req.length];

      for (int i = 0; i < req.length; i++) {
        String[] rDesc = req[i].split(";");

        queries[i] = new Request(
                Long.parseLong(rDesc[0]), //id
                Integer.parseInt(rDesc[1]), //artime
                Integer.parseInt(rDesc[4]), //node
                Float.parseFloat(rDesc[5]), //cost
                rDesc[6] //params
        );
      }

      Clustering myClust = new Clustering(queries, nodeList);
      myClust.computeClusters();
      double[][] costMatrix = myClust.getCosts();

      LpSolving lp = new LpSolving(nodeList, costMatrix);
      lp.calculateOptimalExec();

      for (Node n : nodeList) {
        nodeRequestMap.put(n.getId(),
            Request.vectorToRequest(myClust.getCluster(lp.getOptimalExec().get(n.getId()) - 1)));
      }
    }
  }

  @Override
  public Node loadBalance(Request r) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public Node loadBalance(List<VM> nodeList, Request r) {
    Node requestNode = null;
    double dist = 1000;
    for (Node n : nodeList) {
      Request req = nodeRequestMap.get(n.getId());

      double curr = req.requestToVector().distanceTo(r.requestToVector());
      if (curr < dist) {
        requestNode = n;
        dist = curr;
      }
    }

    return requestNode;
  }

  public static void main(String[] args) {
//    String nodesJson = SimizerUtils.readFile("5nodes.json");
//    List<VM> nodes = SimizerUtils.decodeNodes(nodesJson);
//    Cawa c = new Cawa();
//    c.initialize(nodes, null);
//
//    Request r = new Request((long) 1, (long) 150, (int) 0, 1.0F, "p1=q1&p2=q2");
//
//    System.out.println("Node " + c.loadBalance(nodes, r).getId());
  }

  @Override
  public void addNode(VM vm) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void removeNode(VM vm) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

}
