package fr.isep.simizer.tutorial;

import fr.isep.simizer.Simulation;
import fr.isep.simizer.app.Application;
import fr.isep.simizer.laws.ConstantLaw;
import fr.isep.simizer.laws.GaussianLaw;
import fr.isep.simizer.laws.UniformLaw;
import fr.isep.simizer.network.Network;
import fr.isep.simizer.nodes.ClientNode;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.requests.Request;
import fr.isep.simizer.requests.RequestFactory;
import fr.isep.simizer.storage.StorageElement;

/**
 * Welcome to the second tutorial!
 *
 * Glad to see that you made it this far. :-)
 *
 * In the first tutorial, we went through the basic of configuring a simulation,
 * running it, and interpreting the results.  Now, in this tutorial, we'll dig
 * into the details of how to create the server processing code.  We'll do this
 * by creating a subclass of the Application class.
 */
public class Tutorial02 {
  private static void run() {
    // Let's go ahead and create some of the objects that we had in the last
    // simulation.  We'll keep it simple with just a single client and server,
    // but we'll have the client send multiple requests this time.
    Simulation simulation = new Simulation(10000);

    // Create the objects.
    VM server = new VM();
    ClientNode client = new ClientNode(0, 5);
    client.setServiceAddress(server);
    Network internet = new Network(new GaussianLaw(100, 20));

    // This is a convenience function that will add multiple Nodes to a Network.
    // In addition, it'll automatically add the Nodes to the Simulation if they
    // haven't been added yet.  This replaces five lines of code from the first
    // tutorial.  Neat!
    simulation.toNetworkAddNodes(internet, server, client);
    
    ClientNode.configureLaws(new UniformLaw(2), new ConstantLaw(10), null);

    RequestFactory factory = new RequestFactory();
    factory.addTemplate(0, new Request(0, "short", "", true));
    factory.addTemplate(1, new Request(0, "long", "", true));
    ClientNode.configureRequestFactory(factory);

    // You'll notice that we create the RequestFactory a little bit differently
    // and changed the "requests" law for the clients to be a UniforLaw.  For
    // our application, we're going to simulate sending two different kinds of
    // requests.  One of these will be a "short" action that can be processed
    // quickly, and the other is an action that takes more time to process.  We
    // want to client to choose between the two with equal probability.  That's
    // where the UniformLaw(2) comes in -- it chooses between 0 and 1 (the
    // integers less than 2) with equal probability.  We also add the
    // appropriate Request templates to the RequestFactory that the clients use.


    // The last step before running the Simulation is to create the Application
    // to handle the Requests.  Do do so, we'll create a subclass of the
    // Application class.  Normally, you would probably create a separate file
    // with the subclass, but I'm going to use an anonymous class to keep all
    // the code in one file.  Plus, this example isn't too long.

    // When creating the Application, we need to give it an ID.  We use zero
    // here since that is where the Requests will be sent.  In addition, we need
    // to define the amount of memory needed to run the application on a VM.
    Application handler = new Application(0, 20 * StorageElement.MEGABYTE) {

      @Override
      public void init(VM.TaskScheduler scheduler) {
        // We don't really have anything we need to do in the constructor.
        // However, to demonstrate its use, we'll print some information about
        // the application that is starting.
        System.out.println("Starting Application #" + getId());
        System.out.println("Running on Node " + vm.getId()
                + " (" + vm.getClass().getName() + ")");
      }

      @Override
      public void handle(VM.TaskScheduler scheduler, Node origin, Request request) {
        // This is where we implement the logic to handle each request.
        
        // To begin, let's start by switching on whether the Request's action
        // is "short" or "long."  Within the swithc, we'll want to define the
        // amount of processing time needed to complete the Request, so let's
        // also create a variable to track that.
        int instructions;
        switch (request.getAction()) {
          case "short":
          default:
            instructions = 10_000;
            break;

          case "long":
            instructions = 100_000_000;
            break;
        }

        scheduler.execute(instructions, StorageElement.MEGABYTE, null);
        scheduler.sendResponse(request, origin);
      }
    };

    // The final step before running the simulation is to deploy the Application
    // on the VM.  We do that with the deploy() method.
    server.deploy(handler);

    // Finally, let's run the simulation!
    simulation.runSim();


    // The output should look something like this, though the exact output
    // should be different depending on the randomization.

    // Starting Application #0
    // Running on Node 1 (simizer.nodes.VM)
    //   Request Client Errors      Start  Duration   N Delay  App Action;Params
    //         1      2      0         10       265       265    0 short;
    //         2      2      0        285       688       204    0 long;
    //         3      2      0        983       670       186    0 long;
    //         4      2      0       1663       242       242    0 short;
    //         5      2      0       1915       189       189    0 short;
    // Finished.  78 event(s) completed in 115ms.

    // Here, we can see that the ClientNode sent five requests.  Three of them
    // were "short" requests, and two of them were "long" requests.  We can see
    // that the Network delays are approximately the same for all of the
    // Requests, but also that the "long" requests have noticeably more
    // processing time.  Seems like the application worked!

    // Check out the next tutorial to learn about Resources and ResourceFactory
    // objects.
  }

  public static void main(String[] args) {
    run();
  }
}
