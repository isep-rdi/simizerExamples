package org.isep.simizer.tutorial;

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
import fr.isep.simizer.storage.ResourceFactory;
import fr.isep.simizer.storage.StorageElement;

/**
 * Welcome to the third tutorial!
 *
 * In this tutorial we'll go through a more "real world" application, and we'll
 * also introduce the concept of resources, storage, and the cache.
 *
 * This builds on the information in the previous two tutorials, so you should
 * make sure that you understand them before reading this one.
 *
 * In this tutorial, we're going to build Weatherlator, a simple weather
 * interpolation application.  When we send a Request, we also include a list
 * of relevant resources that should be processed to perform the calculation.
 * The application will read the files from the disk, process them, and then
 * return the result.
 *
 * Let's get started.
 */
public class Tutorial03 {
  private static void run() throws Exception {
    // Let's create the basic elements that we've used in the other tutorials.
    Simulation simulation = new Simulation(10000);

    // This time, however, we're going to use a different constructor for the
    // server so that we can customize the hard drive in the server.  We want
    // to set up a hard drive with our Resources.  To do this, let's start by
    // creating a ResourceFactory, which is responsible for supplying Resource
    // templates to the disk.
    ResourceFactory resources = new ResourceFactory((int) StorageElement.KILOBYTE);

    // We created a resource factory with 10 resources that each have a size of
    // 1 KB.  These Resources are referenced starting from zero.

    // Next, let's create a storage element with these resources.
    StorageElement disk = new StorageElement(StorageElement.MEGABYTE, 7);
    for (int i = 0; i < 10; i++) {
      disk.write(resources.getResource(i));
    }

    // We then use this disk when creating the VM.
    VM server = new VM(null, disk, VM.DEFAULT_MEMORY_SIZE, VM.DEFAULT_COST);
    ClientNode client = new ClientNode(0, 5);
    client.setServiceAddress(server);

    Network internet = new Network(new GaussianLaw(100, 20));
    simulation.toNetworkAddNodes(internet, server, client);

    // For the Requests, let's pretend that we're creating an application that
    // performs some sort of comparison between two locations.  Since that is
    // the case, we'll want each Request to reference two of the Resources.
    // We'll do this with a double for-loop.
    RequestFactory factory = new RequestFactory();
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        String query = "start=" + i + "&end=" + j;
        factory.addTemplate(i * 10 + j, new Request(0, "calc", query, true));
      }
    }

    // The last step for this part of the configuration is to configure the
    // ClientNode instances to use all of these Requests that we just created.
    // First, we need to give the RequestFactory to the ClientNodes.
    ClientNode.configureRequestFactory(factory);

    // Next, we need to set up the Laws for the ClientNodes so that they
    // use all of these available requests.
    ClientNode.configureLaws(new UniformLaw(100), new ConstantLaw(10), null);


    // Last, but not least, we need to write the part of the simulation that
    // handles the Requests.  This will be the most interestiing part since we
    // get to discuss reading Resources.  Once again, we'll write this as an
    // inline application.  In practice, however, it's probably best to put the
    // implementation in its own file.
    server.deploy(new Application(0, StorageElement.MEGABYTE) {

      @Override
      public void init(VM.TaskScheduler scheduler) {}

      @Override
      public void handle(VM.TaskScheduler scheduler, Node origin, Request request) {
        // First, let's read the Resources from the disk.  Internally, the VM
        // makes use of a cache to simulate a real sysetm.  The first time we
        // read a file, it'll take a little while.  Subsequent reads will be
        // faster as long as the file is still in the cache.

        Integer startID = new Integer(request.getParameter("start"));
        Integer endID = new Integer(request.getParameter("end"));

        scheduler.read(startID);
        scheduler.read(endID);

        // Now that we have the resources, let's do a calculation.  To keep
        // things interesting, let's alter the complexity of the calculation
        // depending on the difference between the Resource IDs.  The formula
        // here is completely arbitrary -- any sort of behavior here would be
        // acceptable.

        // To demonstrate the ability to use various algorithms, we'll use an
        // O(n^2) algorithm here.


        int difference = endID - startID;
        scheduler.execute(1_000_000 * difference * difference, (int) StorageElement.KILOBYTE, null);

        // Finally, let's send the result back to the client.
        scheduler.sendResponse(request, origin);
      }
    });

    // Run the simulation.
    simulation.runSim();


    // Here is the sample output from running this simulation.

    // Request Client Errors      Start  Duration   N Delay  App Action;Params
    //       1      2      0         10       229       214    0 calc;start=8&end=9
    //       2      2      0        249       242       227    0 calc;start=3&end=2
    //       3      2      0        501       292       243    0 calc;start=3&end=0
    //       4      2      0        803       307       172    0 calc;start=6&end=1
    //       5      2      0       1120       202       196    0 calc;start=0&end=2
    // Finished.  58 event(s) completed in 35924ms.

    // The most interesting metric is the "duration minus the network delay."
    // I'll create that column below, as well as copying the params.  This'll
    // help to understand the output.

    // Processing Delay   Params
    //               15   start=8&end=9
    //               15   start=3&end=2
    //               49   start=3&end=0
    //              135   start=6&end=1
    //                6   start=0&end=2

    // Here, we can see that there is some variability in the amount of time it
    // takes to process the requests.  This might be expected based on the
    // O(n^2) algorithm that we are simulating.  However, there are a few
    // important things to note.

    // The last request only took 6ms of processing time. There is a difference
    // of 2 between the start and end for this request, yet the time it took to
    // process is lower than the time it took to process the requests with a
    // difference of 1.  Why is this?

    // You'll notice that Resource 0 and Resource 3 are both used at some point
    // in the first four Requests.  This means that they are added to the cache.
    // When it comes time to read them for the final Request, they can be read
    // from the cache to save time.

    // Now it's time to continue to Tutorial 4, where we finally create multiple
    // clients to send Requests.
  }

  public static void main(String[] args) throws Exception {
    run();
  }
}
