/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.api.server;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.common.annotations.Beta;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.K8sVersion;

/** @author Josh Pinkney */
@Api(
    value = "kubernetes",
    description = "Kubernetes REST API for working with Namespaces")
@Path("/kubernetes")
@Beta
public class KubernetesVersionService extends Service {

  private final K8sVersion k8sVersion;

  @Inject
  public KubernetesVersionService(K8sVersion k8sVersion) {
      this.k8sVersion = k8sVersion;
  }

  @GET
  @Path("/version")
  @Produces(APPLICATION_JSON)
  @ApiOperation(
      value = "Get k8s version",
      notes =
          "This operation can be performed only by authorized user."
              + "This is under beta and may be changed significantly",
      response = String.class)
  @ApiResponses({
    @ApiResponse(code = 200, message = "The version successfully fetched"),
    @ApiResponse(code = 500, message = "Internal server error occurred during fetching the kubernetes version")
  })
  public String getVersion() throws InfrastructureException {
    return this.k8sVersion.version().toString();
  }
}
