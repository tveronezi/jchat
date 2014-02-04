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

package jchat.test;

import jchat.data.jpa.BaseEntity;
import jchat.data.jpa.MessageEntity;
import jchat.service.BaseEAO;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

@RunWith(Arquillian.class)
public class BaseEaoTest {

    @EJB
    private BaseEAO eao;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(BaseEAO.class, BaseEntity.class, MessageEntity.class, BaseEaoTest.class)
                .addAsResource(new ClassLoaderAsset("META-INF/persistence.xml"), "META-INF/persistence.xml");
    }

    @Test
    public void testPersist() {
        eao.withQuery("DELETE FROM " + MessageEntity.class.getName(), new BaseEAO.ExecutableQuery<Integer>() {
            @Override
            public Integer execute(Query query) {
                return query.executeUpdate();
            }
        });
        Assert.assertTrue(eao.findAll(MessageEntity.class).isEmpty());
        eao.execute(new BaseEAO.Executable<Object>() {
            @Override
            public Object execute(EntityManager em) {
                MessageEntity bean = new MessageEntity();
                bean.setDate(new Date());
                bean.setMessage("Hi there!");
                bean.setUser("geek");
                em.persist(bean);
                return null;
            }
        });
        List<MessageEntity> allMessages = eao.findAll(MessageEntity.class);
        Assert.assertEquals(1, allMessages.size());
        Assert.assertEquals("Hi there!", allMessages.get(0).getMessage());
        MessageEntity entity = eao.withQuery("SELECT e FROM " + MessageEntity.class.getName()
                + " e WHERE e.user = :pUser", new BaseEAO.ExecutableQuery<MessageEntity>() {
            @Override
            public MessageEntity execute(Query query) {
                query.setParameter("pUser", "geek");
                return (MessageEntity) query.getSingleResult();
            }
        });
        Assert.assertEquals("Hi there!", entity.getMessage());
    }

    @Test
    public void testNoResultException() {
        Assert.assertNull(eao.withQuery("SELECT e FROM " + MessageEntity.class.getName()
                + " e WHERE e.user = :pUser", new BaseEAO.ExecutableQuery<Object>() {
            @Override
            public Object execute(Query query) {
                query.setParameter("pUser", "unknownUser");
                return query.getSingleResult();
            }
        }));
    }

}

