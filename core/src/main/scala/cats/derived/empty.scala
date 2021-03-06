/*
 * Copyright (c) 2016 Miles Sabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats.derived

import alleycats.Empty
import export.{ exports, imports, reexports }
import shapeless._

@reexports[MkEmpty]
object empty {
  @imports[Empty]
  object legacy
}

trait MkEmpty[T] extends Empty[T]

@exports
object MkEmpty {
  def apply[T](implicit e: MkEmpty[T]): MkEmpty[T] = e

  implicit val hnil: MkEmpty[HNil] =
    new MkEmpty[HNil] {
      def empty = HNil
    }

  implicit def hcons[H, T <: HList](implicit eh: Lazy[Empty[H]], et: Lazy[MkEmpty[T]])
    : MkEmpty[H :: T] = new MkEmpty[H :: T] {
      val empty = eh.value.empty :: et.value.empty
    }

  implicit def generic[T, R](implicit gen: Generic.Aux[T, R], er: Lazy[MkEmpty[R]])
    : MkEmpty[T] = new MkEmpty[T] {
      val empty = gen.from(er.value.empty)
    }
}
