package org.isep.simizer.example.consistency;

import java.util.HashMap;
import java.util.Map;
import simizer.app.Application;
import simizer.nodes.Node;
import simizer.nodes.VM.TaskScheduler;
import simizer.requests.Request;

/**
 *
 * @author Sylvain Lefebvre
 */
public abstract class StoreApplication extends Application {

  /**
   * Stores the return addresses for the in-progress {@code Request}s.
   * <p>
   * The {@link Node}s where the responses should be sent are stored with a key
   * that is the ID of the {@link Request}.
   */
  protected final Map<Long, Node> pendingRequests;

  public StoreApplication(int id, int memSize) {
    super(id, memSize);
    this.pendingRequests = new HashMap<>();
  }

  /**
   * Initializes the application, registering with the load balancer.
   *
   * @param scheduler the {@link TaskScheduler} where the initialization
   *            operations should occur
   */
  @Override
  public void init(TaskScheduler scheduler) {
    Request registerRequest = new Request(0, "register", "");

    Node destination = vm.getNetwork().getNode(
            Integer.parseInt(config.getProperty("frontend")));
    scheduler.sendRequest(destination, registerRequest);
  }

  /**
   * Dispatches the request to the appropriate application-specific handler.
   *
   * @param scheduler the {@link TaskScheduler} where the sequence of actions
   *            should be built
   * @param origin the {@link Node} where the {@link Request} originated
   * @param request the {@link Request} that was sent
   */
  @Override
  public void handle(TaskScheduler scheduler, Node origin, Request request) {
    switch (request.getAction()) {
      case "read":
        this.pendingRequests.put(request.getId(), origin);
        this.handleRead(scheduler, request);
        break;
      case "write":
        this.pendingRequests.put(request.getId(), origin);
        this.handleWrite(scheduler, request);
        break;
      case "replicate":
        if (request instanceof ReplicationRequest) {
          this.replicate(scheduler, (ReplicationRequest) request);
        }
        break;
    }
  }

  protected void sendResponse(TaskScheduler scheduler, Request request) {
    Node orig = pendingRequests.remove(request.getId());
    scheduler.sendResponse(request, orig);
  }

  public abstract Request handleRead(TaskScheduler scheduler, Request request);

  public abstract Request handleWrite(TaskScheduler scheduler, Request request);

  /**
   * Replication message reception
   *
   * @param scheduler
   * @param request
   * @return
   */
  public abstract Request replicate(TaskScheduler scheduler,
          ReplicationRequest request);

}
