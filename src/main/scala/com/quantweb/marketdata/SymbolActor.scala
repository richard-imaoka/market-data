package com.quantweb.marketdata

import akka.actor.{ActorRef, Actor}
import akka.routing.{BroadcastRoutingLogic, Router}
import com.quantweb.marketdata.SymbolActor.MarketDataUpdate

/**
 * Created by Richard S, Imaoka on 2014/11/30.
 */
class SymbolActor extends Actor {

    var data: Map[String, Any] = Map[String, Any]()

    /**
     * SymbolActor always performs broadcast when receive() method receives MarketDataUpdate()
     */
    var router: Router = Router(BroadcastRoutingLogic())

    override def receive = {
        case MarketDataUpdate(receivedData) => {
            data = receivedData

            if( router.routees.size > 0 ) //route with empty routee (= NoRoutee) will cause the message sent to DeadLetter offfice
                router.route(data, self)
        }
    }

    /**
     * register a new subscriber to this SymbolActor's routee
     */
    def registerSubscriber(subscriber: ActorRef) = router = router.addRoutee(subscriber)
}

object SymbolActor{
    case class MarketDataUpdate( data: Map[String, Any] )
}