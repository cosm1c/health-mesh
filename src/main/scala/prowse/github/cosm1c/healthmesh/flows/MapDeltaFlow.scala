package prowse.github.cosm1c.healthmesh.flows

import cats.kernel.Monoid

object MapDeltaFlow {

    final case class MapDelta[K, +V](updated: Map[K, V] = Map.empty[K, V],
                                     removed: Set[K] = Set.empty[K])

    implicit def mapDeltaMonoid[K, V]: Monoid[MapDelta[K, V]] = new Monoid[MapDelta[K, V]] {
        override def empty: MapDelta[K, V] = MapDelta[K, V]()

        override def combine(x: MapDelta[K, V], y: MapDelta[K, V]) =
            MapDelta(
                updated = x.updated -- y.removed ++ y.updated,
                removed = x.removed -- y.updated.keySet ++ y.removed
            )
    }

    class MapDeltaOps[K, V] extends DeltaOps[Map[K, V], MapDelta[K, V]] {

        override def applyDelta(state: Map[K, V], delta: MapDelta[K, V]): Map[K, V] =
            state -- delta.removed ++ delta.updated

        override def toDelta(state: Map[K, V]): MapDelta[K, V] = MapDelta(state)
    }

    implicit def mapDeltaOps[K, V]: DeltaOps[Map[K, V], MapDelta[K, V]] = new MapDeltaOps

}
