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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Martin Kouba
 */
@Path("/")
public class NameEndpoint {

    private AtomicBoolean isOn = new AtomicBoolean(true);

    @GET
    @Path("/name")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getName() {
        NameWebSocketEndpoint.send(LocalTime.now().toString() + (isOn.get() ? " OK" : " FAIL"));
        return isOn.get() ? Response.ok("World").build() : Response.serverError().entity("Name service down").build();
    }

    @PUT
    @Path("/state")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ServiceInfo toggle(ServiceInfo info) {
        isOn.set(info.isOn());
        NameWebSocketEndpoint.send("state:" + isOn.get());
        return getInfo();
    }

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public ServiceInfo getInfo() {
        return isOn.get() ? ServiceInfo.OK : ServiceInfo.FAIL;
    }

    static class ServiceInfo {

        static final ServiceInfo OK = new ServiceInfo("ok");
        static final ServiceInfo FAIL = new ServiceInfo("fail");

        private final String state;

        public ServiceInfo() {
            this.state = null;
        }

        public ServiceInfo(String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }

        boolean isOn() {
            return "ok".equals(state);
        }

    }

}
