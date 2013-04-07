package org.scalawag.cascading

import concurrent.{ExecutionContext, Promise, Future, promise}
import akka.actor.{Actor, Props, ActorRef, ActorContext}
import scala.collection.mutable.ArrayBuffer
import util.Try

class Cascader (context:ActorContext,executor:ExecutionContext) extends Logging {

  var receiveResult:Try[Any] = null

  // Send a cascaded message whose acknowledgement will be managed by this Cascader.

  def send(target:ActorRef,message:Any) {
    val sase = SASE(actor,promises.length,message)
    promises += promise[StageResults]
    target ! sase
  }

  // Contains a promise for every cascaded message we've sent.

  private val promises = ArrayBuffer[Promise[StageResults]]()

  // This actor takes care of processing the responses to our cascaded messages from our cascadees.

  private var actorRef:Option[ActorRef] = None

  private def actor:ActorRef = actorRef match {
    case Some(a) =>
      a
    case None =>
      val a = context.actorOf(Props(new AcknowledgementGatherer))
      this.actorRef = Some(a)
      a
  }

  private class AcknowledgementGatherer extends Actor {
    def receive = {
      case Response(index,result) => promises(index).success(result)
    }
  }

  // This method finalizes the Cascader and extracts a Future that can be used to determine when all of
  // the cascaded messages have been processed.

  private[cascading] def toFuture:Future[StageResults] =
    if ( promises.isEmpty ) {
      Promise.successful(StageResults(receiveResult,Seq())).future
    } else {
      val p = Promise[StageResults]

      // This future represents the completion (acknowledgement) of all the cascaded messages.

      implicit val e = executor

      FutureUtils.all(promises.map(_.future)) onSuccess { case results =>

        // Once this Future is complete (representing all of the cascaded messages being acknowledged), it's OK to
        // kill our actor since all it's doing is listening for acknowledgments.

        log.debug("stopping temporary cascader actor: " + actor)

        // I did this with a foreach in case it hasn't been created.  That really shouldn't be possible because we
        // made it past the conditional above that guarantees that promises is not empty.  The only way for it not to
        // be empty is for the send method to have been called, which should also have the effect of creating the
        // actor.  Anyway, I'm being ridiculously careful.

        actorRef.foreach(context.stop)

        // Complete the future so that our caller can register their own callbacks to handle the completion of
        // all the cascaded messages.

        p.success(StageResults(receiveResult,results.map(_.get)))
      }

      p.future
    }
}

case class StageResults(receiveResult:Try[Any],cascadeResults:Seq[StageResults] = Seq.empty)