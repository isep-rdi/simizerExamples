package org.isep.simizer.example.policy;

import java.util.List;
import simizer.nodes.VM;
import simizer.network.MessageReceiver;
import simizer.requests.Request;

public abstract class Policy {

  public abstract void initialize(List<VM> availableNodes);

  public abstract void addNode(VM node);

  public abstract void removeNode(VM node);

  public abstract MessageReceiver loadBalance(Request request);

  public void printAdditionnalStats() {
    System.out.println("No additional stats for: " + this.getClass().getName());
  }

  public abstract static class Callback extends Policy {
    public abstract void receivedRequest(VM node, Request request);
  }

}
