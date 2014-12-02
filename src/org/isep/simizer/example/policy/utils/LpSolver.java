package org.isep.simizer.example.policy.utils;

import java.util.Map;
import java.util.HashMap;
import fr.isep.simizer.nodes.Node;
import java.util.List;

public class LpSolver {
    double[][] costMatrix;
    List<Node> nodeList;
    Map<Integer, Integer> optimalExec = null;

    public LpSolver(List<Node> nodeList, double[][] costMatrix) {
        this.nodeList = nodeList;
        this.costMatrix = costMatrix;
        this.optimalExec = new HashMap<Integer, Integer>(this.nodeList.size());
    }

    public void reinitialize() {
        this.costMatrix = new double[this.nodeList.size()][this.nodeList.size()];
        this.optimalExec = new HashMap<Integer, Integer>(this.nodeList.size());
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

    public void resetCostMatrix() {
        for (int i = 0; i < this.nodeList.size(); i++) {
            for (int j = 0; j < this.nodeList.size(); j++) {
                costMatrix[i][j] = 0;
            }
        }
    }

    public void calculateOptimalExec() {
        try {
            String sumType = "min";

            double[][] array = costMatrix;

            if (array.length > array[0].length) {
                array = HungarianAlgo.transpose(array);
            }
            int[][] assignment = new int[array.length][2];
            assignment = HungarianAlgo.hgAlgorithm(array, sumType);

            double sum = 0;
            for (int i = 0; i < assignment.length; i++) {
                this.optimalExec.put(assignment[i][1], (assignment[i][0] + 1));
            }
            
        } catch (Exception e) {
            System.out.println(e.toString());
        }



    }
}