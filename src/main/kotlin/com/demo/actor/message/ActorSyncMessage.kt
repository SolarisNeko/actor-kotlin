package com.demo.actor.message;

import java.util.concurrent.CountDownLatch


class ActorSyncMessage(
    val sender: String,
    val message: Any
) {
    private val countDownLatch = CountDownLatch(1)

//    fun getMessage(): Any {
//        return message
//    }


    fun finish() {
        countDownLatch.countDown()
    }

    @Throws(Exception::class)
    fun waitFinish() {
        countDownLatch.await()
    }
}