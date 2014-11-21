//Aruth Perum Jothi, Aruth Perum Jothi, Thani Perum Karunai, Aruth Perum Jothi..
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/

package org.isep.simizer.example.policy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import simizer.LBNode;
import simizer.Node;
import simizer.ServerNode;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.CountingFilter;

/**
*
* @author sathya
*/
public class WacaHistory implements Policy {
  
    static Map<Integer,CountingFilter<String>> nodeBloomMap = null;
    static Map<Integer, Integer> histories = null;
    
    static double fp_rate = 1.0/10.0;
    static long bloomhits;
    static List<ServerNode> nodeList = new LinkedList<ServerNode>();
    /*
     * Associates unique CountingFilter for each IP address 
     * @param nodeBloomMap map each IP to unique BF 
     */
    
    @Override
    public void initialize(List<ServerNode> nodeList, LBNode f) {
          synchronized(this) {
             nodeBloomMap = new HashMap<Integer, CountingFilter<String>>();
             histories = new HashMap<Integer,Integer>();
          
            for(ServerNode n: nodeList) {
                nodeBloomMap.put(new Integer(n.getId()), new CountingFilter<String>(n.getCapacity(),fp_rate));
                histories.put(new Integer(n.getId()), new Integer(0));
                this.nodeList.add(n);
            }
          }
    }

    @Override
    public Node loadBalance(Request r) {
        long time = System.nanoTime();
        String result="";
        ServerNode leastLoaded=null;
        ServerNode bloomNode=null;
        //int flag=0;
        String query = r.getParameters();
       
       

            Node target = null;
            
            for(ServerNode n: nodeList)    {
                CountingFilter<String> bf = nodeBloomMap.get(n.getId());

                if(bf.contains(query)) {
                    if(bloomNode == null)
                        bloomNode = n;
                   else if(n.getRequestCount() < bloomNode.getRequestCount());
                        bloomNode = n;

                } else {
                    if(leastLoaded == null)
                        leastLoaded = n;

                    if(histories.get(n.getId()) < histories.get(leastLoaded.getId()))
                        leastLoaded = n;

                    else if(n.getRequestCount() < leastLoaded.getRequestCount())
                        leastLoaded = n;

                    }

                }
           
           if(bloomNode!=null)
            {
                target = bloomNode;
                bloomhits++;
            }
            
            else
            {
                target = leastLoaded;
                //Integer tgtId = new Integer(target.getId());
                int tgtId = target.getId();
                CountingFilter<String> bf = nodeBloomMap.get(tgtId);
                bf.add(query);
                histories.put(tgtId, histories.get(tgtId) +1);
                
                //target.setRequestHistory(target.getRequestHistory()+ 1);
                //System.out.println("Added to the filter :" + query);
            }

        
        
        return target;

    }

   

    @Override
    public void printAdditionnalStats() {
       System.out.println("Number of bloom hits: "+bloomhits );
    }

    @Override
    public void addNode(Node n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeNode(Node n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}