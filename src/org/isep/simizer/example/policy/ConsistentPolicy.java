/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.isep.simizer.example.policy;

import java.util.List;
import simizer.LBNode;
import simizer.Node;
import simizer.ServerNode;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.ConsistentHash;

/**
 *
 * @author isep
 */
public class ConsistentPolicy implements Policy {
    private static boolean init = false;
    ConsistentHash<ServerNode> ch = null;
    
    @Override
    public void initialize(List<ServerNode> availableNodes, LBNode lbn) {
        
        if(availableNodes==null || availableNodes.size() > 0) {
            System.out.println("initializing null");
            ch = new ConsistentHash<>(1,null);
 
        } else {
            ch = new ConsistentHash<>(
                (int) Math.round(Math.log(availableNodes.size()))
                ,availableNodes);
        }
        init = true;
            
    }
    /**
     * Assumes consistent hash is not null
     * @param r
     * @return 
     */
    @Override
    public Node loadBalance(Request r) {
       
       Node n = ch.get(r.getParameters().split("=")[1]);
       
       return ch.get(n);
    }

    @Override
    public void printAdditionnalStats() {
        
    }

    @Override
    public void addNode(Node n) {
        if(ch != null) {
             ch.add((ServerNode)n);
        }
       
    }

    @Override
    public void removeNode(Node n) {
            ch.remove((ServerNode)n);
        }
}
