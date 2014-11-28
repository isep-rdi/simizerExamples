/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.isep.simizer.example.consistency;

import java.util.HashMap;
import java.util.Map;
import simizer.nodes.Node;
import simizer.app.Application;
import simizer.requests.Request;
import simizer.storage.Resource;

/**
 *
 * @author Sylvain Lefebvre
 */
public abstract class StoreApplication extends Application {
    protected final Map<Long, Node> pendingClients;
    public StoreApplication(int id, int memSize) {
        super(id, memSize);
        this.pendingClients = new HashMap<>();
    }
    
    /**
     * Does nothing yet
     */
    @Override
    public void init() {
        Request registerRequest = new Request("register");
        registerRequest.setAppId(0);
        
        vm.send(registerRequest, 
                    vm.getNetwork().getNode(Integer.parseInt(config.getProperty("frontend"))));
    }
    /**
     * Dispatches the request to read write or replicate methods
     * @param orig
     * @param req 
     */
    @Override
    public void handle(Node orig, Request req) {
        
        switch(req.getAction()) {
            case "read":
                this.pendingClients.put(req.getId(), orig);
                this.read(req);
                break;
            case "write":
                this.pendingClients.put(req.getId(), orig);
                this.write(req);
                break;
            case "replicate":
                if(req instanceof ReplicationRequest) 
                    this.replicate((ReplicationRequest) req);
                break;
                    
        }

                
    }
    
     protected void sendResponse(Request r, Resource res) {
         Node orig = pendingClients.remove(r.getId());
         vm.sendResponse(r, orig);
     }
    
    
    
    protected void write(Resource res) {
        this.vm.write(res, (int) res.size());
    }


    public abstract Request read(Request r);
    public abstract Request write(Request r);
    /**
     * Replication message reception
     * @param r
     * @return 
     */
    public abstract Request replicate(ReplicationRequest r);
    

}
