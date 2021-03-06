package com.xtuone.actor

import java.sql.{Connection, ResultSet, PreparedStatement}
import java.util

import akka.actor._
import akka.event.Logging
import com.google.gson.Gson
import com.xtuone.bo.BaseMessageBO
import com.xtuone.message.{Paper, ChatMsg}
import com.xtuone.util.jdbc.JdbcUtil
import com.xtuone.util.model.AnpsMessage
import com.xtuone.util.redis.RedisUtil213
import com.xtuone.util._
import org.slf4j.LoggerFactory

/**
 * Created by Zz on 2015/1/13.
 */
class PaperActor extends Actor with ActorLogging{

  val logBack = LoggerFactory.getLogger(classOf[PaperActor])

  var apnsPaperActor:ActorRef =_

  //失败标识
  var failureFlag = false
  var count = 0

  @throws(classOf[Exception])
  override def preStart(): Unit ={
    super.preStart()
    apnsPaperActor = context.actorOf(Props[ApnsPaperActor])
    context watch apnsPaperActor
  }

  override def receive: Receive = {
    case chatMsg:ChatMsg =>{

      val g = new Gson()
      val pushMessage = new BaseMessageBO
      pushMessage.setPd(g.toJson(chatMsg.pager))
      pushMessage.setMt(MessageType.CHAT)
      val result = GopushUtil.pushMessage( g.toJson(pushMessage) ,MethodHelper.getPushKey(chatMsg.chatIdStr), chatMsg.expireTime)
      MethodHelper.monitorStatus(result)
      logBack.info("gopush-->result:"+result+": chatId :"+chatMsg.chatIdStr )

      //推apns(学生用户)
      if(chatMsg.contactsTypeInt == Constant.contactsTypeInt_student){
        apnsPaperActor ! chatMsg
      }
    }
    case Terminated(a) =>{
      if( a.compareTo(apnsPaperActor) == 0 ){
        context.stop(apnsPaperActor)
        apnsPaperActor = context.actorOf(Props[ApnsPaperActor])
        context watch apnsPaperActor
        logBack.info(" restart apnsPaperActor")
      }

    }
  }

}

/**
 * 极光推送
 */
class ApnsPaperActor extends Actor with ActorLogging{

  val logBack = LoggerFactory.getLogger(classOf[ApnsPaperActor])

  override def receive: Actor.Receive = {
    case chatMsg:ChatMsg =>{
        val deviceToken = MethodHelper.findUserDeviceToken(chatMsg.chatIdStr)

        if(MethodHelper.isNotEmpty(deviceToken)){
          logBack.info("deviceToken:"+deviceToken+"   chatId:"+ chatMsg.chatIdStr )
          val apnsMessage = new AnpsMessage
          //增加badge数量
          val badge = RedisUtil213.init().incr(Constant.KEY_APNS_NO_READ_NUM+chatMsg.chatIdStr)
          apnsMessage.setBadge(badge.toInt)
          apnsMessage.setAlert(chatMsg.pager.nicknameStr+":"+chatMsg.pager.contentStr)
          val extras = new util.HashMap[String,String]()
          //发送人的聊天id
          extras.put("ci",chatMsg.pager.chatIdStr)
          extras.put("mt",MessageType.CHAT+"")
          apnsMessage.setExtras(extras)
          //推送到apns
          val result =  ApnsPushUtil.push(apnsMessage,deviceToken)
          logBack.info("apns-->result:"+result+": chatId :"+chatMsg.chatIdStr )
        }

    }
  }



}
