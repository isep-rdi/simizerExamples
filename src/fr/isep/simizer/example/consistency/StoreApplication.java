package fr.isep.simizer.example.consistency;

import fr.isep.simizer.example.applications.LoadBalancedApplication;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM.TaskScheduler;
import fr.isep.simizer.requests.Request;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Sylvain Lefebvre
 */
public abstract class StoreApplication extends LoadBalancedApplication {

  /**
   * Stores the return addresses for the in-progress {@code Request}s.
   * <p>
   * The {@link Node}s where the responses should be sent are stored with a key
   * that is the ID of the {@link Request}.
   */
  protected final Map<Long, Node> pendingRequests;

  public StoreApplication(int id, int memSize, Node server) {
    super(id, memSize, server);
    this.pendingRequests = new HashMap<>();
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
