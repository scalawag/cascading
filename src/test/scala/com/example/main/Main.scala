package com.example.main

import akka.actor._

import concurrent.ExecutionContext
import util.{Try, Random, Success, Failure}
import org.scalawag.cascading._
import util.Success
import util.Failure

object Main {

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global
//  implicit val timeout = Timeout(5 seconds)

  val random = new Random

  trait Summer extends CascadingActor {
    override def buildResponse(results:StageResults) = {
      if ( results.receiveResult.isFailure )
        StageResults(results.receiveResult)
      else {
        results.cascadeResults.find(_.receiveResult.isFailure) match {
          case Some(fail) => StageResults(fail.receiveResult)
          case None => StageResults(Success(results.receiveResult.get.asInstanceOf[Int] + results.cascadeResults.map(_.receiveResult).foldLeft(0) { (a,b) => a + b.get.asInstanceOf[Int] }))
        }
      }
    }
  }

  class TopActor(delegate:ActorRef) extends CascadingActor with Summer with Logging {

    override def receive(manager:Manager):PartialFunction[Any,Any] = {
      case msg =>
        log.debug("TOP:" + msg)
        manager.add(Cascade(msg + "TOP1",delegate))
        manager.add(Cascade(msg + "TOP2",delegate))
        34
    }

    override def onComplete(msg:Any,results:StageResults) = results.receiveResult match {
      case Success(s) =>
        log.debug(s"ACK: $msg $results")
      case Failure(ex) =>
        log.debug(s"NACK $msg $results")
    }
  }

  class MidActor(delegate:ActorRef) extends CascadingActor with Summer with Logging {

    override def receive(manager:Manager):PartialFunction[Any,Any] = {
      case msg:String =>
        log.debug("MID:" + msg)
        manager.add(Cascade(msg + "MID1",delegate))
        manager.add(Cascade(msg + "MID2",delegate))
        42
    }

  }

  class LeafActor extends CascadingActor with Summer with Logging {

    override def receive(manager:Manager):PartialFunction[Any,Any] = {
      case msg:String =>
        log.debug("LEAF:" + msg)

        val delay = random.nextInt(1000)

        log.debug(s"sleep(${delay})")
        Thread.sleep(delay)

        if ( delay < 1000 )
          delay
        else
          throw new IllegalArgumentException(delay.toString)
    }

  }

  lazy val actorSystem = ActorSystem("default")

  def main(args:Array[String]) {
    val la = actorSystem.actorOf(Props(new LeafActor))
    val ma = actorSystem.actorOf(Props(new MidActor(la)))
    val ta = actorSystem.actorOf(Props(new TopActor(ma)))

    ( 1 to 2 ) foreach { n =>
      ta ! s"C$n"
    }
  }
}


