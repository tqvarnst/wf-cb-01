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

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

/**
 * Wraps the name service invocation.
 *
 * @author Martin Kouba
 */
public class NameCommand extends HystrixCommand<String> {

    static final HystrixCommandKey KEY = HystrixCommandKey.Factory.asKey(NameCommand.class.getSimpleName());

    static final HystrixCommandGroupKey GROUP_KEY = HystrixCommandGroupKey.Factory.asKey(NameCommand.class.getPackage().getName());

    /**
     * Hardcoded name service URI - in real apps this should be configurable
     */
    private static final URI NAME_SERVICE_URI = URI.create("http://wfswarm-circuit-breaker-name:8080/api/name");

    private final Client client;

    NameCommand(Client client) {
        // Set the command key explicitly so that we're able to obtain the circuit breaker status
        super(Setter.withGroupKey(GROUP_KEY).andCommandKey(KEY));
        this.client = client;
    }

    @Override
    protected String run() throws Exception {
        Response response = client.target(NAME_SERVICE_URI).request(MediaType.TEXT_PLAIN_TYPE).get();
        try {
            if (response.getStatus() != 200) {
                throw new RuntimeException("Cannot get name from " + NAME_SERVICE_URI);
            }
            String value = response.readEntity(String.class);
            return value;
        } finally {
            response.close();
        }
    }

    @Override
    protected String getFallback() {
        return "Fallback";
    }
}
