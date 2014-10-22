/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
 * 
 * run ./5nodes.json ./reqDescription_v1.csv ./workload_gene_v1.csv ./test lbsim.policies.CawaDyn
 * 
 * 
 */
package org.isep.simizer.example.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import simizer.LBNode;
import simizer.Node;
import simizer.ServerNode;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.Clustering;
import org.isep.simizer.example.policy.utils.LpSolving;
import simizer.policy.Policy;
import simizer.utils.SimizerUtils;

/**
 *
 * @author rdi
 */
public class Cawa implements Policy{
    static Map<Integer,Request> nodeRequestMap = null;
    public String fileName="request.csv";

    
    /* Associates a prototype request to each node
     * @param filename contains queries for clustering
     */
    
    @Override
    public void initialize(List<ServerNode> nodeList, LBNode f) {
      synchronized(this) {
             nodeRequestMap = new HashMap<Integer, Request>();
             String tmp = SimizerUtils.readFile(fileName);
             String[] req= tmp.split("\\n");
             Request[] queries=new Request[req.length];
             
             for(int i = 0; i < req.length;i++ ) {
             String[] rDesc = req[i].split(";");
             
          
              
             queries[i] = new Request(
                     Integer.parseInt(rDesc[0]), //id
                     Integer.parseInt(rDesc[1]), //artime
                    // Integer.parseInt(rDesc[2]), //chtime
                    // Integer.parseInt(rDesc[3]), //cmtime
                     Integer.parseInt(rDesc[4]), //node
                     Float.parseFloat(rDesc[5]), //cost
                     rDesc[6]   //params    
                     );
             }

            Clustering myClust=new Clustering(queries,nodeList);
            myClust.computeClusters();
            double[][] costMatrix=myClust.getCosts();
           
          //  for(int i=0;i<5;i++) for (int j=0; j<5; j++) System.out.println(costMatrix[i][j]);
            LpSolving lp=new LpSolving(nodeList,costMatrix);
            lp.calculateOptimalExec();
        
          //lp.displayOptimalExec();
            for(Node n: nodeList) {
               
                nodeRequestMap.put(new Integer(n.getId()), 
                        Request.vectorToRequest(myClust.getCluster(lp.getOptimalExec().get(n.getId())-1)));
                
            }
          }
    }

    public Node loadBalance(List<ServerNode> nodeList, Request r) {
        long time = System.nanoTime();
 
        Node requestNode=null;
         Node target;
        double dist=1000;
        for(Node n: nodeList) {
            Request req = nodeRequestMap.get(new Integer(n.getId()));
            
            double curr=req.requestToVector().distanceTo(r.requestToVector());
            if(curr<dist){
                requestNode = n;
                dist=curr;
            }
           }
  
        long timing = System.nanoTime() - time;
        //System.out.println("TTP:"+ timing +"Forwarded to URL :"+target.getId()+"::"+target.getRequestCount());

        return requestNode;
    }


    public void receivedRequest(Node n, Request r) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
    public static void main(String[] args) {
        String nodesJson = SimizerUtils.readFile("5nodes.json");
        List<ServerNode> nodes = SimizerUtils.decodeNodes(nodesJson);
        Cawa c= new Cawa();
        c.initialize(nodes, null);
        //Request r=new Request((long)22,(long)150,(long)0,(long)3,"15,10,7,10,10");
        //long id,long artime, int node,float cost,String params
        Request r = new Request((long)1, (long) 150, (int) 0, 1.0F,"p1=q1&p2=q2");
        
        System.out.println("Node "+ 
                c.loadBalance(nodes, r).getId());
    }

    @Override
    public void printAdditionnalStats() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addNode(Node n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeNode(Node n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Node loadBalance(Request r) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
