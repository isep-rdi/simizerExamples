package org.isep.simizer.tutorial;

import fr.isep.simizer.Simulation;
import fr.isep.simizer.laws.GaussianLaw;
import fr.isep.simizer.laws.ConstantLaw;
import fr.isep.simizer.network.Network;
import fr.isep.simizer.nodes.ClientNode;
import fr.isep.simizer.nodes.VM;
import fr.isep.simizer.requests.Request;
import fr.isep.simizer.requests.RequestFactory;

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

  private static void run() {
    // Start by creating an instance of the Simulation class.  Throughout the
    // example, we will add other components to the Simulation, and then we will
    // run the Simulation at the end (by calling runSim()).  (If you don't
    // believe me, jump to line 124 now.)
    Simulation simulation = new Simulation(10000);

    // Next let's create our client machine.  We'll use a very simple client
    // that only sends a single request for this first example.

    // When creating a ClientNode, we have to specify two parameters. The first
    // is the timestamp in the Simulation when the client should start sending
    // its requests.  Since we want it to start right away, we'll pass zero for
    // this value.  The next parameter is more important.  It's the number of
    // requests that the client should send before finishing.  In this example,
    // we have it set to 1.  Feel free to adjust the value and re-run the
    // Simulation to see the results.
    ClientNode client = new ClientNode(0, 1);

    // We now need to define the Request that our client will send to the
    // server.  The ClientNode retrieve Requests from templates that are stored
    // in a RequestFactory instance, so let's create one now.
    RequestFactory factory = new RequestFactory();
    factory.addTemplate(1, new Request(1, "read", "resources=1_2", true));
    ClientNode.configureRequestFactory(factory);

    // This creates a Request template and stores it in the factory with an ID
    // of 1.  We'll use that ID to retrieve it in the next step.  We also tell
    // the clients where they should look for Request templates.

    // Next, we need to define the behavior of the ClientNode.  All of the
    // clients are controlled by three Laws.  A Law introduces randomness into
    // the simulation according to a probability distribution.

    ClientNode.configureLaws(
            // The first Law determines which Request templates clients will
            // use.  Since we only put one Request template into our
            // RequestFactory, we want to configure the client to use that
            // Request.  To do that, we can use a ConstantLaw that always
            // returns the same value, in this case 1.
            new ConstantLaw(1),

            // The next Law determines how much time clients will spend thinking
            // between each request.  This can be thought of as the amount of
            // time a view spends looking at the result that was returned.
            // Since we are only sending a single request, we can just set this
            // to a constant value of zero.
            new ConstantLaw(0),

            // Finally, this law defines the lifetime of clients.  Since we
            // created our client to send a specific number of requests, this
            // value is ignored in our Simulation.  We can therefore set it to
            // null.
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
    // Gaussian (normal) distribution with a mean of 60 ms and a standard
    // deviation of 10 ms.  You will find that Law instances are used throughout
    // the simulation to add customizable randomness.
    Network internet = new Network(new GaussianLaw(60, 10));

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

    // There should two lines of output.  Yours will vary depending on the
    // randomization, but it should look something like this:

    // Request Client Errors      Start  Duration   N Delay  App Action;Params
    //       1      1      1          0        80        80    1 read;file=1

    // The Simulation will print each response that a client receives.  The
    // meaning of each column is as follows:

    //     Request
    //       The unique ID assigned to each Request.  In this example the
    //       Request got assigned an ID of 1.

    //     Client
    //       This is the ID of the client that sent the Request.  In this
    //       example, it was sent by the client with an ID of 1.

    //     Errors
    //       This is the number of errors that occurred while processing the
    //       Request.  You'll notice that there was an error processing this
    //       Request.  That is because we did not provide any code for the
    //       server to actually handle Requests.  We'll address that issue in
    //       the next example.

    //     Start
    //       The timestamp when the client first sends the request.  This (like
    //       most of the time-based fields) is measured in milliseconds.  In
    //       this example, that value is 0 since we sent the Request right at
    //       the beginning of the simulation.

    //     Duration
    //       This is the total round-trip duration of the Request.  It is the
    //       amount of time from when the client sent the Request to when it
    //       receives its Response.  Here, we can see that this is 80 ms.

    //     N Delay
    //       This the network delay.  It is a measure of the total amount of
    //       time the Request spent traveling through networks.  Here, we can
    //       see that this is 80 ms.  (It is the same as the duration because
    //       the server spent no time processing the Request.  That's what
    //       happens when we don't give the server an application to run!)

    //     App
    //       This is the ID where the Request was sent.  In terms of a URL, this
    //       could be thought of as the domain name.  For example, we used a
    //       value of 0.

    //     Action;Params
    //       This is the action that should be performed, as well as the
    //       parameters to provide to that action.  In the URL analogy, these
    //       would be the path and the query parameters (after the ?).  Here, we
    //       can see that this value is "read;file=1".

    // That was a rather large "first example," but there's a lot of basics to
    // cover.  The future examples will add on more features and demonstrate
    // more of the functionality available in the Simizer framework.  They'll
    // also (hopefully) be a bit shorter since they don't need to provide all of
    // this introductory information.
  }

  public static void main(String[] args) {
    run();
  }
}
