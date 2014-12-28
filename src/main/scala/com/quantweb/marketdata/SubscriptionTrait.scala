package com.quantweb.marketdata

import akka.actor.{Cancellable, Actor, ActorRef}
import com.quantweb.marketdata.SymbolActor.{SubscriptionSuccess, SubscriptionRequest}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt

/**
 * Created by Richar S. Imaoka on 2014/12/07.
 */

trait SubscriptionTrait extends {
    /**
     * Scala self-type annotation - this trait needs to be mixed into an Actor
     */
    this: Actor =>

    val retryInterval: FiniteDuration = 1.second

    val retryCount: Int = 60

    object SubscriptionStop

    var schedulerOption: Option[Cancellable] = None

    /**
     * Receive function when Subscription is not yet established
     * SubscriptionStop => stop repetitive SubscriptionRequest (i.e.) cancel the scheduler
     * SubscriptionSuccess => become(receiveAfterSubscription) where receiveAfterSubscription ignores SubscriptionRetry
     */
    def waitingForSubscriptionSuccess: Receive = {
        case SubscriptionStop => {
            schedulerOption.foreach(x => x.cancel())
        }
        case SubscriptionSuccess(publisher) => {
            schedulerOption.foreach( x => x.cancel() )
            context.watch(publisher)
            context.become(receiveTerminationFromPublisher orElse receiveTerminationFromPublisher orElse receiveMarketData)
        }
    }

    /**
     * Receive behavior once Subscription is established, to be defined in the implementation class
     */
    def receiveMarketData: Receive

    def receiveTerminationFromPublisher: Receive = Actor.emptyBehavior

    /**
     * Initially the receive function should wait for subscription to be established
     */
    final override def receive = waitingForSubscriptionSuccess

    /**
     * Sends SubscriptionRequest to publisher. It retries SubscriptionRequest
     */
    def subscribe(publisher: ActorRef): Unit = {
        implicit val ec: ExecutionContext = context.dispatcher

        schedulerOption = Some(context.system.scheduler.schedule(0.second, retryInterval, new Runnable{
            var remainingRetries: Int = retryCount
            override def run(): Unit = {
                if(remainingRetries > 0)
                    publisher ! SubscriptionRequest(self)
                else
                    self ! SubscriptionStop
                remainingRetries = remainingRetries -1
            }
        }))
    }
}

