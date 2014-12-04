package com.quantweb.marketdata

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.{Matchers, FlatSpecLike}

/**
 * Created by Richar S. Imaoka on 2014/11/30.
 */

class SymbolActorTest extends TestKit(ActorSystem("SymbolActorTest")) with FlatSpecLike with Matchers {

    "SymbolActor" should "update internal state on Market Data update" in {
        val testActorRef = TestActorRef[SymbolActor](Props(new SymbolActor))
        val data1 = Map[String, Any]("bestAsk" -> 101, "bestAskSize" -> 1050, "bestBid" -> 99, "bestBidSize" -> 980, "timeStamp" -> "2014-10-20 13:15:21:99")
        val data2 = Map[String, Any]("bestAsk" -> 102, "bestAskSize" -> 1150, "bestBid" -> 99, "bestBidSize" -> 980, "timeStamp" -> "2014-10-20 13:15:24:99")

        testActorRef ! data1
        testActorRef.underlyingActor.data should be(data1)

        testActorRef ! data2
        testActorRef.underlyingActor.data should not be (data1)
        testActorRef.underlyingActor.data should be(data2)
    }

    "SymbolActor" should "publish Market Data update" in {
    }

    "SymbolActor" should "update internal state on partial Market Data update" in {
    }

    "SymbolActor" should "publish partial Market Data update" in {
    }

    "SymbolActor" should "send entire data upon request" in {
    }
}
