package fr.isep.simizer.example.consistency;

import fr.isep.simizer.nodes.Node;
import fr.isep.simizer.requests.Request;
import fr.isep.simizer.storage.Resource;

/**
 *
 * @author Sylvain Lefebvre
 */
public class ReplicationRequest extends Request {

  private final Resource data;
  private final Node origin;

  ReplicationRequest(Integer applicationId, Resource resource) {
    this(applicationId, resource, null);
  }

  ReplicationRequest(Integer applicationId, Resource resource, Node origin) {
    super(applicationId, "replicate", "");
    
    this.data = resource;
    this.origin = origin;
  }

  public Resource getResource() {
    return this.data;
  }

  public Node getOrigin() {
    return this.origin;
  }
}
