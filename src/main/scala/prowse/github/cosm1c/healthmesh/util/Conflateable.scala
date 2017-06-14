package prowse.github.cosm1c.healthmesh.util

trait Conflateable[T] {

    def aggregate(a: T, b: T): T
}
