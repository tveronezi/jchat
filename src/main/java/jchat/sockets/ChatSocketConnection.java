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

package jchat.sockets;

import jchat.service.ConnectionsService;

import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/ws/connection")
public class ChatSocketConnection {

    @Inject
    private ConnectionsService service;

    @OnOpen
    public void open(Session session, EndpointConfig conf) {
        service.addSession(session);
    }

    @OnClose
    public void close(Session session) {
        service.removeSession(session);
    }

    @OnError
    public void error(Session session, Throwable cause) {
        service.removeSession(session);
    }

}
