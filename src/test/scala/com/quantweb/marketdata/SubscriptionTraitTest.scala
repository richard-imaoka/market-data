package com.quantweb.marketdata

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.quantweb.marketdata.SymbolActor.{SubscriptionRequest, SubscriptionSuccess}
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration.{FiniteDuration, DurationInt}

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
        expectNoMsg(5.milliseconds)

        /**
         * Once received SubscriptionRequest, receiveMarketData() should process messages - in this case,echo back a String message
         */
        subscriberActorRef1.underlyingActor.subscribe(testActor)
        expectMsg[SubscriptionRequest](1.second, SubscriptionRequest(subscriberActorRef1))
        subscriberActorRef1 ! SubscriptionSuccess(testActor)
        subscriberActorRef1 ! "echo"
        expectMsg[String](5.milliseconds, "echo")
    }

    it should "keep sending SubscriptionRequest" in {
        val subscriberActorRef1 = TestActorRef[SubscriptionActor](Props(new SubscriptionActor(){
            override val retryInterval : FiniteDuration  = 1.milliseconds
            override val retryCount : Int = 5
            override def receiveMarketData = Actor.emptyBehavior
        }))
        subscriberActorRef1.underlyingActor.subscribe(testActor)

        expectMsg[SubscriptionRequest](10.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectMsg[SubscriptionRequest](10.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectMsg[SubscriptionRequest](10.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectMsg[SubscriptionRequest](10.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectMsg[SubscriptionRequest](10.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectNoMsg(10.milliseconds)
    }

    it should "stop repeating SubscriptionRequest on receipt of SubscriptionSuccess" in {
        val subscriberActorRef1 = TestActorRef[SubscriptionActor](Props(new SubscriptionActor(){
            override val retryInterval : FiniteDuration  = 20.milliseconds
            override val retryCount : Int = 5
            override def receiveMarketData = Actor.emptyBehavior
        }))

        subscriberActorRef1.underlyingActor.subscribe(testActor)
        expectMsg[SubscriptionRequest](20.milliseconds, SubscriptionRequest(subscriberActorRef1))
        subscriberActorRef1 ! SubscriptionSuccess(testActor)
        expectNoMsg(20.milliseconds)
    }
}
