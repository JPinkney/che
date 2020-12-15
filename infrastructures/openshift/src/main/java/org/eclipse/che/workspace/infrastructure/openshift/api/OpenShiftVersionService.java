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
package org.eclipse.che.workspace.infrastructure.openshift.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.common.annotations.Beta;

import io.fabric8.openshift.client.OpenShiftClient;
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
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;

/** @author Josh Pinkney */
@Api(
    value = "openshift",
    description = "OpenShift REST API for getting versions")
@Path("/openshift")
@Beta
public class OpenShiftVersionService extends Service {

  private final OpenShiftClientFactory clientFactory;

  @Inject
  public OpenShiftVersionService(K8sVersion k8sVersion, OpenShiftClientFactory clientFactory) {
      this.clientFactory = clientFactory;
  }

  @GET
  @Path("/version")
  @Produces(APPLICATION_JSON)
  @ApiOperation(
      value = "Get openshift version",
      notes =
          "This operation can be performed only by authorized user."
              + "This is under beta and may be changed significantly",
      response = String.class)
  @ApiResponses({
    @ApiResponse(code = 200, message = "The version successfully fetched"),
    @ApiResponse(code = 500, message = "Internal server error occurred when fetching the OpenShift version")
  })
  public String getOpenShiftVersion() throws InfrastructureException {
    OpenShiftClient c = this.clientFactory.createOC();
    return c.getVersion().toString();
  }

  @GET
  @Path("/apiserver")
  @Produces(APPLICATION_JSON)
  @ApiOperation(
      value = "Get the apiserver",
      notes =
          "This operation can be performed only by authorized user."
              + "This is under beta and may be changed significantly",
      response = String.class)
  @ApiResponses({
    @ApiResponse(code = 200, message = "The apiserver was successfully fetched"),
    @ApiResponse(code = 500, message = "Internal server error occurred when fetching the apiserver version")
  })
  public String getApiServer() throws InfrastructureException {
    OpenShiftClient c = this.clientFactory.createOC();
    return c.getOpenshiftUrl().toString();
  }

  @GET
  @Path("/channel")
  @Produces(APPLICATION_JSON)
  @ApiOperation(
      value = "Get the channel",
      notes =
          "This operation can be performed only by authorized user."
              + "This is under beta and may be changed significantly",
      response = String.class)
  @ApiResponses({
    @ApiResponse(code = 200, message = "The channel was successfully fetched"),
    @ApiResponse(code = 500, message = "Internal server error occurred when fetching the channel version")
  })
  public String getChannel() throws InfrastructureException {
    OpenShiftClient c = this.clientFactory.createOC();
    return c.getOpenshiftUrl().toString();
  }

  @GET
  @Path("/clusterID")
  @Produces(APPLICATION_JSON)
  @ApiOperation(
      value = "Get the cluster id",
      notes =
          "This operation can be performed only by authorized user."
              + "This is under beta and may be changed significantly",
      response = String.class)
  @ApiResponses({
    @ApiResponse(code = 200, message = "The cluster id was successfully fetched"),
    @ApiResponse(code = 500, message = "Internal server error occurred when fetching the cluster id")
  })
  public String getClusterID() throws InfrastructureException {
    OpenShiftClient c = this.clientFactory.createOC();
    return c.getOpenshiftUrl().toString();
  }
}
