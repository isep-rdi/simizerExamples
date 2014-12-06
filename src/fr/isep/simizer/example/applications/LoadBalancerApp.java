package fr.isep.simizer.example.applications;

import fr.isep.simizer.app.Application;
import fr.isep.simizer.example.policy.Policy;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.nodes.VM.TaskScheduler;
import fr.isep.simizer.requests.Request;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for handling and applying load balancing policies. Nodes register to
 * the application through requests sending.
 *
 * @author Sylvain Lefebvre
 */
public class LoadBalancerApp extends Application {

  private final Policy pol;
  private final Policy.Callback pac;
  private final Map<Long, Node> pending = new HashMap<>();

  public LoadBalancerApp(int id, int memSize, Policy pol) {
    super(id, memSize);
    this.pol = pol;
    if (pol instanceof Policy.Callback) {
      this.pac = (Policy.Callback) pol;
    } else {
      this.pac = null;
    }
  }

  /**
   * Checks whether the request is an application request, a registration
   * request or a response. forwards to the appropriate method.
   *
   * @param orig
   * @param req
   */

  @Override
  public void handle(TaskScheduler scheduler, Node orig, Request req) {
    if (req.getServerFinishTimestamp() == 0) { // Registration or application request
      if (req.getAction().equals("register")) {
        // if it is registering, it must be a VM
        handleRegisterRequest((VM) orig, req);
      } else {
        handleAppRequest(scheduler, orig, req);
      }
    } else {
      // if it is a response, it must be coming from a VM
      handleResponse((VM) orig, req);
    }
  }

  /**
   * Called if the request is an application request, applies the specified load
   * balancing policy and records the client for sending the response.
   *
   * @param scheduler
   * @param orig
   * @param req
   */
  public void handleAppRequest(TaskScheduler scheduler, Node orig, Request req) {
    pending.put(req.getId(), orig);

    long start = System.nanoTime();
    Node target = (Node) pol.loadBalance(req);
    if (target == null) {
      req.reportErrors(1);
      scheduler.sendResponse(req, pending.remove(req.getId()));
    } else {
      req.set("loadBalancingDelay", System.nanoTime() - start);
      scheduler.sendRequest(target, req);
    }

  }

  /**
   * Called when a "register" request is received, adds the originating node to
   * the load balanced machines list in the policy.
   *
   * @param orig
   * @param req
   */
  public void handleRegisterRequest(VM orig, Request req) {
    pol.addNode(orig);
  }

  /**
   * Sends the response to the client that sent the request, removes the client
   * from the pending list.
   *
   * @param orig
   * @param req
   */
  public void handleResponse(VM orig, Request req) {
    if (pac != null) {
      pac.receivedRequest(orig, req);
    }
    if (pending.containsKey(req.getId())) {
      vm.send(req, pending.remove(req.getId()));
    }
  }

  @Override
  public void init(TaskScheduler scheduler) {}

}
