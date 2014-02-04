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

import jchat.data.jpa.MessageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.*;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

@Stateless
public class MessagesService {
    private static Logger log = LoggerFactory.getLogger(MessagesService.class);

    private static final int MESSAGES_LIMIT = 100;

    @Inject
    private BaseEAO eao;

    @Resource
    private ConnectionFactory factory;

    @Resource(mappedName = "NewTextMessageChannel")
    private Destination newTextMessageChannel;

    @Resource
    private SessionContext ctx;

    @RolesAllowed({"chat-user"})
    public void postMessage(String message) {
        try {
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
                notification.setStringProperty("user", ctx.getCallerPrincipal().getName());
                notification.setLongProperty("date", new Date().getTime());
                notification.setStringProperty("message", message);

                // Tell the producer to send the message
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
        } catch (JMSException e) {
            log.error("JMSException caught!", e);
        }
    }

    /**
     * No user calls this method. Only the system can execute inserts (via NewTextMessage topic).
     */
    @RolesAllowed({"chat-system"})
    public MessageEntity save(long date, String user, String message) {
        final MessageEntity bean = new MessageEntity();
        bean.setDate(new Date(date));
        bean.setUser(user);
        bean.setMessage(message);
        eao.execute(new BaseEAO.Executable<Void>() {
            @Override
            public Void execute(EntityManager em) {
                em.persist(bean);
                return null;
            }
        });
        return bean;
    }

    public List<MessageEntity> getMessages() {
        return eao.withQuery("SELECT e FROM " + MessageEntity.class.getName() + " e",
                new BaseEAO.ExecutableQuery<List<MessageEntity>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List<MessageEntity> execute(Query query) {
                        query.setMaxResults(MESSAGES_LIMIT);
                        return query.getResultList();
                    }
                });
    }

}
