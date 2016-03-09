package scala.tools.refactoring.util


import org.junit.Test
import org.junit.Assert._

class IndexedSeqOpsTest {

  @Test
  def testIndexedView(): Unit = {
    def testIndexedView[T](input: IndexedSeq[T], from: Int, until: Int, expected: IndexedSeq[T]): Unit = {
      assertEquals(expected, input.indexedView(from, until))
    }

    testIndexedView(IndexedSeq(), 0, 0, IndexedSeq())
    testIndexedView(IndexedSeq(1), 0, 1, IndexedSeq(1))
    testIndexedView(IndexedSeq(1, 2, 3), 1, 2, IndexedSeq(2))
  }
}
