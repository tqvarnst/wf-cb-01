/*
 *
 *  Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.openshift.booster;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author Martin Kouba
 */
@ServerEndpoint("/cb-ws")
public class CircuitBreakerWebSocketEndpoint {

    static Queue<Session> queue = new ConcurrentLinkedQueue<>();

    static void send(String msg) {
        for (Session session : queue) {
            try {
                session.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                throw new RuntimeException("Cannot send message using " + session, e);
            }
        }
    }

    @OnOpen
    public void open(Session session, EndpointConfig conf) {
        queue.add(session);
    }

    @OnClose
    public void close(Session session, CloseReason reason) {
        queue.remove(session);
    }

}
