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
package org.apache.geronimo.microprofile.common.jaxrs;

import org.apache.geronimo.microprofile.common.registry.HealthChecksRegistry;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@Path("health")
// @ApplicationScoped
public class HealthChecksEndpoint {
    private volatile HealthChecksRegistry registry;

    public void setRegistry(final HealthChecksRegistry registry) {
        this.registry = registry;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChecks() {
        return toResponse(HealthChecksRegistry::getChecks, r -> {});
    }

    @GET
    @Path("live")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLiveChecks() {
        return toResponse(HealthChecksRegistry::getLiveness, r -> {});
    }

    @GET
    @Path("ready")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadyChecks() {
        return toResponse(HealthChecksRegistry::getReadiness, r -> {});
    }

    private Response toResponse(final Function<HealthChecksRegistry, Collection<HealthCheck>> extractor,
                                final Consumer<HealthChecksRegistry> validate) {
        if (registry == null) {
            synchronized (this) {
                if (registry == null) {
                    registry = HealthChecksRegistry.load();
                }
            }
        }
        validate.accept(registry);

        final List<HealthCheckResponse> checks = extractor.apply(registry)
                .stream()
                .map(HealthCheck::call)
                .collect(toList());
        final HealthCheckResponse.Status globalState = checks.stream()
                .reduce(HealthCheckResponse.Status.UP, (a, b) -> combine(a, b.getStatus()), this::combine);
        return Response.status(globalState == HealthCheckResponse.Status.DOWN ? Response.Status.SERVICE_UNAVAILABLE : Response.Status.OK).entity(new AggregatedResponse(globalState, checks)).build();
    }

    private HealthCheckResponse.Status combine(final HealthCheckResponse.Status a, final HealthCheckResponse.Status b) {
        return a == HealthCheckResponse.Status.DOWN || b == HealthCheckResponse.Status.DOWN ? HealthCheckResponse.Status.DOWN : a;
    }

    public static class AggregatedResponse {
        private HealthCheckResponse.Status status;
        private Collection<HealthCheckResponse> checks;

        private AggregatedResponse(final HealthCheckResponse.Status state,
                                  final Collection<HealthCheckResponse> checks) {
            this.status = state;
            this.checks = checks;
        }

        public HealthCheckResponse.Status getStatus() {
            return status;
        }

        public Collection<HealthCheckResponse> getChecks() {
            return checks;
        }
    }
}
