package com.quantweb.marketdata

import akka.actor.{Actor, ActorRef}
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

    case class SubscriptionRetry(publisher: ActorRef)

    val retryInterval: FiniteDuration = 1.second

    /**
     * Receive function when Subscription is not yet established
     * SubscriptionRetry => subscribe(publisher), which sends SubscriptionRequest, ans SubscriptionRetry to self
     * SubscriptionSuccess => become(receiveAfterSubscription) where receiveAfterSubscription ignores SubscriptionRetry
     */
    def waitingForSubscriptionSuccess: Receive = {
        case SubscriptionRetry(publisher) => subscribe(publisher)
        case SubscriptionSuccess => context.become(receiveMarketData orElse receiveTerminationFromPublisher) //orElse handle SymbolActor's Terminated
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

        println( "sending SubscriptionRequest to " + publisher)
        publisher ! SubscriptionRequest(self)
        context.system.scheduler.scheduleOnce(retryInterval, self, SubscriptionRetry)
    }
}

