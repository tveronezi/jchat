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

import jchat.UnmanagedException;
import jchat.data.dto.MessageDto;
import jchat.data.jpa.BaseEntity;
import jchat.data.jpa.MessageEntity;
import jchat.jms.NewTextMessage;
import jchat.service.BaseEAO;
import jchat.service.MessagesService;
import jchat.sockets.ChatSocketConnection;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.*;
import javax.persistence.Query;
import java.util.Date;

@RunWith(Arquillian.class)
public class TopicTest {

    @Resource
    private ConnectionFactory factory;

    @Resource(mappedName = "NewTextMessageChannel")
    private Topic newTextMessageChannel;

    @Inject
    private BaseEAO eao;

    @Inject
    private MessagesService messagesService;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(
                        NewTextMessage.class,
                        ChatSocketConnection.class,
                        MessageDto.class,
                        TopicTest.class,
                        UnmanagedException.class
                )
                .addPackage(BaseEAO.class.getPackage())
                .addPackage(BaseEntity.class.getPackage())
                .addAsResource(new ClassLoaderAsset("META-INF/persistence.xml"), "META-INF/persistence.xml")
                .addAsResource(new ClassLoaderAsset("META-INF/beans.xml"), "META-INF/beans.xml");
    }

    @Test
    public void manualJms() throws Exception {
        final Date now = new Date();
        final String user = "boto";
        final String text = "Hi there! " + System.currentTimeMillis();

        Connection connection = null;
        Session session = null;
        try {
            connection = factory.createConnection();
            connection.start();

            // Create a Session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create a MessageProducer from the Session to the Topic or Queue
            MessageProducer producer = session.createProducer(newTextMessageChannel);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            Message notification = session.createMessage();
            notification.setStringProperty("message", text);
            notification.setStringProperty("user", user);
            notification.setLongProperty("date", now.getTime());
            producer.send(notification);

        } finally {
            // Clean up
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

        int tryCounter = 0;
        MessageEntity entity = null;
        while (++tryCounter < 10) {
            entity = eao.withQuery("SELECT e FROM " + MessageEntity.class.getName()
                    + " e WHERE e.user = :pUser", new BaseEAO.ExecutableQuery<MessageEntity>() {
                @Override
                public MessageEntity execute(Query query) {
                    query.setParameter("pUser", user);
                    return (MessageEntity) query.getSingleResult();
                }
            });
            if (entity == null) {
                // No time enough to the message to be processed. Try it again!
                Thread.sleep(500);
            } else {
                break;
            }
        }
        Assert.assertNotNull("We tried " + tryCounter + " times without success.", entity);
        Assert.assertEquals(text, entity.getMessage());
        Assert.assertEquals(now, entity.getDate());
    }

}



