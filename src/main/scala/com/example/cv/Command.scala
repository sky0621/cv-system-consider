package com.example.cv
import cats.Applicative
import cats.data.EitherT
import com.example.cv.Model.{InvalidMailAddress, UnvalidatedMailAddress, ValidatedMailAddress}

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

object CommandFactory {
  def create(
      commandName: String,
      parameters: Array[String]
  )(implicit ec: ExecutionContext): Command[_, _] =
    commandName match {
      case "applyForCVRegistration" =>
        ApplyForCVRegistrationCommand(UnvalidatedMailAddress(parameters.head))
      case "verifyCVRegistration" =>
        VerifyCVRegistrationCommand(UnvalidatedMailAddress(parameters.head))
      case _ => throw new RuntimeException()
    }
}

sealed trait Command[F[_], E <: Event] {
  def execute(): EitherT[F, String, E]
}

case class ApplyForCVRegistrationCommand[F[_]: Applicative](
    maybeMailAddress: UnvalidatedMailAddress
)(implicit ec: ExecutionContext)
    extends Command[F, AppliedForCVRegistrationEvent] {
  override def execute(): EitherT[F, String, AppliedForCVRegistrationEvent] = {
    val mailOpt: Option[UnvalidatedMailAddress] =
      if (maybeMailAddress.value.isBlank) None else Some(maybeMailAddress)
    for {
      _ <- InMemoryDatabase.unvalidatedMailAddressStorage.save(mailOpt)
    } yield AppliedForCVRegistrationEvent(maybeMailAddress)
  }
}

case class VerifyCVRegistrationCommand[F[_]: Applicative](
    maybeMailAddress: UnvalidatedMailAddress
)(implicit ec: ExecutionContext)
    extends Command[F, VerifiedCVRegistrationEvent] {
  private val emailRegex: Regex =
    "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".r

  override def execute(): EitherT[F, String, VerifiedCVRegistrationEvent] =
    if (emailRegex.matches(maybeMailAddress.value)) {
      EitherT.rightT[F, String](
        ApprovedCVRegistrationEvent(
          ValidatedMailAddress(maybeMailAddress.value)
        )
      )
    } else {
      EitherT.rightT[F, String](
        RejectedCVRegistrationEvent(InvalidMailAddress(maybeMailAddress.value))
      )
    }
}
