package com.demo.samples;

import com.demo.actor.core.Actor;
import com.demo.actor.system.ActorSystem;
import com.demo.actor.system.SimpleActorSystem;
import com.demo.actor.worker.ActorWorkerCenter;
import com.demo.actor.worker.DefaultActorWorkerCenter;
import com.demo.actor.worker.impl.CoroutineActorWorker;
import com.demo.samples.data.DemoActorUser;
import com.demo.samples.data.DemoSyncCallbackMessage;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

@Slf4j
public class ActorSample {

}
