package com.quantweb.marketdata

import akka.actor._
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
    it should "sends SubscriptionRequest upon calling subscribe()" in {
        val subscriberActorRef = TestActorRef[SubscriptionActor](Props(new SubscriptionActor()), "subscriberA")

        subscriberActorRef.underlyingActor.subscribe(testActor)
        expectMsg[SubscriptionRequest](1.second, SubscriptionRequest(subscriberActorRef))
        subscriberActorRef ! SubscriptionSuccess(testActor)
    }

    it should "change the state upon receipt of SubscriptionSuccess message" in {
        val subscriberActorRef = TestActorRef[SubscriptionActor](Props(new SubscriptionActor()), "subscriberB")

        /**
         * In the initial state, it should only process SubscriptionRequest but ignore other messages
         */
        subscriberActorRef ! "echo"
        expectNoMsg(5.milliseconds)

        /**
         * Once received SubscriptionRequest, receiveMarketData() should process messages - in this case,echo back a String message
         */
        subscriberActorRef.underlyingActor.subscribe(testActor)
        expectMsg[SubscriptionRequest](1.second, SubscriptionRequest(subscriberActorRef))
        subscriberActorRef ! SubscriptionSuccess(testActor)
        subscriberActorRef ! "echo"
        expectMsg[String](5.milliseconds, "echo")
    }

    it should "keep sending SubscriptionRequest" in {
        val subscriberActorRef1 = TestActorRef[SubscriptionActor](Props(new SubscriptionActor(){
            override val retryInterval : FiniteDuration  = 1.milliseconds
            override val retryCount : Int = 5
            override def receiveMarketData = Actor.emptyBehavior
        }), "subscriberC")
        subscriberActorRef1.underlyingActor.subscribe(testActor)

        expectMsg[SubscriptionRequest](50.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectMsg[SubscriptionRequest](50.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectMsg[SubscriptionRequest](50.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectMsg[SubscriptionRequest](50.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectMsg[SubscriptionRequest](50.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectNoMsg(50.milliseconds)
    }

    it should "stop repeating SubscriptionRequest on receipt of SubscriptionSuccess" in {
        val subscriberActorRef1 = TestActorRef[SubscriptionActor](Props(new SubscriptionActor(){
            override val retryInterval : FiniteDuration  = 20.milliseconds
            override val retryCount : Int = 5
            override def receiveMarketData = Actor.emptyBehavior
        }), "subscriberD")

        subscriberActorRef1.underlyingActor.subscribe(testActor)
        expectMsg[SubscriptionRequest](50.milliseconds, SubscriptionRequest(subscriberActorRef1))
        expectMsg[SubscriptionRequest](50.milliseconds, SubscriptionRequest(subscriberActorRef1))
        subscriberActorRef1 ! SubscriptionSuccess(testActor)
        expectNoMsg(50.milliseconds)
    }

    it should "receive Terminated message when SymbolActor is stopped" in {
        val symbolActor1 = TestActorRef[SymbolActor](Props(new SymbolActor()), "symbolA")
        val subscriberActorRef1 = TestActorRef[SubscriptionActor](Props(new SubscriptionActor(){
            override val retryInterval : FiniteDuration  = 1.milliseconds
            override val retryCount : Int = 5
            override def receiveTerminationFromPublisher: Receive = {
                case message: Terminated => testActor ! "Terminated!"
            }
        }), "subscriberE")

        subscriberActorRef1.underlyingActor.subscribe(symbolActor1)
        subscriberActorRef1 ! "echo"
        expectMsg[String](50.milliseconds, "echo")
        subscriberActorRef1.underlyingActor.schedulerOption.foreach( x => x.cancel() )

        symbolActor1.stop()
        expectMsg[String](50.milliseconds, "Terminated!")
    }
}
