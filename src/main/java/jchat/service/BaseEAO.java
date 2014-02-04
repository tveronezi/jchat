/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jchat.service;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Stateless
public class BaseEAO {

    @PersistenceContext(unitName = "chatPU")
    private EntityManager em;

    public <T> T execute(Executable<T> closure) {
        return closure.execute(em);
    }

    public <T> T withQuery(String queryString, ExecutableQuery<T> callback) {
        Query query = em.createQuery(queryString);
        try {
            return callback.execute(query);
        } catch (NoResultException e) {
            return null;
        }
    }

    public <T> List<T> findAll(Class<T> beanCls) {
        return withQuery("SELECT e FROM " + beanCls.getName() + " e", new ExecutableQuery<List<T>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<T> execute(Query query) {
                return (List<T>) query.getResultList();
            }
        });
    }

    public interface Executable<T> {
        T execute(EntityManager em);
    }

    public interface ExecutableQuery<T> {
        T execute(Query query);
    }

}
