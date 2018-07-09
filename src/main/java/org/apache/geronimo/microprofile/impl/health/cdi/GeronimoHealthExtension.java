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
package org.apache.geronimo.microprofile.impl.health.cdi;

import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;

public class GeronimoHealthExtension implements Extension {
    private final Collection<Bean<?>> beans = new ArrayList<>();
    private final Collection<CreationalContext<?>> contexts = new ArrayList<>();
    private List<HealthCheck> checks;

    void findChecks(@Observes final ProcessBean<?> bean) {
        if (bean.getAnnotated().isAnnotationPresent(Health.class) && bean.getBean().getTypes().contains(HealthCheck.class)) {
            beans.add(bean.getBean());
        }
    }

    void start(@Observes final AfterDeploymentValidation afterDeploymentValidation, final BeanManager beanManager) {
        checks = beans.stream()
             .map(it -> lookup(it, beanManager))
             .collect(toList());
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

    public List<HealthCheck> getChecks() {
        return checks;
    }

    private HealthCheck lookup(final Bean<?> bean, final BeanManager manager) {
        final Class<?> type = bean.getBeanClass() == null ? HealthCheck.class : bean.getBeanClass();
        final Set<Bean<?>> beans = manager.getBeans(type, bean.getQualifiers().toArray(new Annotation[bean.getQualifiers().size()]));
        final Bean<?> resolvedBean = manager.resolve(beans);
        final CreationalContext<Object> creationalContext = manager.createCreationalContext(null);
        if (!manager.isNormalScope(resolvedBean.getScope())) {
            contexts.add(creationalContext);
        }
        return HealthCheck.class.cast(manager.getReference(resolvedBean, HealthCheck.class, creationalContext));
    }
}
