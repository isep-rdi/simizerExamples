package fr.isep.simizer.tutorial;

import fr.isep.simizer.Simulation;
import fr.isep.simizer.app.Application;
import fr.isep.simizer.laws.ConstantLaw;
import fr.isep.simizer.laws.ExponentialLaw;
import fr.isep.simizer.laws.GaussianLaw;
import fr.isep.simizer.laws.UniformLaw;
import fr.isep.simizer.network.ClientGenerator;
import fr.isep.simizer.network.Network;
import fr.isep.simizer.nodes.ClientNode;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.requests.Request;
import fr.isep.simizer.requests.RequestFactory;
import fr.isep.simizer.storage.StorageElement;

/**
 * Welcome to the fourth tutorial.
 *
 * Here, we'll create multiple clients that all send Requests to a single
 * server.  In the next example we'll add multiple servers to the simulation as
 * well, creating a "real cloud" for the simulation.
 *
 * For both of these examples, we'll go back to our simpler processor that
 * handles "long" and "short" actions.
 */
public class Tutorial04 {

  private static void run() {
    Simulation simulation = new Simulation(500);

    // Create the server and the Network.
    VM server = new VM();
    Network internet = new Network(new GaussianLaw(100, 20));
    simulation.toNetworkAddNode(internet, server);

    // We'll get to creating the clients shortly, but it's still necessary for
    // us to specify the behavior of the clients using the static ClientNode
    // methods.  We'll create the Request factory and then set up the Laws for
    // the clients.
    RequestFactory factory = new RequestFactory();
    factory.addTemplate(0, new Request(0, "short", "", true));
    factory.addTemplate(1, new Request(0, "long", "", true));

    ClientNode.configureRequestFactory(factory);
    ClientNode.configureLaws(new UniformLaw(2), new ConstantLaw(10),
            // This time, we specify the third Law.  This Law controls how long
            // the clients exist before they exit.  For this example, we'll use
            // an ExponentialLaw where the clients exist for 200 ms on average.
            new ExponentialLaw(2000));

    // Let's deploy the application that we created in the second tutorial.
    server.deploy(new Application(0, 20 * StorageElement.MEGABYTE) {

      @Override
      public void init(VM.TaskScheduler scheduler) {}

      @Override
      public void handle(VM.TaskScheduler scheduler, Node origin, Request request) {
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
    });


    // Finally, let's add the code to create the clients.  We could have done
    // this earlier, but I wanted to go through all of the review material
    // first.

    // To have the Simulation automatically create ClientNode objects, we use
    // a ClientGenerator class.  The ClientGenerator class will continue to
    // create clients until the length of the simulation has elapsed.  (That
    // value is set when creating the Simulation object.)  The Simulation will
    // not necessarily end at that point, but the ClientGenerator will stop
    // adding new clients at that point.
    
    // Let's initialize our ClientGenerator now.

    ClientGenerator generator = new ClientGenerator(simulation, internet, new ConstantLaw(2), 100, server, Integer.MAX_VALUE);

    // The constructor takes the follow parameters to create the
    // ClientGenerator: the simulation where the clients should be added, the
    // network that the clients should use, the Law defining the number of
    // clients who arrive in a given interval, the length of that interval, the
    // server where the clients should send their requests, and the maximum
    // number of simultaneous clients that the ClientGenerator should spawn.

    // One extra point about the Law/interval.  Every time "interval" elapsed,
    // the ClientGenerator will use the specified Law to determine the number of
    // clients to add.  The Law is NOT the amount of time between client spawns,
    // but it is the number of clients that are added each time the specified
    // interval elapses.

    // In addition, the final parameter can be used to limit the total number of
    // simultaneous clients in the system.  Even if the Law specifies that some
    // number of clients should be added, this value may restrict that number.
    // This could be useful to limit the total number of clients in the
    // simulation, but it does effectively alter the distribution of the
    // specified Law.

    // However, once a ClientNode has finished, the ClientGenerator will fill
    // that space (at one of the intervals AND if the Law says that ClientNodes
    // should be created).

    simulation.runSim();

    // Here is some sample output from the simulation.  For the first time, you
    // can see that there are multiple IDs listed for the clients.

    // Request Client Errors      Start  Duration   N Delay  App Action;Params
    //       4      5      0        110       198       177    0 short;
    //       6      7      0        210       240       219    0 short;
    //       9      5      0        318       233       218    0 short;
    //       8      9      0        310       258       238    0 short;
    //       7      8      0        310       280       255    0 short;
    //      11     11      0        410       229       204    0 short;
    //      12      7      0        460       228       203    0 short;
    //       2      3      0         10       732       186    0 long;
    //      14      9      0        578       186       177    0 short;
    //       1      2      0         10       774       214    0 long;
    //       3      4      0        110       703       199    0 long;
    //      15      8      0        600       225       204    0 short;
    //       5      6      0        210       660       195    0 long;
    //      20      8      0        835       209       180    0 short;
    //      10     10      0        410       718       155    0 long;
    //      13      5      0        561       781       203    0 long;
    //      16     11      0        649       771       193    0 long;
    //      17      3      0        752       736       154    0 long;
    //      18      9      0        774       744       185    0 long;
    //      19      2      0        794       743       207    0 long;
    //      21      6      0        880       744       211    0 long;
    //      23     11      0       1430       198       177    0 short;

    // In addition, if we once again look at the difference between the "N
    // Delay" and "Duration" columns to get the server processing time, we can
    // see that having multiple requests running simultaneously increased the
    // amount of processing time for each of them.

    // In example two where we ran this same application with a single client,
    // the long requests generally took 484 ms.  Now we can see that some of
    // them take more time.

    // 0
    // 21
    // 21
    // 15
    // 20
    // 25
    // 25
    // 25
    // 546
    // 9
    // 560
    // 504
    // 21
    // 465
    // 29
    // 563
    // 578
    // 578
    // 582
    // 559
    // 536
    // 533
    // 21


    // Check out the next tutorial for the one that brings them all together:
    // the tutorial on load balancing.  We finally talk about how to use
    // multiple servers to serve a single application.
  }

  public static void main(String[] args) {
    run();
  }
}
