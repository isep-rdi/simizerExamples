package simizerexamples;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.isep.simizer.example.LoadBalancerApp;
import org.isep.simizer.example.consistency.OptimisticPolicy;
import static org.isep.simizer.example.consistency.OptimisticPolicy.REP_FACTOR;
import org.isep.simizer.example.consistency.PessimisticPolicy;
import org.isep.simizer.example.policy.ConsistentPolicy;
import org.isep.simizer.example.policy.utils.ConsistentHash;
import simizer.nodes.ClientNode;
import simizer.Simulation;
import simizer.nodes.VM;
import simizer.app.Application;
import simizer.laws.ExponentialLaw;
import simizer.laws.GaussianLaw;
import simizer.network.Network;
import simizer.requests.RequestFactory;
import simizer.storage.ResourceFactory;
import simizer.storage.StorageElement;

/**
 *
 * @author Sylvain Lefebvre
 */
public class ConsistencyExample {

  public static void main(String... args) {

    ClientNode.configureLaws(
            new GaussianLaw(15),
            new GaussianLaw(15),
            new ExponentialLaw(10000, 500.0));
    try {
      ClientNode.configureRequestFactory(
              new RequestFactory(RequestFactory.loadRequests("./readwrite.csv")));
    } catch (IOException ex) {
      Logger.getLogger(SimizerExamples.class.getName()).log(Level.SEVERE, null, ex);
    }

    //1. Network creation
    Network net = new Network(new GaussianLaw(15));

    //3. Create disks:
    ResourceFactory rf = new ResourceFactory(1000, 2000, 1024);
    StorageElement.setFactory(rf);
    StorageElement se1 = new StorageElement(5120000000L, 10L);
    StorageElement se2 = new StorageElement(5120000000L, 10L);

    //2. machines creation
    VM vm1 = new VM(1, net);
    ConsistentPolicy cp = new ConsistentPolicy();
    vm1.deploy(new LoadBalancerApp(0, 20000, new ConsistentPolicy()));

    OptimisticPolicy.hashRing = new ConsistentHash<>(REP_FACTOR, 1, null);
    PessimisticPolicy.hashRing = new ConsistentHash<>(REP_FACTOR, 1, null);
    VM vmApp0 = new VM(2, null, se1, net, VM.DEFAULT_MEMORY_SIZE, VM.DEFAULT_COST);
    VM vmApp1 = new VM(2, null, se2, net, VM.DEFAULT_MEMORY_SIZE, VM.DEFAULT_COST);

    cp.initialize(null);
    //cp.addNode(vmApp0);
    //cp.addNode(vmApp1);
    vm1.deploy(new LoadBalancerApp(0, 20000, cp));

    //3. Client creation
    ClientNode cn1 = new ClientNode(4, net, 0);
    ClientNode cn2 = new ClientNode(5, net, 0);

    cn1.setServiceAddress(vm1);
    cn2.setServiceAddress(vm1);

    Simulation sim = new Simulation(20000);
    sim.toNetworkAddNodes(net, vm1, vmApp0, vmApp1, cn1, cn2);

        // 5. deployment :
    Application app1, app2;
    switch (args[0]) {

      case "pessimistic":
        PessimisticPolicy.hashRing.add(vmApp0);
        PessimisticPolicy.hashRing.add(vmApp1);
        app1 = new PessimisticPolicy(0, 20000);
        app2 = new PessimisticPolicy(0, 20000);
        break;
      default:
        OptimisticPolicy.hashRing.add(vmApp0);
        OptimisticPolicy.hashRing.add(vmApp1);
        app1 = new OptimisticPolicy(0, 20000);
        app2 = new OptimisticPolicy(0, 20000);
        break;
    }

    app1.setConfig("frontend", "1");
    app2.setConfig("frontend", "1");

    vmApp0.deploy(app1);
    vmApp1.deploy(app2);

    try {
      sim.runSim();
    } catch (Exception ex) {
      Logger.getLogger(SimizerExamples.class.getName()).log(Level.SEVERE, null, ex);
    }

  }
}
