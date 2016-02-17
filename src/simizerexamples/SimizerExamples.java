package simizerexamples;

import fr.isep.simizer.Simulation;
import fr.isep.simizer.example.applications.LoadBalancerApplication;
import fr.isep.simizer.example.policy.RoundRobin;
import fr.isep.simizer.laws.ExponentialLaw;
import fr.isep.simizer.laws.GaussianLaw;
import fr.isep.simizer.network.Network;
import fr.isep.simizer.nodes.ClientNode;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.requests.RequestFactory;
import fr.isep.simizer.storage.ResourceFactory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author isep
 */
public class SimizerExamples {

  public static void main(String[] args) throws Exception {

    ClientNode.configureLaws(
            new GaussianLaw(7),  // previous upper bound was 15
            new GaussianLaw(7),  // previous upper bound was 15
            new ExponentialLaw(500.0));  // previous upper bound was 10000
    try {
      RequestFactory factory = new RequestFactory();
      factory.loadTemplates("./reqs.csv");
      ClientNode.configureRequestFactory(factory);
    } catch (IOException ex) {
      Logger.getLogger(SimizerExamples.class.getName()).log(Level.SEVERE, null, ex);
    }

    //1. Network creation
    Network net = new Network(new GaussianLaw(7));  // previous upper bound was 15

    //2. machines creation
    VM vm1 = new VM();
    vm1.deploy(new LoadBalancerApplication(0, 20000, new RoundRobin()));

    ResourceFactory rf = new ResourceFactory(1024, 1000);

    //3. Client creation
    ClientNode cn1 = new ClientNode(0);
    ClientNode cn2 = new ClientNode(0);

    cn1.setServiceAddress(vm1);
    cn2.setServiceAddress(vm1);

    Simulation sim = new Simulation(20000);
    sim.toNetworkAddNodes(net, vm1, cn1, cn2);

    sim.runSim();
  }
}
