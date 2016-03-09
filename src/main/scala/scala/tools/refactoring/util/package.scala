package scala.tools.refactoring

package object util {
  implicit class IndexedSeqOps[T](val underlying: IndexedSeq[T]) extends AnyVal {
    def indexedView(from: Int, until: Int = underlying.size): IndexedSeq[T] = {
      new IndexedSeqView(underlying, from, until)
    }
  }
}
