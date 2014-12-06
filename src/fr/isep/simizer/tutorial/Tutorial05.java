package fr.isep.simizer.tutorial;

import fr.isep.simizer.Simulation;
import fr.isep.simizer.app.Application;
import fr.isep.simizer.example.applications.LoadBalancedApplication;
import fr.isep.simizer.example.applications.LoadBalancerApplication;
import fr.isep.simizer.example.policy.RoundRobin;
import fr.isep.simizer.laws.ConstantLaw;
import fr.isep.simizer.laws.ExponentialLaw;
import fr.isep.simizer.laws.GaussianLaw;
import fr.isep.simizer.laws.UniformLaw;
import fr.isep.simizer.network.ClientGenerator;
import fr.isep.simizer.network.Network;
import fr.isep.simizer.nodes.ClientNode;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.nodes.VM.TaskScheduler;
import fr.isep.simizer.requests.Request;
import fr.isep.simizer.requests.RequestFactory;
import fr.isep.simizer.requests.RequestPrinter;
import fr.isep.simizer.storage.StorageElement;

/**
 * Welcome to the fifth tutorial.
 *
 * In this tutorial, we introduce the LoadBalancerApplication and
 * LoadBalancedApplication classes.  Although they only differ by a letter, they
 * serve two very different purposes.
 *
 * The LoadBalancerApplication class acts as a load balancer, receiving Requests
 * and forwarding them to other Application instances.  Those other instances
 * should inherit from LoadBalancedApplication.  This ensures that the the
 * custom instances properly register with the load balancer when they start up.
 * By registering with the load balancer, the load balancer knows that they
 * exist and can send them Requests to handle.
 *
 * In addition, using this tutorial gives us the opportunity to try a custom
 * output format for our data.  We need to use this in order to see which server
 * gets assigned to handle each of the Requests.
 */
public class Tutorial05 {
  // We need to use the same App ID for the load balancer and for the clients.
  // This is to make it seem like clients are sending to the application when
  // the requests are actually being handled by the load balancer.  When the
  // load balancer forwards the requests, the other instances need the same ID
  // so that they will also receive the Requests.
  private static final Integer APPLICATION_ID = 0;
  
  // Since we need multiple instances of this application, let's make it into
  // an actual class as opposed to an anonymous subclass.  This way, we can
  // easily instantiate multiple instances of the class.
  private static class ApplicationHandler extends LoadBalancedApplication {

    public ApplicationHandler(Node server, Integer loadBalancerId) {
      super(APPLICATION_ID, StorageElement.MEGABYTE, server, loadBalancerId);
    }

    @Override
    public void handle(TaskScheduler scheduler, Node origin, Request request) {
      int instructions;
      switch (request.getAction()) {
        case "short":
        default:
          instructions = 5_000_000;
          break;

        case "long":
          instructions = 100_000_000;
          break;
      }

      scheduler.execute(instructions, StorageElement.MEGABYTE, null);
      scheduler.sendResponse(request, origin);
    }
  }

  private static void run() {
    Simulation simulation = new Simulation(500);

    // Create the Network.
    Network internet = new Network(new GaussianLaw(100, 20));

    // The main server is where we will deploy the load balancer.
    VM mainServer = new VM();
    simulation.toNetworkAddNode(internet, mainServer);

    // Define the two Applications.  We'll need these when creating other
    // objects, so we want to create them early.  When we configure the clients,
    // we should configure them to send Requests to the load balancer.  That
    // application will then forward them to some instance of the actual
    // implementation of the application.

    Application loadBalancer = new LoadBalancerApplication(APPLICATION_ID,
            StorageElement.MEGABYTE,

            // We'll use a round-robin policy for scheduling the Requests to
            // keep things simple.  This sends the first Request to the first
            // server, the second Request to the second server, and so on.
            new RoundRobin());
    
    mainServer.deploy(loadBalancer);

    // Let's create a group of five servers to handle Requests from the clients.
    for (int i = 0; i < 5; i++) {
      VM server = new VM();
      // We deploy an instance of the custom Application on each of them.
      server.deploy(new ApplicationHandler(mainServer, loadBalancer.getId()));
      simulation.toNetworkAddNode(internet, server);
    }


    // Finally, let's create the Request templates and the clients.
    RequestFactory factory = new RequestFactory();
    factory.addTemplate(0, new Request(APPLICATION_ID, "short", "", true));
    factory.addTemplate(1, new Request(APPLICATION_ID, "long", "", true));

    ClientNode.configureRequestFactory(factory);
    ClientNode.configureLaws(new UniformLaw(2), new ConstantLaw(10),
            new ExponentialLaw(2000));

    // We'll use another ClientGenerator so that we have more than one client.
    ClientGenerator generator = new ClientGenerator(simulation, internet,
            new ConstantLaw(1), 100, mainServer, Integer.MAX_VALUE);

    // This time, we are also going to set another static property of the
    // ClientNode: the RequestPrinter.  A RequestPrinter is what defines the
    // output of the simulation.  In all of the other tutorials, we've used the
    // default RequestPrinter which prints a general summary of the data.
    // However, custom applications will likely need to customize the format of
    // the output.  Using this class, the output can be customized.

    // For this example, we want to know which of the servers handles a
    // particular Request.  Since the LoadBalancerApplication class is not part
    // of the Simizer framework, there isn't native support in the framework for
    // handling this custom behavior.  However, the Request class does provide
    // an interface for storing custom data.

    // Our implementation of LoadBalancerApplication stores the ID of the server
    // that handles each Request in the HANDLED_BY custom field.  We can
    // retrieve the value using that key so that we can include it in the
    // output.  As an example, we also remove some of the less interesting
    // fields and provide one "calculated" field.
    ClientNode.setRequestPrinter(new RequestPrinter(System.out) {
      @Override
      public void print(ClientNode client, Request request) {
        output.format("%5d %5s %5d %5d %s%n",
                request.getId(),
                request.get(LoadBalancerApplication.HANDLED_BY),
                request.getClientEndTimestamp()
                        - request.getClientStartTimestamp()
                        - request.getNetworkDelay(),
                request.getClientEndTimestamp(),
                request.getAction());
      }
    });

    // Lastly, let's run the simulation.
    simulation.runSim();


    // Here is a sample of the output:

    //     6     6    21   484 short
    //     8     2    21   622 short
    //    10     3    21   880 short
    //     7     4   484   954 long
    //     9     5   484  1245 long
    //    11     6   484  1347 long
    //    12     4   484  1468 long
    //    13     2   484  1740 long
    //    16     6    21  1759 short
    //    14     5   484  1811 long
    //    19     2    21  2115 short
    //    15     3   484  2143 long
    //    17     4   484  2419 long
    //    20     3    21  2548 short
    //    21     6    21  2620 short
    //    18     5   484  2667 long
    //    24     5    21  3050 short
    //    25     3    21  3090 short
    //    22     4   484  3362 long
    //    23     2   484  3393 long
    //    29     2    21  3760 short
    //    26     6   484  3944 long
    //    27     4   484  3968 long
    //    28     5   484  4284 long
    // Finished.  601 event(s) completed in 137ms.

    // Because of the randomness introduce by network delays, the order of the
    // servers handling the requests (the second column) don't necessarily go
    // in order, but you can see that there is approximately equal distribution
    // among all of the various Requests.
  }

  public static void main(String[] args) {
    run();
  }
}
