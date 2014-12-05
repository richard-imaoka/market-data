package com.quantweb.marketdata

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.quantweb.marketdata.SymbolActor.MarketDataUpdate
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration.DurationInt

/**
 * Created by Richar S. Imaoka on 2014/11/30.
 */

class SymbolActorTest
    extends TestKit(ActorSystem("SymbolActorTest"))
    with FlatSpecLike
    with Matchers       //To use "should" matchers
    with ImplicitSender //To use "testActor" which is the entry point for various expectMsg assertions - http://doc.akka.io/docs/akka/snapshot/scala/testing.html
                         //When mixing in the trait ImplicitSender this test actor is implicitly used as sender reference when dispatching messages from the test procedure.
{
    "SymbolActor" should "update internal state on Market Data update" in {
        val testActorRef = TestActorRef[SymbolActor](Props(new SymbolActor))
        val data1 = Map[String, Any]("bestAsk" -> 101, "bestAskSize" -> 1050, "bestBid" -> 99, "bestBidSize" -> 980, "timeStamp" -> "2014-10-20 13:15:21:99")
        val data2 = Map[String, Any]("bestAsk" -> 102, "bestAskSize" -> 1150, "bestBid" -> 99, "bestBidSize" -> 980, "timeStamp" -> "2014-10-20 13:15:24:99")

        testActorRef ! MarketDataUpdate(data1)
        testActorRef.underlyingActor.data should be(data1)

        testActorRef ! MarketDataUpdate(data2)
        testActorRef.underlyingActor.data should not be (data1)
        testActorRef.underlyingActor.data should be(data2)
    }

    "SymbolActor" should "broadcast Market Data update" in {
        val testActorRef = TestActorRef[SymbolActor](Props(new SymbolActor))

        val subscriberActorRef1 = system.actorOf(Props(new PassThroughActor(testActor)))
        val subscriberActorRef2 = system.actorOf(Props(new PassThroughActor(testActor)))
        val subscriberActorRef3 = system.actorOf(Props(new PassThroughActor(testActor)))

        testActorRef.underlyingActor.registerSubscriber(subscriberActorRef1)
        testActorRef.underlyingActor.registerSubscriber(subscriberActorRef2)
        testActorRef.underlyingActor.registerSubscriber(subscriberActorRef3)

        val data = Map[String, Any]("bestAsk" -> 101, "bestAskSize" -> 1250, "bestBid" -> 99, "bestBidSize" -> 980, "timeStamp" -> "2014-10-20 13:15:21:99")

        /**
         * Broadcast to 3 subscribers - the same message is received 3 times
         */
        testActorRef ! MarketDataUpdate(data)
        expectMsg[Map[String, Any]](1.seconds, data)
        expectMsg[Map[String, Any]](1.seconds, data)
        expectMsg[Map[String, Any]](1.seconds, data)
    }

    "SymbolActor" should "update internal state on partial Market Data update" in {
        val testActorRef = TestActorRef[SymbolActor](Props(new SymbolActor))

        /**
         * Firstly SymbolActor holds "originalData", then receives "partialUpdate", which results in "updatedData" as SymbolActor's (internal) data
         *
         * (i.e.) originalData + partialUpdate = updatedData
         */
        val originalData  = Map[String, Any]("bestAsk" -> 101, "bestAskSize" -> 1050, "bestBid" -> 99, "bestBidSize" -> 980, "timeStamp" -> "2014-10-20 13:15:21:99")
        val partialUpdate = Map[String, Any]("bestAsk" -> 102, "bestAskSize" -> 1150, "timeStamp" -> "2014-10-20 13:15:24:99")
        val updatedData   = Map[String, Any]("bestAsk" -> 102, "bestAskSize" -> 1150, "bestBid" -> 99, "bestBidSize" -> 980, "timeStamp" -> "2014-10-20 13:15:24:99")

        testActorRef.underlyingActor.data = originalData

        testActorRef ! MarketDataUpdate(partialUpdate)

        testActorRef.underlyingActor.data should be (updatedData)
    }

    "SymbolActor" should "broadcast partial Market Data update" in {
        val testActorRef = TestActorRef[SymbolActor](Props(new SymbolActor))

        val subscriberActorRef1 = system.actorOf(Props(new PassThroughActor(testActor)))
        val subscriberActorRef2 = system.actorOf(Props(new PassThroughActor(testActor)))
        val subscriberActorRef3 = system.actorOf(Props(new PassThroughActor(testActor)))

        testActorRef.underlyingActor.registerSubscriber(subscriberActorRef1)
        testActorRef.underlyingActor.registerSubscriber(subscriberActorRef2)
        testActorRef.underlyingActor.registerSubscriber(subscriberActorRef3)

        /**
         * Firstly SymbolActor holds "originalData", then receives "partialUpdate"
         */
        val originalData  = Map[String, Any]("bestAsk" -> 101, "bestAskSize" -> 1050, "bestBid" -> 99, "bestBidSize" -> 980, "timeStamp" -> "2014-10-20 13:15:21:99")
        val partialUpdate = Map[String, Any]("bestAsk" -> 102, "bestAskSize" -> 1150, "timeStamp" -> "2014-10-20 13:15:24:99")

        testActorRef.underlyingActor.data = originalData

        /**
         * Broadcast to 3 subscribers - the same message is received 3 times
         * if partial update is sent to SymbolActor, then partial update is broadcast, not the entire (internal) 'data' of SymbolActor
         */
        testActorRef ! MarketDataUpdate( partialUpdate )
        expectMsg[Map[String, Any]](1.seconds, partialUpdate)
        expectMsg[Map[String, Any]](1.seconds, partialUpdate)
        expectMsg[Map[String, Any]](1.seconds, partialUpdate)
    }

    "SymbolActor" should "send entire data upon request" in {
    }
}
