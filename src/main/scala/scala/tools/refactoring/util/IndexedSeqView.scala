package scala.tools.refactoring.util

import scala.collection.AbstractSeq
import scala.collection.IndexedSeqOptimized

private[util] class IndexedSeqView[T](underlying: IndexedSeq[T], from: Int, until: Int) extends AbstractSeq[T] with IndexedSeq[T] with IndexedSeqOptimized[T, IndexedSeq[T]] {
  def apply(idx: Int) = underlying.apply(from + idx)
  def length = (until - from)
}
