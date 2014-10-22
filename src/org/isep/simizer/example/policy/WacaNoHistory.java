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
import simizer.policy.Policy;

/**
*
* @author sathya
*/
public class WacaNoHistory implements Policy {
    
    static Map<Integer,CountingFilter<String>> nodeBloomMap = null;
    static double fp_rate = 1.0/10.0;
    static long bloomhits=0;
    static List<ServerNode> nodeList = new LinkedList<ServerNode>();
    /* Associates unique CountingFilter for each IP address 
     * @param nodeBloomMap map each IP to unique BF 
     */
    
    @Override
    public void initialize(List<ServerNode> nodeList, LBNode f) {
      synchronized(this) {
             nodeBloomMap = new HashMap<Integer, CountingFilter<String>>();
            
          
            for(ServerNode n: nodeList) {
                nodeBloomMap.put(new Integer(n.getId()), new CountingFilter<String>(n.getCapacity(),fp_rate));
                this.nodeList.add(n);
           
            }
          }
    }

    @Override
    public Node loadBalance(Request r) {
        //long time = System.nanoTime();
        ServerNode result=null;
        ServerNode leastLoaded=null;
        ServerNode bloomNode=null;
        String query =r.getParameters();
        //System.out.println(query);
//        if(nodeBloomMap == null)
//          initialize(nodeList);

        Node target;

        for(ServerNode n: nodeList) {
            CountingFilter<String> bf = nodeBloomMap.get(n.getId());

            if(bf.contains(query)) {
                if(bloomNode == null)
                    bloomNode = n;

                else if(n.getRequestCount() < bloomNode.getRequestCount());
                    bloomNode = n;

            }

            else {
                if(leastLoaded == null)
                    leastLoaded = n;
                else if(n.getRequestCount() < leastLoaded.getRequestCount())
                    leastLoaded = n;
            }

        }

        if(bloomNode!=null)  {
            target = bloomNode;
            bloomhits++;
        }

        else  {
            target = leastLoaded;
            CountingFilter<String> bf = nodeBloomMap.get(target.getId());
            bf.add(query);

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