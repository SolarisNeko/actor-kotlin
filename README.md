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

## Actor Class

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

## Start ActorSystem

```kotlin

@Test
fun demo() {
    val system = ActorApp233.run("demo")
    
    // here
    val a1 = AnnotationActor("demo")
    system.addActor(a1)

    println()

    // test
    a1.send("demo", "demo")
    a1.send("demo", "test-thread2")


    a1.send(
        "demo233", OfflineMockData(
            name = "offline-thread1"
        )
    )
    a1.send(
        "demo233", OfflineMockData(
            name = "offline-thread2"
        )
    )

    println()

}
```
