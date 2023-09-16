package com.demo.actor.system

import com.demo.actor.worker.ActorWorkerCenter

class SimpleActorSystem(
    systemName: String,
    actorWorkerCenter: ActorWorkerCenter
) :
    ActorSystem(systemName, actorWorkerCenter)
{
}