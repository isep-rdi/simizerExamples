/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simizerexamples;

import java.util.logging.Level;
import java.util.logging.Logger;
import simizer.LBNode;
import simizer.ServerNode;
import simizer.Simulation;
import simizer.laws.UniLaw;
import simizer.network.Network;
import org.isep.simizer.example.policy.RoundRobin;
import simizer.processor.NewProcessor;
import simizer.processor.Processor;
import simizer.requests.Request;
import simizer.storage.StorageElement;
import simizer.utils.SimizerUtils;

/**
 *
 * @author isep
 */
public class QualifExample {
    public static void main(String ... args) {
        Simulation sim = new Simulation(20000);
         //LAN
        Network lan = new Network(new UniLaw(0));
        sim.addNetwork("net", lan);
        StorageElement.setFactory(SimizerUtils.getDefaultResourceFactory());
        Processor p = new NewProcessor(2, 1000.0);
        ServerNode sn = new ServerNode(0, 512000, 50, 0.0, new StorageElement(5120000,2), p);
        
        LBNode  frontend = new LBNode(1, new RoundRobin());
        sn.setFrontendNode(frontend);
        sim.addNodeToNet(sn,"net");
        sim.addNodeToNet(frontend,"net");
        
        int typeId = 0;
        String params = "p1=1";
        int arTime = 0;
        String type = "read";
        int nbInst = 10000;
        Request r1 = new Request(0,typeId,arTime, params,50, type, nbInst * 1000000L);
        Request r2 = new Request(1,typeId,arTime, params,50, type, nbInst * 1000000L);
        Request r3 = new Request(2,typeId,arTime, params,50, type, nbInst * 1000000L);
        
              
        lan.send(null, sn,r1 , 0);
        lan.send(null, sn, r2, 0);
        lan.send(null, sn, r3, 0);
        try {
            sim.runSim();
        } catch (Exception ex) {
            Logger.getLogger(QualifExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
