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
package org.apache.geronimo.microprofile.common.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

public class HealthCheckResponseBuilderImpl extends HealthCheckResponseBuilder {
    private HealthCheckResponseImpl response = new HealthCheckResponseImpl();

    @Override
    public HealthCheckResponseBuilder name(final String name) {
        response.setName(name);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(final String key, final String value) {
        data().put(key, value);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(final String key, final long value) {
        data().put(key, value);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(final String key, final boolean value) {
        data().put(key, value);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder up() {
        response.setState(HealthCheckResponse.State.UP);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder down() {
        response.setState(HealthCheckResponse.State.DOWN);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder state(final boolean up) {
        if (up) {
            up();
        } else {
            down();
        }
        return this;
    }

    @Override
    public HealthCheckResponse build() {
        return response;
    }

    private Map<String, Object> data() {
        if (response.getRawData() == null) {
            response.setData(new HashMap<>());
        }
        return response.getRawData();
    }
}
