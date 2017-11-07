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

import java.time.LocalTime;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;

/**
 *
 * @author Martin Kouba
 */
@Path("/")
public class GreetingEndpoint {

    @Inject
    Client client;

    @GET
    @Path("/greeting")
    @Produces("application/json")
    public Greeting greeting() {
        NameCommand command = new NameCommand(client);
        Greeting greeting = new Greeting(String.format("Hello, %s!", command.execute()));
        CircuitBreakerWebSocketEndpoint.send("isOpen:" + command.isCircuitBreakerOpen());
        return greeting;
    }

    static class Greeting {

        private final String content;

        private final String time;

        public Greeting(String content) {
            this.content = content;
            this.time = LocalTime.now().toString();
        }

        public String getContent() {
            return content;
        }

        public String getTime() {
            return time;
        }

    }

}
