package org.isep.simizer.example.policy;

import java.util.List;
import simizer.requests.Request;
import org.isep.simizer.example.policy.utils.ConsistentHash;
import simizer.VM;
import simizer.network.MessageReceiver;

public class ConsistentPolicy extends Policy {

  ConsistentHash<VM> ch = null;

  @Override
  public void initialize(List<VM> availableNodes) {
    int replicas = 1;
    if (availableNodes != null && availableNodes.size() > 0) {
      replicas = (int) Math.round(Math.log(availableNodes.size()));
    }
    if (replicas < 1) {
      replicas = 1;
    }

    ch = new ConsistentHash<>(replicas, availableNodes);
  }

  /**
   * {@inheritDoc}
   * <p>
   * This implementation assumes that {@link #initialize(java.util.List)} has
   * already been called.  This should always be the case.
   *
   * @param request the {@link Request} to balance
   * @return the {@link MessageReceiver} that should receive the {@link Request}
   */
  @Override
  public MessageReceiver loadBalance(Request request) {
    return ch.get(request.getParameters().split("=")[1]);
  }

  @Override
  public void addNode(VM vm) {
    ch.add(vm);
  }

  @Override
  public void removeNode(VM vm) {
    ch.remove(vm);
  }

}
