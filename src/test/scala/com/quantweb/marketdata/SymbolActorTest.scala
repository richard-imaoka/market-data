package com.quantweb.marketdata

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.{Matchers, FlatSpecLike}

/**
 * Created by Richar S. Imaoka on 2014/11/30.
 */

class SymbolActorTest extends TestKit(ActorSystem("SymbolActorTest")) with FlatSpecLike with Matchers {

    "SymbolActor" should "update internal state on Market Data update" in {
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
