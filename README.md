# actor-kotlin

Actor 的实现

# JDK version
JDK 17+

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