package prowse.github.cosm1c.healthmesh.util

import scala.collection.immutable
import scala.reflect.ClassTag

class RingBuffer[T](size: Int)(implicit t: ClassTag[T]) {

    private val elems = new Array[T](size)
    private var index = 0
    private var isFull = false

    def put(elem: T): Unit = {
        elems(index) = elem
        index = (index + 1) % size
        if (index == 0) isFull = true
    }

    def get(): immutable.Seq[T] =
        if (isFull)
            immutable.Seq(elems: _*)
        else
            immutable.Seq(elems.take(index): _*)
}
