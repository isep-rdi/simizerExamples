package org.isep.simizer.example.policy;

import java.util.List;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.requests.Request;

public class WacaAfter extends Policy.Callback {

  @Override
  public void initialize(List<VM> availableNodes) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void addNode(VM vm) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void removeNode(VM vm) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Node loadBalance(Request r) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void receivedRequest(VM vm, Request request) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

}
