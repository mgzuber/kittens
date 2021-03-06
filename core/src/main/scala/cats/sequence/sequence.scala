/*
 * Originally adapted from shapeless-contrib scalaz
 * https://github.com/typelevel/shapeless-contrib/blob/v0.4/scalaz/main/scala/sequence.scala
 *
 */
package cats.sequence

import shapeless._

import cats._
import shapeless.ops.hlist.ZipWithKeys
import shapeless.ops.record.{Values, Keys}

import scala.annotation.implicitNotFound

trait HListApply2[FH, OutT] extends Serializable {
  type Out

  def apply(fh: FH, outT: OutT): Out
}

object HListApply2 extends MkHListApply2 {
  type Aux[FH, OutT, Out0] = HListApply2[FH, OutT] { type Out = Out0 }
}

trait MkHListApply2 extends MkHListApply2_0 {

  implicit def mkHListApply2[F[_], H, T <: HList]
  (implicit app: Apply[F]): HListApply2.Aux[F[H], F[T], F[H :: T]] =
    new HListApply2[F[H], F[T]] {
      type Out = F[H :: T]

      def apply(fh: F[H], outT: F[T]): Out = app.map2(fh, outT)(_ :: _)
    }

}

trait MkHListApply2_0 extends MkHListApply2_1 {
  self: MkHListApply2 =>
  implicit def mkHListApply2a[F[_, _], A, H, T <: HList]
  (implicit app: Apply[F[A, ?]]): HListApply2.Aux[F[A, H], F[A, T], F[A, H :: T]] = mkHListApply2[F[A, ?], H, T]

}

trait MkHListApply2_1 {
  self: MkHListApply2 =>
  implicit def mkHListApply3right[G[_], F[_[_], _, _], A, H, T <: HList]
  (implicit app: Apply[F[G, A, ?]]): HListApply2.Aux[F[G, A, H], F[G, A, T], F[G, A, H :: T]] = mkHListApply2[F[G, A, ?], H, T]

}

@implicitNotFound("cannot construct sequencer, make sure that every item of you hlist ${L} is an Applicative")
trait Sequencer[L <: HList]  extends Serializable {
  type Out

  def apply(in: L): Out
}

trait LowPrioritySequencer {
  type Aux[L <: HList, Out0] = Sequencer[L] { type Out = Out0 }

  implicit def consSequencerAux[FH, FT <: HList, OutT]
    (implicit st: Aux[FT, OutT], ap: HListApply2[FH, OutT]): Aux[FH :: FT, ap.Out] =
      new Sequencer[FH :: FT] {
        type Out = ap.Out

        def apply(in: FH :: FT): Out = ap(in.head, st(in.tail))
      }
}

object Sequencer extends LowPrioritySequencer {

  implicit def singleSequencerAux[FH]
    (implicit un: Unapply[Functor, FH]): Aux[FH :: HNil, un.M[un.A :: HNil]] =
      new Sequencer[FH :: HNil] {
        type Out = un.M[un.A :: HNil]

        def apply(in: FH :: HNil): Out = un.TC.map(un.subst(in.head)) {
          _ :: HNil
        }
      }
}


trait ValueSequencer[L <: HList] extends Serializable {
  type Out

  def apply(in: L): Out
}


object ValueSequencer {
  type Aux[L <: HList, Out0] = ValueSequencer[L] { type Out = Out0 }

  implicit def recordValueAux[L <: HList, V <: HList]
    (implicit values: Values.Aux[L, V], sequencer: Sequencer[V]): Aux[L, sequencer.Out] =
      new ValueSequencer[L] {
        type Out = sequencer.Out

        def apply(in: L): Out = sequencer(values(in))
      }

}


@implicitNotFound("cannot construct sequencer, make sure that every field value of you record ${L} is an Applicative")
trait RecordSequencer[L <: HList] extends Serializable {
  type Out

  def apply(in: L): Out
}


object RecordSequencer extends MkRecordSequencer {
  type Aux[L <: HList, Out0] = RecordSequencer[L] { type Out = Out0 }
}

trait MkRecordSequencer extends MkRecordSequencer0 {
  implicit def mkRecordSequencer[L <: HList, VFOut, F[_], K <: HList, VOut <: HList]
  ( implicit
    vs: ValueSequencer.Aux[L, VFOut],
    un: Unapply.Aux1[Functor, VFOut, F, VOut],
    keys: Keys.Aux[L, K],
    zip: ZipWithKeys[K, VOut]
  ): RecordSequencer.Aux[L, F[zip.Out]] =
    new RecordSequencer[L] {
      type Out = F[zip.Out]

      def apply(in: L): Out = {
        un.TC.map(un.subst(vs(in)))(zip(_))
      }
    }
}

trait MkRecordSequencer0 extends MkRecordSequencer1 {
  self: MkRecordSequencer =>
  implicit def mkRecordSequencer2Right[L <: HList, VFOut, A, F[_, _], K <: HList, VOut <: HList]
  ( implicit
    vs: ValueSequencer.Aux[L, VFOut],
    un: Unapply.Aux2Right[Functor, VFOut, F, A, VOut],
    keys: Keys.Aux[L, K],
    zip: ZipWithKeys[K, VOut]
  ): RecordSequencer.Aux[L, F[A, zip.Out]] =
    mkRecordSequencer[L, VFOut, F[A, ?], K, VOut]
}

trait MkRecordSequencer1 {
  self: MkRecordSequencer =>
  implicit def mkRecordSequencer3Right[L <: HList, VFOut, A, G[_], F[_[_], _, _], K <: HList, VOut <: HList]
  ( implicit
    vs: ValueSequencer.Aux[L, VFOut],
    un: Unapply.Aux3MTRight[Functor, VFOut, F, G, A, VOut],
    keys: Keys.Aux[L, K],
    zip: ZipWithKeys[K, VOut]
  ): RecordSequencer.Aux[L, F[G, A, zip.Out]] =
    mkRecordSequencer[L, VFOut, F[G, A, ?], K, VOut]
}

  trait GenericSequencer[L <: HList, T] extends Serializable {
  type Out

  def apply(l: L): Out
}

object GenericSequencer extends MkGenericSequencer {
  type Aux[L <: HList, T, Out0] = GenericSequencer[L, T] { type Out = Out0 }
}

trait MkGenericSequencer extends MkGenericSequencer0 {

  implicit def mkGenericSequencer[L <: HList, T, SOut <: HList, FOut, F[_]]
  ( implicit
    rs: RecordSequencer.Aux[L, FOut],
    gen: LabelledGeneric.Aux[T, SOut],
    un: Unapply.Aux1[Functor, FOut, F, SOut]
  ): GenericSequencer.Aux[L, T, F[T]] =
    new GenericSequencer[L, T] {
      type Out = F[T]

      def apply(in: L): Out = {
        un.TC.map(un.subst(rs(in)))(gen.from)
      }
    }

}

trait MkGenericSequencer0 extends MkGenericSequencer1 {
  self: MkGenericSequencer =>
  implicit def mkGenericSequencer2Right[L <: HList, T, SOut <: HList, FOut, A, F[_, _]]
  ( implicit
    rs: RecordSequencer.Aux[L, FOut],
    gen: LabelledGeneric.Aux[T, SOut],
    un: Unapply.Aux2Right[Functor, FOut, F, A, SOut]
  ): GenericSequencer.Aux[L, T, F[A, T]] =
    mkGenericSequencer[L, T, SOut, FOut, F[A, ?]]

}

trait MkGenericSequencer1 {
  self: MkGenericSequencer =>

  implicit def mkGenericSequencer3Right[L <: HList, T, SOut <: HList, FOut, A, G[_], F[_[_], _, _]]
  ( implicit
    rs: RecordSequencer.Aux[L, FOut],
    gen: LabelledGeneric.Aux[T, SOut],
    un: Unapply.Aux3MTRight[Functor, FOut, F, G, A, SOut]
  ): GenericSequencer.Aux[L, T, F[G, A, T]] =
    mkGenericSequencer[L, T, SOut, FOut, F[G, A, ?]]
}


trait LowPrioritySequenceOps {
  import SequenceOps._
  // fallback for non-records
  implicit def mkNonRecordOps[L <: HList](l: L): NonRecordOps[L] = new NonRecordOps(l)
}


trait SequenceOps extends LowPrioritySequenceOps {
  import SequenceOps._
  implicit def mkRecordOps[L <: HList](l: L)
                                      (implicit keys: Keys[L]): RecordOps[L] = new RecordOps(l)

  object sequence extends ProductArgs {
    def applyProduct[L <: HList](l: L)(implicit seq: Sequencer[L]): seq.Out = seq(l)
  }

  object sequenceRecord extends RecordArgs {
    def applyRecord[L <: HList](l: L)(implicit seq: RecordSequencer[L]): seq.Out = seq(l)
  }

  class sequenceGen[T] extends RecordArgs {
    def applyRecord[L <: HList](l: L)(implicit seq: GenericSequencer[L, T]): seq.Out = seq(l)
  }

  def sequenceGeneric[T] = new sequenceGen[T]

}

object SequenceOps {
  // Syntax for non-records
  class NonRecordOps[L <: HList](self: L) {
    def sequence(implicit seq: Sequencer[L]): seq.Out = seq(self)
  }

  // Syntax for records
  class RecordOps[L <: HList](self: L) {
    def sequence(implicit seq: RecordSequencer[L]): seq.Out = seq(self)
  }

}
