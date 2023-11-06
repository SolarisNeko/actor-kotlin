package com.neko233.actor.system

import com.neko233.actor.worker.ActorWorkerCenterApi

class SimpleActorSystem(
    systemName: String,
    actorWorkerCenter: ActorWorkerCenterApi
) :
    ActorSystem(systemName, actorWorkerCenter)
{
}