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

package jchat.jms;

import jchat.UnmanagedException;
import jchat.data.dto.MessageDto;
import jchat.data.jpa.MessageEntity;
import jchat.service.ConnectionsService;
import jchat.service.MessagesService;

import javax.annotation.security.RunAs;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

@MessageDriven(messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/Messages"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "jchat")
})
@RunAs("chat-system")
public class NewTextMessage implements MessageListener {

    @Inject
    private ConnectionsService connections;

    @Inject
    private MessagesService messagesService;

    @Override
    public void onMessage(Message jms) {
        MessageDto dto = new MessageDto();
        try {
            dto.setContent(jms.getStringProperty("message"));
            dto.setFrom(jms.getStringProperty("user"));
            dto.setTimestamp(jms.getLongProperty("date"));
        } catch (JMSException e) {
            throw new UnmanagedException(e);
        }
        MessageEntity bean = messagesService.save(dto.getTimestamp(), dto.getFrom(), dto.getContent());
        dto.setId(bean.getUid());
        connections.sendToAll("text", dto);
    }

}
