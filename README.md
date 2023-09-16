# actor-kotlin

Actor 的实现

# JDK version
JDK 8+

# Introduce

kotlin actor implement, because kotlin have 'coroutine' to use.

and also provide 'thread' to handle actor message

使用 kotlin 实现只是为了更高效率的 coroutine

# Use

## maven

```xml

<dependency>
    <groupId>com.neko233</groupId>
    <artifactId>actor-kotlin</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Gradle

```kotlin
implementation("com.neko233:actor-kotlin:0.0.1")
```

# Architecture

## Struct

ActorSystem

- Actor
- ActorWorkerCenter

## Lifecycle

ActorSystem Lifecycle:

- start
- shutdown


# Code
## Actor Object
```java

// @must extends Actor
@Slf4j
public class DemoActorUser extends Actor {

    public DemoActorUser(String actorId) {
        super(actorId);
        
        // 可以此处注册 fixed message type callback
//        registerMessageHandler(String.class, (sender, receiver, message) -> {
//            log.info("收到 string msg = {}", message);
//        })
    }

    @Override
    public void onReceiveMessageWithoutAnyMatch(String sender,
                                                Object message) {
        // 未匹配的数据
        log.error("no match message type. message = {}", message);
    }
}
```

## Start system
```java

 ActorWorkerCenter actorWorkerCenter = new DefaultActorWorkerCenter(
                "actorWorkerCenter",
                Runtime.getRuntime().availableProcessors() * 2,
                CoroutineActorWorker::new
        );
        ActorSystem actorSystem = new SimpleActorSystem(
                "actorSystem",
                actorWorkerCenter
        );
        actorSystem.start();

        
```

## 声明 actor + register to ActorSystem
```java
 // build actor
        final Actor actor1 = new DemoActorUser("actor1");
        
        // actor1 注册 message type callback
        actor1.registerMessageHandler(String.class, (sender, receiver, message) -> {
                    log.info("收到 string msg = {}", message);
                })
                .registerMessageHandler(DemoSyncCallbackMessage.class, (sender, receiver, msg) -> {
                    String request = msg.getRequest();
                    log.info("[sync] 收到 msg = {}", request);

                    msg.setResponse("已收到 sync msg. receiver = " + receiver.getActorId());
                });
        final Actor actor2 = new DemoActorUser("actor2");

        actorSystem.addActor(actor1);
        actorSystem.addActor(actor2);
```

## message 通信
### Async 异步
```java

// actor send
        actor2.send("actor1", "halo actor1");

```

### sync 同步
```java
        // message data by any class 
        DemoSyncCallbackMessage build = DemoSyncCallbackMessage.builder()
                .request("[sync] demo callback msg")
                .build();

        // 2、sync invoke
        actor2.talkOrderly("actor1", build);
       
```