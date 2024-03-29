/*
 * Copyright (c) 2015 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package cats
package laws

import cats.data.Nested

trait UnorderedTraverseLaws[F[_]] extends UnorderedFoldableLaws[F] {
  implicit def F: UnorderedTraverse[F]

  def unorderedTraverseIdentity[A, B](fa: F[A])(f: A => B)(implicit ev: Functor[F]): IsEq[F[B]] =
    F.unorderedTraverse[Id, A, B](fa)(f) <-> (ev.map(fa)(f))

  def unorderedTraverseSequentialComposition[A, B, C, M[_], N[_]](fa: F[A], f: A => M[B], g: B => N[C])(implicit
    N: CommutativeApplicative[N],
    M: CommutativeApplicative[M]
  ): IsEq[Nested[M, N, F[C]]] = {

    val lhs = Nested(M.map(F.unorderedTraverse(fa)(f))(fb => F.unorderedTraverse(fb)(g)))
    val rhs = F.unorderedTraverse[Nested[M, N, *], A, C](fa)(a => Nested(M.map(f(a))(g)))
    lhs <-> rhs
  }

  def unorderedTraverseParallelComposition[A, B, M[_], N[_]](fa: F[A], f: A => M[B], g: A => N[B])(implicit
    N: CommutativeApplicative[N],
    M: CommutativeApplicative[M]
  ): IsEq[(M[F[B]], N[F[B]])] = {

    type MN[Z] = (M[Z], N[Z])
    implicit val MN: CommutativeApplicative[MN] = new CommutativeApplicative[MN] {
      def pure[X](x: X): MN[X] = (M.pure(x), N.pure(x))
      def ap[X, Y](f: MN[X => Y])(fa: MN[X]): MN[Y] = {
        val (fam, fan) = fa
        val (fm, fn) = f
        (M.ap(fm)(fam), N.ap(fn)(fan))
      }
      override def map[X, Y](fx: MN[X])(f: X => Y): MN[Y] = {
        val (mx, nx) = fx
        (M.map(mx)(f), N.map(nx)(f))
      }
      override def product[X, Y](fx: MN[X], fy: MN[Y]): MN[(X, Y)] = {
        val (mx, nx) = fx
        val (my, ny) = fy
        (M.product(mx, my), N.product(nx, ny))
      }
    }
    val lhs: MN[F[B]] = F.unorderedTraverse[MN, A, B](fa)(a => (f(a), g(a)))
    val rhs: MN[F[B]] = (F.unorderedTraverse(fa)(f), F.unorderedTraverse(fa)(g))
    lhs <-> rhs
  }

  def unorderedSequenceConsistent[A, G[_]: CommutativeApplicative](fga: F[G[A]]): IsEq[G[F[A]]] =
    F.unorderedTraverse(fga)(identity) <-> F.unorderedSequence(fga)

}

object UnorderedTraverseLaws {
  def apply[F[_]](implicit ev: UnorderedTraverse[F]): UnorderedTraverseLaws[F] =
    new UnorderedTraverseLaws[F] { def F: UnorderedTraverse[F] = ev }
}
