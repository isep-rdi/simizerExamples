package simizerexamples;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.isep.simizer.example.LoadBalancerApp;
import simizer.ClientNode;
import simizer.Simulation;
import simizer.VM;
import simizer.laws.ExponentialLaw;
import simizer.laws.GaussianLaw;
import simizer.network.Network;
import org.isep.simizer.example.policy.RoundRobin;
import simizer.requests.RequestFactory;
import simizer.storage.ResourceFactory;
import simizer.storage.StorageElement;

/**
 *
 * @author isep
 */
public class SimizerExamples {

  public static void main(String[] args) {

    ClientNode.configureLaws(
            new GaussianLaw(15),
            new GaussianLaw(15),
            new ExponentialLaw(10000, 500.0));
    try {
      ClientNode.configureRequestFactory(
              new RequestFactory(RequestFactory.loadRequests("./reqs.csv")));
    } catch (IOException ex) {
      Logger.getLogger(SimizerExamples.class.getName()).log(Level.SEVERE, null, ex);
    }

    //1. Network creation
    Network net = new Network(new GaussianLaw(15));

    //2. machines creation
    VM vm1 = new VM(1, net);
    vm1.deploy(new LoadBalancerApp(0, 20000, new RoundRobin()));

    ResourceFactory rf = new ResourceFactory(1000, 2000, 1024);
    StorageElement.setFactory(rf);

    //3. Client creation
    ClientNode cn1 = new ClientNode(4, net, 0);
    ClientNode cn2 = new ClientNode(5, net, 0);

    cn1.setServiceAddress(vm1);
    cn2.setServiceAddress(vm1);

    Simulation sim = new Simulation(20000);
    sim.toNetworkAddNodes(net, vm1, cn1, cn2);

    try {
      sim.runSim();
    } catch (Exception ex) {
      Logger.getLogger(SimizerExamples.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
