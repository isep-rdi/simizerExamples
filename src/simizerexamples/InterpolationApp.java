/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simizerexamples;

import java.util.ArrayList;
import java.util.List;
import simizer.Node;
import simizer.app.Application;
import simizer.requests.Request;
import simizer.storage.Resource;

/**
 *
 * @author isep
 */
public class InterpolationApp extends Application {
    /**
     * @TODO move these constants in Units class
     */
    private static int KILO = 1024;
    
    public InterpolationApp() {
        super(0,128*KILO*KILO);
        
    }
    public InterpolationApp(int port) {
        super(port,128*KILO*KILO);
    }
    @Override
    public void init() {
        Request registerRequest = new Request("register");
        registerRequest.setAppId(0);
        vm.send(registerRequest, 
                    vm.getNetwork().getNode(Integer.parseInt(config.getProperty("frontend"))));
    }
    /**
     * Adds the frontend id to application configuration
     * @param ftdId 
     */
    public void setFrontend(int ftdId) {
        this.config.setProperty("frontend", Integer.toString(ftdId));
    }
    @Override
    public void handle(Node orig, Request req) {
       
        List<Resource> rList =new ArrayList<>();
    
        for(Integer rId: req.getResources()) {
            Resource r = vm.read(rId, 15 *KILO);
            if(r!=null)
                rList.add(r);
        }
        
        // If some files don't exist we send an error back to the
        // client.
        if(rList.size() == req.getResources().size()) {
            long nbInst = 24*15*1500*rList.size();
             vm.execute(nbInst, 15*KILO*rList.size(), rList);
        }
        else {
            req.reportErrors(req.getResources().size() - rList.size());
        }
        
        vm.sendResponse(req, orig);
        
        
    }
    
}
