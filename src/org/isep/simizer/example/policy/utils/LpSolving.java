/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.isep.simizer.example.policy.utils;
import java.util.Map;
import rcaller.*;
import java.util.HashMap;
import simizer.Node;
import java.util.List;
import simizer.ServerNode;
//import simizer.Node;

/**
 *
 * @author rchiky
 */
public class LpSolving {
    // matrix of Costs
    double[][] costMatrix; 


    List<ServerNode> nodeList;
    
    // <instanceId, clusterId> optimaleExec : map associating the cluster of request to an instance.
    Map<Integer,Integer> optimalExec=null;
    
    
   
    
    //Constructors
    public LpSolving(List<ServerNode> nodeList,double[][] costMatrix){
        this.nodeList=nodeList;
        this.costMatrix=costMatrix;
        this.optimalExec   = new HashMap<Integer,Integer>(this.nodeList.size());
        
    }
    // This method loads the ID of active sensors and re-initializes the structures used in the optimal rates calculation 
    public void reinitialize(){
        this.costMatrix  = new double [this.nodeList.size()][this.nodeList.size()];
        this.optimalExec   = new HashMap<Integer,Integer>(this.nodeList.size());
        
    }
    
  

    
    public double[][] getCostMatrix(){
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
	for(i=0; i<nodeList.size();i++) {
		id = nodeList.get(i).getId();
		System.out.println("ID cluster: "+ id +" on instance: "+optimalExec.get(id));
		//System.out.println("Sensor : "+ id +" optimal rate "+(100/optimalRates.get(id))+" %");
	}
    }	
		
	
    public void resetCostMatrix(){ // set error matrix to zero
	for(int i =0; i< this.nodeList.size(); i++)
		for(int j= 0; j<this.nodeList.size();j++)
			costMatrix[i][j]=0;	
    }
    
    // At the end of a time period: computes the optimal execution and reinitializes the data structures
    public void calculateOptimalExec() {
    try{
        //Creating an instance of class RCaller
        RCaller caller = new RCaller();
        RCode code = new RCode();
        
        //Where is the Rscript
        //caller.setRscriptExecutable("C:/Program Files/R/R-2.15.2/bin/x64/Rscript.exe");
        //caller.setRExecutable("C:/Program Files/R/R-2.15.2/bin/x64/R.exe");
        caller.setRscriptExecutable("/usr/bin/Rscript");
        caller.setRExecutable("/usr/bin/R");
        
        double[] costVector = new double[this.nodeList.size()*this.nodeList.size()];
        int i,j,k=0;
        for (i=0; i<this.nodeList.size();i++)
            for(j=0;j<this.nodeList.size();j++)
             costVector[k++]=costMatrix[i][j];
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
     //   System.out.println("Available results from optimisation");
     //   System.out.println(caller.getParser().getNames());
        int[] results;
        results = caller.getParser().getAsIntArray("pas");//Node-->Cluster
        i=0;
        for(Node n: nodeList) {
             
             this.optimalExec.put(new Integer(n.getId()), new Integer(results[n.getId()]));
        }
        while (i<results.length){
           // optimalExec.put(i+1, results[i]);
            System.out.println("node"+(i)+ " for cluster: "+(results[i]));
            i++;
        }
   
        
        }catch(Exception e){
            System.out.println(e.toString());
        }
    }
    
 /*   public static void main(String[] args) {

        double[][] A = {
            {  1,  1,  0 },
            {  1,  4,  0 },
            {  2,  1,  0 },
            {  3,  4,  0 },
        };
        LpSolving c=new LpSolving(4,3,A);
        c.calculateOptimalExec();

    }*/


}



