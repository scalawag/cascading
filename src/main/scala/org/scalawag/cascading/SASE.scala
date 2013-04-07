package org.scalawag.cascading

import akka.actor.ActorRef

case class SASE[T](private val parent:ActorRef,
                   private val index:Int,
                   private val enclosure:T)
