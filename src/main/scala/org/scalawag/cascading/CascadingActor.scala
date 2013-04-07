package org.scalawag.cascading

import akka.actor.{ActorContext, ActorRef, Actor}
import concurrent.duration.Duration
import concurrent.ExecutionContext
import util.Try

abstract class CascadingActor(implicit executor:ExecutionContext) extends Actor with Logging {

  override final def receive = {
    case SASE(parent,index,enclosure) => handleMessage(Some((parent,index)),enclosure)
    case message => handleMessage(None,message)
  }

  private def handleMessage(returnAddress:Option[(ActorRef,Int)],message:Any) = {
    val manager:Manager = new Manager
    val result = Try(receive(manager)(message))
    val cascader = new Cascader(context,executor)
    manager.cascades.foreach( c => cascader.send(c.target,c.message))
    cascader.receiveResult = result
    cascader.toFuture.onSuccess { case r =>
      val response = buildResponse(r)
      returnAddress match {
        case Some((parent,index)) => sendAcknowledgement(parent,index,response)
        case None => // noop
      }
      onComplete(message,response)
    }
  }

  private def sendAcknowledgement(parent:ActorRef,index:Int,result:StageResults) {
    // This notifies the parent of the outcome of processing this cascaded message.
    parent ! Response(index,result)
    // This allows any actor-specific handling to proceed (e.g., restarting the actor).
    if ( result.receiveResult.isFailure )
      throw result.receiveResult.failed.get
  }

  def receive(manager:Manager):PartialFunction[Any,Any]

  def buildResponse(results:StageResults):StageResults = results

  def onComplete(msg:Any,results:StageResults) {}

}

case class Cascade(message:Any,target:ActorRef)
//implicit def toCascade(pair:Pair[Any,ActorRef]) = Cascade(target,message)

class Manager {
  var cascades:Seq[Cascade] = Seq.empty
  var timeout:Option[Duration] = None
  var data:Option[Any] = None

  def add(cascade:Cascade) = this.cascades +:= cascade
  def add(cascades:Iterable[Cascade]) = this.cascades ++:= cascades
  def timeoutAfter(duration:Duration) = this.timeout = Some(duration)
  def setReceiveData(data:Any) = this.data = Some(data)
}
