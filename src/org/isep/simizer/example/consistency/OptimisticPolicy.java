/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.isep.simizer.example.consistency;

import java.util.List;
import org.isep.simizer.example.policy.utils.ConsistentHash;
import simizer.Node;
import simizer.requests.Request;
import simizer.storage.Resource;

/**
 *
 * @author Sylvain Lefebvre
 */
public class OptimisticPolicy extends StoreApplication {

    public static final int REP_FACTOR = 2;
    
    public static ConsistentHash<Node> hashRing = null;
    
    public OptimisticPolicy(int id, int memSize) {
        super(id, memSize);
        
    }

    /**
     * Registers in hashring. Locks the hashring for addition since it is based
     * on TreeMap which is not synchronized.
     */
    @Override
    public void init() {
        super.init();
        synchronized (hashRing) {
            hashRing.add(this.vm);
        }
    }

    /**
     * Writes locally without waiting for acknowledgment
     *
     * @param r
     * @return
     */
    @Override
    public Request write(Request r) {
        // Local write
        Integer id = r.getResources().get(0);
        Integer val = new Integer(r.getParameters().split("&|=")[3]);
        Resource res = read(id);
        if (res == null) {
            res = new Resource(id);
            res.setVersion(val);
        } else if (res.getVersion() < val) {
            res.setVersion(val);
        }
        
        write(res);
        sendResponse(r, res); // Asynchronous, we reply before replication
        List<Node> replicas = hashRing.getList(res.getId());
        replicas.remove(this.vm);
        // fire and forget : optimistic approach
        for (Node n : replicas) {
            sendReplicationRequest(n, res);            
        }        
        
        return r;
    }
    
    @Override
    public Request read(Request r) {
        Integer id = r.getResources().get(0);
        Resource res = read(id);
        sendResponse(r, res);
        return r;
    }
    
    @Override
    public Request replicate(ReplicationRequest r) {
        // apply only if higher,
       Resource resRep = r.getResource();
       Resource resLocal = read(resRep.getId());
        // /!\ must be atomic
        if (resLocal != null && resLocal.getVersion() < resRep.getVersion()) {
             write(resRep);
        }
       
        return r;
    }
    protected void sendReplicationRequest(Node n, Resource res) {
          Request req = new ReplicationRequest(res);
          req.setAppId(super.getId());
          req.setArtime(vm.getClock());
          this.sendOneWay(n, req);
      }
}
