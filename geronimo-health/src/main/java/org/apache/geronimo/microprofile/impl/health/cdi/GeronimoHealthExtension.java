/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.microprofile.impl.health.cdi;

import org.apache.geronimo.microprofile.common.registry.HealthChecksRegistry;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class GeronimoHealthExtension implements Extension, HealthChecksRegistry {
    private final Collection<Bean<?>> beans = new ArrayList<>();
    private final Collection<Bean<?>> livenessBeans = new ArrayList<>();
    private final Collection<Bean<?>> readinessBeans = new ArrayList<>();
    private final Collection<CreationalContext<?>> contexts = new ArrayList<>();
    private List<HealthCheck> checks;
    private List<HealthCheck> liveness;
    private List<HealthCheck> readiness;
    private boolean started = false;

    private static class LivenessLiteral extends AnnotationLiteral<Liveness> {
        private static final Annotation INSTANCE = new LivenessLiteral();
    }

    private static class ReadinessLiteral extends AnnotationLiteral<Readiness> {
        private static final Annotation INSTANCE = new ReadinessLiteral();
    }

    void findChecks(@Observes final ProcessBean<?> bean) {
        if (!bean.getBean().getTypes().contains(HealthCheck.class)) {
            return;
        }
        if (bean.getBean().getQualifiers().contains(LivenessLiteral.INSTANCE)) {
            livenessBeans.add(bean.getBean());
        }
        if (bean.getBean().getQualifiers().contains(ReadinessLiteral.INSTANCE)) {
            readinessBeans.add(bean.getBean());
        }
    }

    void start(@Observes final AfterDeploymentValidation afterDeploymentValidation, final BeanManager beanManager) {
        liveness = livenessBeans.stream()
                .map(it -> lookup(it, beanManager))
                .collect(toList());

        readiness = readinessBeans.stream()
                .map(it -> lookup(it, beanManager))
                .collect(toList());

        // per spec, checks has everything including liveness and readiness
        checks = Stream.concat(Stream.concat(
                beans.stream().map(it -> lookup(it, beanManager)),
                liveness.stream()),
                readiness.stream())
                .collect(toList());

        started = true;
    }

    void stop(@Observes final BeforeShutdown beforeShutdown) {
        final IllegalStateException ise = new IllegalStateException("Something went wrong releasing health checks");
        contexts.forEach(c -> {
            try {
                c.release();
            } catch (final RuntimeException re) {
                ise.addSuppressed(re);
            }
        });
        final Throwable[] suppressed = ise.getSuppressed();
        if (suppressed.length == 1) {
            throw RuntimeException.class.cast(suppressed[0]);
        } else if (suppressed.length > 1) {
            throw ise;
        }
    }

    @Override
    public List<HealthCheck> getChecks() {
        return checks;
    }

    @Override
    public Collection<HealthCheck> getReadiness() {
        return readiness;
    }

    @Override
    public Collection<HealthCheck> getLiveness() {
        return liveness;
    }

    @Override
    public boolean isReady() {
        return started;
    }

    private HealthCheck lookup(final Bean<?> bean, final BeanManager manager) {
        // if this is not an instance of HealthCheck, then it's a producer (not sure it's enough)
        final Class<?> type = bean.getBeanClass() == null ?
                HealthCheck.class :
                (bean.getTypes().contains(bean.getBeanClass()) ? bean.getBeanClass() : HealthCheck.class);
        final Bean<?> resolvedBean;
        if (type != HealthCheck.class) {
            final Set<Bean<?>> beans = manager.getBeans(type, bean.getQualifiers().toArray(new Annotation[0]));
            resolvedBean = manager.resolve(beans);
        } else {
            resolvedBean = bean;
        }
        final CreationalContext<Object> creationalContext = manager.createCreationalContext(null);
        if (!manager.isNormalScope(resolvedBean.getScope())) {
            contexts.add(creationalContext);
        }
        return HealthCheck.class.cast(manager.getReference(resolvedBean, HealthCheck.class, creationalContext));
    }
}
