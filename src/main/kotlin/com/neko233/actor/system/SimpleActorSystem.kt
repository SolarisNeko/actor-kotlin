package com.neko233.actor.system

import com.neko233.actor.worker.ActorWorkerCenter

class SimpleActorSystem(
    systemName: String,
    actorWorkerCenter: ActorWorkerCenter
) :
    ActorSystem(systemName, actorWorkerCenter)
{
}