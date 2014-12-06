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
 * Class for handling and applying load balancing policies.
 * <p>
 * The {@link VM}s that are serving the "real" {@link Application} should
 * subclass {@code LoadBalancedApplication} so that they can be properly
 * registered with this load balancer.
 * <p>
 * When this {@link Application} receives a {@link Request}, it will forward it
 * according to its load balancing policy.
 *
 * @author Sylvain Lefebvre
 */
public class LoadBalancerApplication extends Application {

  public static final String HANDLED_BY = "handledBy";

  /** The load balancing policy that we should use. */
  private final Policy policy;

  /** Links Request IDs to the {@code Node} that sent them. */
  private final Map<Long, Node> pending = new HashMap<>();

  public LoadBalancerApplication(int id, long memSize, Policy policy) {
    super(id, memSize);

    this.policy = policy;
  }

  @Override
  public void init(TaskScheduler scheduler) {}


  /**
   * Checks whether the request is an application request, a registration
   * request or a response. forwards to the appropriate method.
   *
   * @param origin
   * @param request
   */

  @Override
  public void handle(TaskScheduler scheduler, Node origin, Request request) {
    if (pending.containsKey(request.getId())) {
      // if it is a response, it must be coming from a VM
      handleResponse(scheduler, (VM) origin, request);
    } else {
      if (request.getAction().equals("register")) {
        // if it is registering, it must be a VM
        handleRegisterRequest((VM) origin, request);
      } else {
        handleAppRequest(scheduler, origin, request);
      }
    }
  }

  /**
   * Called if the request is an application request, applies the specified load
   * balancing policy and records the client for sending the response.
   *
   * @param scheduler
   * @param origin
   * @param request
   */
  public void handleAppRequest(TaskScheduler scheduler, Node origin, Request request) {
    pending.put(request.getId(), origin);

    long start = System.nanoTime();
    Node target = (Node) policy.loadBalance(request);
    if (target == null) {
      request.reportErrors(1);
      scheduler.sendResponse(request, pending.remove(request.getId()));
    } else {
      request.set("loadBalancingDelay", System.nanoTime() - start);
      request.set(HANDLED_BY, target.getId().toString());
      scheduler.sendRequest(target, request);
    }
  }

  /**
   * Called when a "register" request is received, adds the originating node to
   * the load balanced machines list in the policy.
   *
   * @param origin
   * @param request
   */
  public void handleRegisterRequest(VM origin, Request request) {
    policy.addNode(origin);
  }

  /**
   * Sends the response to the client that sent the request, removes the client
   * from the pending list.
   *
   * @param scheduler
   * @param origin
   * @param request
   */
  public void handleResponse(TaskScheduler scheduler, VM origin, Request request) {
    if (policy instanceof Policy.Callback) {
      ((Policy.Callback) policy).receivedRequest(origin, request);
    }

    Node source = pending.remove(request.getId());
    if (source != null) {
      scheduler.sendResponse(request, source);
    } else {
      // TODO: Handle this error in some way.  It shouldn't happen for
      // well-behaved simulations.
    }
  }

}
