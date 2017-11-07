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

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.json.Json;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Route;
import io.openshift.booster.test.OpenShiftTestAssistant;

/**
 * @author Martin Kouba
 */
public class OpenshiftIT {

    private static final String NAME_SERVICE_APP = "wfswarm-circuit-breaker-name";
    private static final String GREETING_SERVICE_APP = "wfswarm-circuit-breaker-greeting";

    private static final String OK = "ok";
    private static final String FAIL = "fail";
    private static final String CLOSED = "closed";
    private static final String OPEN = "open";
    private static final String HELLO_OK = "Hello, World!";
    private static final String HELLO_FALLBACK = "Hello, Fallback!";

    // See also circuitBreaker.sleepWindowInMilliseconds
    private static final long SLEEP_WINDOW = 5000l;
    // See also circuitBreaker.requestVolumeThreshold
    private static final long REQUEST_THRESHOLD = 3;

    private static final OpenShiftTestAssistant OPENSHIFT = new OpenShiftTestAssistant();

    private static String nameBaseUri;
    private static String greetingBaseUri;

    @BeforeClass
    public static void setup() throws Exception {

        nameBaseUri = deployApp(NAME_SERVICE_APP, System.getProperty("nameServiceTemplate"));
        greetingBaseUri = deployApp(GREETING_SERVICE_APP, System.getProperty("greetingServiceTemplate"));

        await().atMost(5, TimeUnit.MINUTES).until(() -> {
            List<Pod> list = OPENSHIFT.client().pods().inNamespace(OPENSHIFT.project()).list().getItems();
            return list.stream()
                    .filter(pod -> pod.getMetadata().getName().startsWith(NAME_SERVICE_APP) || pod.getMetadata().getName().startsWith(GREETING_SERVICE_APP))
                    .filter(pod -> "running".equalsIgnoreCase(pod.getStatus().getPhase())).collect(Collectors.toList()).size() >= 2;
        });

        System.out.println("Pods running, waiting for probes...");
        String greetingProbeUri = greetingBaseUri;
        String nameProbeUri = nameBaseUri + "/api/info";

        await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.MINUTES).until(() -> {
            try {
                Response response = get(greetingProbeUri);
                if (response.getStatusCode() == 200) {
                    response = get(nameProbeUri);
                    if (response.getStatusCode() == 200) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
            return false;
        });

    }

    @AfterClass
    public static void teardown() throws Exception {
        OPENSHIFT.cleanup();
    }

    @Test
    public void testCircuitBreaker() throws InterruptedException {
        assertCircuitBreaker(CLOSED);
        assertGreeting(HELLO_OK);
        changeNameServiceState(FAIL);
        for (int i = 0; i < REQUEST_THRESHOLD; i++) {
            assertGreeting(HELLO_FALLBACK);
        }
        // Circuit breaker should be open now
        // Wait a little to get the current health counts - see also metrics.healthSnapshot.intervalInMilliseconds
        await().atMost(5, TimeUnit.SECONDS).until(() -> testCircuitBreakerState(OPEN));
        changeNameServiceState(OK);
        // See also circuitBreaker.sleepWindowInMilliseconds
        await().atMost(7, TimeUnit.SECONDS).pollDelay(SLEEP_WINDOW, TimeUnit.MILLISECONDS).until(() -> testGreeting(HELLO_OK));
        // The health counts should be reset
        assertCircuitBreaker(CLOSED);
    }

    private Response greetingResponse() {
        return RestAssured.when().get(greetingBaseUri + "/api/greeting");
    }

    private void assertGreeting(String expected) {
        Response response = greetingResponse();
        response.then().statusCode(200).body(containsString(expected));
    }

    private boolean testGreeting(String expected) {
        Response response = greetingResponse();
        response.then().statusCode(200);
        return response.getBody().asString().contains(expected);
    }

    private Response circuitBreakerResponse() {
        return RestAssured.when().get(greetingBaseUri + "/api/cb-state");
    }

    private void assertCircuitBreaker(String expectedState) {
        Response response = circuitBreakerResponse();
        response.then().statusCode(200).body("state", equalTo(expectedState));
    }

    private boolean testCircuitBreakerState(String expectedState) {
        Response response = circuitBreakerResponse();
        response.then().statusCode(200);
        return response.getBody().asString().contains(expectedState);
    }

    private void changeNameServiceState(String state) {
        Response response = RestAssured.given().header("Content-type", "application/json")
                .body(Json.createObjectBuilder().add("state", state).build().toString()).put(nameBaseUri + "/api/state");
        response.then().assertThat().statusCode(200).body("state", equalTo(state));
    }

    /**
     *
     * @param name
     * @param templatePath
     * @return the app route
     * @throws IOException
     */
    private static String deployApp(String name, String templatePath) throws IOException {
        String appName = "";
        List<? extends HasMetadata> entities = OPENSHIFT.deploy(name, new File(templatePath));

        Optional<String> first = entities.stream().filter(hm -> hm instanceof DeploymentConfig).map(hm -> (DeploymentConfig) hm)
                .map(dc -> dc.getMetadata().getName()).findFirst();
        if (first.isPresent()) {
            appName = first.get();
        } else {
            throw new IllegalStateException("Application deployment config not found");
        }
        Route route = OPENSHIFT.client().routes().inNamespace(OPENSHIFT.project()).withName(appName).get();
        assertThat(route).isNotNull();
        return "http://" + route.getSpec().getHost();
    }

}
