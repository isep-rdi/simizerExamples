package fr.isep.simizer.example.applications;

import fr.isep.simizer.app.Application;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM.TaskScheduler;
import fr.isep.simizer.requests.Request;
import fr.isep.simizer.storage.Resource;
import fr.isep.simizer.storage.StorageElement;
import java.util.ArrayList;
import java.util.List;

public class InterpolationApp extends Application {

  public InterpolationApp() {
    this(0);
  }

  public InterpolationApp(int port) {
    super(port, 128 * StorageElement.MEGABYTE);
  }

  @Override
  public void init(TaskScheduler scheduler) {
    Request registerRequest = new Request(0, "register", "");

    Node destination = vm.getNetwork().getNode(
            Integer.parseInt(config.getProperty("frontend")));
    scheduler.sendRequest(destination, registerRequest);
  }

  /**
   * Adds the frontend id to application configuration
   *
   * @param ftdId
   */
  public void setFrontend(int ftdId) {
    this.config.setProperty("frontend", Integer.toString(ftdId));
  }

  @Override
  public void handle(TaskScheduler scheduler, Node orig, Request req) {

    List<Resource> rList = new ArrayList<>();

    for (Integer rId : req.getResources()) {
      Resource r = scheduler.read(rId, (15 * StorageElement.KILOBYTE));
      if (r != null) {
        rList.add(r);
      }
    }

    // If some files don't exist we send an error back to the client.
    if (rList.size() == req.getResources().size()) {
      long nbInst = 24 * 15 * 1500 * rList.size();
      long memSize = 15 * StorageElement.KILOBYTE * rList.size();
      scheduler.execute(nbInst, memSize, rList);
    } else {
      req.reportErrors(req.getResources().size() - rList.size());
    }

    scheduler.sendResponse(req, orig);
  }

}
