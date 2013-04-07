package org.scalawag.cascading

private[cascading] case class Response[T](private val index:Int,private val result:StageResults)
