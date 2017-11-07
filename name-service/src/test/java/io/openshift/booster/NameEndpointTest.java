/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
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
 */
package io.openshift.booster;

import static org.hamcrest.core.IsEqual.equalTo;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import com.jayway.restassured.RestAssured;

/**
 *
 * @author Martin Kouba
 */
@RunWith(Arquillian.class)
@DefaultDeployment
public class NameEndpointTest {

    private static final String OK = "{\"state\":\"ok\"}";

    private static final String FAIL = "{\"state\":\"fail\"}";

    private static final String BASE_URI = "http://localhost:8080/api";

    @Test
    @RunAsClient
    public void testGetName() {
        RestAssured.when().get(BASE_URI + "/name").then().assertThat().statusCode(200).body(equalTo("World"));
    }

    @Test
    @RunAsClient
    public void testGetInfo() {
        RestAssured.when().get(BASE_URI + "/info").then().assertThat().statusCode(200).body(equalTo(OK));
    }

    @Test
    @RunAsClient
    public void testToggle() {
        RestAssured.given().header("Content-type", MediaType.APPLICATION_JSON).body(FAIL).put(BASE_URI + "/state").then().assertThat().statusCode(200)
                .body(equalTo(FAIL));
        RestAssured.when().get(BASE_URI + "/info").then().assertThat().statusCode(200).body(equalTo(FAIL));
        RestAssured.given().header("Content-type", MediaType.APPLICATION_JSON).body(OK).put(BASE_URI + "/state").then().assertThat().statusCode(200)
                .body(equalTo(OK));
        RestAssured.when().get(BASE_URI + "/info").then().assertThat().statusCode(200).body(equalTo(OK));
    }

}
