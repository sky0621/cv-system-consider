package com.example.cv.implementation.api

import cats.Monad
import cats.data.EitherT
import com.example.cv.design.Command.{NotifyCVRegistrationResult, SaveApplyForCVRegistrationCommand, VerifyCVRegistrationCommand}
import com.example.cv.implementation.Workflow.{ApplyForCVRegistrationInput, ApplyForCVRegistrationOutput, applyForCVRegistration}
import com.example.cv.implementation.api.ApiError.toApiError

class ApplyForCVRegistrationApi[F[_]](
    saveApplyForCVRegistrationCommand: SaveApplyForCVRegistrationCommand[F],
    verifyCVRegistrationCommand: VerifyCVRegistrationCommand[F],
    notifyCVRegistrationResult: NotifyCVRegistrationResult[F]
) extends Api[F] {
  override def execute(
      request: Request
  )(implicit monad: Monad[F]): EitherT[F, ApiError, Response] = {
    for {
      input <- ApplyForCVRegistrationApi.parse[F](
        request.values
      )(monad)
      output <-
        applyForCVRegistration(monad)(saveApplyForCVRegistrationCommand)(
          verifyCVRegistrationCommand
        )(notifyCVRegistrationResult)(input).leftMap(toApiError)
    } yield ApplyForCVRegistrationResponse(output)
  }
}

object ApplyForCVRegistrationApi {
  private def parse[F[_]: Monad](
      parameters: Array[String]
  ): EitherT[F, ApiError, ApplyForCVRegistrationInput] = {
    EitherT.cond[F](
      parameters.length == 6,
      ApplyForCVRegistrationInput(
        parameters.head,
        parameters(1),
        parameters(2).toInt,
        parameters(3).toInt,
        parameters(4).toInt,
        parameters(5)
      ),
      BadRequest(
        s"invalid parameter: ${parameters.mkString("Array(", ", ", ")")}"
      ): ApiError
    )
  }
}

case class ApplyForCVRegistrationResponse(output: ApplyForCVRegistrationOutput)
    extends Response
