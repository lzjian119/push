package com.xtuone.actor

import akka.actor.{Actor, ActorLogging}
import akka.event.Logging
import com.xtuone.model.Check
import com.xtuone.util.Const
import org.slf4j.LoggerFactory

/**
 * Created by Zz on 2015/4/7.
 */
class ClientCheckActor extends Actor with ActorLogging{

  val logBack = LoggerFactory.getLogger(classOf[ClientCheckActor])

  override def receive: Receive = {

    case Check =>{

      val map = Const.clientMap
      map.foreach{
        case (key,client) =>{
          if(System.currentTimeMillis() - client.addTime > Const.timeout){
            //移除对应主题
            Const.clientMap =  Const.clientMap.-(key)
          }
        }
      }
      logBack.info("clientMap:"+Const.clientMap)

    }

  }

}
