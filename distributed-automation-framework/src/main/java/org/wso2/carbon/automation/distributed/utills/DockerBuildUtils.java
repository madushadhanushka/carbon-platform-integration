/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.automation.distributed.utills;

import io.fabric8.docker.client.Config;
import io.fabric8.docker.client.ConfigBuilder;
import io.fabric8.docker.client.DefaultDockerClient;
import io.fabric8.docker.client.DockerClient;
import io.fabric8.docker.dsl.EventListener;
import io.fabric8.docker.dsl.OutputHandle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * DockerBuildUtils utilities for building docker.
 */
public class DockerBuildUtils {
    private static final Log log = LogFactory.getLog(DockerBuildUtils.class);

    public static boolean buildDockerImage(String dockerUrl, String image, String imageFolder)
            throws InterruptedException, IOException {

        Config config = new ConfigBuilder()
                .withDockerUrl(dockerUrl)
                .build();

        DockerClient client = new DefaultDockerClient(config);
        final CountDownLatch buildDone = new CountDownLatch(1);


        OutputHandle handle = client.image().build()
                .withRepositoryName(image)
                .usingListener(new EventListener() {
                    @Override
                    public void onSuccess(String message) {
                        log.info("Success:" + message);
                        buildDone.countDown();
                    }

                    @Override
                    public void onError(String messsage) {
                        log.error("Failure:" + messsage);
                        buildDone.countDown();
                    }

                    @Override
                    public void onEvent(String event) {
                        log.info("Success:" + event);
                    }
                })
                .fromFolder(imageFolder);

        buildDone.await();
        handle.close();
        client.close();
        return true;
    }

    public static boolean pushDockerImageToRegistry(String dockerUrl, String registry, String namespace, String image)
            throws InterruptedException, IOException {
        String repositoryName = registry + "/" + namespace + "/" + image;

        Config config = new ConfigBuilder()
                .withDockerUrl(dockerUrl)
                .build();

        DockerClient client = new DefaultDockerClient(config);
        final CountDownLatch pushDone = new CountDownLatch(1);

        client.image().withName(image).tag().inRepository(repositoryName).force().withTagName("1.0");

        OutputHandle handle = client.image().withName(repositoryName).push()
                .usingListener(new EventListener() {
                    @Override
                    public void onSuccess(String message) {
                        log.info("Success:" + message);
                        pushDone.countDown();
                    }

                    @Override
                    public void onError(String messsage) {
                        log.error("Failure:" + messsage);
                        pushDone.countDown();
                    }

                    @Override
                    public void onEvent(String event) {
                        log.info("Success:" + event);
                    }
                })
                .withTag("1.0")
                .toRegistry();

        pushDone.await();
        handle.close();
        client.close();
        return true;
    }

}
