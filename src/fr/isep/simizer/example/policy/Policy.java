package fr.isep.simizer.example.policy;

import fr.isep.simizer.network.MessageReceiver;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.requests.Request;
import java.util.List;

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
