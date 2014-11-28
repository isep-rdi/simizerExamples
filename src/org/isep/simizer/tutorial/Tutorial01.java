package org.isep.simizer.tutorial;

import java.util.HashMap;
import simizer.Simulation;
import simizer.laws.GaussianLaw;
import simizer.laws.ConstantLaw;
import simizer.network.Network;
import simizer.nodes.ClientNode;
import simizer.nodes.VM;
import simizer.requests.Request;
import simizer.requests.RequestFactory;

/**
 * Welcome to the tutorial!
 * 
 * The files in this package provide an introduction to the Simizer framework.
 * While it is not necessary to follow through the tutorial in order, the
 * examples grow in complexity as you go through them, and it is probably
 * easiest to start at the beginning.
 *
 * Having said that, let's get started!
 *
 * The Simizer framework is a tool for simulating the interactions between
 * clients and servers in a large-scale "cloud computing" environment.  The user
 * (that's you) defines the various "nodes" (clients, servers, etc.) and their
 * behavior, configures the parameters of the simulation (for example, network
 * delays and simulation duration), and then runs the simulation.
 *
 * As we go through the tutorial, the exact behavior will become more apparent,
 * but I think it's probably best to start with a simple example.
 */
public class Tutorial01 {

  private static void run() throws Exception {
    // Start by creating an instance of the Simulation class.  Throughout the
    // example, we will add other components to the Simulation, and then we will
    // run the Simulation at the end (by calling runSim()).  (If you don't
    // believe me, jump to line 88 now.)
    Simulation simulation = new Simulation(10000);

    // Next let's create our client machine.  We'll use a very simple client
    // that only sends a single request for this first example.

    // When creating a ClientNode, we have to specify two parameters. The first
    // is the timestamp in the Simulation when the client should start sending
    // its requests.  Since we want it to start right away, we'll pass zero for
    // this value.  The final parameter is the one that we care about the most.
    // It's the number of requests that the client should send before finishing.
    // In this example, we have it set to 1.  Feel free to adjust the value and
    // re-run the Simulation to see the results.
    ClientNode client = new ClientNode(0, 1);

    // We now need to define the Request that our client will send to the
    // server.  The ClientNode retrieve Requests from templates that are stored
    // in a RequestFactory instance, so let's create one now.
    RequestFactory factory = new RequestFactory(new HashMap<Integer, Request>());
    factory.addRequest(1, new Request(0, 0, "p=1", 1000, "read", 0));
    ClientNode.configureRequestFactory(factory);

    // Next, we need to define the behavior of the ClientNode.  All of the
    // clients are controlled by three Laws.  A Law introduces randomness into
    // the simulation according to a probability distribution.

    ClientNode.configureLaws(
            // The first Law determines which Requests clients will send.  Since
            // we only put one Request into our RequestFactory, we want to
            // configure the client to use that Request.  To do that, we can use
            // a ConstantLaw that always returns the same value.
            new ConstantLaw(1),

            // The next Law determines how much time clients will spend thinking
            // between each request.  This can be thought of as the amount of
            // time a view spends looking at the result that was returned.
            // Since we are only sending a single request, it does not matter
            // which law we use for this value.
            new ConstantLaw(0),

            // Finally, this law defines the lifetime of clients.  Since we
            // created our client to send a specific number of requests, this
            // value is ignored in our Simulation.  We can therefore leave it
            // as null.
            null);

    // Now that we have the client, let's create a simple server.  The VM class
    // is a general machine that we can use as a server.  To get started, let's
    // create an instance of the class.
    VM server = new VM();

    // Now, we need to establish a way for the client and the server to
    // communicate.  To do that, we'll use a Network.  A network allows messages
    // to be sent between the various nodes in a simulation.

    // Here, we create the "internet" Network.  When creating the Network, we
    // specify a Law that defines how much delay should be added to requests
    // when they pass through the Network.  In this case, we are using a
    // Gaussian (normal) distribution with an upper-bound of 60.  {TODO}  You
    // will find that Law instances are used throughout the simulation to add
    // customizable randomness.
    Network internet = new Network(new GaussianLaw(60));

    // Finally, we need to connect everything up so that all of the various
    // elements know about each other.
    
    // First, tell the client where it should send its Requests.
    client.setServiceAddress(server);
    
    // For the others, we use the Simulation instance that we created at the
    // beginning of this method.  Add the Nodes to the Simulation.
    simulation.addNode(client);
    simulation.addNode(server);

    // Next, add the Network to the Simulation.
    simulation.addNetwork(internet);

    // Finally, associate the Nodes we added with the Network we added.
    simulation.toNetworkAddNode(internet, client);
    simulation.toNetworkAddNode(internet, server);

    // Last, but certainly not least, we want to run the Simulation.
    simulation.runSim();

    // Try running this file now to see the results.

    // There should be a single line of output, and it will look something like
    // this:

    //     0;0;p=1;47;98;0;0;0.0;1r;98;1

    // The Simulation will print each response that a client receives.  In
    // order, the columns are:

    //     Request ID
    //       The unique ID assigned to each Request.

    //     Client Start Timestamp
    //       The timestamp when the client first sends the request.  This (like
    //       most of the time-based fields) is measure in milliseconds.

    //     Params
    //       These are the custom parameters sent with the Request.

    //     Server Finish Timestamp
    //       This is the timestamp when the server finished processing the
    //       Request.

    //     Network Delay
    //       This is the total amount of time the Request spent being sent
    //       through networks.

    //     Node
    //       This is the ID of the Node that processed the Request.

    //     Loading Balancing Delay
    //       This is the amount of time the Request spent being load-balanced.
    //       It is not used in this example.

    //     Cost
    //       This is the cost associated with the Request.

    //     Error Count
    //       This is the number of errors that occurred while processing the
    //       Request.  You'll notice that there was an error processing this
    //       Request.  That is because we did not provide any code for the
    //       server that actually handles the Requests.  We'll do that in the
    //       next example.

    //     Total Roundtrip Time
    //       This is the total time spent from when the Request was first sent
    //       to when the response was received by the client.

    //     Client ID
    //       This is the ID of the client that sent the Request.

    // That was a rather large "first example," but there's a lot of basics to
    // cover.  The future examples will add on more features and demonstrate
    // more of the functionality available in the Simizer framework.  They'll
    // also (hopefully) be a bit shorter since they don't need to provide the
    // basic details.
  }

  public static void main(String[] args) throws Exception {
    run();
  }
}
