package com.quantweb.marketdata

import akka.actor.{ActorRef, Actor}
import akka.routing.{BroadcastRoutingLogic, Router}
import com.quantweb.marketdata.SymbolActor.{SubscriptionSuccess, SubscriptionRequest, SendEntireData, MarketDataUpdate}

/**
 * Created by Richard S, Imaoka on 2014/11/30.
 */
class SymbolActor extends Actor {

    /**
     * Mutable reference to immutable Map - this "data" can be passed outside the Actor, and then updated without affecting the Actor
     */
    var data: Map[String, Any] = Map[String, Any]()

    /**
     * SymbolActor always performs broadcast when receive() method receives MarketDataUpdate()
     */
    var router: Router = Router(BroadcastRoutingLogic())

    override def receive = {
        case MarketDataUpdate(receivedData) => {
            /**
             * receivedData can be partial update. So merge it with the original 'data'
             * if the same key exists in both 'data' and 'receivedData', receivedData values are preferred
             */
            data = data ++ receivedData

            /**
             * route() with empty routee (= NoRoutee) will cause the message sent to DeadLetter offfice
             */
            if( router.routees.size > 0 )
                router.route(receivedData, self)
        }
        case SendEntireData(ref) => {
            /**
             * send the entire internal data back to ref
             */
            ref ! data
        }
        case SubscriptionRequest(subscriber) => {
            /**
             * register a new subscriber to this SymbolActor's routee
             */
            router = router.addRoutee(subscriber)
            subscriber ! SubscriptionSuccess
        }
    }

    /**
     * register a new subscriber to this SymbolActor's routee
     */
    def registerSubscriber(subscriber: ActorRef) = router = router.addRoutee(subscriber)

}

object SymbolActor{
    sealed abstract class SymbolActorMessage
    case class MarketDataUpdate( data: Map[String, Any] ) extends SymbolActorMessage
    case class SendEntireData( ref: ActorRef ) extends SymbolActorMessage
    case class SubscriptionRequest( subscriber: ActorRef ) extends SymbolActorMessage
    case object SubscriptionSuccess extends SymbolActorMessage
}
