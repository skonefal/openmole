/**
 * Created by Romain Reuillon on 22/09/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.tool

import org.apache.commons.math3.random.RandomGenerator
import org.openmole.tool.cache._

package object random {

  type RandomProvider = Lazy[util.Random]

  object RandomProvider {
    def empty: RandomProvider = emptyLazyRandom
    def apply(random: util.Random) = Lazy(random)
  }

  def emptyLazyRandom = Lazy[util.Random](throw new RuntimeException("No random number generator is available"))

  @transient lazy val longInterval = {
    val min = BigDecimal(Long.MinValue)
    val max = BigDecimal(Long.MaxValue) + 1
    max - min
  }

  def shuffle[T](a: Array[T])(implicit rng: util.Random): Array[T] = {
    for (i ← 1 until a.size reverse) {
      val j = rng.nextInt(i + 1)
      val t = a(i)
      a(i) = a(j)
      a(j) = t
    }
    a
  }

  def shuffled[T](a: Iterable[T])(implicit rng: util.Random) = {
    val indexed = a.toIndexedSeq
    shuffle((0 until a.size).toArray).map(i ⇒ indexed(i))
  }

  implicit def randomDecorator(rng: util.Random) = new {
    def shuffle[T](a: Array[T]): Array[T] = random.shuffle(a)(rng)

    def nextLong(max: Long): Long = {
      val v = BigDecimal(rng.nextLong)
      ((v - Long.MinValue) * (BigDecimal(max) / longInterval)).toLong
    }
  }

  implicit def iterableShuffleDecorator[T](a: Iterable[T]) = new {
    def shuffled(implicit rng: util.Random): Seq[T] = random.shuffled(a)(rng)
  }

  class SynchronizedRandom(generator: RandomGenerator) extends java.util.Random {
    override def nextBoolean = synchronized { generator.nextBoolean }
    override def nextBytes(bytes: Array[Byte]) = synchronized { generator.nextBytes(bytes) }
    override def nextDouble = synchronized { generator.nextDouble }
    override def nextFloat = synchronized { generator.nextFloat }
    override def nextGaussian = synchronized { generator.nextGaussian }
    override def nextInt = synchronized { generator.nextInt }
    override def nextInt(n: Int) = synchronized { generator.nextInt(n) }
    override def nextLong = synchronized { generator.nextLong }
    override def setSeed(seed: Long) = synchronized { generator.setSeed(seed) }
    def toScala = new util.Random(this)
  }

}
