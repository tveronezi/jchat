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

package jchat.test;

import jchat.data.dto.MessageDto;
import jchat.data.jpa.BaseEntity;
import jchat.data.jpa.MessageEntity;
import jchat.jms.NewTextMessage;
import jchat.rest.ApplicationRestConfig;
import jchat.service.BaseEAO;
import jchat.sockets.ChatSocketConnection;
import org.apache.commons.io.FileUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.Query;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

@RunWith(Arquillian.class)
public class RestTest {

    @ArquillianResource
    URL deploymentURL;

    @Inject
    private BaseEAO eao;

    private static void copyFile(String name) {
        final ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
        try {
            FileUtils.copyInputStreamToFile(
                    ctxCl.getResourceAsStream("test/" + name),
                    new File(System.getProperty("arquillian.tomee.path") + "/conf/" + name)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Deployment
    public static WebArchive createDeployment() {
        copyFile("loginscript.js");
        return ShrinkWrap.create(WebArchive.class, "jchat.war")
                .addClasses(
                        BaseEntity.class,
                        MessageEntity.class,
                        NewTextMessage.class,
                        ChatSocketConnection.class,
                        MessageDto.class,
                        RestTest.class
                )
                .addPackage(ApplicationRestConfig.class.getPackage())
                .addPackage(BaseEAO.class.getPackage())
                .addAsResource(new ClassLoaderAsset("META-INF/persistence.xml"), "META-INF/persistence.xml")
                .addAsResource(new ClassLoaderAsset("META-INF/beans.xml"), "META-INF/beans.xml")
                .addAsWebResource(new File("src/main/webapp/META-INF/context.xml"), "META-INF/context.xml")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"));
    }


    private String getBody(CloseableHttpResponse response) throws Exception {
        String body = null;
        try {
            HttpEntity entity = response.getEntity();
            body = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        } finally {
            response.close();
        }
        return body;
    }

    private String post(CloseableHttpClient client, String path, NameValuePair... postParams) throws Exception {
        HttpPost post = new HttpPost(deploymentURL.toURI() + path);
        post.setEntity(new UrlEncodedFormEntity(new ArrayList<NameValuePair>(Arrays.asList(postParams)), Consts.UTF_8));
        return getBody(client.execute(post));
    }

    @Test
    public void test() throws Exception {
        eao.withQuery("DELETE FROM " + MessageEntity.class.getName(), new BaseEAO.ExecutableQuery<Integer>() {
            @Override
            public Integer execute(Query query) {
                return query.executeUpdate();
            }
        });
        CloseableHttpClient client = HttpClients.custom().build();

        // If this fails, don't forget to change you JS code
        Assert.assertEquals(
                "{\"messageDto\":[]}",
                getBody(client.execute(new HttpGet(deploymentURL.toURI() + "rest/messages")))
        );

        String text = "Hi there! [" + System.currentTimeMillis() + "]";
        try {
            // login
            post(client, "rest/auth", new BasicNameValuePair("user", "bototaxi"));
            // post message
            post(client, "rest/messages", new BasicNameValuePair("message", text));

            int tryCounter = 0;
            while (true) {
                String json = getBody(client.execute(new HttpGet(deploymentURL.toURI() + "rest/messages")));
                if (json.contains(text)) {
                    break;
                }

                if (++tryCounter > 10) {
                    Assert.fail("We tried " + tryCounter + " times without success. message: [" + text + "]; JSON: [" + json + "]");
                }

                // Not time enough to process the message. Try it again!
                Thread.sleep(500);
            }

        } finally {
            client.close();
        }
    }
}



