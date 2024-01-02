# actor-kotlin

Actor 的实现 by kotlin

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
    <version>0.1.0</version>
</dependency>
```

## Gradle

```kotlin
implementation("com.neko233:actor-kotlin:0.1.0")
```

# Architecture 架构

## Core 关注点
1. Actor online/offline 两种状态. 当 offline 时候也能处理.
2. Actor all operator will through actor-system schedule.

## Struct

ActorSystem

- Actor
- ActorWorkerCenter

## Lifecycle

ActorSystem Lifecycle:

- start
- shutdown

# Code

## Actor Class | 创建你的 Actor 类

```kotlin

class AnnotationActor(actorId: String) : Actor(actorId) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(AnnotationActor::class.java)!!

    }

    @ActorMethodOnline
    fun onlineFun(
        sender: Actor,
        data: String,
    ) {
        LOGGER.warn(
            "\nfromActorId={}, toActorId={}, data={}\n", sender.getActorId(),
            this.getActorId(),
            data
        )
    }

    @ActorMethodOffline
    fun offlineFun(
        toActorId: String,
        data: OfflineMockData,
    ) {
        LOGGER.warn(
            "\nmyActorId={}, toActorId={}, data={}\n", this.getActorId(),
            toActorId,
            data
        )
    }


}
```

## 2、Run a ActorSystem quickly | 快速启动一个 Actor System

```kotlin

@Test
fun demo() {
    // create a Actor-System name of "demo"
    val actorSystem = ActorApp233.run("demo")


    // register actor
    val actor1 = AnnotationActor("demo")
    actorSystem.addActor(actor1)

    println()

    // send to online actor (myself)
    actor1.send("demo", "demo")
    actor1.send("demo", "test-thread2")


    // online-actor send to offline-actor
    actor1.send(
        "demo233", OfflineMockData(
            name = "offline-thread1"
        )
    )
    actor1.send(
        "demo233", OfflineMockData(
            name = "offline-thread2"
        )
    )

    println()

}

}
```
