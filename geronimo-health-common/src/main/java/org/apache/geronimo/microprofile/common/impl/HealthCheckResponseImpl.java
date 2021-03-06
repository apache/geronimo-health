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

import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import java.util.Map;
import java.util.Optional;

public class HealthCheckResponseImpl extends HealthCheckResponse {
    private String name;
    private Status state;
    private Map<String, Object> data;

    @Override
    public String getName() {
        return name;
    }

    public Status getStatus() {
        return state;
    }

    @JsonbProperty("data")
    public Map<String, Object> getRawData() {
        return data;
    }

    @Override
    @JsonbTransient
    public Optional<Map<String, Object>> getData() {
        return Optional.ofNullable(data);
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setState(final Status state) {
        this.state = state;
    }

    public void setData(final Map<String, Object> data) {
        this.data = data;
    }
}
