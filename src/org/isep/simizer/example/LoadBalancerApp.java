/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.isep.simizer.example;

import java.util.HashMap;
import java.util.Map;
import simizer.Node;
import simizer.app.Application;
import simizer.policy.Policy;
import simizer.policy.PolicyAfterCallback;
import simizer.requests.Request;

/**
 * Class for handling and applying load balancing policies.
 * Nodes register to the application through requests sending.
 * @author Sylvain Lefebvre
 * 
 */
public class LoadBalancerApp extends Application {

    private final Policy pol;
    private PolicyAfterCallback pac = null;
    private final Map<Long, Node> pending = new HashMap<>();

    public LoadBalancerApp(int id, int memSize, Policy pol) {
        super(id, memSize);
        this.pol = pol;
        if (pol instanceof PolicyAfterCallback) {
            this.pac = (PolicyAfterCallback) pol;
        }
    }
    /**
     * Checks whether the request is an application request, a registration request or a response.
     * forwards to the appropriate method.
     * @param orig
     * @param req 
     */

    @Override
    public void handle(Node orig, Request req) {

        if (req.getFtime() == 0) { // Registration or application request
            if (req.getParameters().equals("register")) {
                handleRegisterRequest(orig, req);
            } else {
                handleAppRequest(orig, req);
            }
        } else {
            handleResponse(orig, req);

        }
    }
    /**
     * Called if the request is an application request, applies the specified load balancing policy
     * and records the client for sending the response.
     * 
     * @param orig
     * @param req 
     */
    public void handleAppRequest(Node orig, Request req) {
        pending.put(req.getId(), orig);

        long start = System.nanoTime();
        Node target = pol.loadBalance(req);
        if(target==null) {
            req.setError(1);
            vm.send(req, pending.remove(req.getId()));
        } else {
            req.setFwdTime(System.nanoTime() - start);
            vm.send(req,target);
        }

    }
    /**
     * Called when a "register" request is received, 
     * adds the originating node to the load balanced machines list in the policy.
     * 
     * @param orig
     * @param req 
     */
    public void handleRegisterRequest(Node orig, Request req) {
        pol.addNode(orig);
    }
    
    /**
     * Sends the response to the client that sent the request,
     * removes the client from the pending list.
     * @param orig
     * @param req 
     */

    public void handleResponse(Node orig, Request req) {
       
        if (pac != null) {
            pac.receivedRequest(orig, req);
        }
        if (pending.containsKey(req.getId())) {
            vm.send(req, pending.remove(req.getId()));
        }

    }

    @Override
    public void init() {
    }
}
