package modules

import cats.effect.IO
import doobie.util.transactor.Transactor

trait TransactorCtx {
   def xa:Transactor[IO]
}
