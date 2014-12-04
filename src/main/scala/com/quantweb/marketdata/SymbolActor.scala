package com.quantweb.marketdata

import akka.actor.Actor
import com.quantweb.marketdata.SymbolActor.MarketDataUpdate

/**
 * Created by Richard S, Imaoka on 2014/11/30.
 */
class SymbolActor extends Actor {

    var data: Map[String, Any] = Map[String, Any]()

    override def receive = {
        case MarketDataUpdate(receivedData) => data = receivedData
    }

}

object SymbolActor{
    case class MarketDataUpdate( data: Map[String, Any] )
}