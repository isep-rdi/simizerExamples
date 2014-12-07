package fr.isep.simizer.example.consistency;

import fr.isep.simizer.example.policy.utils.ConsistentHash;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM.TaskScheduler;
import fr.isep.simizer.requests.Request;
import fr.isep.simizer.storage.Resource;
import java.util.List;

/**
 *
 * @author Sylvain Lefebvre
 */
public class PessimisticPolicy extends StoreApplication {

  public static final int REP_FACTOR = 2;

  public static ConsistentHash<Node> hashRing = null;

  public PessimisticPolicy(int id, int memSize, Node server) {
    super(id, memSize, server);
  }

  /**
   * This handle adds a "acknowledgement request" case for replication
   *
   * @param scheduler
   * @param origin
   * @param request
   */
  @Override
  public void handle(TaskScheduler scheduler, Node origin, Request request) {
    // check if this is replication answer.
    if (request instanceof ReplicationRequest && request.getServerFinishTimestamp() > 0) {
      this.handleReplicationAck(origin, (ReplicationRequest) request);
    } else {
      super.handle(scheduler, origin, request);
    }
  }

  /**
   * Registers in hashring. Locks the hashring for addition since it is based on
   * TreeMap which is not synchronized.
   */
  @Override
  public void init(TaskScheduler scheduler) {
    super.init(scheduler);
    synchronized (hashRing) {
      hashRing.add(scheduler.getVM());
    }
  }

  @Override
  public Request handleRead(TaskScheduler scheduler, Request request) {
    Integer id = request.getResources().get(0);
    // we don't use the result, but we want to simulate the read
    scheduler.read(id);
    sendResponse(scheduler, request);
    return request;
  }

  @Override
  public Request handleWrite(TaskScheduler scheduler, Request request) {
    // Local handleWrite
    Integer id = request.getResources().get(0);
    Integer val = new Integer(request.getParameter("val"));
    Resource resource = scheduler.read(id);
    if (resource == null) {
      resource = new Resource(id);
      resource.setVersion(val);
    } else if (resource.getVersion() < val) {
      resource.setVersion(val);
    }

    scheduler.write(resource, resource.size());

    List<Node> replicas = hashRing.getList(resource.getId());
    replicas.remove(scheduler.getVM());
    // fire and forget : optimistic approach
    for (Node node : replicas) {
      sendReplicationRequest(scheduler, node, resource);
    }
    sendResponse(scheduler, request);
    return request;
  }

  @Override
  public Request replicate(TaskScheduler scheduler, ReplicationRequest request) {
    // store it so that it can be removed when we respond in a few lines
    pendingRequests.put(request.getId(), request.getOrigin());

    Resource requestResource = request.getResource();
    Resource localResource = scheduler.read(requestResource.getId());
    // /!\ must be atomic
    if (localResource == null || localResource.getVersion() < requestResource.getVersion()) {
      scheduler.write(requestResource, requestResource.size());
    }
    sendResponse(scheduler, request);
    return request;
  }

  protected void sendReplicationRequest(TaskScheduler scheduler, Node node,
          Resource resource) {

    Request request = new ReplicationRequest(getId(), resource, scheduler.getVM());
    request.setClientStartTimestamp(scheduler.getVM().getClock());
    scheduler.sendRequest(node, request);
  }

  private void handleReplicationAck(Node origin,
          ReplicationRequest replicationRequest) {}
}
