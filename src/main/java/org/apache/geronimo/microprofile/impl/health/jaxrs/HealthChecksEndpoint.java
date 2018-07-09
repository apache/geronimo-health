/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.microprofile.impl.health.jaxrs;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.geronimo.microprofile.impl.health.cdi.GeronimoHealthExtension;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@Path("health")
@ApplicationScoped
public class HealthChecksEndpoint {
    @Inject
    private GeronimoHealthExtension extension;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChecks() {
        final List<HealthCheckResponse> checks = extension.getChecks()
                                                           .stream()
                                                           .map(HealthCheck::call)
                                                           .collect(toList());
        final HealthCheckResponse.State globalState = checks.stream()
                    .reduce(HealthCheckResponse.State.UP, (a, b) -> combine(a, b.getState()), this::combine);
        return Response.status(globalState == HealthCheckResponse.State.DOWN ? Response.Status.SERVICE_UNAVAILABLE : Response.Status.OK).entity(new AggregatedResponse(globalState, checks)).build();
    }

    private HealthCheckResponse.State combine(final HealthCheckResponse.State a, final HealthCheckResponse.State b) {
        return a == HealthCheckResponse.State.DOWN || b == HealthCheckResponse.State.DOWN ? HealthCheckResponse.State.DOWN : a;
    }

    public static class AggregatedResponse {
        private HealthCheckResponse.State status;
        private Collection<HealthCheckResponse> checks;

        private AggregatedResponse(final HealthCheckResponse.State state,
                                  final Collection<HealthCheckResponse> checks) {
            this.status = state;
            this.checks = checks;
        }

        public HealthCheckResponse.State getStatus() {
            return status;
        }

        @Deprecated
        public HealthCheckResponse.State getOutcome() {
            return status;
        }

        public Collection<HealthCheckResponse> getChecks() {
            return checks;
        }
    }
}
