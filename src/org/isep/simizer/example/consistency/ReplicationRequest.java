/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.isep.simizer.example.consistency;

import simizer.nodes.Node;
import simizer.requests.Request;
import simizer.storage.Resource;

/**
 *
 * @author Sylvain Lefebvre
 */
public class ReplicationRequest extends Request {
    private final Resource data;
    private final Node origin;
    ReplicationRequest(Resource res) {
        this(res, null);
        
    }
    
     ReplicationRequest(Resource res, Node origin) {
        super("replicate");
        this.data= res;
        this.action = "replicate";
        this.params= Integer.toString(res.getVersion());
        this.origin = origin;
        
    }
    public Resource getResource() {
        return this.data;
    }
    
    public Node getOrigin() {
        return this.origin;
    }
}
