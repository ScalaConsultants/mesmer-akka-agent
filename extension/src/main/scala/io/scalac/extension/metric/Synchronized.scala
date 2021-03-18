package io.scalac.extension.metric

trait Synchronized {
  type Instrument[_]
  def atomically[A, B](first: Instrument[A], second: Instrument[B]): (A, B) => Unit
}
