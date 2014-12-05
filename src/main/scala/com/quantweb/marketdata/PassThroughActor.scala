package com.quantweb.marketdata

import akka.actor.{ActorRef, Actor}

/**
 * Created by Richar S. Imaoka on 2014/12/05.
 */

/**
 * An Actor whose receive() method always sends every single message to targetRef. which is passed in to the constructor
 */
class PassThroughActor(targetRef: ActorRef) extends Actor {

    /**
     * Sends (passes through) every single message to targetRef
     */
    override def receive = {
        case message: Any => targetRef ! message
    }
}
