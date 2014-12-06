package fr.isep.simizer.example.consistency;

import java.util.List;
import fr.isep.simizer.example.policy.utils.ConsistentHash;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM.TaskScheduler;
import fr.isep.simizer.requests.Request;
import fr.isep.simizer.storage.Resource;

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
   * Initializes, registering in the hash ring.
   * <p>
   * Locks the hashRing for addition since it is based on TreeMap which is not
   * synchronized.  Note that under the current implementation of the framework,
   * this doesn't really do anything since the application is not
   * multi-threaded.
   *
   * @param scheduler the {@link TaskScheduler} for this operation
   */
  @Override
  public void init(TaskScheduler scheduler) {
    super.init(scheduler);
    
    synchronized (hashRing) {
      hashRing.add(this.vm);
    }
  }

  /**
   * Writes locally without waiting for acknowledgment
   *
   * @param scheduler
   * @param request
   * @return
   */
  @Override
  public Request handleWrite(TaskScheduler scheduler, Request request) {
    // Local handleWrite
    Integer id = request.getResources().get(0);
    Integer val = new Integer(request.getParameter("val"));
    Resource res = scheduler.read(id);
    if (res == null) {
      res = new Resource(id);
      res.setVersion(val);
    } else if (res.getVersion() < val) {
      res.setVersion(val);
    }
    scheduler.write(res, res.size());

    // asynchronous, we reply before replication
    sendResponse(scheduler, request);

    List<Node> replicas = hashRing.getList(res.getId());
    replicas.remove(this.vm);
    // fire and forget : optimistic approach
    for (Node node : replicas) {
      sendReplicationRequest(scheduler, node, res);
    }

    return request;
  }

  @Override
  public Request handleRead(TaskScheduler scheduler, Request request) {
    Integer id = request.getResources().get(0);
    // we ignore the read, but we still want it to occur for timing purposes
    scheduler.read(id);
    sendResponse(scheduler, request);
    return request;
  }

  @Override
  public Request replicate(TaskScheduler scheduler,
          ReplicationRequest request) {

    // apply only if higher,
    Resource requestResource = request.getResource();
    Resource localResource = scheduler.read(requestResource.getId());
    
    // /!\ must be atomic
    if (localResource != null && localResource.getVersion() < requestResource.getVersion()) {
      scheduler.write(requestResource, requestResource.size());
    }

    return request;
  }

  protected void sendReplicationRequest(TaskScheduler scheduler, Node node,
          Resource resource) {

    Request request = new ReplicationRequest(getId(), resource);
    request.setClientStartTimestamp(vm.getClock());
    scheduler.sendOneWay(node, request);
  }
}
