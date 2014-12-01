package org.isep.simizer.example.consistency;

import simizer.nodes.Node;
import simizer.requests.Request;
import simizer.storage.Resource;

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
    super(applicationId, "replicate", Integer.toString(resource.getVersion()));
    
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
