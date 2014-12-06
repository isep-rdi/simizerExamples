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

  private Integer loadBalancerApplicationId;
  private Node loadBalancer;

  public LoadBalancedApplication(Integer applicationId, long memorySize,
          Node loadBalancer, Integer loadBalancerApplicationId) {
    super(applicationId, memorySize);

    this.loadBalancer = loadBalancer;
    this.loadBalancerApplicationId = loadBalancerApplicationId;
  }

  @Override
  public void init(TaskScheduler scheduler) {
    Request register = new Request(loadBalancerApplicationId, "register", "");

    scheduler.sendOneWay(loadBalancer, register);

    loadBalancerApplicationId = null;
    loadBalancer = null;
  }

}
