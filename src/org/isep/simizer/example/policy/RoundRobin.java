package org.isep.simizer.example.policy;

import java.util.ArrayList;
import java.util.List;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.network.MessageReceiver;
import fr.isep.simizer.requests.Request;

public class RoundRobin extends Policy {

  /** Stores the list of Nodes used by this Policy. */
  private final List<VM> nodes = new ArrayList<>();

  /** Stores a request counter used to select a server for each Request. */
  protected int index = 0;

  @Override
  public MessageReceiver loadBalance(Request request) {
    int count = nodes.size();
    if (count > 0) {
      VM vm = nodes.get(index % count);
      index++;
      return vm;
    } else {
      return null;
    }
  }

  @Override
  public void initialize(List<VM> nodes) {
    nodes.addAll(nodes);
  }

  @Override
  public void addNode(VM vm) {
    if (!nodes.contains(vm)) {
      nodes.add(vm);
    }
  }

  @Override
  public void removeNode(VM vm) {
    if (nodes.contains(vm)) {
      nodes.remove(vm);
    }
  }

}
