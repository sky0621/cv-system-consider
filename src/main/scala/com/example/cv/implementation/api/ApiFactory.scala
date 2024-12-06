package com.example.cv.implementation.api

import cats.Monad
import com.example.cv.implementation.Command.{notifyCVRegistrationResult, saveApplyForCVRegistrationCommand, verifyCVRegistrationCommand}

object ApiFactory {
  def create[F[_]: Monad](
      apiName: String
  ): Api[F] = apiName match {
    case "applyForCVRegistration" =>
      new ApplyForCVRegistrationApi[F](
        saveApplyForCVRegistrationCommand[F],
        verifyCVRegistrationCommand[F],
        notifyCVRegistrationResult[F]
      ).asInstanceOf[Api[F]]
    case _ => throw new IllegalArgumentException(apiName)
  }
}
