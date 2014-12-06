package fr.isep.simizer.example.policy.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rcaller.RCaller;
import rcaller.RCode;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM;

/**
 *
 * @author rchiky
 */
public class LpSolving {

  // matrix of Costs

  double[][] costMatrix;

  List<VM> nodeList;

  // <instanceId, clusterId> optimaleExec : map associating the cluster of request to an instance.
  Map<Integer, Integer> optimalExec = null;

  //Constructors
  public LpSolving(List<VM> nodeList, double[][] costMatrix) {
    this.nodeList = nodeList;
    this.costMatrix = costMatrix;
    this.optimalExec = new HashMap<>(this.nodeList.size());

  }

  // This method loads the ID of active sensors and re-initializes the structures used in the optimal rates calculation

  public void reinitialize() {
    this.costMatrix = new double[this.nodeList.size()][this.nodeList.size()];
    this.optimalExec = new HashMap<>(this.nodeList.size());

  }

  public double[][] getCostMatrix() {
    return costMatrix;
  }

  public void setCostMatrix(double[][] costMatrix) {
    this.costMatrix = costMatrix;
  }

  public Map<Integer, Integer> getOptimalExec() {
    return optimalExec;
  }

  public void setOptimalExec(Map<Integer, Integer> optimalExec) {
    this.optimalExec = optimalExec;
  }

  public void displayOptimalExec() {
    int i, id;
    System.out.println("Optimale solution :");
    for (i = 0; i < nodeList.size(); i++) {
      id = nodeList.get(i).getId();
      System.out.println("ID cluster: " + id + " on instance: " + optimalExec.get(id));
    }
  }

  public void resetCostMatrix() { // set error matrix to zero
    for (int i = 0; i < this.nodeList.size(); i++) {
      for (int j = 0; j < this.nodeList.size(); j++) {
        costMatrix[i][j] = 0;
      }
    }
  }

  // At the end of a time period: computes the optimal execution and reinitializes the data structures
  public void calculateOptimalExec() {
    try {
      //Creating an instance of class RCaller
      RCaller caller = new RCaller();
      RCode code = new RCode();

      //Where is the Rscript
      caller.setRscriptExecutable("/usr/bin/Rscript");
      caller.setRExecutable("/usr/bin/R");

      double[] costVector = new double[this.nodeList.size() * this.nodeList.size()];
      int i, j, k = 0;
      for (i = 0; i < this.nodeList.size(); i++) {
        for (j = 0; j < this.nodeList.size(); j++) {
          costVector[k++] = costMatrix[i][j];
        }
      }

      //Generating R code
      //addDoubleArray() method converts Java arrays to R vectors
      code.addDoubleArray("cost.mat", costVector);
      code.addIntArray("nbcol", new int[]{this.nodeList.size()});
      code.addIntArray("nbrow", new int[]{this.nodeList.size()});
      code.R_source("optim.R");
      code.addRCode("res<-optimisation(cost.mat,nbrow,nbcol)");

      //run the R code and return the result to Java
      caller.setRCode(code);
      caller.runAndReturnResult("res");
      System.out.println(caller.getRCode());

      int[] results;
      results = caller.getParser().getAsIntArray("pas");//Node-->Cluster
      i = 0;
      for (Node n : nodeList) {
        this.optimalExec.put(n.getId(), results[n.getId()]);
      }
      while (i < results.length) {
        System.out.println("node" + (i) + " for cluster: " + (results[i]));
        i++;
      }

    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

}
