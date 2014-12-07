package fr.isep.simizer.example.applications;

import fr.isep.simizer.app.Application;
import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.nodes.VM.TaskScheduler;
import fr.isep.simizer.requests.Request;

/**
 * An {@code Application} that receives requests from a load balancer.
 * 
 * @author Max Radermacher
 */
public abstract class LoadBalancedApplication extends Application {

  private Node loadBalancer;

  public LoadBalancedApplication(Integer applicationId, long memorySize,
          Node loadBalancer) {
    super(applicationId, memorySize);

    this.loadBalancer = loadBalancer;
  }

  @Override
  public void init(TaskScheduler scheduler) {
    // Since the "load balancer" and the "load balanced" applications need to
    // use the same ID, we can use the ID of this application when registering.
    Request register = new Request(getId(), "register", "");

    scheduler.sendOneWay(loadBalancer, register);

    loadBalancer = null;
  }

}
