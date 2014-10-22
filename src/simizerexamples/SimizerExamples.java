/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ClientNode.configureLaws(
                new GaussianLaw(15), 
                new GaussianLaw(15), 
                new ExponentialLaw(10000,500.0));
        try {
            ClientNode.configureRequestFactory(
                    new RequestFactory(RequestFactory.loadRequests("./reqs.csv")));
        } catch (IOException ex) {
            Logger.getLogger(SimizerExamples.class.getName()).log(Level.SEVERE, null, ex);
        }
      
       //1. Network creation
        Network net = new Network(new GaussianLaw(15));
        
       //2. machines creation
        VM vm1 =new VM(1, net);
        vm1.deploy(new LoadBalancerApp(0, 20000, new RoundRobin()));
        VM vmApp0 = new VM(2,net),
            vmApp1 = new VM(3,net);
        InterpolationApp cap1 = new InterpolationApp(0);
        cap1.setFrontend(1);
        InterpolationApp cap2 = new InterpolationApp(0);
        cap2.setFrontend(1);
        ResourceFactory rf = new ResourceFactory(1000, 2000, 1024);
        StorageElement.setFactory(rf);
        StorageElement se = new StorageElement(5120000000L, 10L);
        se.write(rf.getStartList());
        vmApp0.setStorage(se);
        se = new StorageElement(5120000000L, 10L);
         se.write(rf.getStartList());
        vmApp1.setStorage(se);
        
        //3. Client creation
        ClientNode cn1 = new ClientNode(4,net, 0);
        ClientNode cn2 = new ClientNode(5, net, 0);
        
        cn1.setServiceAddress(vm1);
        cn2.setServiceAddress(vm1);
        Simulation sim = new Simulation(20000);
        sim.addClient(cn2);
        sim.addClient(cn1);
        sim.addNetwork("net", net);
        sim.addNodeToNet(vm1, "net");
        sim.addNodeToNet(vmApp1,"net");
        sim.addNodeToNet(vmApp0, "net");
        sim.addNodeToNet(cn2, "net");
        sim.addNodeToNet(cn1, "net");
        vmApp0.deploy(cap1);
        vmApp1.deploy(cap2);
        
        try {
            sim.runSim();
        } catch (Exception ex) {
            Logger.getLogger(SimizerExamples.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
