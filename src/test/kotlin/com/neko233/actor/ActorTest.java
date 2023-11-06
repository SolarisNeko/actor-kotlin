package com.neko233.actor;

import com.neko233.actor.core.Actor;
import com.neko233.actor.system.ActorSystem;
import com.neko233.actor.system.SimpleActorSystem;
import com.neko233.actor.worker.ActorWorkerCenter;
import com.neko233.actor.worker.ActorWorkerCenterApi;
import com.neko233.actor.worker.impl.CoroutineActorWorker;
import com.neko233.mock.DemoActorUser;
import com.neko233.mock.DemoSyncCallbackMessage;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/**
 * @author SolarisNeko
 * Date on 2023-08-27
 */
@Slf4j
public class ActorTest {

    @Test
    public void demo() throws InterruptedException {

        // worker
        ActorWorkerCenterApi actorWorkerCenter = new ActorWorkerCenter(
                "actorWorkerCenter",
                Runtime.getRuntime().availableProcessors() * 2,
                CoroutineActorWorker::new
        );
        // system
        ActorSystem actorSystem = new SimpleActorSystem(
                "actorSystem",
                actorWorkerCenter
        );
        actorSystem.start();

        // build actor
        final Actor actor1 = new DemoActorUser("actor1")
                .registerOnlineMessageHandler(String.class, (sender, receiver, message) -> {
                    log.info("收到 string msg = {}", message);
                })
                .registerOnlineMessageHandler(DemoSyncCallbackMessage.class, (sender, receiver, msg) -> {
                    String request = msg.getRequest();
                    log.info("[sync] 收到 msg = {}", request);

                    msg.setResponse("已收到 sync msg. receiver = " + receiver.getActorId());
                });
        final Actor actor2 = new DemoActorUser("actor2");

        actorSystem.addActor(actor1);
        actorSystem.addActor(actor2);

        // 1、async invoke
        actorSystem.sendMessageAsync("actor1", "actor2", "Hello from actor1!");
        actorSystem.sendMessageAsync("actor2", "actor1", "Hello from actor2!");

        // actor send
        actor2.send("actor1", "halo actor1");

        DemoSyncCallbackMessage build = DemoSyncCallbackMessage.builder()
                .request("[sync] demo callback msg")
                .build();
        // 2、sync invoke
        actor2.talkOrderly("actor1", build);
        log.info("[sync-callback] message = {}", build.getResponse());

        log.warn("==================================================");

        val es = Executors.newFixedThreadPool(30);
        // thread = 30 x executeLoop = 10w = 300w times
        int threadCount = 30;

        int loopCount = 100;

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            es.execute(() -> {
                int count = 1;
                while (true) {
                    actor1.send("actor2", "test -- " + count);
                    count++;

                    // 检测协程
//                    try {
//                        TimeUnit.SECONDS.sleep(5);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }

                    if (count > loopCount) {
                        break;
                    }
                }

                countDownLatch.countDown();
            });
        }

        countDownLatch.await();

        es.shutdown();

        actorSystem.shutdown();

        log.warn("==================================================");
    }
}