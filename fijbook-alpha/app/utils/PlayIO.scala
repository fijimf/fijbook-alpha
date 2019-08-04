package utils

import cats.effect.IO
import play.api.mvc.{Action, ActionBuilder, BaseControllerHelpers, Result}

import scala.concurrent.ExecutionContext


// Taken from https://stackoverflow.com/questions/52896223/how-to-implement-multiple-thread-pools-in-a-play-application-using-cats-effect-i
trait PlayIO {
  self: BaseControllerHelpers =>

  implicit class IOActionBuilder[R[_], A](actionBuilder: ActionBuilder[R, A]) {

    def io(block: R[A] => IO[Result]): Action[A] = {
      actionBuilder.apply(block.andThen(_.unsafeRunSync()))
    }

    def io(executionContext: ExecutionContext)(block: R[A] => IO[Result]): Action[A] = {
      if (executionContext == defaultExecutionContext) {
        io(block)
      } else {
        val shiftedBlock = block.andThen { ioResult =>
          IO.shift(executionContext).bracket(_ => ioResult)(_ => IO.shift(defaultExecutionContext))
        }
        io(shiftedBlock)
      }
    }
  }
}