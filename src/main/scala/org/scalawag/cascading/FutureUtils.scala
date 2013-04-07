package org.scalawag.cascading

import scala.language.higherKinds

import scala.util.Try
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder

object FutureUtils {

  def all[A](in:Future[A]*)(implicit executor:ExecutionContext):Future[Seq[Try[A]]] = {
    all(in)
  }

  def all[A,M[_] <: TraversableOnce[_]](in:M[Future[A]])(implicit cbf:CanBuildFrom[M[Future[A]],Try[A],M[Try[A]]],executor:ExecutionContext):Future[M[Try[A]]] = {
    in.foldLeft(Promise.successful(cbf(in)).future) { (fr,fa) =>
      val p = Promise[Builder[Try[A],M[Try[A]]]]

      fr onSuccess { case r =>
        fa.asInstanceOf[Future[A]] onComplete { a =>
          r += a
          p success r
        }
      }

      p.future
    } map (_.result)
  }

}
