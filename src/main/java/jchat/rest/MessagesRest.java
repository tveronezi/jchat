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

package jchat.rest;

import jchat.data.dto.MessageDto;
import jchat.data.jpa.MessageEntity;
import jchat.service.MessagesService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/messages")
public class MessagesRest {

    @Inject
    private MessagesService messages;

    @POST
    public Response postMessage(@FormParam("message") String message) {
        messages.postMessage(message);
        return Response.ok().build();
    }

    private MessageDto buildMessageDtoFromEntity(MessageEntity it) {
        MessageDto dto = new MessageDto();
        dto.setId(it.getUid());
        dto.setContent(it.getMessage());
        dto.setFrom(it.getUser());
        dto.setTimestamp(it.getDate().getTime());
        return dto;
    }

    @GET
    @Produces("application/json")
    public List<MessageDto> getMessages() {
        List<MessageDto> result = new ArrayList<MessageDto>();
        for (MessageEntity it : messages.getMessages()) {
            result.add(buildMessageDtoFromEntity(it));
        }
        return result;
    }

}
