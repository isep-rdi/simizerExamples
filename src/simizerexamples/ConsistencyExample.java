package simizerexamples;

import fr.isep.simizer.Simulation;
import fr.isep.simizer.app.Application;
import fr.isep.simizer.example.applications.LoadBalancerApplication;
import fr.isep.simizer.example.consistency.OptimisticPolicy;
import static fr.isep.simizer.example.consistency.OptimisticPolicy.REP_FACTOR;
import fr.isep.simizer.example.consistency.PessimisticPolicy;
import fr.isep.simizer.example.policy.ConsistentPolicy;
import fr.isep.simizer.example.policy.utils.ConsistentHash;
import fr.isep.simizer.laws.ExponentialLaw;
import fr.isep.simizer.laws.GaussianLaw;
import fr.isep.simizer.network.Network;
import fr.isep.simizer.nodes.ClientNode;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.requests.RequestFactory;
import fr.isep.simizer.storage.ResourceFactory;
import fr.isep.simizer.storage.StorageElement;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sylvain Lefebvre
 */
public class ConsistencyExample {

  public static void main(String... args) {

    ClientNode.configureLaws(
            new GaussianLaw(7),  // previous upper bound was 15
            new GaussianLaw(7),  // previous upper bound was 15
            new ExponentialLaw(500.0));  // previous upper bound was 10000
    try {
      RequestFactory factory = new RequestFactory();
      factory.loadTemplates("./readwrite.csv");
      ClientNode.configureRequestFactory(factory);
    } catch (IOException ex) {
      Logger.getLogger(SimizerExamples.class.getName()).log(Level.SEVERE, null, ex);
    }

    //1. Network creation
    Network net = new Network(new GaussianLaw(7));  // previous upper bound was 15

    //3. Create disks:
    ResourceFactory rf = new ResourceFactory(1024, 1000);
    StorageElement se1 = new StorageElement(5120000000L, 10L);
    StorageElement se2 = new StorageElement(5120000000L, 10L);

    //2. machines creation
    VM vm1 = new VM();
    ConsistentPolicy cp = new ConsistentPolicy();
    vm1.deploy(new LoadBalancerApplication(0, 20000, new ConsistentPolicy()));

    OptimisticPolicy.hashRing = new ConsistentHash<>(REP_FACTOR, 1, null);
    PessimisticPolicy.hashRing = new ConsistentHash<>(REP_FACTOR, 1, null);
    VM vmApp0 = new VM(null, se1, VM.DEFAULT_MEMORY_SIZE, VM.DEFAULT_COST);
    VM vmApp1 = new VM(null, se2, VM.DEFAULT_MEMORY_SIZE, VM.DEFAULT_COST);

    cp.initialize(null);
    //cp.addNode(vmApp0);
    //cp.addNode(vmApp1);
    vm1.deploy(new LoadBalancerApplication(0, 20000, cp));

    //3. Client creation
    ClientNode cn1 = new ClientNode(0);
    ClientNode cn2 = new ClientNode(0);

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
        app1 = new PessimisticPolicy(0, 20000, vm1);
        app2 = new PessimisticPolicy(0, 20000, vm1);
        break;
      default:
        OptimisticPolicy.hashRing.add(vmApp0);
        OptimisticPolicy.hashRing.add(vmApp1);
        app1 = new OptimisticPolicy(0, 20000, vm1);
        app2 = new OptimisticPolicy(0, 20000, vm1);
        break;
    }

    vmApp0.deploy(app1);
    vmApp1.deploy(app2);

    sim.runSim();
  }
}
