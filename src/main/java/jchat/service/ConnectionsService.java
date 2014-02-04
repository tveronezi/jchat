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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.websocket.Session;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class ConnectionsService {
    private static Logger log = LoggerFactory.getLogger(ConnectionsService.class);

    private Set<Session> sessions = new HashSet<Session>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Lock(LockType.WRITE)
    public void addSession(Session session) {
        sessions.add(session);
    }

    @Lock(LockType.WRITE)
    public void removeSession(Session session) {
        sessions.remove(session);
    }

    @Lock(LockType.READ)
    public void sendToAll(String typeName, Object data) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", typeName);
        map.put("data", data);
        final String messageJson = gson.toJson(map);
        for (Session it : sessions) {
            try {
                it.getBasicRemote().sendText(messageJson);
            } catch (Exception e) {
                log.error("Session unreachable", e);
            }
        }
    }

}
