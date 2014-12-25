package com.quantweb.marketdata

import akka.actor.{Actor, ActorSystem, Props}
import akka.routing.ActorRefRoutee
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.quantweb.marketdata.SymbolActor.{SubscriptionRequest, SubscriptionSuccess, SymbolActorMessage}
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration.DurationInt

/**
 * Created by Richar S. Imaoka on 2014/12/07.
 */

class SubscriptionActor extends SubscriptionTrait with Actor{
    override def receiveMarketData = {
        case a: String => sender() ! a
    }
}

class SubscriptionTraitTest
    extends TestKit(ActorSystem("SubscriptionTraitTest"))
    with FlatSpecLike
    with Matchers       //To use "should" matchers
    with ImplicitSender //To use "testActor" which is the entry point for various expectMsg assertions - http://doc.akka.io/docs/akka/snapshot/scala/testing.html
    //When mixing in the trait ImplicitSender this test actor is implicitly used as sender reference when dispatching messages from the test procedure.
{
    it should "subscribe to SymbolActor" in {
        val subscriberActorRef1 = TestActorRef[SubscriptionActor](Props(new SubscriptionActor()))

        subscriberActorRef1.underlyingActor.subscribe(testActor)
        expectMsg[SubscriptionRequest](1.second, SubscriptionRequest(subscriberActorRef1))
    }

    it should "change the state upon receipt of SubscriptionSuccess message" in {
        val subscriberActorRef1 = TestActorRef[SubscriptionActor](Props(new SubscriptionActor()))

        /**
         * In the initial state, it should only process SubscriptionRequest but ignore other messages
         */
        subscriberActorRef1 ! "echo"
        expectNoMsg(100.milliseconds)

        /**
         * Once received SubscriptionRequest, receiveMarketData() should process messages - in this case,echo back a String message
         */
        subscriberActorRef1.underlyingActor.subscribe(testActor)
        expectMsg[SubscriptionRequest](1.second, SubscriptionRequest(subscriberActorRef1))
        subscriberActorRef1 ! SubscriptionSuccess
        subscriberActorRef1 ! "echo"
        expectMsg[String](100.milliseconds, "echo")
    }

    it should "allow a single subscriber to subscribe to multiple SymbolActors" in {
        val subscriberActorRef1 = TestActorRef[SubscriptionActor](Props(new SubscriptionActor()))
        val symbolActorRef1 = TestActorRef[SymbolActor](Props(new SymbolActor))
        val symbolActorRef2 = TestActorRef[SymbolActor](Props(new SymbolActor))
        val symbolActorRef3 = TestActorRef[SymbolActor](Props(new SymbolActor))

        /**
         * subscribeActor subscribes to 3 symbol actors, and confirm the subscriber is registered in all these 3
         */
        subscriberActorRef1.underlyingActor.subscribe(symbolActorRef1)
        subscriberActorRef1.underlyingActor.subscribe(symbolActorRef2)
        subscriberActorRef1.underlyingActor.subscribe(symbolActorRef3)

        symbolActorRef1.underlyingActor.router.routees should contain (ActorRefRoutee(subscriberActorRef1))
        symbolActorRef2.underlyingActor.router.routees should contain (ActorRefRoutee(subscriberActorRef1))
        symbolActorRef3.underlyingActor.router.routees should contain (ActorRefRoutee(subscriberActorRef1))
    }
}
